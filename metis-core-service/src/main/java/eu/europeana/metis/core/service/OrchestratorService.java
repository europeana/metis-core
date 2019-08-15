package eu.europeana.metis.core.service;

import eu.europeana.metis.authentication.user.AccountRole;
import eu.europeana.metis.authentication.user.MetisUser;
import eu.europeana.metis.core.common.DaoFieldNames;
import eu.europeana.metis.core.dao.DatasetDao;
import eu.europeana.metis.core.dao.WorkflowDao;
import eu.europeana.metis.core.dao.WorkflowExecutionDao;
import eu.europeana.metis.core.dataset.Dataset;
import eu.europeana.metis.core.dataset.DatasetExecutionInformation;
import eu.europeana.metis.core.exceptions.NoDatasetFoundException;
import eu.europeana.metis.core.exceptions.NoWorkflowExecutionFoundException;
import eu.europeana.metis.core.exceptions.NoWorkflowFoundException;
import eu.europeana.metis.core.exceptions.PluginExecutionNotAllowed;
import eu.europeana.metis.core.exceptions.WorkflowAlreadyExistsException;
import eu.europeana.metis.core.exceptions.WorkflowExecutionAlreadyExistsException;
import eu.europeana.metis.core.execution.WorkflowUtils;
import eu.europeana.metis.core.execution.WorkflowExecutorManager;
import eu.europeana.metis.core.rest.VersionEvolution;
import eu.europeana.metis.core.rest.VersionEvolution.VersionEvolutionStep;
import eu.europeana.metis.core.rest.execution.overview.ExecutionAndDatasetView;
import eu.europeana.metis.core.workflow.Workflow;
import eu.europeana.metis.core.workflow.WorkflowExecution;
import eu.europeana.metis.core.workflow.WorkflowStatus;
import eu.europeana.metis.core.workflow.plugins.AbstractExecutablePlugin;
import eu.europeana.metis.core.workflow.plugins.AbstractExecutablePluginMetadata;
import eu.europeana.metis.core.workflow.plugins.AbstractMetisPlugin;
import eu.europeana.metis.core.workflow.plugins.DataStatus;
import eu.europeana.metis.core.workflow.plugins.ExecutablePlugin;
import eu.europeana.metis.core.workflow.plugins.ExecutablePluginType;
import eu.europeana.metis.core.workflow.plugins.PluginStatus;
import eu.europeana.metis.core.workflow.plugins.PluginType;
import eu.europeana.metis.exception.BadContentException;
import eu.europeana.metis.exception.ExternalTaskException;
import eu.europeana.metis.exception.GenericMetisException;
import eu.europeana.metis.exception.UserUnauthorizedException;
import eu.europeana.metis.utils.DateUtils;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service class that controls the communication between the different DAOs of the system.
 *
 * @author Simon Tzanakis (Simon.Tzanakis@europeana.eu)
 * @since 2017-05-24
 */
@Service
public class OrchestratorService {

  private static final Logger LOGGER = LoggerFactory.getLogger(OrchestratorService.class);
  //Use with String.format to suffix the datasetId
  private static final String EXECUTION_FOR_DATASETID_SUBMITION_LOCK = "EXECUTION_FOR_DATASETID_SUBMITION_LOCK_%s";

  private final WorkflowExecutionDao workflowExecutionDao;
  private final WorkflowDao workflowDao;
  private final DatasetDao datasetDao;
  private final WorkflowExecutorManager workflowExecutorManager;
  private final RedissonClient redissonClient;
  private final Authorizer authorizer;
  private final WorkflowExecutionFactory workflowExecutionFactory;
  private int solrCommitPeriodInMins; // Use getter and setter for this field!

  /**
   * Constructor with all the required parameters
   *
   * @param workflowDao the Dao instance to access the Workflow database
   * @param workflowExecutionDao the Dao instance to access the WorkflowExecution database
   * @param datasetDao the Dao instance to access the Dataset database
   * @param workflowExecutionFactory the orchestratorHelper instance
   * @param workflowExecutorManager the instance that handles the production and consumption of
   * workflowExecutions
   * @param redissonClient the instance of Redisson library that handles distributed locks
   * @param authorizer the authorizer
   */
  @Autowired
  public OrchestratorService(WorkflowExecutionFactory workflowExecutionFactory,
      WorkflowDao workflowDao, WorkflowExecutionDao workflowExecutionDao, DatasetDao datasetDao,
      WorkflowExecutorManager workflowExecutorManager, RedissonClient redissonClient,
      Authorizer authorizer) {
    this.workflowExecutionFactory = workflowExecutionFactory;
    this.workflowDao = workflowDao;
    this.workflowExecutionDao = workflowExecutionDao;
    this.datasetDao = datasetDao;
    this.workflowExecutorManager = workflowExecutorManager;
    this.redissonClient = redissonClient;
    this.authorizer = authorizer;
  }

