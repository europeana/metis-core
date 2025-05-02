package eu.europeana.metis.core.engine.ecloud;

import eu.europeana.cloud.client.dps.rest.DpsClient;
import eu.europeana.cloud.common.model.dps.AttributeStatistics;
import eu.europeana.cloud.common.model.dps.ErrorDetails;
import eu.europeana.cloud.common.model.dps.NodeReport;
import eu.europeana.cloud.common.model.dps.NodeStatistics;
import eu.europeana.cloud.common.model.dps.RecordState;
import eu.europeana.cloud.common.model.dps.StatisticsReport;
import eu.europeana.cloud.common.model.dps.SubTaskInfo;
import eu.europeana.cloud.common.model.dps.TaskErrorsInfo;
import eu.europeana.cloud.common.model.dps.TaskInfo;
import eu.europeana.cloud.service.dps.exception.DpsException;
import eu.europeana.cloud.service.dps.metis.indexing.TargetIndexingDatabase;
import eu.europeana.metis.core.engine.base.IndexDatabase;
import eu.europeana.metis.core.engine.base.EngineTaskClient;
import eu.europeana.metis.core.engine.base.report.item.DataItemState;
import eu.europeana.metis.core.engine.base.report.item.DataItemStatus;
import eu.europeana.metis.core.engine.base.report.item.content.ContentAttributeStatistics;
import eu.europeana.metis.core.engine.base.report.item.content.ContentNodeReport;
import eu.europeana.metis.core.engine.base.report.item.content.ContentNodeStatistics;
import eu.europeana.metis.core.engine.base.report.item.content.ContentStatisticsReport;
import eu.europeana.metis.core.engine.base.report.task.EngineTaskErrorDetails;
import eu.europeana.metis.core.engine.base.report.task.EngineTaskErrorInfo;
import eu.europeana.metis.core.engine.base.report.task.EngineTaskErrors;
import eu.europeana.metis.core.engine.base.report.task.EngineTaskProgress;
import eu.europeana.metis.core.engine.base.report.task.EngineTaskState;
import eu.europeana.metis.exception.ExternalTaskException;
import eu.europeana.metis.exception.UnrecoverableExternalTaskException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class DpsEngineTaskClient implements
    EngineTaskClient<DpsEngineTaskSettings, DpsEngineTask> {

  private final DpsClient dpsClient;
  private final DpsEngineTaskSettings dpsEngineTaskSettings;

  public DpsEngineTaskClient(DpsClient dpsClient,
      DpsEngineTaskSettings dpsEngineTaskSettings) {
    this.dpsClient = dpsClient;
    this.dpsEngineTaskSettings = dpsEngineTaskSettings;
  }

  @Override
  public DpsEngineTaskSettings getEngineTaskSettings() {
    return dpsEngineTaskSettings;
  }

  @Override
  public Supplier<DpsEngineTask> getEngineTaskCreator() {
    return DpsEngineTask::new;
  }

  @Override
  public long submitEngineTask(DpsEngineTask engineTask, String topologyName) throws ExternalTaskException {
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
  public Map<String, Boolean> getRecordStatus(String topologyName, long taskId, int from, int to) throws ExternalTaskException {
    try {
      List<SubTaskInfo> detailedTaskReportBetweenChunks =
          dpsClient.getDetailedTaskReportBetweenChunks(topologyName, taskId, from, to);
      return detailedTaskReportBetweenChunks.stream()
                                            .collect(Collectors.toMap(SubTaskInfo::getResource,
                                                subTaskInfo -> subTaskInfo.getRecordState().equals(RecordState.SUCCESS)));
    } catch (DpsException e) {
      throw new ExternalTaskException("Retrieve records status failed", e);
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
  public boolean hasErrorReport(String topologyName, long taskId) throws ExternalTaskException {
    try {
      return dpsClient.checkIfErrorReportExists(topologyName, taskId);
    } catch (DpsException e) {
      throw new ExternalTaskException(String.format(
          "Checking if the error report exists failed. topologyName: %s, externalTaskId: %s", topologyName, taskId), e);
    }
  }

  @Override
  public EngineTaskErrors getEngineTaskErrors(String topologyName, long taskId, String error, int idsCount)
      throws ExternalTaskException {
    try {
      TaskErrorsInfo taskErrorsInfo = dpsClient.getTaskErrorsReport(topologyName, taskId, null, idsCount);

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
          topologyName, taskId, idsCount), e);
    }
  }

  @Override
  public ContentStatisticsReport getEngineTaskContentStatisticsReport(String topologyName, long taskId)
      throws ExternalTaskException {
    final StatisticsReport statisticsReport;
    try {
      statisticsReport = dpsClient.getTaskStatisticsReport(topologyName, taskId);
      List<ContentNodeStatistics> contentNodeStatisticsList = new ArrayList<>();
      for (NodeStatistics nodeStatistics : statisticsReport.getNodeStatistics()) {
        Set<ContentAttributeStatistics> contentAttributeStatisticsList = new HashSet<>();
        for (AttributeStatistics attributeStatistics : nodeStatistics.getAttributesStatistics()) {
          contentAttributeStatisticsList.add(
              new ContentAttributeStatistics(attributeStatistics.getName(), attributeStatistics.getValue(),
                  attributeStatistics.getOccurrence())
          );
        }
        contentNodeStatisticsList.add(
            new ContentNodeStatistics(nodeStatistics.getParentXpath(), nodeStatistics.getXpath(), nodeStatistics.getValue(),
                nodeStatistics.getOccurrence(), contentAttributeStatisticsList)
        );
      }
      return new ContentStatisticsReport(taskId, contentNodeStatisticsList);
    } catch (DpsException e) {
      throw new ExternalTaskException(String.format(
          "Getting the task statistics failed. topologyName: %s, externalTaskId: %s",
          topologyName, taskId), e);
    }
  }

  @Override
  public List<ContentNodeReport> getContentNodeReport(String topologyName, long taskId, String nodePath)
      throws ExternalTaskException {
    final List<NodeReport> nodeReports;
    try {
      nodeReports = dpsClient.getElementReport(topologyName, taskId, nodePath);
      List<ContentNodeReport> contentNodeReportList = new ArrayList<>();
      for (NodeReport nodeReport : nodeReports) {
        ContentNodeReport contentNodeReport = getContentNodeReport(nodeReport);
        contentNodeReportList.add(contentNodeReport);
      }
      return contentNodeReportList;
    } catch (DpsException e) {
      throw new ExternalTaskException(String.format(
          "Getting the additional node statistics failed. topologyName: %s, externalTaskId: %s",
          topologyName, taskId), e);
    }
  }

  private static ContentNodeReport getContentNodeReport(NodeReport nodeReport) {
    List<ContentAttributeStatistics> contentAttributeStatisticsList = new ArrayList<>();
    for (AttributeStatistics attributeStatistics : nodeReport.getAttributeStatistics()) {
      ContentAttributeStatistics contentAttributeStatistics = new ContentAttributeStatistics(attributeStatistics.getName(),
          attributeStatistics.getValue(), attributeStatistics.getOccurrence());
      contentAttributeStatisticsList.add(contentAttributeStatistics);
    }
    return new ContentNodeReport(nodeReport.getNodeValue(), nodeReport.getOccurrence(), contentAttributeStatisticsList);
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
}
