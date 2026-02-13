package eu.europeana.metis.core.service;

import com.google.common.collect.Sets;
import eu.europeana.metis.core.common.DaoFieldNames;
import eu.europeana.metis.core.dao.DataEvolutionUtils;
import eu.europeana.metis.core.dao.DatasetDao;
import eu.europeana.metis.core.dao.DepublishRecordIdDao;
import eu.europeana.metis.core.dao.PluginWithExecutionId;
import eu.europeana.metis.core.dao.WorkflowDao;
import eu.europeana.metis.core.dao.WorkflowExecutionDao;
import eu.europeana.metis.core.dao.WorkflowExecutionDao.ExecutionDatasetPair;
import eu.europeana.metis.core.dao.WorkflowExecutionDao.ResultList;
import eu.europeana.metis.core.dao.WorkflowValidationUtils;
import eu.europeana.metis.core.dataset.Dataset;
import eu.europeana.metis.core.dataset.DatasetExecutionInformation;
import eu.europeana.metis.core.dataset.DatasetExecutionInformation.PublicationStatus;
import eu.europeana.metis.core.engine.base.EngineTask;
import eu.europeana.metis.core.engine.base.EngineTaskSettings;
import eu.europeana.metis.core.exceptions.NoDatasetFoundException;
import eu.europeana.metis.core.exceptions.NoWorkflowExecutionFoundException;
import eu.europeana.metis.core.exceptions.NoWorkflowFoundException;
import eu.europeana.metis.core.exceptions.PluginExecutionNotAllowed;
import eu.europeana.metis.core.exceptions.WorkflowAlreadyExistsException;
import eu.europeana.metis.core.exceptions.WorkflowExecutionAlreadyExistsException;
import eu.europeana.metis.core.execution.WorkflowExecutorSettings;
import eu.europeana.metis.core.rest.ExecutionHistory;
import eu.europeana.metis.core.rest.ExecutionHistory.Execution;
import eu.europeana.metis.core.rest.PluginsWithDataAvailability;
import eu.europeana.metis.core.rest.PluginsWithDataAvailability.PluginWithDataAvailability;
import eu.europeana.metis.core.rest.ResponseListWrapper;
import eu.europeana.metis.core.rest.VersionEvolution;
import eu.europeana.metis.core.rest.VersionEvolution.VersionEvolutionStep;
import eu.europeana.metis.core.rest.execution.overview.ExecutionAndDatasetView;
import eu.europeana.metis.core.user.User;
import eu.europeana.metis.core.workflow.Workflow;
import eu.europeana.metis.core.workflow.WorkflowExecution;
import eu.europeana.metis.core.workflow.WorkflowExecutionHelper;
import eu.europeana.metis.core.workflow.WorkflowStatus;
import eu.europeana.metis.core.workflow.execution.MetisPluginDTO;
import eu.europeana.metis.core.workflow.execution.SystemId;
import eu.europeana.metis.core.workflow.execution.WorkflowExecutionConverter;
import eu.europeana.metis.core.workflow.execution.WorkflowExecutionDTO;
import eu.europeana.metis.core.workflow.plugins.AbstractExecutablePlugin;
import eu.europeana.metis.core.workflow.plugins.AbstractHarvestPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.AbstractMetisPlugin;
import eu.europeana.metis.core.workflow.plugins.DataStatus;
import eu.europeana.metis.core.workflow.plugins.DepublishPlugin;
import eu.europeana.metis.core.workflow.plugins.ExecutablePlugin;
import eu.europeana.metis.core.workflow.plugins.ExecutablePluginType;
import eu.europeana.metis.core.workflow.plugins.MetisPlugin;
import eu.europeana.metis.core.workflow.plugins.PluginStatus;
import eu.europeana.metis.core.workflow.plugins.PluginType;
import eu.europeana.metis.exception.BadContentException;
import eu.europeana.metis.exception.ExternalTaskException;
import eu.europeana.metis.exception.GenericMetisException;
import eu.europeana.metis.utils.DateUtils;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jspecify.annotations.Nullable;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service class that controls the communication between the different DAOs of the system.
 *
 * @param <S> The type representing the task settings required for the engine tasks.
 * @param <T> The type representing the tasks to be managed by the engine.
 */