  void validateWorkflow(Workflow workflow, ExecutablePluginType enforcedPredecessorType)
      throws GenericMetisException {
    new WorkflowUtils(workflowExecutionDao)
        .validateWorkflowPlugins(workflow, enforcedPredecessorType);
  }

  /**
   * Create a workflow using a datasetId and the {@link Workflow} that contains the requested
   * plugins. When creating a new workflow all the plugins specified will be automatically enabled.
   *
   * @param metisUser the user wishing to perform this operation
   * @param datasetId the identifier of the dataset for which the workflow should be created
   * @param workflow the workflow with the plugins requested
   * @param enforcedPredecessorType optional, the plugin type to be used as source data
   * @throws GenericMetisException which can be one of:
   * <ul>
   * <li>{@link WorkflowAlreadyExistsException} if a workflow for the dataset identifier provided
   * already exists</li>
   * <li>{@link NoDatasetFoundException} if the dataset identifier provided does not exist</li>
   * <li>{@link UserUnauthorizedException} if the user is not authorized to perform this task</li>
   * <li>{@link BadContentException} if the workflow parameters have unexpected values</li>
   * </ul>
   */
  public void createWorkflow(MetisUser metisUser, String datasetId, Workflow workflow,
      ExecutablePluginType enforcedPredecessorType) throws GenericMetisException {

    // Authorize (check dataset existence) and set dataset ID to avoid discrepancy.
    authorizer.authorizeWriteExistingDatasetById(metisUser, datasetId);
    workflow.setDatasetId(datasetId);

    // Check that the workflow does not yet exist.
    if (workflowDao.workflowExistsForDataset(workflow.getDatasetId())) {
      throw new WorkflowAlreadyExistsException(
          String.format("Workflow with datasetId: %s, already exists", workflow.getDatasetId()));
    }

    // Validate the new workflow.
    validateWorkflow(workflow, enforcedPredecessorType);

    // Save the workflow.
    workflow.getMetisPluginsMetadata()
        .forEach(abstractMetisPluginMetadata -> abstractMetisPluginMetadata.setEnabled(true));
    workflowDao.create(workflow);
  }

  /**
   * Update an already existent workflow using a datasetId and the {@link Workflow} that contains
   * the requested plugins. When updating an existent workflow all specified plugins will be enabled
   * and all plugins that were existent in the system beforehand will be kept with their
   * configuration but will be disabled.
   *
   * @param metisUser the user wishing to perform this operation
   * @param datasetId the identifier of the dataset for which the workflow should be updated
   * @param workflow the workflow with the plugins requested
   * @param enforcedPredecessorType optional, the plugin type to be used as source data
   * @throws GenericMetisException which can be one of:
   * <ul>
   * <li>{@link NoWorkflowFoundException} if a workflow for the dataset identifier provided does
   * not exist</li>
   * <li>{@link NoDatasetFoundException} if the dataset identifier provided does not exist</li>
   * <li>{@link UserUnauthorizedException} if the user is not authorized to perform this task</li>
   * <li>{@link BadContentException} if the workflow parameters have unexpected values</li>
   * </ul>
   */
  public void updateWorkflow(MetisUser metisUser, String datasetId, Workflow workflow,
      ExecutablePluginType enforcedPredecessorType) throws GenericMetisException {

    // Authorize (check dataset existence) and set dataset ID to avoid discrepancy.
    authorizer.authorizeWriteExistingDatasetById(metisUser, datasetId);
    workflow.setDatasetId(datasetId);

    // Get the current workflow in the database. If it doesn't exist, throw exception.
    final Workflow storedWorkflow = workflowDao.getWorkflow(workflow.getDatasetId());
    if (storedWorkflow == null) {
      throw new NoWorkflowFoundException(String.format(
          "Workflow with datasetId: %s, not found", workflow.getDatasetId()));
    }

    // Validate the new workflow.
    validateWorkflow(workflow, enforcedPredecessorType);

    // Overwrite the workflow.
    workflow.setId(storedWorkflow.getId());
    overwriteNewPluginMetadataOnWorkflowAndDisableOtherPluginMetadata(workflow, storedWorkflow);
    workflowDao.update(workflow);
  }

