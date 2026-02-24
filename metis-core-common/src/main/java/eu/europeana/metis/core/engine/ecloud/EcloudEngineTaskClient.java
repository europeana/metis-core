package eu.europeana.metis.core.engine.ecloud;

import static java.lang.Long.parseLong;
import static java.lang.String.format;

import eu.europeana.cloud.client.dps.rest.DpsClient;
import eu.europeana.cloud.common.model.dps.ErrorDetails;
import eu.europeana.cloud.common.model.dps.NodeReport;
import eu.europeana.cloud.common.model.dps.StatisticsReport;
import eu.europeana.cloud.common.model.dps.SubTaskInfo;
import eu.europeana.cloud.common.model.dps.TaskErrorsInfo;
import eu.europeana.cloud.common.model.dps.TaskInfo;
import eu.europeana.cloud.service.dps.exception.AccessDeniedOrObjectDoesNotExistException;
import eu.europeana.cloud.service.dps.exception.DpsException;
import eu.europeana.cloud.service.dps.metis.indexing.TargetIndexingDatabase;
import eu.europeana.metis.core.dataset.Dataset;
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
import eu.europeana.metis.core.rest.Record;
import eu.europeana.metis.core.rest.stats.NodePathStatisticsDTO;
import eu.europeana.metis.core.rest.stats.RecordStatisticsDTO;
import eu.europeana.metis.exception.ExternalTaskException;
import eu.europeana.metis.exception.UnrecoverableExternalTaskException;
import eu.europeana.metis.sandbox.common.batch.FullBatchJobType;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client for managing and interacting with tasks in the Ecloud processing engine. Handles task creation, submission, monitoring,
 * error reporting, and record operations.
 */
public class EcloudEngineTaskClient implements EngineTaskClient<EcloudEngineTaskSettings, EcloudEngineTask> {

  private static final Logger LOGGER = LoggerFactory.getLogger(EcloudEngineTaskClient.class);
  private final DpsClient dpsClient;
  private final EcloudEngineTaskSettings ecloudEngineTaskSettings;
  private final EcloudEngineDatasetRecordClient ecloudEngineDatasetRecordClient;

  /**
   * Constructor.
   *
   * @param dpsClient The DpsClient instance used to interact with the DPS service.
   * @param ecloudEngineTaskSettings The settings specific to EcloudEngine tasks.
   * @param ecloudEngineDatasetRecordClient The client used to manage dataset records for EcloudEngine.
   */
  public EcloudEngineTaskClient(DpsClient dpsClient, EcloudEngineTaskSettings ecloudEngineTaskSettings,
      EcloudEngineDatasetRecordClient ecloudEngineDatasetRecordClient) {
    this.dpsClient = dpsClient;
    this.ecloudEngineTaskSettings = ecloudEngineTaskSettings;
    this.ecloudEngineDatasetRecordClient = ecloudEngineDatasetRecordClient;
  }

  private static EngineTaskProgress convertToProcessingEngineTaskProgress(TaskInfo taskInfo) {
    EngineTaskProgress engineTaskProgress = new EngineTaskProgress();
    engineTaskProgress.setExpectedRecords(taskInfo.getExpectedRecordsNumber());
    engineTaskProgress.setProcessedRecords(taskInfo.getProcessedRecordsCount());
    engineTaskProgress.setDeletedRecords(taskInfo.getDeletedRecordsCount());
    engineTaskProgress.setIgnoredRecords(taskInfo.getIgnoredRecordsCount());
    engineTaskProgress.setProcessedErrors(taskInfo.getProcessedErrorsCount());
    engineTaskProgress.setPostProcessedRecordsCount(taskInfo.getPostProcessedRecordsCount());
    engineTaskProgress.setDeletedErrors(taskInfo.getDeletedErrorsCount());
    EngineTaskState engineTaskState = EngineTaskState.valueOf(taskInfo.getState().name());
    engineTaskProgress.setEngineTaskState(engineTaskState);
    engineTaskProgress.setEngineTaskStateInfo(taskInfo.getStateDescription());
    return engineTaskProgress;
  }

  @Override
  public EcloudEngineTaskSettings getEngineTaskSettings() {
    return ecloudEngineTaskSettings;
  }

  @Override
  public EcloudEngineTask createEngineTask(Map<EngineTaskKey, String> parameters, InputDataEndpoint inputDataEndpoint,
      DataRevision outputDataRevision) {
    return new EcloudEngineTask(parameters, inputDataEndpoint, outputDataRevision);
  }

  @Override
  public String submitEngineTask(EcloudEngineTask engineTask, String topologyName) throws ExternalTaskException {
    try {
      long taskId = dpsClient.submitTask(engineTask.toDpsTask(), topologyName);
      return Long.toString(taskId);
    } catch (DpsException | RuntimeException e) {
      throw new ExternalTaskException("Submitting task to DPS failed", e);
    }
  }

