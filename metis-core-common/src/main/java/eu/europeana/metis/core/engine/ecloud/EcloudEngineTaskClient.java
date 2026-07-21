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
import eu.europeana.cloud.service.dps.DpsTask;
import eu.europeana.cloud.service.dps.exception.AccessDeniedOrObjectDoesNotExistException;
import eu.europeana.cloud.service.dps.exception.DpsException;
import eu.europeana.cloud.service.dps.metis.indexing.TargetIndexingDatabase;
import eu.europeana.metis.core.dataset.Dataset;
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
import eu.europeana.metis.core.workflow.plugins.ExecutablePluginType;
import eu.europeana.metis.exception.ExternalTaskException;
import eu.europeana.metis.exception.UnrecoverableExternalTaskException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * Client for managing and interacting with tasks in the Ecloud processing engine. Handles task creation, submission, monitoring,
 * error reporting, and record operations.
 */
@Slf4j
public class EcloudEngineTaskClient implements EngineTaskClient<EcloudEngineTaskSettings, EcloudEngineTask> {

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
    engineTaskProgress.setExpectedRecords(taskInfo.getExpectedRecords());
    engineTaskProgress.setProcessedRecords(taskInfo.getProcessedRecords());
    engineTaskProgress.setSuccessRecords(taskInfo.getSuccessRecords());
    engineTaskProgress.setFailRecords(taskInfo.getFailRecords());
    engineTaskProgress.setWarningRecords(taskInfo.getWarningRecords());
    engineTaskProgress.setDuplicateRecords(taskInfo.getDuplicateRecords());
    engineTaskProgress.setUnchangedRecords(taskInfo.getUnchangedRecords());
    engineTaskProgress.setExpectedDepublishRecords(taskInfo.getExpectedDepublishRecords());
    engineTaskProgress.setSuccessDepublishRecords(taskInfo.getSuccessDepublishRecords());
    engineTaskProgress.setFailDepublishRecords(taskInfo.getFailDepublishRecords());
    engineTaskProgress.setProcessedDepublishRecords(taskInfo.getProcessedDepublishRecords());

    EngineTaskState engineTaskState = EngineTaskState.valueOf(taskInfo.getEngineTaskState().name());
    engineTaskProgress.setEngineTaskState(engineTaskState);
    engineTaskProgress.setEngineTaskStateInfo(taskInfo.getEngineTaskStateInfo());
    return engineTaskProgress;
  }

  @Override
  public EcloudEngineTaskSettings getEngineTaskSettings() {
    return ecloudEngineTaskSettings;
  }

  @Override
  public EcloudEngineTask createEngineTask(
      Map<EngineTaskKey, String> parameters, InputDataEndpoint inputDataEndpoint, String topologyName)
      throws ExternalTaskException {
    EcloudEngineTaskRequest ecloudEngineTaskRequest = new EcloudEngineTaskRequest(parameters, inputDataEndpoint);
    DpsTask dpsTask;
    try {
      dpsTask = dpsClient.createTask(ecloudEngineTaskRequest.getCreateDpsTaskRequest(), topologyName);
    } catch (DpsException e) {
      throw new ExternalTaskException("Create task in DPS failed", e);
    }
    return new EcloudEngineTask(dpsTask);
  }

  @Override
  public String submitEngineTask(EcloudEngineTask engineTask, String topologyName) throws ExternalTaskException {
    try {
      dpsClient.startTask(topologyName, Long.parseLong(engineTask.getExternalTaskId()));
      return engineTask.getExternalTaskId();
    } catch (DpsException | RuntimeException e) {
      throw new ExternalTaskException("Submitting task to DPS failed", e);
    }
  }

  @Override
  public EngineTaskProgress getEngineTaskProgress(String topologyName, String taskId, ExecutablePluginType pluginType)
      throws ExternalTaskException {
    try {
      TaskInfo taskInfo = dpsClient.getTaskProgress(topologyName, parseLong(taskId));
      log.info("Getting task progress for task id '{}'::{}::=>{}", taskId, topologyName, taskInfo);
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
  public void cancelEngineTask(String topologyName, String taskId, String message, ExecutablePluginType pluginType)
      throws ExternalTaskException {
    try {
      dpsClient.killTask(topologyName, parseLong(taskId), message);
    } catch (DpsException | RuntimeException e) {
      throw new ExternalTaskException("Requesting task cancellation failed", e);
    }
  }

  @Override
  public String createEngineDatasetId(Dataset dataset) {
    return "NOT_REQUIRED_ANYMORE";
  }

  @Override
  public List<Record> getRecords(String engineDatasetId, String batchId, int numberOfRecords) throws ExternalTaskException {
    return ecloudEngineDatasetRecordClient.getRecords(ecloudEngineTaskSettings.getProvider(), batchId, numberOfRecords);
  }

  @Override
  public List<Record> getRecords(List<String> recordIds, String batchId) throws ExternalTaskException {
    return ecloudEngineDatasetRecordClient.getRecords(ecloudEngineTaskSettings.getProvider(), recordIds, batchId);
  }

  @Override
  public Record getRecord(String engineDatasetId, String recordId, String batchId, ExecutablePluginType pluginType) throws ExternalTaskException {
    return ecloudEngineDatasetRecordClient.getRecord(ecloudEngineTaskSettings.getProvider(), recordId, batchId);
  }

  @Override
  public void close() {
    dpsClient.close();
  }
}