  private void overwriteNewPluginMetadataOnWorkflowAndDisableOtherPluginMetadata(Workflow workflow,
      Workflow storedWorkflow) {
    //Overwrite only ones provided and disable the rest, already stored, plugins
    workflow.getMetisPluginsMetadata().forEach(pluginMetadata -> pluginMetadata.setEnabled(true));
    List<AbstractExecutablePluginMetadata> storedPluginsExcludingNewPlugins = storedWorkflow
        .getMetisPluginsMetadata()
        .stream().filter(
            pluginMetadata -> hasNoPluginOfType(workflow, pluginMetadata.getExecutablePluginType()))
        .peek(pluginMetadata -> pluginMetadata.setEnabled(false))
        .collect(Collectors.toList());
    workflow.setMetisPluginsMetadata(Stream.concat(storedPluginsExcludingNewPlugins.stream(),
        workflow.getMetisPluginsMetadata().stream()).collect(Collectors.toList()));
  }

  private static boolean hasNoPluginOfType(Workflow workflow, ExecutablePluginType pluginType) {
    return workflow.getMetisPluginsMetadata().stream()
        .map(AbstractExecutablePluginMetadata::getExecutablePluginType)
        .noneMatch(pluginType::equals);
  }

  /**
   * Deletes a workflow.
   *
   * @param metisUser the user wishing to perform this operation
   * @param datasetId the dataset identifier that corresponds to the workflow to be deleted
   * @throws GenericMetisException which can be one of:
   * <ul>
   * <li>{@link NoDatasetFoundException} if the dataset identifier provided does not exist</li>
   * <li>{@link UserUnauthorizedException} if the user is not authorized to perform this task</li>
   * </ul>
   */
  public void deleteWorkflow(MetisUser metisUser, String datasetId) throws GenericMetisException {
    authorizer.authorizeWriteExistingDatasetById(metisUser, datasetId);
    workflowDao.deleteWorkflow(datasetId);
  }

  /**
   * Get a workflow for a dataset identifier.
   *
   * @param metisUser the user wishing to perform this operation
   * @param datasetId the dataset identifier
   * @return the Workflow object
   * @throws GenericMetisException which can be one of:
   * <ul>
   * <li>{@link NoDatasetFoundException} if the dataset identifier provided does not exist</li>
   * <li>{@link UserUnauthorizedException} if the user is not authorized to perform this task</li>
   * </ul>
   */
  public Workflow getWorkflow(MetisUser metisUser, String datasetId) throws GenericMetisException {
    authorizer.authorizeReadExistingDatasetById(metisUser, datasetId);
    return workflowDao.getWorkflow(datasetId);
  }

  /**
   * Get a WorkflowExecution using an execution identifier.
   *
   * @param metisUser the user wishing to perform this operation
   * @param executionId the execution identifier
   * @return the WorkflowExecution object
   * @throws GenericMetisException which can be one of:
   * <ul>
   * <li>{@link NoDatasetFoundException} if the dataset identifier provided does not exist</li>
   * <li>{@link UserUnauthorizedException} if the user is not authorized to perform this task</li>
   * </ul>
   */
  public WorkflowExecution getWorkflowExecutionByExecutionId(MetisUser metisUser,
      String executionId) throws GenericMetisException {
    final WorkflowExecution result = workflowExecutionDao.getById(executionId);
    if (result != null) {
      authorizer.authorizeReadExistingDatasetById(metisUser, result.getDatasetId());
    }
    return result;
  }