  @Override
  public EngineTaskProgress getEngineTaskProgress(String topologyName, String taskId, FullBatchJobType step)
      throws ExternalTaskException {
    try {
      TaskInfo taskInfo = dpsClient.getTaskProgress(topologyName, parseLong(taskId));
      LOGGER.info("Getting task progress for task id '{}'::{}::=>{}", taskId, topologyName, taskInfo);
      return convertToProcessingEngineTaskProgress(taskInfo);
    } catch (AccessDeniedOrObjectDoesNotExistException e) {
      throw new ExternalTaskException("Fetching task progress failed",
          new UnrecoverableExternalTaskException("Access denied or task does not exists!", e));
    } catch (DpsException | RuntimeException e) {
      throw new ExternalTaskException("Fetching task progress failed", e);
    }
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
  public List<DataItemStatus> getDataItemStatuses(String topologyName, String taskId, int from, int to)
      throws ExternalTaskException {
    try {
      List<SubTaskInfo> detailedTaskReportBetweenChunks =
          dpsClient.getDetailedTaskReportBetweenChunks(topologyName, parseLong(taskId), from, to);
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
      throw new ExternalTaskException(format(
          "Getting the task detailed logs failed. topologyName: %s, externalTaskId: %s, from: %s, to: %s",
          topologyName, taskId, from, to), e);
    }
  }

  @Override
  public boolean hasEngineTaskErrorReport(String topologyName, String taskId) throws ExternalTaskException {
    try {
      return dpsClient.checkIfErrorReportExists(topologyName, parseLong(taskId));
    } catch (DpsException e) {
      throw new ExternalTaskException(format(
          "Checking if the error report exists failed. topologyName: %s, externalTaskId: %s", topologyName, taskId), e);
    }
  }

  @Override
  public EngineTaskErrors getEngineTaskErrors(String topologyName, String taskId, int maxEntries)
      throws ExternalTaskException {
    try {
      TaskErrorsInfo taskErrorsInfo = dpsClient.getTaskErrorsReport(topologyName, parseLong(taskId), null, maxEntries);

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
      return new EngineTaskErrors(Long.toString(taskErrorsInfo.getId()), engineTaskErrorInfoList);
    } catch (DpsException e) {
      throw new ExternalTaskException(format(
          "Getting the task error report failed. topologyName: %s, externalTaskId: %s, idsPerError: %s",
          topologyName, taskId, maxEntries), e);
    }
  }

  @Override
  public RecordStatisticsDTO getEngineTaskContentRecordStatistics(String topologyName, String taskId)
      throws ExternalTaskException {
    final StatisticsReport statisticsReport;
    try {
      statisticsReport = dpsClient.getTaskStatisticsReport(topologyName, parseLong(taskId));
      return EcloudEngineRecordStatisticsConverter.compileRecordStatistics(statisticsReport);
    } catch (DpsException e) {
      throw new ExternalTaskException(format(
          "Getting the task statistics failed. topologyName: %s, externalTaskId: %s",
          topologyName, taskId), e);
    }
  }

  @Override
  public NodePathStatisticsDTO getEngineTaskContentNodePathStatistics(String topologyName, String taskId, String nodePath)
      throws ExternalTaskException {
    final List<NodeReport> nodeReports;
    try {
      nodeReports = dpsClient.getElementReport(topologyName, parseLong(taskId), nodePath);
      return EcloudEngineRecordStatisticsConverter.compileNodePathStatistics(nodePath, nodeReports);
    } catch (DpsException e) {
      throw new ExternalTaskException(format(
          "Getting the additional node statistics failed. topologyName: %s, externalTaskId: %s",
          topologyName, taskId), e);
    }
  }

  @Override
  public void cancelEngineTask(String topologyName, String taskId, String message, FullBatchJobType step) throws ExternalTaskException {
    try {
      dpsClient.killTask(topologyName, parseLong(taskId), message);
    } catch (DpsException | RuntimeException e) {
      throw new ExternalTaskException("Requesting task cancellation failed", e);
    }
  }

  @Override
  public boolean createEngineDatasetId(String engineDatasetId) throws ExternalTaskException {
    return ecloudEngineDatasetRecordClient.createEngineDatasetId(ecloudEngineTaskSettings.getProvider(), engineDatasetId);
  }

  @Override
  public String createEngineDatasetId(Dataset dataset) throws ExternalTaskException {
    return null;
  }

  @Override
  public List<Record> getRecords(String engineDatasetId, String representationName, String revisionName, Date revisionTimestamp,
      int numberOfRecords) throws ExternalTaskException {
    return ecloudEngineDatasetRecordClient.getRecords(ecloudEngineTaskSettings.getProvider(), engineDatasetId, representationName,
        revisionName, revisionTimestamp, numberOfRecords);
  }

  @Override
  public List<Record> getRecords(List<String> recordIds, String revisionName, Date revisionTimestamp)
      throws ExternalTaskException {
    return ecloudEngineDatasetRecordClient.getRecords(ecloudEngineTaskSettings.getProvider(), recordIds, revisionName,
        revisionTimestamp);
  }

  @Override
  public Record getRecord(String recordId, String revisionName, Date revisionTimestamp) throws ExternalTaskException {
    return ecloudEngineDatasetRecordClient.getRecord(ecloudEngineTaskSettings.getProvider(), recordId, revisionName,
        revisionTimestamp);
  }

  @Override
  public void close() {
    dpsClient.close();
  }
}
