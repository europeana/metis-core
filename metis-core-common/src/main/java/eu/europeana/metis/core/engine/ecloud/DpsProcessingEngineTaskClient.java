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
import eu.europeana.metis.core.engine.base.ProcessingEngineTaskClient;
import eu.europeana.metis.core.engine.base.report.item.DataItemState;
import eu.europeana.metis.core.engine.base.report.item.DataItemStatus;
import eu.europeana.metis.core.engine.base.report.item.content.ContentAttributeStatistics;
import eu.europeana.metis.core.engine.base.report.item.content.ContentNodeReport;
import eu.europeana.metis.core.engine.base.report.item.content.ContentNodeStatistics;
import eu.europeana.metis.core.engine.base.report.item.content.ContentStatisticsReport;
import eu.europeana.metis.core.engine.base.report.task.ProcessingEngineTaskErrorDetails;
import eu.europeana.metis.core.engine.base.report.task.ProcessingEngineTaskErrorInfo;
import eu.europeana.metis.core.engine.base.report.task.ProcessingEngineTaskErrors;
import eu.europeana.metis.core.engine.base.report.task.ProcessingEngineTaskProgress;
import eu.europeana.metis.core.workflow.plugins.AbstractExecutablePlugin;
import eu.europeana.metis.exception.ExternalTaskException;
import eu.europeana.metis.exception.UnrecoverableExternalTaskException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class DpsProcessingEngineTaskClient implements ProcessingEngineTaskClient<DpsProcessingEngineTaskSettings, DpsProcessingEngineTask> {

  private final DpsClient dpsClient;
  private final DpsProcessingEngineTaskSettings dpsProcessingEngineTaskProcessing;

  public DpsProcessingEngineTaskClient(DpsClient dpsClient,
      DpsProcessingEngineTaskSettings dpsProcessingEngineTaskProcessing) {
    this.dpsClient = dpsClient;
    this.dpsProcessingEngineTaskProcessing = dpsProcessingEngineTaskProcessing;
  }

  @Override
  public DpsProcessingEngineTaskSettings getProcessingEngineTaskSettings() {
    return dpsProcessingEngineTaskProcessing;
  }

  @Override
  public Supplier<DpsProcessingEngineTask> getTaskCreator() {
    return DpsProcessingEngineTask::new;
  }

  @Override
  public long submitTask(DpsProcessingEngineTask externalTask, String topologyName) throws ExternalTaskException {
    try {
      return dpsClient.submitTask(externalTask.toDpsTask(), topologyName);
    } catch (DpsException | RuntimeException e) {
      throw new ExternalTaskException("Submitting task to DPS failed", e);
    }
  }

  @Override
  public ProcessingEngineTaskProgress getTaskProgress(String topologyName, long taskId)
      throws ExternalTaskException, UnrecoverableExternalTaskException {
    try {
      TaskInfo taskInfo = dpsClient.getTaskProgress(topologyName, taskId);
      return AbstractExecutablePlugin.getExternalTaskProgress(taskInfo);
    } catch (DpsException e) {
      throw new UnrecoverableExternalTaskException("Fetching task progress failed", e);
    } catch (RuntimeException e) {
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
  public List<DataItemStatus> getExternalRecordStatuses(String topologyName, long taskId, int from, int to)
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
  public ProcessingEngineTaskErrors getTaskErrorReport(String topologyName, long taskId, String error, int idsCount)
      throws ExternalTaskException {
    try {
      TaskErrorsInfo taskErrorsInfo = dpsClient.getTaskErrorsReport(topologyName, taskId, null, idsCount);

      List<ProcessingEngineTaskErrorInfo> processingEngineTaskErrorInfoList = taskErrorsInfo.getErrors().stream().map(taskErrorInfo -> {
        List<ProcessingEngineTaskErrorDetails> processingEngineTaskErrorDetailsList = new ArrayList<>();
        for (ErrorDetails errorDetail : taskErrorInfo.getErrorDetails()) {
          ProcessingEngineTaskErrorDetails processingEngineTaskErrorDetails = new ProcessingEngineTaskErrorDetails(errorDetail.getIdentifier(),
              errorDetail.getAdditionalInfo());
          processingEngineTaskErrorDetailsList.add(processingEngineTaskErrorDetails);
        }
        return new ProcessingEngineTaskErrorInfo(taskErrorInfo.getErrorType(), taskErrorInfo.getMessage(),
            taskErrorInfo.getOccurrences(), processingEngineTaskErrorDetailsList);
      }).toList();
      return new ProcessingEngineTaskErrors(taskErrorsInfo.getId(), processingEngineTaskErrorInfoList);
    } catch (DpsException e) {
      throw new ExternalTaskException(String.format(
          "Getting the task error report failed. topologyName: %s, externalTaskId: %s, idsPerError: %s",
          topologyName, taskId, idsCount), e);
    }
  }

  @Override
  public ContentStatisticsReport getTaskStatisticsReport(String topologyName, long taskId)
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
  public List<ContentNodeReport> getElementReport(String topologyName, long taskId, String elementPath)
      throws ExternalTaskException {
    final List<NodeReport> nodeReports;
    try {
      nodeReports = dpsClient.getElementReport(topologyName, taskId, elementPath);
      List<ContentNodeReport> contentNodeReportList = new ArrayList<>();
      for (NodeReport nodeReport : nodeReports) {
        List<ContentAttributeStatistics> contentAttributeStatisticsList = new ArrayList<>();
        for (AttributeStatistics attributeStatistics : nodeReport.getAttributeStatistics()){
          ContentAttributeStatistics contentAttributeStatistics = new ContentAttributeStatistics(attributeStatistics.getName(),
              attributeStatistics.getValue(), attributeStatistics.getOccurrence());
          contentAttributeStatisticsList.add(contentAttributeStatistics);
        }
        ContentNodeReport contentNodeReport = new ContentNodeReport(nodeReport.getNodeValue(), nodeReport.getOccurrence(),
            contentAttributeStatisticsList);
        contentNodeReportList.add(contentNodeReport);
      }
      return contentNodeReportList;
    } catch (DpsException e) {
      throw new ExternalTaskException(String.format(
          "Getting the additional node statistics failed. topologyName: %s, externalTaskId: %s",
          topologyName, taskId), e);
    }
  }

  @Override
  public void cancel(String topologyName, long taskId, String message) throws ExternalTaskException {
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