  /**
   * <p> Does checking, prepares and adds a WorkflowExecution in the queue. That means it updates
   * the status of the WorkflowExecution to {@link WorkflowStatus#INQUEUE}, adds it to the database
   * and also it's identifier goes into the distributed queue of WorkflowExecutions. The source data
   * for the first plugin in the workflow can be controlled, if required, from the {@code
   * enforcedPredecessorType}, which means that the last valid plugin that is provided with that
   * parameter, will be used as the source data. </p>
   * <p> <b>Please note:</b> this method is not checked for authorization: it is only meant to be
   * called from a scheduled task. </p>
   *
   * @param datasetId the dataset identifier for which the execution will take place
   * @param enforcedPredecessorType optional, the plugin type to be used as source data
   * @param priority the priority of the execution in case the system gets overloaded, 0 lowest, 10
   * highest
   * @return the WorkflowExecution object that was generated
   * @throws GenericMetisException which can be one of:
   * <ul>
   * <li>{@link NoWorkflowFoundException} if a workflow for the dataset identifier provided does
   * not exist</li>
   * <li>{@link BadContentException} if the workflow is empty or no plugin enabled</li>
   * <li>{@link NoDatasetFoundException} if the dataset identifier provided does not exist</li>
   * <li>{@link ExternalTaskException} if there was an exception when contacting the external
   * resource(ECloud)</li>
   * <li>{@link PluginExecutionNotAllowed} if the execution of the first plugin was not allowed,
   * because a valid source plugin could not be found</li>
   * <li>{@link WorkflowExecutionAlreadyExistsException} if a workflow execution for the generated
   * execution identifier already exists, almost impossible to happen since ids are UUIDs</li>
   * </ul>
   */
  public WorkflowExecution addWorkflowInQueueOfWorkflowExecutionsWithoutAuthorization(
      String datasetId, ExecutablePluginType enforcedPredecessorType, int priority)
      throws GenericMetisException {
    final Dataset dataset = datasetDao.getDatasetByDatasetId(datasetId);
    if (dataset == null) {
      throw new NoDatasetFoundException(
          String.format("No dataset found with datasetId: %s, in METIS", datasetId));
    }
    return addWorkflowInQueueOfWorkflowExecutions(dataset, enforcedPredecessorType, priority);
  }

  /**
   * Does checking, prepares and adds a WorkflowExecution in the queue. That means it updates the
   * status of the WorkflowExecution to {@link WorkflowStatus#INQUEUE}, adds it to the database and
   * also it's identifier goes into the distributed queue of WorkflowExecutions. The source data for
   * the first plugin in the workflow can be controlled, if required, from the {@code
   * enforcedPredecessorType}, which means that the last valid plugin that is provided with that
   * parameter, will be used as the source data.
   *
   * @param metisUser the user wishing to perform this operation
   * @param datasetId the dataset identifier for which the execution will take place
   * @param enforcedPredecessorType optional, the plugin type to be used as source data
   * @param priority the priority of the execution in case the system gets overloaded, 0 lowest, 10
   * highest
   * @return the WorkflowExecution object that was generated
   * @throws GenericMetisException which can be one of:
   * <ul>
   * <li>{@link NoWorkflowFoundException} if a workflow for the dataset identifier provided does
   * not exist</li>
   * <li>{@link BadContentException} if the workflow is empty or no plugin enabled</li>
   * <li>{@link NoDatasetFoundException} if the dataset identifier provided does not exist</li>
   * <li>{@link UserUnauthorizedException} if the user is not authorized to perform this task</li>
   * <li>{@link ExternalTaskException} if there was an exception when contacting the external
   * resource(ECloud)</li>
   * <li>{@link PluginExecutionNotAllowed} if the execution of the first plugin was not allowed,
   * because a valid source plugin could not be found</li>
   * <li>{@link WorkflowExecutionAlreadyExistsException} if a workflow execution for the generated
   * execution identifier already exists, almost impossible to happen since ids are UUIDs</li>
   * </ul>
   */
  public WorkflowExecution addWorkflowInQueueOfWorkflowExecutions(MetisUser metisUser,
      String datasetId, ExecutablePluginType enforcedPredecessorType, int priority)
      throws GenericMetisException {
    final Dataset dataset = authorizer.authorizeWriteExistingDatasetById(metisUser, datasetId);
    return addWorkflowInQueueOfWorkflowExecutions(dataset, enforcedPredecessorType, priority);
  }

  private WorkflowExecution addWorkflowInQueueOfWorkflowExecutions(Dataset dataset,
      ExecutablePluginType enforcedPredecessorType, int priority) throws GenericMetisException {

    // Get the workflow.
    final Workflow workflow = workflowDao.getWorkflow(dataset.getDatasetId());
    if (workflow == null) {
      throw new NoWorkflowFoundException(
          String.format("No workflow found with datasetId: %s, in METIS", dataset.getDatasetId()));
    }

    // Make sure that eCloud knows this dataset (needs to happen before we create the workflow).
    datasetDao.checkAndCreateDatasetInEcloud(dataset);

    // Create the workflow execution (without adding it to the database).
    final WorkflowExecution workflowExecution = workflowExecutionFactory
        .createWorkflowExecution(workflow, dataset, enforcedPredecessorType, priority);

    // Add the workflow execution to the queue.
    workflowExecution.setWorkflowStatus(WorkflowStatus.INQUEUE);
    RLock executionDatasetIdLock = redissonClient
        .getFairLock(String.format(EXECUTION_FOR_DATASETID_SUBMITION_LOCK, dataset.getDatasetId()));
    executionDatasetIdLock.lock();
    String storedWorkflowExecutionId = workflowExecutionDao
        .existsAndNotCompleted(dataset.getDatasetId());
    if (storedWorkflowExecutionId != null) {
      executionDatasetIdLock.unlock();
      throw new WorkflowExecutionAlreadyExistsException(
          String.format("Workflow execution already exists with id %s and is not completed",
              storedWorkflowExecutionId));
    }
    workflowExecution.setCreatedDate(new Date());
    String objectId = workflowExecutionDao.create(workflowExecution);
    executionDatasetIdLock.unlock();
    workflowExecutorManager.addWorkflowExecutionToQueue(objectId, priority);
    LOGGER.info("WorkflowExecution with id: {}, added to execution queue", objectId);
    return workflowExecutionDao.getById(objectId);
  }

