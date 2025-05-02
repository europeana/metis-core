package eu.europeana.metis.core.service;

import eu.europeana.cloud.client.uis.rest.CloudException;
import eu.europeana.cloud.common.model.File;
import eu.europeana.cloud.common.model.Representation;
import eu.europeana.cloud.common.model.Revision;
import eu.europeana.cloud.common.response.CloudTagsResponse;
import eu.europeana.cloud.service.dps.exception.DpsException;
import eu.europeana.cloud.service.mcs.exception.MCSException;
import eu.europeana.cloud.service.uis.exception.RecordDoesNotExistException;
import eu.europeana.metis.core.common.RecordIdUtils;
import eu.europeana.metis.core.dao.DataEvolutionUtils;
import eu.europeana.metis.core.dao.DatasetDao;
import eu.europeana.metis.core.dao.WorkflowExecutionDao;
import eu.europeana.metis.core.engine.base.EngineTaskSettings;
import eu.europeana.metis.core.exceptions.NoWorkflowExecutionFoundException;
import eu.europeana.metis.core.engine.base.EngineTask;
import eu.europeana.metis.core.engine.base.item.content.report.ContentNodeReport;
import eu.europeana.metis.core.engine.base.item.report.DataItemStatus;
import eu.europeana.metis.core.engine.base.item.content.report.ContentStatisticsReport;
import eu.europeana.metis.core.engine.base.task.report.EngineTaskErrors;
import eu.europeana.metis.core.rest.ListOfIds;
import eu.europeana.metis.core.rest.PaginatedRecordsResponse;
import eu.europeana.metis.core.rest.Record;
import eu.europeana.metis.core.rest.RecordsResponse;
import eu.europeana.metis.core.rest.stats.NodePathStatistics;
import eu.europeana.metis.core.rest.stats.RecordStatistics;
import eu.europeana.metis.core.util.EngineClients;
import eu.europeana.metis.core.workflow.WorkflowExecution;
import eu.europeana.metis.core.workflow.WorkflowExecutionHelper;
import eu.europeana.metis.core.workflow.plugins.ExecutablePlugin;
import eu.europeana.metis.core.workflow.plugins.ExecutablePluginType;
import eu.europeana.metis.core.workflow.plugins.MetisPlugin;
import eu.europeana.metis.core.workflow.plugins.PluginType;
import eu.europeana.metis.exception.BadContentException;
import eu.europeana.metis.exception.ExternalTaskException;
import eu.europeana.metis.exception.GenericMetisException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Proxies Service which encapsulates functionality that has to be proxied to an external resource.
 */
public class ProxiesService {

