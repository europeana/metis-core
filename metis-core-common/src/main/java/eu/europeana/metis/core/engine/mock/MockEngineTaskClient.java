package eu.europeana.metis.core.engine.mock;

import eu.europeana.cloud.client.dps.rest.DpsClient;
import eu.europeana.cloud.client.uis.rest.CloudException;
import eu.europeana.cloud.client.uis.rest.UISClient;
import eu.europeana.cloud.common.model.File;
import eu.europeana.cloud.common.model.Representation;
import eu.europeana.cloud.common.model.Revision;
import eu.europeana.cloud.common.model.dps.ErrorDetails;
import eu.europeana.cloud.common.model.dps.NodeReport;
import eu.europeana.cloud.common.model.dps.StatisticsReport;
import eu.europeana.cloud.common.model.dps.SubTaskInfo;
import eu.europeana.cloud.common.model.dps.TaskErrorsInfo;
import eu.europeana.cloud.common.model.dps.TaskInfo;
import eu.europeana.cloud.common.response.CloudTagsResponse;
import eu.europeana.cloud.mcs.driver.DataSetServiceClient;
import eu.europeana.cloud.mcs.driver.FileServiceClient;
import eu.europeana.cloud.mcs.driver.RecordServiceClient;
import eu.europeana.cloud.service.dps.exception.DpsException;
import eu.europeana.cloud.service.dps.metis.indexing.TargetIndexingDatabase;
import eu.europeana.cloud.service.mcs.exception.MCSException;
import eu.europeana.cloud.service.uis.exception.RecordDoesNotExistException;
import eu.europeana.metis.core.engine.base.DataRevision;
import eu.europeana.metis.core.engine.base.EngineTaskClient;
import eu.europeana.metis.core.engine.base.EngineTaskKey;
import eu.europeana.metis.core.engine.base.IndexDatabase;
import eu.europeana.metis.core.engine.base.item.report.DataItemState;
import eu.europeana.metis.core.engine.base.item.report.DataItemStatus;
import eu.europeana.metis.core.engine.base.task.input.InputDataEndpoint;
import eu.europeana.metis.core.engine.base.task.report.EngineTaskErrorDetails;
import eu.europeana.metis.core.engine.base.task.report.EngineTaskErrorInfo;
import eu.europeana.metis.core.engine.base.task.report.EngineTaskErrors;
import eu.europeana.metis.core.engine.base.task.report.EngineTaskProgress;
import eu.europeana.metis.core.engine.base.task.report.EngineTaskState;
import eu.europeana.metis.core.engine.ecloud.EcloudEngineRecordStatisticsConverter;
import eu.europeana.metis.core.rest.Record;
import eu.europeana.metis.core.rest.stats.NodePathStatisticsDTO;
import eu.europeana.metis.core.rest.stats.RecordStatisticsDTO;
import eu.europeana.metis.core.workflow.plugins.MetisPlugin;
import eu.europeana.metis.exception.ExternalTaskException;
import eu.europeana.metis.exception.UnrecoverableExternalTaskException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.io.IOUtils;

public class MockEngineTaskClient implements EngineTaskClient<MockEngineTaskSettings, MockEngineTask> {