  /**
   * Request to cancel a workflow execution. The execution will go into a cancelling state until
   * it's properly {@link WorkflowStatus#CANCELLED} from the system
   *
   * @param metisUser the user wishing to perform this operation
   * @param executionId the execution identifier of the execution to cancel
   * @throws GenericMetisException which can be one of:
   * <ul>
   * <li>{@link NoWorkflowExecutionFoundException} if no worklfowExecution could be found</li>
   * <li>{@link NoDatasetFoundException} if the dataset identifier provided does not exist</li>
   * <li>{@link UserUnauthorizedException} if the user is not authorized to perform this task</li>
   * </ul>
   */
  public void cancelWorkflowExecution(MetisUser metisUser, String executionId)
      throws GenericMetisException {

    WorkflowExecution workflowExecution = workflowExecutionDao.getById(executionId);
    if (workflowExecution != null) {
      authorizer.authorizeWriteExistingDatasetById(metisUser, workflowExecution.getDatasetId());
    }
    if (workflowExecution != null && (
        workflowExecution.getWorkflowStatus() == WorkflowStatus.RUNNING
            || workflowExecution.getWorkflowStatus() == WorkflowStatus.INQUEUE)) {
      workflowExecutionDao.setCancellingState(workflowExecution, metisUser);
      LOGGER.info(
          "Cancelling user workflow execution with id: {}", workflowExecution.getId());
    } else {
      throw new NoWorkflowExecutionFoundException(String.format(
          "Running workflowExecution with executionId: %s, does not exist or not active",
          executionId));
    }
  }

  /**
   * The number of WorkflowExecutions that would be returned if a get all request would be
   * performed.
   *
   * @return the number representing the size during a get all request
   */
  public int getWorkflowExecutionsPerRequest() {
    return workflowExecutionDao.getWorkflowExecutionsPerRequest();
  }

  /**
   * The number of Workflows that would be returned if a get all request would be performed.
   *
   * @return the number representing the size during a get all request
   */
  public int getWorkflowsPerRequest() {
    return workflowDao.getWorkflowsPerRequest();
  }

  /**
   * Check if a specified {@code pluginType} is allowed for execution. This is checked based on, if
   * there was a previous successful finished plugin that follows a specific order unless the {@code
   * enforcedPredecessorType} is used.
   *
   * @param metisUser the user wishing to perform this operation
   * @param datasetId the dataset identifier of which the executions are based on
   * @param pluginType the pluginType to be checked for allowance of execution
   * @param enforcedPredecessorType optional, the plugin type to be used as source data
   * @return the abstractMetisPlugin that the execution on {@code pluginType} will be based on. Can
   * be null if the {@code pluginType} is the first one in the total order of executions e.g. One of
   * the harvesting plugins.
   * @throws GenericMetisException which can be one of:
   * <ul>
   * <li>{@link PluginExecutionNotAllowed} if the no plugin was found so the {@code pluginType}
   * will be based upon</li>
   * <li>{@link NoDatasetFoundException} if the dataset identifier provided does not exist</li>
   * <li>{@link UserUnauthorizedException} if the user is not authorized to perform this task</li>
   * </ul>
   */
  public AbstractExecutablePlugin getLatestFinishedPluginByDatasetIdIfPluginTypeAllowedForExecution(
      MetisUser metisUser, String datasetId, ExecutablePluginType pluginType,
      ExecutablePluginType enforcedPredecessorType) throws GenericMetisException {
    authorizer.authorizeReadExistingDatasetById(metisUser, datasetId);
    return new WorkflowUtils(workflowExecutionDao)
        .getPredecessorPlugin(pluginType, enforcedPredecessorType, datasetId);
  }

