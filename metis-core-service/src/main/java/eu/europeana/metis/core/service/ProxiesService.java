package eu.europeana.metis.core.service;

import static java.lang.String.format;

import eu.europeana.cloud.service.dps.exception.DpsException;
import eu.europeana.metis.core.common.RecordIdUtils;
import eu.europeana.metis.core.dao.DataEvolutionUtils;
import eu.europeana.metis.core.dao.DatasetDao;
import eu.europeana.metis.core.dao.WorkflowExecutionDao;
import eu.europeana.metis.core.engine.base.EngineTask;
import eu.europeana.metis.core.engine.base.EngineTaskClient;
import eu.europeana.metis.core.engine.base.EngineTaskSettings;
import eu.europeana.metis.core.engine.base.item.report.DataItemStatus;
import eu.europeana.metis.core.engine.base.task.report.EngineTaskErrors;
import eu.europeana.metis.core.exceptions.NoWorkflowExecutionFoundException;
import eu.europeana.metis.core.rest.ListOfIds;
import eu.europeana.metis.core.rest.PaginatedRecordsResponse;
import eu.europeana.metis.core.rest.Record;
import eu.europeana.metis.core.rest.RecordsResponse;
import eu.europeana.metis.core.rest.stats.NodePathStatisticsDTO;
import eu.europeana.metis.core.rest.stats.RecordStatisticsDTO;
import eu.europeana.metis.core.workflow.WorkflowExecution;
import eu.europeana.metis.core.workflow.WorkflowExecutionHelper;
import eu.europeana.metis.core.workflow.plugins.ExecutablePlugin;
import eu.europeana.metis.core.workflow.plugins.ExecutablePluginType;
import eu.europeana.metis.core.workflow.plugins.MetisPlugin;
import eu.europeana.metis.core.workflow.plugins.PluginType;
import eu.europeana.metis.exception.BadContentException;
import eu.europeana.metis.exception.ExternalTaskException;
import eu.europeana.metis.exception.GenericMetisException;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Proxies Service which encapsulates functionality that has to be proxied to an external resource.
 *
 * @param <S> The type representing the task settings required for the engine tasks.
 * @param <T> The type representing the tasks to be managed by the engine.
 */