@Service
public class OrchestratorService<S extends EngineTaskSettings, T extends EngineTask> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  //Use with String.format to suffix the datasetId
  private static final String EXECUTION_FOR_DATASETID_SUBMITION_LOCK = "EXECUTION_FOR_DATASETID_SUBMITION_LOCK_%s";

  public static final Set<ExecutablePluginType> HARVEST_TYPES = Sets
      .immutableEnumSet(ExecutablePluginType.HTTP_HARVEST, ExecutablePluginType.OAIPMH_HARVEST);
  public static final Set<ExecutablePluginType> EXECUTABLE_PREVIEW_TYPES = Sets
      .immutableEnumSet(ExecutablePluginType.PREVIEW);
  public static final Set<ExecutablePluginType> EXECUTABLE_PUBLISH_TYPES = Sets
      .immutableEnumSet(ExecutablePluginType.PUBLISH);
  public static final Set<ExecutablePluginType> EXECUTABLE_DEPUBLISH_TYPES = Sets
      .immutableEnumSet(ExecutablePluginType.DEPUBLISH);
  public static final Set<PluginType> PREVIEW_TYPES = Sets
      .immutableEnumSet(PluginType.PREVIEW, PluginType.REINDEX_TO_PREVIEW);
  public static final Set<PluginType> PUBLISH_TYPES = Sets
      .immutableEnumSet(PluginType.PUBLISH, PluginType.REINDEX_TO_PUBLISH);

  private final WorkflowExecutionDao workflowExecutionDao;
  private final WorkflowValidationUtils workflowValidationUtils;
  private final DataEvolutionUtils dataEvolutionUtils;
  private final WorkflowDao workflowDao;
  private final DatasetDao datasetDao;
  private final WorkflowExecutorSettings<S, T> workflowExecutorSettings;
  private final RedissonClient redissonClient;
  private final WorkflowExecutionFactory workflowExecutionFactory;
  private final DepublishRecordIdDao depublishRecordIdDao;
  private final UserService userService;
  private final WorkflowExecutionHelper workflowExecutionHelper = new WorkflowExecutionHelper();
  private int solrCommitPeriodInMins; // Use getter and setter for this field!

  /**
   * Constructor.
   *
   * @param workflowExecutionFactory Factory for creating workflow execution objects.
   * @param workflowDao Data Access Object for workflows.
   * @param workflowExecutionDao Data Access Object for workflow executions.
   * @param workflowValidationUtils Utility for validating workflows.
   * @param dataEvolutionUtils Utility for handling data evolution processes.
   * @param datasetDao Data Access Object for datasets.
   * @param workflowExecutorSettings Configuration settings for the workflow executor manager.
   * @param redissonClient Redis client for distributed operations.
   * @param depublishRecordIdDao Data Access Object for managing depublish record IDs.
   * @param userService Service for managing user-related operations.
   */
  @Autowired
  public OrchestratorService(WorkflowExecutionFactory workflowExecutionFactory,
      WorkflowDao workflowDao, WorkflowExecutionDao workflowExecutionDao,
      WorkflowValidationUtils workflowValidationUtils, DataEvolutionUtils dataEvolutionUtils,
      DatasetDao datasetDao, WorkflowExecutorSettings<S, T> workflowExecutorSettings,
      RedissonClient redissonClient, DepublishRecordIdDao depublishRecordIdDao, UserService userService) {
    this.workflowExecutionFactory = workflowExecutionFactory;
    this.workflowDao = workflowDao;
    this.workflowExecutionDao = workflowExecutionDao;
    this.workflowValidationUtils = workflowValidationUtils;
    this.dataEvolutionUtils = dataEvolutionUtils;
    this.datasetDao = datasetDao;
    this.workflowExecutorSettings = workflowExecutorSettings;
    this.redissonClient = redissonClient;
    this.depublishRecordIdDao = depublishRecordIdDao;
    this.userService = userService;
  }

  /**
   * Create a workflow using a datasetId and the {@link Workflow} that contains the requested plugins. If plugins are disabled,
   * they (their settings) are still saved.
   *
   * @param datasetId the identifier of the dataset for which the workflow should be created
   * @param workflow the workflow with the plugins requested
   * @param enforcedPredecessorType optional, the plugin type to be used as source data
   * @throws GenericMetisException which can be one of:
   * <ul>
   * <li>{@link WorkflowAlreadyExistsException} if a workflow for the dataset identifier provided
   * already exists</li>
   * <li>{@link NoDatasetFoundException} if the dataset identifier provided does not exist</li>
   * <li>{@link BadContentException} if the workflow parameters have unexpected values</li>
   * </ul>
   */
  public void createWorkflow(String datasetId, Workflow workflow,
      ExecutablePluginType enforcedPredecessorType) throws GenericMetisException {

    // Authorize (check dataset existence) and set dataset ID to avoid discrepancy.
    datasetDao.getDatasetOrThrow(datasetId);
    workflow.setDatasetId(datasetId);

    // Check that the workflow does not yet exist.
    if (workflowDao.workflowExistsForDataset(workflow.getDatasetId())) {
      throw new WorkflowAlreadyExistsException(
          String.format("Workflow with datasetId: %s, already exists", workflow.getDatasetId()));
    }

    // Validate the new workflow.
    workflowValidationUtils.validateWorkflowPlugins(workflow, enforcedPredecessorType);

    // Save the workflow.
    workflowDao.create(workflow);
  }

  /**
   * Update an already existent workflow using a datasetId and the {@link Workflow} that contains the requested plugins. If
   * plugins are disabled, they (their settings) are still saved. Any settings in plugins that are not sent in the request are
   * removed.
   *
   * @param datasetId the identifier of the dataset for which the workflow should be updated
   * @param workflow the workflow with the plugins requested
   * @param enforcedPredecessorType optional, the plugin type to be used as source data
   * @throws GenericMetisException which can be one of:
   * <ul>
   * <li>{@link NoWorkflowFoundException} if a workflow for the dataset identifier provided does
   * not exist</li>
   * <li>{@link NoDatasetFoundException} if the dataset identifier provided does not exist</li>
   * <li>{@link BadContentException} if the workflow parameters have unexpected values</li>
   * </ul>
   */
  public void updateWorkflow(String datasetId, Workflow workflow,
      ExecutablePluginType enforcedPredecessorType) throws GenericMetisException {

    // Authorize (check dataset existence) and set dataset ID to avoid discrepancy.
    datasetDao.getDatasetOrThrow(datasetId);
    workflow.setDatasetId(datasetId);

    // Get the current workflow in the database. If it doesn't exist, throw exception.
    final Workflow storedWorkflow = workflowDao.getWorkflow(workflow.getDatasetId());
    if (storedWorkflow == null) {
      throw new NoWorkflowFoundException(
          String.format("Workflow with datasetId: %s, not found", workflow.getDatasetId()));
    }

    // Validate the new workflow.
    workflowValidationUtils.validateWorkflowPlugins(workflow, enforcedPredecessorType);

    // Overwrite the workflow.
    workflow.setId(storedWorkflow.getId());
    workflowDao.update(workflow);
  }

  /**
   * Deletes a workflow.
   *
   * @param datasetId the dataset identifier that corresponds to the workflow to be deleted
   * @throws GenericMetisException which can be one of:
   * <ul>
   * <li>{@link NoDatasetFoundException} if the dataset identifier provided does not exist</li>
   * </ul>
   */
  public void deleteWorkflow(String datasetId) throws GenericMetisException {
    datasetDao.getDatasetOrThrow(datasetId);
    workflowDao.deleteWorkflow(datasetId);
  }

  /**
   * Get a workflow for a dataset identifier.
   *
   * @param datasetId the dataset identifier
   * @return the Workflow object
   * @throws GenericMetisException which can be one of:
   * <ul>
   * <li>{@link NoDatasetFoundException} if the dataset identifier provided does not exist</li>
   * </ul>
   */
  public Workflow getWorkflow(String datasetId) throws GenericMetisException {
    datasetDao.getDatasetOrThrow(datasetId);
    return workflowDao.getWorkflow(datasetId);
  }

  /**
   * Retrieves a WorkflowExecutionDTO by its execution ID.
   *
   * @param executionId the ID of the workflow execution to retrieve
   * @return the WorkflowExecutionDTO associated with the given execution ID, or null if no such execution exists
   * @throws GenericMetisException if an error occurs while retrieving the workflow execution
   */
  public WorkflowExecutionDTO getWorkflowExecutionDTOByExecutionId(String executionId) throws GenericMetisException {
    WorkflowExecution workflowExecution = getWorkflowExecutionByExecutionId(executionId);
    User startedUser = null;
    User cancelledUser = null;
    if (workflowExecution != null) {
      startedUser = userService.getUserFromCache(workflowExecution.getStartedBy());
      cancelledUser = userService.getUserFromCache(workflowExecution.getCancelledBy());
    }
    return WorkflowExecutionConverter.toDTO(workflowExecution, workflowExecution != null && isIncremental(workflowExecution),
        startedUser, cancelledUser);
  }

  /**
   * Get a WorkflowExecution using an execution identifier.
   *
   * @param executionId the execution identifier
   * @return the WorkflowExecution object
   * @throws GenericMetisException which can be one of:
   * <ul>
   * <li>{@link NoDatasetFoundException} if the dataset identifier provided does not exist</li>
   * </ul>
   */
  private WorkflowExecution getWorkflowExecutionByExecutionId(String executionId) throws GenericMetisException {
    final WorkflowExecution result = workflowExecutionDao.getById(executionId);
    if (result != null) {
      datasetDao.getDatasetOrThrow(result.getDatasetId());
    }
    return result;
  }

  /**
   * Does checking, prepares and adds a WorkflowExecution in the queue. That means it updates the status of the WorkflowExecution
   * to {@link WorkflowStatus#INQUEUE}, adds it to the database, and also it's identifier goes into the distributed queue of
   * WorkflowExecutions. The source data for the first plugin in the workflow can be controlled, if required, from the
   * {@code enforcedPredecessorType}, which means that the last valid plugin that is provided with that parameter, will be used as
   * the source data.
   *
   * @param datasetId the dataset identifier for which the execution will take place
   * @param workflowProvided optional, the workflow to use instead of retrieving the saved one from the db
   * @param enforcedPredecessorType optional, the plugin type to be used as source data
   * @param userId the userId of the user
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
  public WorkflowExecutionDTO addWorkflowInQueueOfWorkflowExecutions(String datasetId, @Nullable Workflow workflowProvided,
      @Nullable ExecutablePluginType enforcedPredecessorType, String userId)
      throws GenericMetisException {
    final Dataset dataset = datasetDao.getDatasetOrThrow(datasetId);
    WorkflowExecution workflowExecution = addWorkflowInQueueOfWorkflowExecutions(dataset, workflowProvided,
        enforcedPredecessorType, userId);
    return WorkflowExecutionConverter.toDTO(workflowExecution, workflowExecution != null && isIncremental(workflowExecution),
        userService.getUserFromCache(userId), null);
  }

  private WorkflowExecution addWorkflowInQueueOfWorkflowExecutions(Dataset dataset,
      @Nullable Workflow workflowProvided,
      @Nullable ExecutablePluginType enforcedPredecessorType,
      String userId)
      throws GenericMetisException {

    // Get the workflow or use the one provided.
    final Workflow workflow;
    if (Objects.isNull(workflowProvided)) {
      workflow = workflowDao.getWorkflow(dataset.getDatasetId());
    } else {
      workflow = workflowProvided;
    }
    if (workflow == null) {
      throw new NoWorkflowFoundException(
          String.format("No workflow found with datasetId: %s, in METIS", dataset.getDatasetId()));
    }

    // Validate the workflow and obtain the predecessor.
    final PluginWithExecutionId<ExecutablePlugin> predecessor = workflowValidationUtils
        .validateWorkflowPlugins(workflow, enforcedPredecessorType);

    // Make sure that eCloud knows the dataset (needs to happen before we create the workflow).
    createEngineDatasetId(dataset);

    // Create the workflow execution (without adding it to the database).
    final WorkflowExecution workflowExecution = workflowExecutionFactory
        .createWorkflowExecution(workflow, dataset, predecessor);

    // Obtain the lock.
    RLock executionDatasetIdLock = redissonClient
        .getFairLock(String.format(EXECUTION_FOR_DATASETID_SUBMITION_LOCK, dataset.getDatasetId()));
    executionDatasetIdLock.lock();

    // Add the workflow execution to the database. Then release the lock.
    final String objectId;
    try {
      String storedWorkflowExecutionId = workflowExecutionDao
          .existsAndNotCompleted(dataset.getDatasetId());
      if (storedWorkflowExecutionId != null) {
        throw new WorkflowExecutionAlreadyExistsException(String
            .format("Workflow execution already exists with id %s and is not completed",
                storedWorkflowExecutionId));
      }
      workflowExecution.setWorkflowStatus(WorkflowStatus.INQUEUE);
      if (StringUtils.isBlank(userId)) {
        workflowExecution.setStartedBy(SystemId.STARTED_BY_SYSTEM.name());
      } else {
        workflowExecution.setStartedBy(userId);
      }
      workflowExecution.setCreatedDate(new Date());
      objectId = workflowExecutionDao.create(workflowExecution).getId().toString();
    } finally {
      executionDatasetIdLock.unlock();
    }

    // Done. Get a fresh copy of the workflow execution to return.
    return workflowExecutionDao.getById(objectId);
  }

  private String createEngineDatasetId(Dataset dataset) throws ExternalTaskException {
    if (StringUtils.isEmpty(dataset.getEcloudDatasetId())
        || dataset.getEcloudDatasetId().startsWith("NOT_CREATED_YET")) {
      final String engineDatasetUuid = UUID.randomUUID().toString();
      boolean isEngineDatasetIdCreated = workflowExecutorSettings.engineTaskClient().createEngineDatasetId(engineDatasetUuid);
      if (!isEngineDatasetIdCreated) {
        throw new ExternalTaskException(
            String.format("Could not create engine dataset id for datasetId: %s", dataset.getDatasetId()));
      }
      dataset.setEcloudDatasetId(engineDatasetUuid);
      datasetDao.update(dataset);
    } else {
      LOGGER.info("Dataset with datasetId {} already has a dataset initialized in Ecloud with id {}",
              dataset.getDatasetId(), dataset.getEcloudDatasetId());
    }
    return dataset.getEcloudDatasetId();
  }

  /**
   * Request to cancel a workflow execution. The execution will go into a cancelling state until it's properly
   * {@link WorkflowStatus#CANCELLED} from the system
   *
   * @param executionId the execution identifier of the execution to cancel
   * @param userId the userId of the user
   * @throws GenericMetisException which can be one of:
   * <ul>
   * <li>{@link NoWorkflowExecutionFoundException} if no workflowExecution could be found</li>
   * <li>{@link NoDatasetFoundException} if the dataset identifier provided does not exist</li>
   * </ul>
   */
  public void cancelWorkflowExecution(String executionId, String userId) throws GenericMetisException {
    WorkflowExecution workflowExecution = workflowExecutionDao.getById(executionId);
    if (workflowExecution != null) {
      datasetDao.getDatasetOrThrow(workflowExecution.getDatasetId());
    }
    if (workflowExecution != null && (
        workflowExecution.getWorkflowStatus() == WorkflowStatus.RUNNING
            || workflowExecution.getWorkflowStatus() == WorkflowStatus.INQUEUE)) {
      workflowExecutionDao.setCancellingState(workflowExecution, userId);
      LOGGER.info("Cancelling user workflow execution with id: {}", workflowExecution.getId());
    } else {
      throw new NoWorkflowExecutionFoundException(String
          .format("Running workflowExecution with executionId: %s, does not exist or not active",
              executionId));
    }
  }

  /**
   * The number of WorkflowExecutions that would be returned if a get all request would be performed.
   *
   * @return the number representing the size during a get all request
   */
  public int getWorkflowExecutionsPerRequest() {
    return workflowExecutionDao.getWorkflowExecutionsPerRequest();
  }

  /**
   * Check if a specified {@code pluginType} is allowed for execution. This is checked based on, if there was a previous
   * successful finished plugin that follows a specific order (unless the {@code enforcedPredecessorType} is used) and that has
   * the latest successful harvest plugin as an ancestor.
   *
   * @param datasetId the dataset identifier of which the executions are based on
   * @param pluginType the pluginType to be checked for allowance of execution
   * @param enforcedPredecessorType optional, the plugin type to be used as source data
   * @return the abstractMetisPlugin that the execution on {@code pluginType} will be based on. Can be null if the
   * {@code pluginType} is the first one in the total order of executions e.g. One of the harvesting plugins.
   * @throws GenericMetisException which can be one of:
   * <ul>
   * <li>{@link PluginExecutionNotAllowed} if the no plugin was found so the {@code pluginType}
   * will be based upon</li>
   * <li>{@link NoDatasetFoundException} if the dataset identifier provided does not exist</li>
   * </ul>
   */
  public ExecutablePlugin getLatestFinishedPluginByDatasetIdIfPluginTypeAllowedForExecution(String datasetId,
      ExecutablePluginType pluginType,
      ExecutablePluginType enforcedPredecessorType) throws GenericMetisException {
    datasetDao.getDatasetOrThrow(datasetId);
    return Optional.ofNullable(
                       dataEvolutionUtils.computePredecessorPlugin(pluginType, enforcedPredecessorType, datasetId))
                   .map(PluginWithExecutionId::getPlugin).orElse(null);
  }

  /**
   * Get all WorkflowExecutions paged.
   *
   * @param datasetId the dataset identifier filter, can be null to get all datasets
   * @param workflowStatuses a set of workflow statuses to filter, can be empty or null
   * @param orderField the field to be used to sort the results
   * @param ascending a boolean value to request the ordering to ascending or descending
   * @param nextPage the nextPage token
   * @return A list of all the WorkflowExecutions found.
   * @throws GenericMetisException which can be one of:
   * <ul>
   * <li>{@link NoDatasetFoundException} if the dataset identifier provided does not exist</li>
   * </ul>
   */
  public ResponseListWrapper<WorkflowExecutionDTO> getAllWorkflowExecutions(
      String datasetId, Set<WorkflowStatus> workflowStatuses, DaoFieldNames orderField,
      boolean ascending, int nextPage) throws GenericMetisException {

    // Authorize
    if (datasetId != null) {
      datasetDao.getDatasetOrThrow(datasetId);
    }

    // Determine the dataset IDs to filter on.
    final Set<String> datasetIds;
    if (datasetId == null) {
      datasetIds = getAllDatasetIds();
    } else {
      datasetIds = Collections.singleton(datasetId);
    }

    // Find the executions.
    final ResultList<WorkflowExecution> data = workflowExecutionDao
        .getAllWorkflowExecutions(datasetIds, workflowStatuses, orderField, ascending, nextPage, 1,
            false);

    // Compile and return the result.
    final List<WorkflowExecutionDTO> convertedData = data.results().stream().map(
        execution ->
        {
          User startedUser = userService.getUserFromCache(execution.getStartedBy());
          User cancelledUser = userService.getUserFromCache(execution.getCancelledBy());
          return WorkflowExecutionConverter.toDTO(execution, isIncremental(execution), startedUser, cancelledUser);
        }).toList();

    final ResponseListWrapper<WorkflowExecutionDTO> result = new ResponseListWrapper<>();
    result.setResultsAndLastPage(convertedData, getWorkflowExecutionsPerRequest(), nextPage,
        data.maxResultCountReached());
    return result;
  }

  /**
   * Checks if a workflow execution is an incremental one based on root ancestor information
   *
   * @param workflowExecution the workflow execution to check
   * @return true if incremental, false otherwise
   */
  private boolean isIncremental(WorkflowExecution workflowExecution) {
    final AbstractMetisPlugin<?> firstPluginInList = workflowExecution.getMetisPlugins().getFirst();
    // Non-executable plugins are not to be checked
    if (!(firstPluginInList instanceof AbstractExecutablePlugin)) {
      return false;
    }

    final ExecutablePlugin harvestPlugin = new DataEvolutionUtils(workflowExecutionDao)
        .getRootAncestor(new PluginWithExecutionId<>(workflowExecution,
            ((AbstractExecutablePlugin<?>) firstPluginInList)))
        .getPlugin();

    // depublication can also be a root ancestor.
    if (harvestPlugin.getPluginMetadata().getExecutablePluginType()
        == ExecutablePluginType.DEPUBLISH) {
      return false;
    }

    // Check the harvesting types
    if (!DataEvolutionUtils.getHarvestPluginGroup()
                           .contains(harvestPlugin.getPluginMetadata().getExecutablePluginType())) {
      throw new IllegalStateException(String.format(
          "workflowExecutionId: %s, pluginId: %s - Found plugin root that is not a harvesting plugin.",
          workflowExecution.getId(), harvestPlugin.getId()));
    }
    return (harvestPlugin.getPluginMetadata() instanceof AbstractHarvestPluginMetadata abstractHarvestPluginMetadata)
        && abstractHarvestPluginMetadata.isIncrementalHarvest();
  }

  /**
   * Get the overview of WorkflowExecutions. This returns a list of executions ordered to display an overview. First the ones in
   * queue, then those in progress and then those that are finalized. They will be sorted by creation date. This method does
   * support pagination.
   *
   * @param pluginStatuses the plugin statuses to filter. Can be null.
   * @param pluginTypes the plugin types to filter. Can be null.
   * @param fromDate the date from where the results should start. Can be null.
   * @param toDate the date to where the results should end. Can be null.
   * @param nextPage the nextPage token, the end of the list is marked with -1 on the response
   * @param pageCount the number of pages that are requested
   * @return a list of all the WorkflowExecutions together with the datasets that they belong to.
   */
  public ResponseListWrapper<ExecutionAndDatasetView> getWorkflowExecutionsOverview(Set<PluginStatus> pluginStatuses,
      Set<PluginType> pluginTypes, Date fromDate, Date toDate, int nextPage, int pageCount) {
    final Set<String> datasetIds = getAllDatasetIds();
    final ResultList<ExecutionDatasetPair> resultList;
    if (datasetIds == null || !datasetIds.isEmpty()) {
      //Match results filtering using specified dataset ids or without dataset id filter if it's null
      resultList = workflowExecutionDao
          .getWorkflowExecutionsOverview(datasetIds, pluginStatuses, pluginTypes, fromDate, toDate,
              nextPage, pageCount);
    } else {
      //Result should be empty if dataset set is empty
      resultList = new ResultList<>(Collections.emptyList(), false);
    }
    final List<ExecutionAndDatasetView> views = resultList.results().stream()
                                                          .map(result -> new ExecutionAndDatasetView(result.getExecution(),
                                                              result.getDataset()))
                                                          .toList();
    final ResponseListWrapper<ExecutionAndDatasetView> result = new ResponseListWrapper<>();
    result.setResultsAndLastPage(views, getWorkflowExecutionsPerRequest(), nextPage, pageCount,
        resultList.maxResultCountReached());
    return result;
  }

  /**
   * Retrieves a set of dataset IDs to filter on.
   *
   * @return a set of dataset IDs
   */
  private Set<String> getAllDatasetIds() {
    return datasetDao.getAllDatasets().stream().map(Dataset::getDatasetId).collect(Collectors.toSet());
  }

  /**
   * Retrieve dataset level information of past executions {@link DatasetExecutionInformation}
   *
   * @param datasetId the dataset identifier to generate the information for
   * @return the structured class containing all the execution information
   * @throws GenericMetisException which can be one of:
   * <ul>
   * <li>{@link NoDatasetFoundException} if the dataset identifier provided does not exist</li>
   * </ul>
   */
  public DatasetExecutionInformation getDatasetExecutionInformation(String datasetId) throws GenericMetisException {
    datasetDao.getDatasetOrThrow(datasetId);
    // Obtain the relevant parts of the execution history
    final ExecutablePlugin lastHarvestPlugin = Optional.ofNullable(
                                                           workflowExecutionDao.getLatestSuccessfulExecutablePlugin(datasetId, HARVEST_TYPES, false))
                                                       .map(PluginWithExecutionId::getPlugin).orElse(null);
    final PluginWithExecutionId<MetisPlugin> firstPublishPluginWithExecutionId = workflowExecutionDao
        .getFirstSuccessfulPlugin(datasetId, PUBLISH_TYPES);
    final MetisPlugin firstPublishPlugin = firstPublishPluginWithExecutionId == null ? null
        : firstPublishPluginWithExecutionId.getPlugin();
    final ExecutablePlugin lastExecutablePreviewPlugin = Optional.ofNullable(workflowExecutionDao
                                                                     .getLatestSuccessfulExecutablePlugin(datasetId, EXECUTABLE_PREVIEW_TYPES, false))
                                                                 .map(PluginWithExecutionId::getPlugin).orElse(null);
    final ExecutablePlugin lastExecutablePublishPlugin = Optional.ofNullable(workflowExecutionDao
                                                                     .getLatestSuccessfulExecutablePlugin(datasetId, EXECUTABLE_PUBLISH_TYPES, false))
                                                                 .map(PluginWithExecutionId::getPlugin).orElse(null);
    final PluginWithExecutionId<MetisPlugin> latestPreviewPluginWithExecutionId = workflowExecutionDao
        .getLatestSuccessfulPlugin(datasetId, PREVIEW_TYPES);
    final PluginWithExecutionId<MetisPlugin> latestPublishPluginWithExecutionId = workflowExecutionDao
        .getLatestSuccessfulPlugin(datasetId, PUBLISH_TYPES);
    final MetisPlugin lastPreviewPlugin = latestPreviewPluginWithExecutionId == null ? null
        : latestPreviewPluginWithExecutionId.getPlugin();
    final MetisPlugin lastPublishPlugin = latestPublishPluginWithExecutionId == null ? null
        : latestPublishPluginWithExecutionId.getPlugin();
    final ExecutablePlugin lastExecutableDepublishPlugin = Optional.ofNullable(workflowExecutionDao
                                                                       .getLatestSuccessfulExecutablePlugin(datasetId, EXECUTABLE_DEPUBLISH_TYPES, false))
                                                                   .map(PluginWithExecutionId::getPlugin).orElse(null);

    // Obtain the relevant current executions
    final WorkflowExecution runningOrInQueueExecution = getRunningOrInQueueExecution(datasetId);
    final boolean isPreviewCleaningOrRunning = isPluginInWorkflowCleaningOrRunning(
        runningOrInQueueExecution, PREVIEW_TYPES);
    final boolean isPublishCleaningOrRunning = isPluginInWorkflowCleaningOrRunning(
        runningOrInQueueExecution, PUBLISH_TYPES);

    final DatasetExecutionInformation executionInfo = new DatasetExecutionInformation();
    // Set the last harvest information
    if (Objects.nonNull(lastHarvestPlugin)) {
      executionInfo.setLastHarvestedDate(lastHarvestPlugin.getFinishedDate());
      executionInfo.setLastHarvestedRecords(
          lastHarvestPlugin.getExecutionProgress().getProcessedRecords() - lastHarvestPlugin
              .getExecutionProgress().getErrors());
    }
    final Date now = new Date();
    setPreviewInformation(executionInfo, lastExecutablePreviewPlugin, lastPreviewPlugin,
        isPreviewCleaningOrRunning, now);
    setPublishInformation(executionInfo, firstPublishPlugin, lastExecutablePublishPlugin,
        lastPublishPlugin, lastExecutableDepublishPlugin, isPublishCleaningOrRunning, now,
        datasetId);

    return executionInfo;
  }

  WorkflowExecution getRunningOrInQueueExecution(String datasetId) {
    return workflowExecutionDao.getRunningOrInQueueExecution(datasetId);
  }

  private void setPreviewInformation(DatasetExecutionInformation executionInfo,
      ExecutablePlugin lastExecutablePreviewPlugin, MetisPlugin lastPreviewPlugin,
      boolean isPreviewCleaningOrRunning, Date date) {

    boolean lastPreviewHasDeletedRecords = computeRecordCountsAndCheckDeletedRecords(lastExecutablePreviewPlugin,
        executionInfo::setLastPreviewRecords, executionInfo::setTotalPreviewRecords);

    //Compute more general information of the plugin
    if (Objects.nonNull(lastPreviewPlugin)) {
      executionInfo.setLastPreviewDate(lastPreviewPlugin.getFinishedDate());
      //Check if we have information about the total records
      final boolean recordsAvailable;
      if (executionInfo.getTotalPreviewRecords() > 0) {
        recordsAvailable = true;
      } else if (executionInfo.getTotalPreviewRecords() == 0) {
        recordsAvailable = false;
      } else {
        recordsAvailable = executionInfo.getLastPreviewRecords() > 0 || lastPreviewHasDeletedRecords;
      }

      executionInfo.setLastPreviewRecordsReadyForViewing(recordsAvailable &&
          !isPreviewCleaningOrRunning && isPreviewOrPublishReadyForViewing(lastPreviewPlugin,
          date));
    }
  }

  private void setPublishInformation(DatasetExecutionInformation executionInfo,
      MetisPlugin firstPublishPlugin, ExecutablePlugin lastExecutablePublishPlugin,
      MetisPlugin lastPublishPlugin, ExecutablePlugin lastExecutableDepublishPlugin,
      boolean isPublishCleaningOrRunning, Date date, String datasetId) {

    // Set the first publication information
    executionInfo.setFirstPublishedDate(firstPublishPlugin == null ? null : firstPublishPlugin.getFinishedDate());

    // Determine the depublication situation of the dataset
    final boolean datasetCurrentlyDepublished = isDatasetCurrentlyDepublished(lastExecutablePublishPlugin,
        lastExecutableDepublishPlugin);

    boolean lastPublishHasDeletedRecords = computeRecordCountsAndCheckDeletedRecords(lastExecutablePublishPlugin,
        executionInfo::setLastPublishedRecords, executionInfo::setTotalPublishedRecords);

    //Compute depublish count
    final int depublishedRecordCount;
    if (datasetCurrentlyDepublished) {
      depublishedRecordCount = executionInfo.getLastPublishedRecords();
    } else {
      depublishedRecordCount = (int) depublishRecordIdDao
          .countSuccessfullyDepublishedRecordIdsForDataset(datasetId);
    }

    //Compute more general information of the plugin
    if (Objects.nonNull(lastPublishPlugin)) {
      executionInfo.setLastPublishedDate(lastPublishPlugin.getFinishedDate());

      //Check if we have information about the total records
      final boolean recordsAvailable;
      if (executionInfo.getTotalPublishedRecords() > 0) {
        recordsAvailable = true;
      } else if (executionInfo.getTotalPublishedRecords() == 0) {
        recordsAvailable = false;
      } else {
        recordsAvailable =
            !datasetCurrentlyDepublished && (executionInfo.getLastPublishedRecords() > depublishedRecordCount
                || lastPublishHasDeletedRecords);
      }
      executionInfo.setLastPublishedRecordsReadyForViewing(
          recordsAvailable && !isPublishCleaningOrRunning && isPreviewOrPublishReadyForViewing(
              lastPublishPlugin, date));
    }

    // Set the last depublished information.
    executionInfo.setLastDepublishedRecords(depublishedRecordCount);
    if (Objects.nonNull(lastExecutableDepublishPlugin)) {
      executionInfo.setLastDepublishedDate(lastExecutableDepublishPlugin.getFinishedDate());
    }

    // Set the publication status.
    final PublicationStatus status;
    if (datasetCurrentlyDepublished) {
      status = PublicationStatus.DEPUBLISHED;
    } else if (lastExecutablePublishPlugin != null) {
      status = PublicationStatus.PUBLISHED;
    } else {
      status = null;
    }
    executionInfo.setPublicationStatus(status);
  }

  private static boolean isDatasetCurrentlyDepublished(ExecutablePlugin lastExecutablePublishPlugin,
      ExecutablePlugin lastExecutableDepublishPlugin) {
    final boolean depublishHappenedAfterLatestExecutablePublish =
        lastExecutableDepublishPlugin != null && lastExecutablePublishPlugin != null &&
            lastExecutablePublishPlugin.getFinishedDate().compareTo(lastExecutableDepublishPlugin.getFinishedDate()) < 0;
    /* TODO JV below we use the fact that a record depublish cannot follow a dataset depublish (so
        we don't have to look further into the past for all depublish actions after the last
        publish). We should make this code more robust by not assuming that here. */
    return depublishHappenedAfterLatestExecutablePublish
        && (lastExecutableDepublishPlugin instanceof DepublishPlugin depublishPlugin)
        && depublishPlugin.getPluginMetadata().isDatasetDepublish();
  }

  private boolean computeRecordCountsAndCheckDeletedRecords(ExecutablePlugin executablePlugin, IntConsumer lastRecordsSetter,
      IntConsumer totalRecordsSetter) {
    int recordCount = 0;
    int totalRecordCount = -1;
    boolean hasDeletedRecords = false;
    if (Objects.nonNull(executablePlugin)) {
      recordCount = executablePlugin.getExecutionProgress().getProcessedRecords()
          - executablePlugin.getExecutionProgress().getErrors();
      totalRecordCount = executablePlugin.getExecutionProgress().getTotalDatabaseRecords();
      hasDeletedRecords = executablePlugin.getExecutionProgress().getDeletedRecords() > 0;
    }
    lastRecordsSetter.accept(recordCount);
    totalRecordsSetter.accept(totalRecordCount);
    return hasDeletedRecords;
  }

  private boolean isPreviewOrPublishReadyForViewing(MetisPlugin plugin, Date now) {
    final boolean dataIsValid = !(plugin instanceof ExecutablePlugin executablePlugin)
        || MetisPlugin.getDataStatus(executablePlugin) == DataStatus.VALID;
    final boolean enoughTimeHasPassed = getSolrCommitPeriodInMinutes() < DateUtils
        .calculateDateDifference(plugin.getFinishedDate(), now, TimeUnit.MINUTES);
    return dataIsValid && enoughTimeHasPassed;
  }

  private boolean isPluginInWorkflowCleaningOrRunning(WorkflowExecution runningOrInQueueExecution,
      Set<PluginType> pluginTypes) {
    return runningOrInQueueExecution != null && runningOrInQueueExecution.getMetisPlugins().stream()
                                                                         .filter(metisPlugin -> pluginTypes.contains(
                                                                             metisPlugin.getPluginType()))
                                                                         .map(AbstractMetisPlugin::getPluginStatus).anyMatch(
            pluginStatus -> pluginStatus == PluginStatus.CLEANING
                || pluginStatus == PluginStatus.RUNNING);
  }

  /**
   * Retrieve dataset level history of past executions {@link DatasetExecutionInformation}
   *
   * @param datasetId the dataset identifier to generate the history for
   * @return the structured class containing all the execution history, ordered by date descending.
   * @throws GenericMetisException which can be one of:
   * <ul>
   * <li>{@link NoDatasetFoundException} if the dataset identifier provided does not exist</li>
   * </ul>
   */
  public ExecutionHistory getDatasetExecutionHistory(String datasetId)
      throws GenericMetisException {

    // Check that the user is authorized
    datasetDao.getDatasetOrThrow(datasetId);

    // Get the executions from the database
    final ResultList<WorkflowExecution> allExecutions = workflowExecutionDao
        .getAllWorkflowExecutions(Set.of(datasetId), null, DaoFieldNames.STARTED_DATE, false, 0,
            null, false);

    List<WorkflowExecutionDTO> workflowExecutionDTOList =
        allExecutions.results().stream()
                     .map(workflowExecution -> {
                       User startedUser = userService.getUserFromCache(workflowExecution.getStartedBy());
                       User cancelledUser = userService.getUserFromCache(workflowExecution.getCancelledBy());
                       return WorkflowExecutionConverter.toDTO(workflowExecution, isIncremental(workflowExecution), startedUser,
                           cancelledUser);
                     })
                     .toList();

    // Filter the executions.
    final List<Execution> executions = workflowExecutionDTOList.stream().filter(
                                                                   entry -> entry.getMetisPlugins().stream().anyMatch(
                                                                       MetisPluginDTO::isCanDisplayRawXml))
                                                               .map(OrchestratorService::convert).toList();

    // Done
    final ExecutionHistory result = new ExecutionHistory();
    result.setExecutions(executions);
    return result;
  }

  private static Execution convert(WorkflowExecutionDTO workflowExecutionDTO) {
    final Execution result = new Execution();
    result.setWorkflowExecutionId(workflowExecutionDTO.getId());
    result.setStartedDate(workflowExecutionDTO.getStartedDate());
    return result;
  }

  /**
   * Retrieve a list of plugins with data availability {@link PluginsWithDataAvailability} for a given workflow execution.
   *
   * @param executionId the identifier of the execution for which to get the plugins
   * @return the structured class containing all the execution history, ordered by date descending.
   * @throws GenericMetisException which can be one of:
   * <ul>
   * <li>{@link NoWorkflowExecutionFoundException} if a
   * non-existing execution ID or version is provided.</li>
   * </ul>
   */
  public PluginsWithDataAvailability getExecutablePluginsWithDataAvailability(String executionId) throws GenericMetisException {

    // Get the execution and do the authorization check.
    final WorkflowExecution workflowExecution = getWorkflowExecutionByExecutionId(executionId);
    if (workflowExecution == null) {
      throw new NoWorkflowExecutionFoundException(
          String.format("No workflow execution found for workflowExecutionId: %s", executionId));
    }

    User startedUser = userService.getUserFromCache(workflowExecution.getStartedBy());
    User cancelledUser = userService.getUserFromCache(workflowExecution.getCancelledBy());
    WorkflowExecutionDTO workflowExecutionDTO =
        WorkflowExecutionConverter.toDTO(workflowExecution, isIncremental(workflowExecution), startedUser, cancelledUser);

    // Compile the result.
    final List<PluginWithDataAvailability> plugins = workflowExecutionDTO.getMetisPlugins().stream()
                                                                         .filter(MetisPluginDTO::isCanDisplayRawXml)
                                                                         .map(OrchestratorService::convert).toList();
    final PluginsWithDataAvailability result = new PluginsWithDataAvailability();
    result.setPlugins(plugins);

    // Done.
    return result;
  }

  private static PluginWithDataAvailability convert(MetisPluginDTO plugin) {
    final PluginWithDataAvailability result = new PluginWithDataAvailability();
    result.setCanDisplayRawXml(plugin.isCanDisplayRawXml()); // If this method is called, it is known that it can display.
    result.setPluginType(plugin.getPluginType());
    return result;
  }

  /**
   * Get the evolution of the records from when they were first imported until (and excluding) the specified version.
   *
   * @param executionId The ID of the workflow exection in which the version is created.
   * @param pluginType The step within the workflow execution that created the version.
   * @return The record evolution.
   * @throws GenericMetisException which can be one of:
   * <ul>
   * <li>{@link NoWorkflowExecutionFoundException} if a non-existing execution ID or version is provided.</li>
   * </ul>
   */
  public VersionEvolution getRecordEvolutionForVersion(String executionId, PluginType pluginType) throws GenericMetisException {

    // Get the execution and do the authorization check.
    final WorkflowExecution execution = getWorkflowExecutionByExecutionId(executionId);
    if (execution == null) {
      throw new NoWorkflowExecutionFoundException(
          String.format("No workflow execution found for workflowExecutionId: %s", executionId));
    }

    // Find the plugin (workflow step) in question.
    final AbstractMetisPlugin<?> targetPlugin = workflowExecutionHelper.getMetisPluginWithType(execution, pluginType)
                                                                       .orElseThrow(
                                                                           () -> new NoWorkflowExecutionFoundException(String
                                                                               .format(
                                                                                   "No plugin of type %s found for workflowExecution with id: %s",
                                                                                   pluginType.name(), execution)));

    // Compile the version evolution.
    final Collection<Pair<ExecutablePlugin, WorkflowExecution>> evolutionSteps = dataEvolutionUtils
        .compileVersionEvolution(targetPlugin, execution);
    final VersionEvolution versionEvolution = new VersionEvolution();
    versionEvolution.setEvolutionSteps(evolutionSteps.stream().map(step -> {
      final VersionEvolutionStep evolutionStep = new VersionEvolutionStep();
      final ExecutablePlugin plugin = step.getLeft();
      evolutionStep.setWorkflowExecutionId(step.getRight().getId().toString());
      evolutionStep.setPluginType(plugin.getPluginMetadata().getExecutablePluginType());
      evolutionStep.setFinishedTime(plugin.getFinishedDate());
      return evolutionStep;
    }).toList());
    return versionEvolution;
  }

  /**
   * This method returns whether currently it is permitted/possible to perform incremental harvesting for the given dataset.
   *
   * @param datasetId The ID of the dataset for which to check.
   * @return Whether we can perform incremental harvesting for the dataset.
   * @throws GenericMetisException which can be one of:
   * <ul>
   * <li>{@link NoDatasetFoundException} if the dataset identifier provided does not exist</li>
   * </ul>
   */
  public boolean isIncrementalHarvestingAllowed(String datasetId)
      throws GenericMetisException {
    datasetDao.getDatasetOrThrow(datasetId);

    // Do the check.
    return workflowValidationUtils.isIncrementalHarvestingAllowed(datasetId);
  }

  public int getSolrCommitPeriodInMinutes() {
    synchronized (this) {
      return solrCommitPeriodInMins;
    }
  }

  public void setSolrCommitPeriodInMinutes(int solrCommitPeriodInMins) {
    synchronized (this) {
      this.solrCommitPeriodInMins = solrCommitPeriodInMins;
    }
  }
}