  /**
   * Get all WorkflowExecutions paged.
   *
   * @param metisUser the user wishing to perform this operation
   * @param datasetId the dataset identifier filter, can be null to get all datasets
   * @param workflowStatuses a set of workflow statuses to filter, can be empty or null
   * @param orderField the field to be used to sort the results
   * @param ascending a boolean value to request the ordering to ascending or descending
   * @param nextPage the nextPage token
   * @return A list of all the WorkflowExecutions found. If the user is not admin, the list is
   * filtered to only show those executions that are in the user's organization.
   * @throws GenericMetisException which can be one of:
   * <ul>
   * <li>{@link NoDatasetFoundException} if the dataset identifier provided does not exist</li>
   * <li>{@link UserUnauthorizedException} if the user is not authorized to perform this task</li>
   * </ul>
   */
  public List<WorkflowExecution> getAllWorkflowExecutions(MetisUser metisUser, String datasetId,
      Set<WorkflowStatus> workflowStatuses, DaoFieldNames orderField, boolean ascending, int nextPage)
      throws GenericMetisException {

    // Authorize
    if (datasetId == null) {
      authorizer.authorizeReadAllDatasets(metisUser);
    } else {
      authorizer.authorizeReadExistingDatasetById(metisUser, datasetId);
    }

    // Determine the dataset IDs to filter on.
    final Set<String> datasetIds;
    if (datasetId == null) {
      datasetIds = getDatasetIdsToFilterOn(metisUser);
    } else {
      datasetIds = Collections.singleton(datasetId);
    }

    // Find the executions.
    return workflowExecutionDao.getAllWorkflowExecutions(datasetIds, workflowStatuses, orderField,
        ascending, nextPage);
  }

  /**
   * Get the overview of WorkflowExecutions. This returns a list of executions ordered to display an
   * overview. First the ones in queue, then those in progress and then those that are finalized.
   * They will be sorted by creation date. This method does support pagination.
   *
   * @param metisUser the user wishing to perform this operation
   * @param pluginStatuses the plugin statuses to filter. Can be null.
   * @param pluginTypes the plugin types to filter. Can be null.
   * @param fromDate the date from where the results should start. Can be null.
   * @param toDate the date to where the results should end. Can be null.
   * @param nextPage the nextPage token, the end of the list is marked with -1 on the response
   * @param pageCount the number of pages that are requested
   * @return a list of all the WorkflowExecutions together with the datasets that they belong to.
   * @throws GenericMetisException which can be one of:
   * <ul>
   * <li>{@link eu.europeana.metis.exception.UserUnauthorizedException} if the user is not
   * authenticated or authorized to perform this operation</li>
   * </ul>
   */
  public List<ExecutionAndDatasetView> getWorkflowExecutionsOverview(MetisUser metisUser,
      Set<PluginStatus> pluginStatuses, Set<PluginType> pluginTypes, Date fromDate,
      Date toDate, int nextPage, int pageCount) throws GenericMetisException {
    authorizer.authorizeReadAllDatasets(metisUser);
    final Set<String> datasetIds = getDatasetIdsToFilterOn(metisUser);
    return workflowExecutionDao
        .getWorkflowExecutionsOverview(datasetIds, pluginStatuses, pluginTypes, fromDate, toDate,
            nextPage, pageCount)
        .stream()
        .map(result -> new ExecutionAndDatasetView(result.getExecution(), result.getDataset()))
        .collect(Collectors.toList());
  }

  private Set<String> getDatasetIdsToFilterOn(MetisUser metisUser) {
    final Set<String> datasetIds;
    if (metisUser.getAccountRole() == AccountRole.METIS_ADMIN) {
      datasetIds = null;
    } else {
      datasetIds = datasetDao.getAllDatasetsByOrganizationId(metisUser.getOrganizationId()).stream()
          .map(Dataset::getDatasetId).collect(Collectors.toSet());
    }
    return datasetIds;
  }