  protected final DateFormat pluginDateFormatForEcloud = new SimpleDateFormat(
      "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US);

  private final WorkflowExecutionDao workflowExecutionDao;
  private final DatasetDao datasetDao;
  private final ProxiesHelper proxiesHelper;
  private final DataEvolutionUtils dataEvolutionUtils;
  private final EngineClients<? extends EngineTaskSettings, ? extends EngineTask> engineClients;
  private final String ecloudProvider;
  private final WorkflowExecutionHelper workflowExecutionHelper = new WorkflowExecutionHelper();

  /**
   * Constructor with required parameters.
   *
   * @param engineClients the ecloud components
   * @param workflowExecutionDao {@link WorkflowExecutionDao}
   * @param ecloudProvider the ecloud provider
   * @param datasetDao the Dao instance to access the Dataset database
   */
  public ProxiesService(EngineClients<? extends EngineTaskSettings, ? extends EngineTask> engineClients, String ecloudProvider,
      WorkflowExecutionDao workflowExecutionDao,
      DatasetDao datasetDao) {
    this(engineClients, ecloudProvider, workflowExecutionDao, datasetDao, new ProxiesHelper());
  }

  ProxiesService(EngineClients<? extends EngineTaskSettings, ? extends EngineTask> engineClients, String ecloudProvider, WorkflowExecutionDao workflowExecutionDao,
      DatasetDao datasetDao, ProxiesHelper proxiesHelper) {
    this.engineClients = engineClients;
    this.ecloudProvider = ecloudProvider;
    this.workflowExecutionDao = workflowExecutionDao;
    this.datasetDao = datasetDao;
    this.proxiesHelper = proxiesHelper;
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
  public List<DataItemStatus> getExternalTaskLogs(String topologyName, long externalTaskId, int from, int to)
      throws GenericMetisException {
    datasetDao.getDatasetOrThrow(getDatasetIdFromExternalTaskId(externalTaskId));
    List<DataItemStatus> dataItemStatuses;
    dataItemStatuses = engineClients.engineTaskClient()
                                    .getDataItemStatuses(topologyName, externalTaskId, from, to);
    return dataItemStatuses;
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
  public boolean existsExternalTaskReport(String topologyName, long externalTaskId) throws GenericMetisException {
    datasetDao.getDatasetOrThrow(getDatasetIdFromExternalTaskId(externalTaskId));
    return engineClients.engineTaskClient().hasErrorReport(topologyName, externalTaskId);
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
  public EngineTaskErrors getExternalTaskReport(String topologyName, long externalTaskId, int idsPerError)
      throws GenericMetisException {
    datasetDao.getDatasetOrThrow(getDatasetIdFromExternalTaskId(externalTaskId));
    return engineClients.engineTaskClient().getEngineTaskErrors(topologyName, externalTaskId, null, idsPerError);
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
  public RecordStatistics getExternalTaskStatistics(String topologyName, long externalTaskId) throws GenericMetisException {
    datasetDao.getDatasetOrThrow(getDatasetIdFromExternalTaskId(externalTaskId));
    final ContentStatisticsReport contentStatisticsReport;
    contentStatisticsReport = engineClients.engineTaskClient().getEngineTaskContentStatisticsReport(topologyName, externalTaskId);
    return proxiesHelper.compileRecordStatisticsExternal(contentStatisticsReport);
  }

  /**
   * Get additional statistics on a node. This method can be used to elaborate on one of the items returned by
   * {@link #getExternalTaskStatistics(String, long)}.
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
  public NodePathStatistics getAdditionalNodeStatistics(String topologyName, long externalTaskId, String nodePath)
      throws GenericMetisException {
    datasetDao.getDatasetOrThrow(getDatasetIdFromExternalTaskId(externalTaskId));
    final List<ContentNodeReport> nodeReports;
    nodeReports = engineClients.engineTaskClient().getContentNodeReport(topologyName, externalTaskId, nodePath);
    return proxiesHelper.compileNodePathStatisticsExternal(nodePath, nodeReports);
  }

  private String getDatasetIdFromExternalTaskId(long externalTaskId)
      throws NoWorkflowExecutionFoundException {
    final WorkflowExecution workflowExecution = this.workflowExecutionDao.getByExternalTaskId(externalTaskId);
    if (workflowExecution == null) {
      throw new NoWorkflowExecutionFoundException(String
          .format("No workflow execution found for externalTaskId: %d, in METIS", externalTaskId));
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
    final String revisionTimestamp = pluginDateFormatForEcloud
        .format(executionAndPlugin.getRight().getStartedDate());
    final List<CloudTagsResponse> revisionsWithDeletedFlagSetToFalse;
    try {
      revisionsWithDeletedFlagSetToFalse = engineClients.ecloudDataSetServiceClient()
                                                        .getRevisionsWithDeletedFlagSetToFalse(
                                                                    ecloudProvider, datasetId, representationName, revisionName,
                                                                    ecloudProvider,
                                                                    revisionTimestamp, numberOfRecords);
    } catch (MCSException e) {
      throw new ExternalTaskException(String.format(
          "Getting record list with file content failed. workflowExecutionId: %s, pluginType: %s",
          workflowExecutionId, pluginType), e);
    }

    // Get the records themselves.
    final List<Record> records = new ArrayList<>(revisionsWithDeletedFlagSetToFalse.size());
    for (CloudTagsResponse cloudTagsResponse : revisionsWithDeletedFlagSetToFalse) {
      final Record eloudXmlRecord = getRecord(executionAndPlugin.getRight(), cloudTagsResponse.getCloudId());
      if (eloudXmlRecord == null) {
        throw new IllegalStateException("This can't happen: eCloud just told us the record exists");
      }
      records.add(eloudXmlRecord);
    }

    // Compile the result.
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

    // Get the records.
    final List<Record> records = new ArrayList<>(ecloudIds.getIds().size());
    for (String cloudId : ecloudIds.getIds()) {
      Optional.ofNullable(getRecord(executionAndPlugin.getRight(), cloudId)).ifPresent(records::add);
    }

    // Done.
    return new RecordsResponse(records);
  }

  private static void existsOrThrowNoWorkflowExecutionFoundException(String workflowExecutionId, ExecutablePluginType pluginType,
      Pair<WorkflowExecution, ExecutablePlugin> executionAndPlugin) throws NoWorkflowExecutionFoundException {
    if (executionAndPlugin == null) {
      throw new NoWorkflowExecutionFoundException(String
          .format("No executable plugin of type %s found for workflowExecution with id: %s",
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
      throw new NoWorkflowExecutionFoundException(String
          .format("No predecessor for executable plugin of type %s found for workflowExecution with id: %s",
              pluginType.name(), workflowExecutionId));
    }

    ExecutablePlugin predecessorExecutablePlugin = (ExecutablePlugin) predecessorPlugin.getLeft();

    // Get the records.
    final List<Record> records = new ArrayList<>(ecloudIds.getIds().size());
    for (String cloudId : ecloudIds.getIds()) {
      Optional.ofNullable(getRecord(predecessorExecutablePlugin, cloudId)).ifPresent(records::add);
    }

    // Done.
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
    String ecloudId = null;
    try {
      final String normalizedRecordId = RecordIdUtils.checkAndNormalizeRecordId(datasetId, idToSearch)
                                                     .map(id -> RecordIdUtils.composeFullRecordId(datasetId, id)).orElse(null);
      if (normalizedRecordId != null) {
        ecloudId = engineClients.uisClient().getCloudId(ecloudProvider, normalizedRecordId).getId();
      }
    } catch (BadContentException e) {
      // Normalization failed. Check whether the ID is already an eCloud ID.
      ecloudId = verifyExistenceOfEcloudId(idToSearch);
    } catch (CloudException e) {
      if (e.getCause() instanceof RecordDoesNotExistException) {
        // The record ID does not exist. Check whether the ID is already an eCloud ID.
        ecloudId = verifyExistenceOfEcloudId(idToSearch);
      } else {
        // Some other connectivity issue.
        throw new ExternalTaskException(
            String.format("Failed to lookup cloudId for idToSearch: %s", idToSearch), e);
      }
    }

    // Try to retrieve the record. Note: we need to know if the eCloud ID exists at this point
    // because getRecord() cannot detect non-existing eCloud IDs.
    return ecloudId == null ? null : getRecord(executionAndPlugin.getRight(), ecloudId);
  }

  private String verifyExistenceOfEcloudId(String potentialEcloudId) {
    try {
      return engineClients.uisClient().getRecordId(potentialEcloudId).getResults().isEmpty() ? null
          : potentialEcloudId;
    } catch (CloudException e) {
      // TODO currently we can't distinguish between a connection issue and a non-existing eCloud ID.
      //  The client should be changed to allow for this. We assume here that there is not a connection
      //  issue because, where this method is called, we just did a successful call to the UIS service.
      return null;
    }
  }

  Pair<WorkflowExecution, ExecutablePlugin> getExecutionAndPlugin(String workflowExecutionId, ExecutablePluginType pluginType)
      throws GenericMetisException {

    // Get the workflow execution - check that the user has rights to access this.
    final WorkflowExecution workflowExecution = workflowExecutionDao.getById(workflowExecutionId);
    if (workflowExecution == null) {
      throw new NoWorkflowExecutionFoundException(
          String.format("No workflow execution found for workflowExecutionId: %s, in METIS",
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

  Record getRecord(ExecutablePlugin plugin, String ecloudId) throws ExternalTaskException {

    // Get the representation(s) for the given combination of plugin and record ID.
    final List<Representation> representations;
    try {
      final Revision revision = new Revision(plugin.getPluginType().name(), ecloudProvider,
          plugin.getStartedDate());
      representations = engineClients.recordServiceClient().getRepresentationsByRevision(ecloudId,
          MetisPlugin.getRepresentationName(), revision);
    } catch (MCSException e) {
      throw new ExternalTaskException(String.format(
          "Getting record list with file content failed. externalTaskId: %s, pluginType: %s, ecloudId: %s",
          plugin.getExternalTaskId(), plugin.getPluginType(), ecloudId), e);
    }

    // If no representation is found, return null.
    if (representations == null || representations.isEmpty()) {
      return null;
    }
    final Representation representation = representations.getFirst();

    // Perform checks on the file lists.
    if (representation.getFiles() == null || representation.getFiles().isEmpty()) {
      throw new ExternalTaskException(String.format(
          "Expecting one file in the representation, but received none. externalTaskId: %s, pluginType: %s, ecloudId: %s",
          plugin.getExternalTaskId(), plugin.getPluginType(), ecloudId));
    }
    final File file = representation.getFiles().getFirst();

    // Obtain the file contents belonging to this representation version.
    try {
      final InputStream inputStream = engineClients.fileServiceClient().getFile(file.getContentUri().toString());
      return new Record(ecloudId, IOUtils.toString(inputStream, StandardCharsets.UTF_8.name()));
    } catch (MCSException e) {
      throw new ExternalTaskException(String.format(
          "Getting record list with file content failed. externalTaskId: %s, pluginType: %s",
          plugin.getExternalTaskId(), plugin.getPluginType()), e);
    } catch (IOException e) {
      throw new ExternalTaskException("Problem while reading the contents of the file.", e);
    }
  }

  String getEcloudProvider() {
    return ecloudProvider;
  }
}