  protected final DateFormat pluginDateFormatForEcloud = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US);
  private final DpsClient dpsClient;
  private final DataSetServiceClient dataSetServiceClient;
  private final RecordServiceClient recordServiceClient;
  private final FileServiceClient fileServiceClient;
  private final UISClient uisClient;
  private final MockEngineTaskSettings mockEngineTaskSettings;

  public MockEngineTaskClient(DpsClient dpsClient, DataSetServiceClient dataSetServiceClient,
      RecordServiceClient recordServiceClient, FileServiceClient fileServiceClient,
      UISClient uisClient, MockEngineTaskSettings mockEngineTaskSettings) {
    this.dpsClient = dpsClient;
    this.dataSetServiceClient = dataSetServiceClient;
    this.recordServiceClient = recordServiceClient;
    this.fileServiceClient = fileServiceClient;
    this.uisClient = uisClient;
    this.mockEngineTaskSettings = mockEngineTaskSettings;
  }

  @Override
  public MockEngineTaskSettings getEngineTaskSettings() {
    return mockEngineTaskSettings;
  }

  @Override
  public MockEngineTask createEngineTask(Map<EngineTaskKey, String> parameters, InputDataEndpoint inputDataEndpoint,
      DataRevision outputDataRevision) {
    return new MockEngineTask(parameters, inputDataEndpoint, outputDataRevision);
  }

  @Override
  public long submitEngineTask(MockEngineTask engineTask, String topologyName) throws ExternalTaskException {
    try {
      return dpsClient.submitTask(engineTask.toDpsTask(), topologyName);
    } catch (DpsException | RuntimeException e) {
      throw new ExternalTaskException("Submitting task to DPS failed", e);
    }
  }

  @Override
  public EngineTaskProgress getEngineTaskProgress(String topologyName, long taskId)
      throws ExternalTaskException, UnrecoverableExternalTaskException {
    try {
      TaskInfo taskInfo = dpsClient.getTaskProgress(topologyName, taskId);
      return convertToProcessingEngineTaskProgress(taskInfo);
    } catch (DpsException e) {
      throw new UnrecoverableExternalTaskException("Fetching task progress failed", e);
    } catch (RuntimeException e) {
      throw new ExternalTaskException("Fetching task progress failed", e);
    }
  }

  private static EngineTaskProgress convertToProcessingEngineTaskProgress(TaskInfo taskInfo) {
    EngineTaskProgress engineTaskProgress = new EngineTaskProgress();
    engineTaskProgress.setExpectedRecords(taskInfo.getExpectedRecordsNumber());
    engineTaskProgress.setProcessedRecords(taskInfo.getProcessedRecordsCount());
    engineTaskProgress.setDeletedRecords(taskInfo.getDeletedRecordsCount());
    engineTaskProgress.setIgnoredRecords(taskInfo.getIgnoredRecordsCount());
    engineTaskProgress.setProcessedErrors(taskInfo.getProcessedErrorsCount());
    engineTaskProgress.setDeletedErrors(taskInfo.getDeletedErrorsCount());
    EngineTaskState engineTaskState = EngineTaskState.valueOf(taskInfo.getState().name());
    engineTaskProgress.setEngineTaskState(engineTaskState);
    engineTaskProgress.setEngineTaskStateInfo(taskInfo.getStateDescription());
    return engineTaskProgress;
  }

  @Override
  public long getTotalIndexedRecords(String datasetId, IndexDatabase indexDatabase) throws ExternalTaskException {
    final TargetIndexingDatabase targetIndexingDatabase = TargetIndexingDatabase.valueOf(indexDatabase.name());
    try {
      return dpsClient.getTotalMetisDatabaseRecords(datasetId, targetIndexingDatabase);
    } catch (DpsException e) {
      throw new ExternalTaskException("Retrieve total indexed records failed", e);
    }
  }

  @Override
  public List<String> getPublishedRecords(String datasetId, List<String> recordsIds) throws ExternalTaskException {
    try {
      return dpsClient.searchPublishedDatasetRecords(datasetId, recordsIds);
    } catch (DpsException e) {
      throw new ExternalTaskException("Retrieve published records failed", e);
    }
  }

  @Override
  public List<DataItemStatus> getDataItemStatuses(String topologyName, long taskId, int from, int to)
      throws ExternalTaskException {
    try {
      List<SubTaskInfo> detailedTaskReportBetweenChunks =
          dpsClient.getDetailedTaskReportBetweenChunks(topologyName, taskId, from, to);
      List<DataItemStatus> dataItemStatuses = new ArrayList<>();
      for (SubTaskInfo subTaskInfo : detailedTaskReportBetweenChunks) {
        DataItemStatus dataItemStatus = new DataItemStatus(
            subTaskInfo.getResourceNum(),
            subTaskInfo.getResource(),
            DataItemState.valueOf(subTaskInfo.getRecordState().name()),
            subTaskInfo.getInfo(),
            subTaskInfo.getEuropeanaId(),
            subTaskInfo.getProcessingTime(),
            subTaskInfo.getResultResource());
        dataItemStatuses.add(dataItemStatus);
      }
      return dataItemStatuses;
    } catch (DpsException e) {
      throw new ExternalTaskException(String.format(
          "Getting the task detailed logs failed. topologyName: %s, externalTaskId: %s, from: %s, to: %s",
          topologyName, taskId, from, to), e);
    }
  }

  @Override
  public boolean hasEngineTaskErrorReport(String topologyName, long taskId) throws ExternalTaskException {
    try {
      return dpsClient.checkIfErrorReportExists(topologyName, taskId);
    } catch (DpsException e) {
      throw new ExternalTaskException(String.format(
          "Checking if the error report exists failed. topologyName: %s, externalTaskId: %s", topologyName, taskId), e);
    }
  }

  @Override
  public EngineTaskErrors getEngineTaskErrors(String topologyName, long taskId, int maxEntries)
      throws ExternalTaskException {
    try {
      TaskErrorsInfo taskErrorsInfo = dpsClient.getTaskErrorsReport(topologyName, taskId, null, maxEntries);

      List<EngineTaskErrorInfo> engineTaskErrorInfoList =
          taskErrorsInfo.getErrors().stream()
                        .map(taskErrorInfo -> {
                          List<EngineTaskErrorDetails> engineTaskErrorDetailsList = new ArrayList<>();
                          for (ErrorDetails errorDetail : taskErrorInfo.getErrorDetails()) {
                            EngineTaskErrorDetails engineTaskErrorDetails = new EngineTaskErrorDetails(
                                errorDetail.getIdentifier(),
                                errorDetail.getAdditionalInfo());
                            engineTaskErrorDetailsList.add(
                                engineTaskErrorDetails);
                          }
                          return new EngineTaskErrorInfo(
                              taskErrorInfo.getErrorType(),
                              taskErrorInfo.getMessage(),
                              taskErrorInfo.getOccurrences(),
                              engineTaskErrorDetailsList);
                        }).toList();
      return new EngineTaskErrors(taskErrorsInfo.getId(), engineTaskErrorInfoList);
    } catch (DpsException e) {
      throw new ExternalTaskException(String.format(
          "Getting the task error report failed. topologyName: %s, externalTaskId: %s, idsPerError: %s",
          topologyName, taskId, maxEntries), e);
    }
  }

  @Override
  public RecordStatisticsDTO getEngineTaskContentRecordStatistics(String topologyName, long taskId)
      throws ExternalTaskException {
    final StatisticsReport statisticsReport;
    try {
      statisticsReport = dpsClient.getTaskStatisticsReport(topologyName, taskId);
      return EcloudEngineRecordStatisticsConverter.compileRecordStatistics(statisticsReport);
    } catch (DpsException e) {
      throw new ExternalTaskException(String.format(
          "Getting the task statistics failed. topologyName: %s, externalTaskId: %s",
          topologyName, taskId), e);
    }
  }

  @Override
  public NodePathStatisticsDTO getEngineTaskContentNodePathStatistics(String topologyName, long taskId, String nodePath)
      throws ExternalTaskException {
    final List<NodeReport> nodeReports;
    try {
      nodeReports = dpsClient.getElementReport(topologyName, taskId, nodePath);
      return EcloudEngineRecordStatisticsConverter.compileNodePathStatistics(nodePath, nodeReports);
    } catch (DpsException e) {
      throw new ExternalTaskException(String.format(
          "Getting the additional node statistics failed. topologyName: %s, externalTaskId: %s",
          topologyName, taskId), e);
    }
  }

  @Override
  public void cancelEngineTask(String topologyName, long taskId, String message) throws ExternalTaskException {
    try {
      dpsClient.killTask(topologyName, taskId, message);
    } catch (DpsException | RuntimeException e) {
      throw new ExternalTaskException("Requesting task cancellation failed", e);
    }
  }

  @Override
  public void close() {
    dpsClient.close();
  }

  @Override
  public boolean createEngineDatasetId(String datasetId) throws ExternalTaskException {
    try {
      dataSetServiceClient.createDataSet(mockEngineTaskSettings.getProvider(), datasetId, "Metis generated dataset id");
    } catch (MCSException e) {
      throw new ExternalTaskException("An error has occurred during ecloud dataset creation.", e);
    }
    return true;
  }

  @Override
  public List<Record> getRecords(String datasetId, String representationName, String revisionName, Date revisionTimestamp,
      int numberOfRecords) throws ExternalTaskException {
    final List<CloudTagsResponse> revisionsWithDeletedFlagSetToFalse;
    try {
      revisionsWithDeletedFlagSetToFalse = dataSetServiceClient.getRevisionsWithDeletedFlagSetToFalse(
          mockEngineTaskSettings.getProvider(), datasetId, representationName, revisionName,
          mockEngineTaskSettings.getProvider(), pluginDateFormatForEcloud.format(revisionTimestamp), numberOfRecords);
    } catch (MCSException e) {
      throw new ExternalTaskException("Getting record list with file content failed.", e);
    }

    // Get the records themselves.
    final List<Record> records = new ArrayList<>(revisionsWithDeletedFlagSetToFalse.size());
    for (CloudTagsResponse cloudTagsResponse : revisionsWithDeletedFlagSetToFalse) {
      final Record eloudXmlRecord = getRecordByEcloudIdAndRevision(cloudTagsResponse.getCloudId(), revisionName, revisionTimestamp);
      if (eloudXmlRecord == null) {
        throw new IllegalStateException("This can't happen: eCloud just told us the record exists");
      }
      records.add(eloudXmlRecord);
    }

    return records;
  }

  @Override
  public List<Record> getRecords(List<String> recordIds, String revisionName, Date revisionTimestamp) throws ExternalTaskException {

    final List<Record> records = new ArrayList<>(recordIds.size());
    for (String recordId : recordIds) {
      Optional.ofNullable(getRecordByEcloudIdAndRevision(recordId, revisionName, revisionTimestamp)).ifPresent(records::add);
    }

    return records;
  }

  @Override
  public Record getRecord(String recordId, String revisionName, Date revisionTimestamp) throws ExternalTaskException {
    String ecloudId = null;
    try {

      if (recordId != null) {
        ecloudId = uisClient.getCloudId(mockEngineTaskSettings.getProvider(), recordId).getId();
      }
    } catch (CloudException e) {
      if (e.getCause() instanceof RecordDoesNotExistException) {
        // The record ID does not exist. Check whether the ID is already an eCloud ID.
        ecloudId = verifyExistenceOfEcloudId(recordId);
      } else {
        // Some other connectivity issue.
        throw new ExternalTaskException(
            String.format("Failed to lookup cloudId for idToSearch: %s", recordId), e);
      }
    }

    // Try to retrieve the record. Note: we need to know if the eCloud ID exists at this point
    // because getRecord() cannot detect non-existing eCloud IDs.
    return ecloudId == null ? null : getRecordByEcloudIdAndRevision(ecloudId, revisionName, revisionTimestamp);
  }

  private Record getRecordByEcloudIdAndRevision(String ecloudId, String revisionName, Date revisionTimestamp) throws ExternalTaskException {

    // Get the representation(s) for the given combination of plugin and record ID.
    final List<Representation> representations;
    try {
      final Revision revision = new Revision(revisionName, mockEngineTaskSettings.getProvider(), revisionTimestamp);
      representations = recordServiceClient.getRepresentationsByRevision(ecloudId,
          MetisPlugin.getRepresentationName(), revision);
    } catch (MCSException e) {
      throw new ExternalTaskException(String.format(
          "Getting record list with file content failed. ecloudId: %s", ecloudId), e);
    }

    // If no representation is found, return null.
    if (representations == null || representations.isEmpty()) {
      return null;
    }
    final Representation representation = representations.getFirst();

    // Perform checks on the file lists.
    if (representation.getFiles() == null || representation.getFiles().isEmpty()) {
      throw new ExternalTaskException(String.format(
          "Expecting one file in the representation, but received none. ecloudId: %s", ecloudId));
    }
    final File file = representation.getFiles().getFirst();

    // Obtain the file contents belonging to this representation version.
    try {
      final InputStream inputStream = fileServiceClient.getFile(file.getContentUri().toString());
      return new Record(ecloudId, IOUtils.toString(inputStream, StandardCharsets.UTF_8.name()));
    } catch (MCSException e) {
      throw new ExternalTaskException("Getting record list with file content failed.", e);
    } catch (IOException e) {
      throw new ExternalTaskException("Problem while reading the contents of the file.", e);
    }
  }

  private String verifyExistenceOfEcloudId(String potentialEcloudId) {
    try {
      return uisClient.getRecordId(potentialEcloudId).getResults().isEmpty() ? null : potentialEcloudId;
    } catch (CloudException e) {
      // TODO currently we can't distinguish between a connection issue and a non-existing eCloud ID.
      //  The client should be changed to allow for this. We assume here that there is not a connection
      //  issue because, where this method is called, we just did a successful call to the UIS service.
      return null;
    }
  }
}