public class ProxiesService<S extends EngineTaskSettings, T extends EngineTask> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final WorkflowExecutionDao workflowExecutionDao;
  private final DatasetDao datasetDao;
  private final DataEvolutionUtils dataEvolutionUtils;
  private final EngineTaskClient<S, T> engineTaskClient;
  private final WorkflowExecutionHelper workflowExecutionHelper = new WorkflowExecutionHelper();

  /**
   * Constructs an instance of ProxiesService with the specified dependencies.
   *
   * @param engineTaskClient Client used to interact with engine tasks.
   * @param workflowExecutionDao DAO for accessing workflow execution data.
   * @param datasetDao DAO for managing dataset information.
   */
  public ProxiesService(EngineTaskClient<S, T> engineTaskClient, WorkflowExecutionDao workflowExecutionDao,
      DatasetDao datasetDao) {
    this.engineTaskClient = engineTaskClient;
    this.workflowExecutionDao = workflowExecutionDao;
    this.datasetDao = datasetDao;
    this.dataEvolutionUtils = new DataEvolutionUtils(this.workflowExecutionDao);
  }

  /**
   * Get logs from a specific topology task paged.
   *
   * @param topologyName the topology name of the task
   * @param externalTaskId the task identifier
   * @param from integer to start getting logs from
   * @param to integer until where logs should be received
   * @return the list of logs
   * @throws GenericMetisException can be one of:
   * <ul>
   * <li>{@link DpsException} if an error occurred while retrieving the logs from the external
   * resource</li>
   * <li>{@link NoWorkflowExecutionFoundException} if no
   * workflow execution exists for the provided external task identifier</li>
   * </ul>
   */
  public List<DataItemStatus> getExternalTaskLogs(String topologyName, String externalTaskId, int from, int to)
      throws GenericMetisException {
    datasetDao.getDatasetOrThrow(getDatasetIdFromExternalTaskId(externalTaskId));
    return engineTaskClient.getDataItemStatuses(topologyName, externalTaskId, from, to);
  }

  /**
   * Check if final report is available.
   *
   * @param topologyName the topology name of the task
   * @param externalTaskId the task identifier
   * @return true if final report available, false otherwise
   * @throws GenericMetisException can be one of:
   * <ul>
   * <li>{@link NoWorkflowExecutionFoundException} if no
   * workflow execution exists for the provided external task identifier</li>
   * <li>{@link ExternalTaskException} containing {@link DpsException} if an error occurred while checking if the error report exists</li>
   * </ul>
   */
  public boolean existsExternalTaskReport(String topologyName, String externalTaskId) throws GenericMetisException {
    datasetDao.getDatasetOrThrow(getDatasetIdFromExternalTaskId(externalTaskId));
    return engineTaskClient.hasEngineTaskErrorReport(topologyName, externalTaskId);
  }

  /**
   * Get the final report that includes all the errors grouped. The number of ids per error can be specified through the
   * parameters.
   *
   * @param topologyName the topology name of the task
   * @param externalTaskId the task identifier
   * @param idsPerError the number of ids that should be displayed per error group
   * @return the list of errors grouped
   * @throws GenericMetisException can be one of:
   * <ul>
   * <li>{@link DpsException} if an error occurred while retrieving the report from the external
   * resource</li>
   * <li>{@link NoWorkflowExecutionFoundException} if no
   * workflow execution exists for the provided external task identifier</li>
   * </ul>
   */
  public EngineTaskErrors getExternalTaskReport(String topologyName, String externalTaskId, int idsPerError)
      throws GenericMetisException {
    datasetDao.getDatasetOrThrow(getDatasetIdFromExternalTaskId(externalTaskId));
    return engineTaskClient.getEngineTaskErrors(topologyName, externalTaskId, idsPerError);
  }

  /**
   * Get the statistics of an external task.
   *
   * @param topologyName the topology name of the task
   * @param externalTaskId the task identifier
   * @return the record statistics for the given task.
   * @throws GenericMetisException can be one of:
   * <ul>
   * <li>{@link DpsException} if an error occurred while retrieving the statistics from the
   * external resource</li>
   * <li>{@link NoWorkflowExecutionFoundException} if no
   * workflow execution exists for the provided external task identifier</li>
   * </ul>
   */
  public RecordStatisticsDTO getExternalTaskStatistics(String topologyName, String externalTaskId) throws GenericMetisException {
    datasetDao.getDatasetOrThrow(getDatasetIdFromExternalTaskId(externalTaskId));
    return engineTaskClient.getEngineTaskContentRecordStatistics(topologyName, externalTaskId);
  }

  /**
   * Get additional statistics on a node. This method can be used to elaborate on one of the items returned by
   * {@link #getExternalTaskStatistics(String, String)}.
   *
   * @param topologyName the topology name of the task
   * @param externalTaskId the task identifier
   * @param nodePath the path of the node for which this request is made
   * @return the node statistics for the given path in the given task.
   * @throws GenericMetisException can be one of:
   * <ul>
   * <li>{@link DpsException} if an error occurred while retrieving the statistics from the
   * external resource</li>
   * <li>{@link NoWorkflowExecutionFoundException} if no
   * workflow execution exists for the provided external task identifier</li>
   * </ul>
   */
  public NodePathStatisticsDTO getAdditionalNodeStatistics(String topologyName, String externalTaskId, String nodePath)
      throws GenericMetisException {
    datasetDao.getDatasetOrThrow(getDatasetIdFromExternalTaskId(externalTaskId));
    return engineTaskClient.getEngineTaskContentNodePathStatistics(topologyName, externalTaskId, nodePath);
  }

  private String getDatasetIdFromExternalTaskId(String externalTaskId)
      throws NoWorkflowExecutionFoundException {
    final WorkflowExecution workflowExecution = this.workflowExecutionDao.getByExternalTaskId(externalTaskId);
    if (workflowExecution == null) {
      throw new NoWorkflowExecutionFoundException(
          format("No workflow execution found for externalTaskId: %s, in METIS", externalTaskId));
    }
    return workflowExecution.getDatasetId();
  }

  /**
   * Get a list with record contents from the external resource based on a workflow execution and {@link PluginType}.
   *
   * @param workflowExecutionId the execution identifier of the workflow
   * @param pluginType the {@link ExecutablePluginType} that is to be located inside the workflow
   * @param nextPage the string representation of the next page which is provided from the response and can be used to get the
   * next page of results.
   * TODO: The nextPage parameter is currently ignored and we should decide if we would support it again in the future.
   * @param numberOfRecords the number of records per response
   * @return the list of records from the external resource
   * @throws GenericMetisException can be one of:
   * <ul>
   * <li>{@link ExternalTaskException} if an error occurred while
   * retrieving the records from the external resource</li>
   * <li>{@link NoWorkflowExecutionFoundException} if no
   * workflow execution exists for the provided identifier</li>
   * </ul>
   */
  public PaginatedRecordsResponse getListOfFileContentsFromPluginExecution(
      String workflowExecutionId, ExecutablePluginType pluginType, String nextPage,
      int numberOfRecords) throws GenericMetisException {

    // Get the right workflow execution and plugin type.
    final Pair<WorkflowExecution, ExecutablePlugin> executionAndPlugin = getExecutionAndPlugin(workflowExecutionId, pluginType);
    if (executionAndPlugin == null) {
      return new PaginatedRecordsResponse(Collections.emptyList(), null);
    }

    // Get the list of records.
    final String datasetId = executionAndPlugin.getLeft().getEcloudDatasetId();
    final String representationName = MetisPlugin.getRepresentationName();
    final String revisionName = executionAndPlugin.getRight().getPluginType().name();

    List<Record> records = engineTaskClient.getRecords(datasetId, representationName, revisionName,
        executionAndPlugin.getRight().getStartedDate(), numberOfRecords);

    return new PaginatedRecordsResponse(records, null);
  }

  /**
   * Get a list with record contents from the external resource based on a workflow execution and {@link PluginType}.
   *
   * @param workflowExecutionId the execution identifier of the workflow
   * @param pluginType the {@link ExecutablePluginType} that is to be located inside the workflow
   * @param ecloudIds the list of ecloud IDs of the records we wish to obtain
   * @return the list of records from the external resource
   * @throws GenericMetisException can be one of:
   * <ul>
   * <li>{@link ExternalTaskException} if an error occurred while
   * retrieving the records from the external resource</li>
   * <li>{@link NoWorkflowExecutionFoundException} if no workflow
   * execution exists for the provided identifier</li>
   * </ul>
   */
  public RecordsResponse getListOfFileContentsFromPluginExecution(String workflowExecutionId, ExecutablePluginType pluginType,
      ListOfIds ecloudIds) throws GenericMetisException {

    // Get the right workflow execution and plugin type.
    final Pair<WorkflowExecution, ExecutablePlugin> executionAndPlugin = getExecutionAndPlugin(workflowExecutionId, pluginType);
    existsOrThrowNoWorkflowExecutionFoundException(workflowExecutionId, pluginType, executionAndPlugin);

    final String revisionName = executionAndPlugin.getRight().getPluginType().name();

    List<Record> records = engineTaskClient.getRecords(ecloudIds.getIds(), revisionName,
        executionAndPlugin.getRight().getStartedDate());

    return new RecordsResponse(records);
  }

  private static void existsOrThrowNoWorkflowExecutionFoundException(String workflowExecutionId, ExecutablePluginType pluginType,
      Pair<WorkflowExecution, ExecutablePlugin> executionAndPlugin) throws NoWorkflowExecutionFoundException {
    if (executionAndPlugin == null) {
      throw new NoWorkflowExecutionFoundException(
          format("No executable plugin of type %s found for workflowExecution with id: %s",
              pluginType.name(), workflowExecutionId));
    }
  }

  /**
   * Get a list with record contents from the external resource based on a workflow execution and the predecessor of the given
   * {@link PluginType}.
   *
   * @param workflowExecutionId the execution identifier of the workflow
   * @param pluginType the {@link ExecutablePluginType} that is to be located inside the workflow
   * @param ecloudIds the list of ecloud IDs of the records we wish to obtain
   * @return the list of records from the external resource
   * @throws GenericMetisException can be one of:
   * <ul>
   * <li>{@link ExternalTaskException} if an error occurred while retrieving the records from the external
   * resource</li>
   * <li>{@link NoWorkflowExecutionFoundException} if no workflow
   * execution exists for the provided identifier</li>
   * </ul>
   */
  public RecordsResponse getListOfFileContentsFromPredecessorPluginExecution(String workflowExecutionId,
      ExecutablePluginType pluginType, ListOfIds ecloudIds) throws GenericMetisException {

    // Get the right workflow execution and plugin type.
    final Pair<WorkflowExecution, ExecutablePlugin> executionAndPlugin = getExecutionAndPlugin(workflowExecutionId, pluginType);
    existsOrThrowNoWorkflowExecutionFoundException(workflowExecutionId, pluginType, executionAndPlugin);

    Pair<MetisPlugin, WorkflowExecution> predecessorPlugin =
        dataEvolutionUtils.getPreviousExecutionAndPlugin(executionAndPlugin.getRight(),
            executionAndPlugin.getLeft().getDatasetId());
    if (predecessorPlugin == null) {
      throw new NoWorkflowExecutionFoundException(
          format("No predecessor for executable plugin of type %s found for workflowExecution with id: %s",
              pluginType.name(), workflowExecutionId));
    }

    ExecutablePlugin predecessorExecutablePlugin = (ExecutablePlugin) predecessorPlugin.getLeft();

    final String revisionName = predecessorExecutablePlugin.getPluginType().name();
    List<Record> records = engineTaskClient.getRecords(ecloudIds.getIds(), revisionName,
        predecessorExecutablePlugin.getStartedDate());
    return new RecordsResponse(records);
  }

  /**
   * Get a record from the external resource based on o searchId, workflow execution and {@link PluginType}.
   *
   * @param workflowExecutionId the execution identifier of the workflow
   * @param pluginType the {@link ExecutablePluginType} that is to be located inside the workflow
   * @param idToSearch the ID we are searching for and for which we want to find a record
   * @return the record from the external resource
   * @throws GenericMetisException can be one of:
   * <ul>
   * <li>{@link ExternalTaskException} if an error occurred while
   * retrieving the records from the external resource</li>
   * <li>{@link NoWorkflowExecutionFoundException} if no workflow
   * execution exists for the provided identifier</li>
   * </ul>
   */
  public Record searchRecordByIdFromPluginExecution(String workflowExecutionId, ExecutablePluginType pluginType,
      String idToSearch) throws GenericMetisException {

    // Get the right workflow execution and plugin type.
    final Pair<WorkflowExecution, ExecutablePlugin> executionAndPlugin = getExecutionAndPlugin(workflowExecutionId, pluginType);
    existsOrThrowNoWorkflowExecutionFoundException(workflowExecutionId, pluginType, executionAndPlugin);

    // Check whether the searched ID is known as a Europeana ID or an ecloudId.
    final String datasetId = executionAndPlugin.getLeft().getDatasetId();
    final String revisionName = executionAndPlugin.getRight().getPluginType().name();

    //Check engine record id and then europeana record id.
    Record recordData = engineTaskClient.getRecord(idToSearch, revisionName, executionAndPlugin.getRight().getStartedDate());
    if (recordData == null) {
      String normalizedRecordId = idToSearch;
      try {
        normalizedRecordId = RecordIdUtils.checkAndNormalizeRecordId(datasetId, idToSearch)
                                          .map(id -> RecordIdUtils.composeFullRecordId(datasetId, id)).orElse(null);
      } catch (BadContentException e) {
        LOGGER.info(format("Normalization of recordId '%s' failed. Using as is.", normalizedRecordId), e);
      }
      recordData = engineTaskClient.getRecord(normalizedRecordId, revisionName, executionAndPlugin.getRight().getStartedDate());
    }
    return recordData;
  }

  Pair<WorkflowExecution, ExecutablePlugin> getExecutionAndPlugin(String workflowExecutionId, ExecutablePluginType pluginType)
      throws GenericMetisException {

    // Get the workflow execution - check that the user has rights to access this.
    final WorkflowExecution workflowExecution = workflowExecutionDao.getById(workflowExecutionId);
    if (workflowExecution == null) {
      throw new NoWorkflowExecutionFoundException(
          format("No workflow execution found for workflowExecutionId: %s, in METIS",
              workflowExecutionId));
    }
    datasetDao.getDatasetOrThrow(workflowExecution.getDatasetId());

    // Get the plugin for which to get the records and return.
    final MetisPlugin plugin = workflowExecutionHelper.getMetisPluginWithType(workflowExecution, pluginType.toPluginType())
                                                      .orElse(null);
    if (plugin instanceof ExecutablePlugin executablePlugin) {
      return new ImmutablePair<>(workflowExecution, executablePlugin);
    }
    return null;
  }
}