  /**
   * Retrieve dataset level information of past executions {@link DatasetExecutionInformation}
   *
   * @param metisUser the user wishing to perform this operation
   * @param datasetId the dataset identifier to generate the information for
   * @return the structured class containing all the execution information
   * @throws GenericMetisException which can be one of:
   * <ul>
   * <li>{@link NoDatasetFoundException} if the dataset identifier provided does not exist</li>
   * <li>{@link UserUnauthorizedException} if the user is not authorized to perform this task</li>
   * </ul>
   */
  public DatasetExecutionInformation getDatasetExecutionInformation(MetisUser metisUser,
      String datasetId) throws GenericMetisException {
    authorizer.authorizeReadExistingDatasetById(metisUser, datasetId);

    AbstractExecutablePlugin lastHarvestPlugin = workflowExecutionDao
        .getLatestSuccessfulPlugin(datasetId, EnumSet.of(ExecutablePluginType.HTTP_HARVEST,
            ExecutablePluginType.OAIPMH_HARVEST), false);
    AbstractExecutablePlugin firstPublishPlugin = workflowExecutionDao
        .getFirstSuccessfulPlugin(datasetId, EnumSet.of(ExecutablePluginType.PUBLISH));
    AbstractExecutablePlugin lastPreviewPlugin = workflowExecutionDao
        .getLatestSuccessfulPlugin(datasetId, EnumSet.of(ExecutablePluginType.PREVIEW), false);
    AbstractExecutablePlugin lastPublishPlugin = workflowExecutionDao
        .getLatestSuccessfulPlugin(datasetId, EnumSet.of(ExecutablePluginType.PUBLISH), false);

    final WorkflowExecution runningOrInQueueExecution = workflowExecutionDao
        .getRunningOrInQueueExecution(datasetId);
    final boolean isPreviewCleaningOrRunning =
        isPluginInWorkflowCleaningOrRunning(runningOrInQueueExecution, PluginType.PREVIEW);
    final boolean isPublishCleaningOrRunning =
        isPluginInWorkflowCleaningOrRunning(runningOrInQueueExecution, PluginType.PUBLISH);

    DatasetExecutionInformation datasetExecutionInformation = new DatasetExecutionInformation();
    if (lastHarvestPlugin != null) {
      datasetExecutionInformation.setLastHarvestedDate(lastHarvestPlugin.getFinishedDate());
      datasetExecutionInformation.setLastHarvestedRecords(
          lastHarvestPlugin.getExecutionProgress().getProcessedRecords() - lastHarvestPlugin
              .getExecutionProgress().getErrors());
    }
    datasetExecutionInformation.setFirstPublishedDate(firstPublishPlugin == null ? null :
        firstPublishPlugin.getFinishedDate());
    final Date now = new Date();
    if (lastPreviewPlugin != null) {
      datasetExecutionInformation.setLastPreviewDate(lastPreviewPlugin.getFinishedDate());
      datasetExecutionInformation.setLastPreviewRecords(
          lastPreviewPlugin.getExecutionProgress().getProcessedRecords() - lastPreviewPlugin
              .getExecutionProgress().getErrors());
      datasetExecutionInformation.setLastPreviewRecordsReadyForViewing(
          !isPreviewCleaningOrRunning && isPreviewOrPublishReadyForViewing(lastPreviewPlugin, now));
    }
    if (lastPublishPlugin != null) {
      datasetExecutionInformation.setLastPublishedDate(lastPublishPlugin.getFinishedDate());
      datasetExecutionInformation.setLastPublishedRecords(
          lastPublishPlugin.getExecutionProgress().getProcessedRecords() - lastPublishPlugin
              .getExecutionProgress().getErrors());
      datasetExecutionInformation.setLastPublishedRecordsReadyForViewing(
          !isPublishCleaningOrRunning && isPreviewOrPublishReadyForViewing(lastPublishPlugin, now));
    }

    return datasetExecutionInformation;
  }

  private boolean isPreviewOrPublishReadyForViewing(AbstractExecutablePlugin plugin, Date now) {
    final boolean dataIsValid = ExecutablePlugin.getDataStatus(plugin) == DataStatus.VALID;
    final boolean enoughTimeHasPassed = getSolrCommitPeriodInMins() <
        DateUtils.calculateDateDifference(plugin.getFinishedDate(), now, TimeUnit.MINUTES);
    return dataIsValid && enoughTimeHasPassed;
  }

  private boolean isPluginInWorkflowCleaningOrRunning(WorkflowExecution runningOrInQueueExecution,
      PluginType pluginType) {
    return runningOrInQueueExecution != null && runningOrInQueueExecution.getMetisPlugins().stream()
        .filter(metisPlugin -> metisPlugin.getPluginType() == pluginType)
        .map(AbstractMetisPlugin::getPluginStatus)
        .anyMatch(pluginStatus -> pluginStatus == PluginStatus.CLEANING
            || pluginStatus == PluginStatus.RUNNING);
  }

