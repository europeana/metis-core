package eu.europeana.metis.core.engine.mock;

import eu.europeana.cloud.client.dps.rest.DpsClient;
import eu.europeana.cloud.common.model.dps.ErrorDetails;
import eu.europeana.cloud.common.model.dps.NodeReport;
import eu.europeana.cloud.common.model.dps.StatisticsReport;
import eu.europeana.cloud.common.model.dps.SubTaskInfo;
import eu.europeana.cloud.common.model.dps.TaskErrorsInfo;
import eu.europeana.cloud.common.model.dps.TaskInfo;
import eu.europeana.cloud.service.dps.exception.DpsException;
import eu.europeana.cloud.service.dps.metis.indexing.TargetIndexingDatabase;
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
import eu.europeana.metis.core.engine.ecloud.EcloudEngineDatasetRecordClient;
import eu.europeana.metis.core.engine.ecloud.EcloudEngineRecordStatisticsConverter;
import eu.europeana.metis.core.rest.Record;
import eu.europeana.metis.core.rest.stats.NodePathStatisticsDTO;
import eu.europeana.metis.core.rest.stats.RecordStatisticsDTO;
import eu.europeana.metis.exception.ExternalTaskException;
import eu.europeana.metis.exception.UnrecoverableExternalTaskException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Client for managing and interacting with tasks in the Mock processing engine. Handles task creation, submission, monitoring,
 * error reporting, and record operations.
 */
public class MockEngineTaskClient implements EngineTaskClient<MockEngineTaskSettings, MockEngineTask> {

  private final DpsClient dpsClient;
  private final MockEngineTaskSettings mockEngineTaskSettings;
  private final EcloudEngineDatasetRecordClient ecloudEngineDatasetRecordClient;

  /**
   * Constructor.
   *
   * @param dpsClient The DpsClient instance used to interact with the DPS service.
   * @param mockEngineTaskSettings The settings specific to Mock engine tasks.
   * @param ecloudEngineDatasetRecordClient The client used to manage dataset records for a mock engine.
   */
  public MockEngineTaskClient(DpsClient dpsClient, MockEngineTaskSettings mockEngineTaskSettings,
      EcloudEngineDatasetRecordClient ecloudEngineDatasetRecordClient) {
    this.dpsClient = dpsClient;
    this.ecloudEngineDatasetRecordClient = ecloudEngineDatasetRecordClient;
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
  public EngineTaskProgress getEngineTaskProgress(String topologyName, long taskId) throws ExternalTaskException {
    try {
      TaskInfo taskInfo = dpsClient.getTaskProgress(topologyName, taskId);
      return convertToProcessingEngineTaskProgress(taskInfo);
    } catch (DpsException e) {
      throw new ExternalTaskException("Fetching task progress failed",
          new UnrecoverableExternalTaskException("Fetching task progress failed", e));
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
    return ecloudEngineDatasetRecordClient.createEngineDatasetId(mockEngineTaskSettings.getProvider(), datasetId);
  }

  @Override
  public List<Record> getRecords(String datasetId, String representationName, String revisionName, Date revisionTimestamp,
      int numberOfRecords) throws ExternalTaskException {
    return ecloudEngineDatasetRecordClient.getRecords(mockEngineTaskSettings.getProvider(), datasetId, representationName,
        revisionName, revisionTimestamp, numberOfRecords);
  }

  @Override
  public List<Record> getRecords(List<String> recordIds, String revisionName, Date revisionTimestamp)
      throws ExternalTaskException {
    return ecloudEngineDatasetRecordClient.getRecords(mockEngineTaskSettings.getProvider(), recordIds, revisionName,
        revisionTimestamp);
  }

  @Override
  public Record getRecord(String recordId, String revisionName, Date revisionTimestamp) throws ExternalTaskException {
    return ecloudEngineDatasetRecordClient.getRecord(mockEngineTaskSettings.getProvider(), recordId, revisionName,
        revisionTimestamp);
  }
}