  /**
   * Get the evolution of the records from when they were first imported until (and excluding) the
   * specified version.
   *
   * @param metisUser the user wishing to perform this operation
   * @param workflowExecutionId The ID of the workflow exection in which the version is created.
   * @param pluginType The step within the workflow execution that created the version.
   * @return The record evolution.
   * @throws GenericMetisException which can be one of:
   * <ul>
   * <li>{@link eu.europeana.metis.core.exceptions.NoWorkflowExecutionFoundException} if an
   * non-existing version is provided.</li>
   * <li>{@link eu.europeana.metis.exception.UserUnauthorizedException} if the user is not
   * authenticated or authorized to perform this operation</li>
   * </ul>
   */
  public VersionEvolution getRecordEvolutionForVersion(MetisUser metisUser,
      String workflowExecutionId, PluginType pluginType) throws GenericMetisException {

    // Get the workflow execution in question
    final WorkflowExecution workflowExecution = workflowExecutionDao.getById(workflowExecutionId);
    if (workflowExecution == null) {
      throw new NoWorkflowExecutionFoundException(
          String.format("No workflow execution found for workflowExecutionId: %s",
              workflowExecutionId));
    }

    // Check that the user is authorized.
    authorizer.authorizeReadExistingDatasetById(metisUser, workflowExecution.getDatasetId());

    // Find the plugin (workflow step) in question.
    final AbstractMetisPlugin plugin = workflowExecution.getMetisPluginWithType(pluginType)
        .orElseThrow(() -> new NoWorkflowExecutionFoundException(
            String.format("No plugin of type %s found for workflowExecution with id: %s",
                pluginType.name(), workflowExecutionId)));

    // Loop backwards to find the plugin. Don't add the first plugin to the result list.
    Pair<WorkflowExecution, AbstractMetisPlugin> currentExecutionAndPlugin = new ImmutablePair<>(
        workflowExecution, plugin);
    final ArrayDeque<VersionEvolutionStep> evolutionSteps = new ArrayDeque<>();
    while (true) {

      // Move to the previous execution: stop when we have none or it is not executable.
      currentExecutionAndPlugin = getPreviousExecutionAndPlugin(
          currentExecutionAndPlugin.getRight(), currentExecutionAndPlugin.getLeft().getDatasetId());
      if (currentExecutionAndPlugin == null || !(currentExecutionAndPlugin
          .getRight() instanceof AbstractExecutablePlugin)) {
        break;
      }

      // Add step to the beginning of the list.
      final AbstractExecutablePlugin<?> executablePlugin = (AbstractExecutablePlugin<?>) currentExecutionAndPlugin
          .getRight();
      final VersionEvolutionStep evolutionStep = new VersionEvolutionStep();
      evolutionStep.setWorkflowExecutionId(currentExecutionAndPlugin.getLeft().getId().toString());
      evolutionStep.setPluginType(executablePlugin.getPluginMetadata().getExecutablePluginType());
      evolutionStep.setFinishedTime(executablePlugin.getFinishedDate());
      evolutionSteps.addFirst(evolutionStep);
    }

    // Done
    final VersionEvolution versionEvolution = new VersionEvolution();
    versionEvolution.setEvolutionSteps(evolutionSteps);
    return versionEvolution;
  }

  Pair<WorkflowExecution, AbstractMetisPlugin> getPreviousExecutionAndPlugin(
      AbstractMetisPlugin plugin, String datasetId) {

    // Check whether we are at the end of the chain.
    final Date previousPluginTimestamp = plugin.getPluginMetadata()
        .getRevisionTimestampPreviousPlugin();
    final PluginType previousPluginType = PluginType.getPluginTypeFromEnumName(
        plugin.getPluginMetadata().getRevisionNamePreviousPlugin());
    if (previousPluginTimestamp == null || previousPluginType == null) {
      return null;
    }

    // Obtain the previous execution and plugin.
    final WorkflowExecution previousExecution = workflowExecutionDao
        .getByTaskExecution(previousPluginTimestamp, previousPluginType, datasetId);
    final AbstractMetisPlugin previousPlugin = previousExecution == null ? null
        : previousExecution.getMetisPluginWithType(previousPluginType).orElse(null);
    if (previousExecution == null || previousPlugin == null) {
      return null;
    }

    // Done
    return new ImmutablePair<>(previousExecution, previousPlugin);
  }

  public int getSolrCommitPeriodInMins() {
    synchronized (this) {
      return solrCommitPeriodInMins;
    }
  }

  public void setSolrCommitPeriodInMins(int solrCommitPeriodInMins) {
    synchronized (this) {
      this.solrCommitPeriodInMins = solrCommitPeriodInMins;
    }
  }
}
