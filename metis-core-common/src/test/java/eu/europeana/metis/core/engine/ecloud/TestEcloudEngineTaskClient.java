package eu.europeana.metis.core.engine.ecloud;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europeana.cloud.client.dps.rest.DpsClient;
import eu.europeana.cloud.common.model.dps.ErrorDetails;
import eu.europeana.cloud.common.model.dps.NodeReport;
import eu.europeana.cloud.common.model.dps.RecordState;
import eu.europeana.cloud.common.model.dps.StatisticsReport;
import eu.europeana.cloud.common.model.dps.SubTaskInfo;
import eu.europeana.cloud.common.model.dps.TaskErrorInfo;
import eu.europeana.cloud.common.model.dps.TaskErrorsInfo;
import eu.europeana.cloud.common.model.dps.TaskInfo;
import eu.europeana.cloud.common.model.dps.TaskState;
import eu.europeana.cloud.service.dps.DpsTask;
import eu.europeana.cloud.service.dps.exception.DpsException;
import eu.europeana.cloud.service.dps.metis.indexing.TargetIndexingDatabase;
import eu.europeana.metis.core.engine.base.DataRevision;
import eu.europeana.metis.core.engine.base.EngineTaskKey;
import eu.europeana.metis.core.engine.base.IndexDatabase;
import eu.europeana.metis.core.engine.base.item.report.DataItemState;
import eu.europeana.metis.core.engine.base.item.report.DataItemStatus;
import eu.europeana.metis.core.engine.base.task.input.InputDataEndpoint;
import eu.europeana.metis.core.engine.base.task.input.InternalInputDataEndpoint;
import eu.europeana.metis.core.engine.base.task.report.EngineTaskErrorInfo;
import eu.europeana.metis.core.engine.base.task.report.EngineTaskErrors;
import eu.europeana.metis.core.engine.base.task.report.EngineTaskProgress;
import eu.europeana.metis.core.engine.base.task.report.EngineTaskState;
import eu.europeana.metis.core.rest.Record;
import eu.europeana.metis.core.rest.stats.NodePathStatisticsDTO;
import eu.europeana.metis.core.rest.stats.RecordStatisticsDTO;
import eu.europeana.metis.core.workflow.plugins.ThrottlingValues;
import eu.europeana.metis.exception.ExternalTaskException;
import eu.europeana.metis.exception.UnrecoverableExternalTaskException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TestEcloudEngineTaskClient {

  @Mock
  private DpsClient dpsClient;

  @Mock
  private EcloudEngineDatasetRecordClient ecloudEngineDatasetRecordClient;
  private EcloudEngineTaskSettings ecloudEngineTaskSettings;
  private EcloudEngineTaskClient ecloudEngineTaskClient;

  @BeforeEach
  void setup() {
    ecloudEngineTaskSettings = new EcloudEngineTaskSettings("http://base.url", "provider", "metisCoreBaseUrl",
        new ThrottlingValues(1, 2, 3));
    ecloudEngineTaskClient = new EcloudEngineTaskClient(dpsClient, ecloudEngineTaskSettings, ecloudEngineDatasetRecordClient);
  }

  @Test
  void getEngineTaskSettings_returnsSettings() {
    assertEquals(ecloudEngineTaskSettings, ecloudEngineTaskClient.getEngineTaskSettings());
  }

  @Test
  void createEngineTask() {
    Map<EngineTaskKey, String> parameters = Map.of();
    InputDataEndpoint inputDataEndpoint = new InternalInputDataEndpoint("http://internal.url",
        new DataRevision("name", "provider", new Date(), false));
    DataRevision outputDataRevision = new DataRevision("name", "provider", new Date(), false);
    EcloudEngineTask ecloudEngineTask = ecloudEngineTaskClient.createEngineTask(parameters, inputDataEndpoint,
        outputDataRevision);
    assertNotNull(ecloudEngineTask);
  }

  @Test
  void submitEngineTask() throws Exception {
    EcloudEngineTask ecloudEngineTask = mock(EcloudEngineTask.class);
    when(ecloudEngineTask.toDpsTask()).thenReturn(new DpsTask());
    when(dpsClient.submitTask(any(), eq("topologyName"))).thenReturn(123L);
    String taskId = ecloudEngineTaskClient.submitEngineTask(ecloudEngineTask, "topologyName");
    assertEquals("123", taskId);
  }

  @Test
  void submitEngineTask_throws() throws DpsException {
    EcloudEngineTask ecloudEngineTask = mock(EcloudEngineTask.class);
    when(ecloudEngineTask.toDpsTask()).thenReturn(new DpsTask());
    when(dpsClient.submitTask(any(), any())).thenThrow(new DpsException(""));
    assertThrows(ExternalTaskException.class, () -> ecloudEngineTaskClient.submitEngineTask(ecloudEngineTask, "topologyName"));
  }

  @Test
  void submitEngineTask_throwsRuntimeException() throws DpsException {
    EcloudEngineTask ecloudEngineTask = mock(EcloudEngineTask.class);
    when(ecloudEngineTask.toDpsTask()).thenReturn(new DpsTask());
    when(dpsClient.submitTask(any(), any())).thenThrow(new RuntimeException(""));
    assertThrows(ExternalTaskException.class, () -> ecloudEngineTaskClient.submitEngineTask(ecloudEngineTask, "topologyName"));
  }

  @Test
  void getEngineTaskProgress() throws Exception {
    TaskInfo taskInfo = new TaskInfo();
    taskInfo.setExpectedRecordsNumber(100);
    taskInfo.setProcessedRecordsCount(100);
    taskInfo.setDeletedRecordsCount(1);
    taskInfo.setIgnoredRecordsCount(2);
    taskInfo.setProcessedErrorsCount(3);
    taskInfo.setDeletedErrorsCount(5);
    taskInfo.setState(TaskState.PROCESSED);
    taskInfo.setStateDescription("stateDescription");

    when(dpsClient.getTaskProgress("topologyName", 1L)).thenReturn(taskInfo);

    EngineTaskProgress engineTaskProgress = ecloudEngineTaskClient.getEngineTaskProgress("topologyName", "1");

    assertEquals(taskInfo.getExpectedRecordsNumber(), engineTaskProgress.getExpectedRecords());
    assertEquals(taskInfo.getProcessedRecordsCount(), engineTaskProgress.getProcessedRecords());
    assertEquals(taskInfo.getDeletedRecordsCount(), engineTaskProgress.getDeletedRecords());
    assertEquals(taskInfo.getIgnoredRecordsCount(), engineTaskProgress.getIgnoredRecords());
    assertEquals(taskInfo.getProcessedErrorsCount(), engineTaskProgress.getProcessedErrors());
    assertEquals(taskInfo.getDeletedErrorsCount(), engineTaskProgress.getDeletedErrors());
    assertEquals(EngineTaskState.valueOf(taskInfo.getState().name()), engineTaskProgress.getEngineTaskState());
    assertEquals(taskInfo.getStateDescription(), engineTaskProgress.getEngineTaskStateInfo());
  }

  @Test
  void getEngineTaskProgress_throws() throws Exception {
    when(dpsClient.getTaskProgress("topologyName", 1L)).thenThrow(new DpsException(""));
    ExternalTaskException externalTaskException = assertThrows(ExternalTaskException.class,
        () -> ecloudEngineTaskClient.getEngineTaskProgress("topologyName", "1"));

    Throwable cause = externalTaskException.getCause();
    assertNotNull(cause);
    assertInstanceOf(UnrecoverableExternalTaskException.class, cause);
  }

  @Test
  void getEngineTaskProgress_throwsRuntimeException() throws DpsException {
    when(dpsClient.getTaskProgress("topologyName", 1L)).thenThrow(new RuntimeException(""));
    ;
    assertThrows(ExternalTaskException.class, () -> ecloudEngineTaskClient.getEngineTaskProgress("topologyName", "1"));
  }

  @Test
  void getTotalIndexedRecords() throws Exception {
    when(dpsClient.getTotalMetisDatabaseRecords("datasetId", TargetIndexingDatabase.PUBLISH)).thenReturn(100L);
    long totalIndexedRecords = ecloudEngineTaskClient.getTotalIndexedRecords("datasetId", IndexDatabase.PUBLISH);
    assertEquals(100L, totalIndexedRecords);
  }

  @Test
  void getTotalIndexedRecords_throws() throws DpsException {
    when(dpsClient.getTotalMetisDatabaseRecords("datasetId", TargetIndexingDatabase.PUBLISH)).thenThrow(new DpsException(""));
    assertThrows(ExternalTaskException.class,
        () -> ecloudEngineTaskClient.getTotalIndexedRecords("datasetId", IndexDatabase.PUBLISH));
  }

  @Test
  void getPublishedRecords() throws Exception {
    List<String> recordIds = List.of("record1", "record2");
    when(dpsClient.searchPublishedDatasetRecords("datasetId", recordIds)).thenReturn(recordIds);
    assertEquals(recordIds, ecloudEngineTaskClient.getPublishedRecords("datasetId", recordIds));
  }

  @Test
  void getPublishedRecords_throws() throws DpsException {
    List<String> recordIds = List.of("record1", "record2");
    when(dpsClient.searchPublishedDatasetRecords("datasetId", recordIds)).thenThrow(new DpsException(""));
    assertThrows(ExternalTaskException.class, () -> ecloudEngineTaskClient.getPublishedRecords("datasetId", recordIds));
  }

  @Test
  void getDataItemStatuses() throws Exception {
    SubTaskInfo subTaskInfo = new SubTaskInfo();
    subTaskInfo.setResourceNum(1);
    subTaskInfo.setResource("resource");
    subTaskInfo.setRecordState(RecordState.SUCCESS);
    subTaskInfo.setInfo("info");
    subTaskInfo.setEuropeanaId("europeanaId");
    subTaskInfo.setProcessingTime(100L);
    subTaskInfo.setResultResource("resultResource");

    when(dpsClient.getDetailedTaskReportBetweenChunks("topologyName", 1L, 0, 1)).thenReturn(singletonList(subTaskInfo));
    List<DataItemStatus> dataItemStatuses = ecloudEngineTaskClient.getDataItemStatuses("topologyName", "1", 0, 1);
    assertEquals(1, dataItemStatuses.size());
    DataItemStatus dataItemStatus = dataItemStatuses.getFirst();
    assertEquals(subTaskInfo.getResourceNum(), dataItemStatus.resourceNum());
    assertEquals(subTaskInfo.getResource(), dataItemStatus.resource());
    assertEquals(DataItemState.valueOf(subTaskInfo.getRecordState().name()), dataItemStatus.dataItemState());
    assertEquals(subTaskInfo.getInfo(), dataItemStatus.info());
    assertEquals(subTaskInfo.getEuropeanaId(), dataItemStatus.europeanaId());
    assertEquals(subTaskInfo.getProcessingTime(), dataItemStatus.processingTime());
    assertEquals(subTaskInfo.getResultResource(), dataItemStatus.resultResource());
  }

  @Test
  void getDataItemStatuses_throws() throws DpsException {
    when(dpsClient.getDetailedTaskReportBetweenChunks("topologyName", 1L, 0, 1)).thenThrow(new DpsException(""));
    assertThrows(ExternalTaskException.class, () -> ecloudEngineTaskClient.getDataItemStatuses("topologyName", "1", 0, 1));
  }

  @Test
  void hasEngineTaskErrorReport() throws Exception {
    when(dpsClient.checkIfErrorReportExists("topologyName", 1L)).thenReturn(true);
    assertTrue(ecloudEngineTaskClient.hasEngineTaskErrorReport("topologyName", "1"));
  }

  @Test
  void hasEngineTaskErrorReport_throws() throws DpsException {
    when(dpsClient.checkIfErrorReportExists("topologyName", 1L)).thenThrow(new DpsException(""));
    assertThrows(ExternalTaskException.class, () -> ecloudEngineTaskClient.hasEngineTaskErrorReport("topologyName", "1"));
  }

  @Test
  void getEngineTaskErrors() throws Exception {
    TaskErrorsInfo taskErrorsInfo = new TaskErrorsInfo();
    taskErrorsInfo.setId(1L);
    TaskErrorInfo taskErrorInfo = new TaskErrorInfo();
    taskErrorInfo.setErrorType("errorType");
    taskErrorInfo.setMessage("message");
    taskErrorInfo.setOccurrences(1);
    ErrorDetails errorDetails = new ErrorDetails();
    errorDetails.setIdentifier("identifier");
    errorDetails.setAdditionalInfo("additionalInfo");
    taskErrorInfo.setErrorDetails(List.of(errorDetails));
    taskErrorsInfo.setErrors(List.of(taskErrorInfo));

    when(dpsClient.getTaskErrorsReport("topologyName", 1L, null, 100)).thenReturn(taskErrorsInfo);
    EngineTaskErrors engineTaskErrors = ecloudEngineTaskClient.getEngineTaskErrors("topologyName", "1", 100);

    assertEquals("1", engineTaskErrors.id());
    assertEquals(1, engineTaskErrors.errors().size());
    EngineTaskErrorInfo engineTaskErrorInfo = engineTaskErrors.errors().getFirst();
    assertEquals(taskErrorInfo.getErrorType(), engineTaskErrorInfo.errorType());
    assertEquals(taskErrorInfo.getMessage(), engineTaskErrorInfo.message());
    assertEquals(taskErrorInfo.getOccurrences(), engineTaskErrorInfo.occurrences());
    assertEquals(errorDetails.getIdentifier(), engineTaskErrorInfo.errorDetails().getFirst().identifier());
    assertEquals(errorDetails.getAdditionalInfo(), engineTaskErrorInfo.errorDetails().getFirst().additionalInfo());
  }

  @Test
  void getEngineTaskErrors_throws() throws DpsException {
    when(dpsClient.getTaskErrorsReport("topologyName", 1L, null, 100)).thenThrow(new DpsException(""));
    assertThrows(ExternalTaskException.class, () -> ecloudEngineTaskClient.getEngineTaskErrors("topologyName", "1", 100));
  }

  @Test
  void getEngineTaskContentRecordStatistics() throws Exception {
    StatisticsReport statisticsReport = new StatisticsReport();

    when(dpsClient.getTaskStatisticsReport("topologyName", 1L)).thenReturn(statisticsReport);
    RecordStatisticsDTO recordStatisticsDTO = ecloudEngineTaskClient.getEngineTaskContentRecordStatistics("topologyName", "1");
    assertEquals("0", recordStatisticsDTO.taskId());
    assertEquals(0, recordStatisticsDTO.nodePathStatistics().size());
  }

  @Test
  void getEngineTaskContentRecordStatistics_throws() throws DpsException {
    when(dpsClient.getTaskStatisticsReport("topologyName", 1L)).thenThrow(new DpsException(""));
    assertThrows(ExternalTaskException.class,
        () -> ecloudEngineTaskClient.getEngineTaskContentRecordStatistics("topologyName", "1"));
  }

  @Test
  void getEngineTaskContentNodePathStatistics() throws Exception {
    List<NodeReport> nodeReports = List.of();
    when(dpsClient.getElementReport("topologyName", 1L, "xPath")).thenReturn(nodeReports);
    NodePathStatisticsDTO nodePathStatisticsDTO = ecloudEngineTaskClient.getEngineTaskContentNodePathStatistics(
        "topologyName", "1", "xPath");
    assertEquals("xPath", nodePathStatisticsDTO.xPath());
    assertEquals(0, nodePathStatisticsDTO.nodeValueStatistics().size());
  }

  @Test
  void getEngineTaskContentNodePathStatistics_throws() throws DpsException {
    when(dpsClient.getElementReport("topologyName", 1L, "xPath")).thenThrow(new DpsException(""));
    assertThrows(ExternalTaskException.class, () -> ecloudEngineTaskClient.getEngineTaskContentNodePathStatistics(
        "topologyName", "1", "xPath"));
  }

  @Test
  void cancelEngineTask_success() throws Exception {
    ecloudEngineTaskClient.cancelEngineTask("topologyName", "1", "");
    verify(dpsClient).killTask("topologyName", 1L, "");
  }

  @Test
  void cancelEngineTask_throws() throws DpsException {
    when(dpsClient.killTask("topologyName", 1L, "")).thenThrow(new DpsException(""));
    assertThrows(ExternalTaskException.class, () -> ecloudEngineTaskClient.cancelEngineTask("topologyName", "1", ""));
  }

  @Test
  void cancelEngineTask_throwsRuntimeException() throws DpsException {
    when(dpsClient.killTask("topologyName", 1L, "")).thenThrow(new DpsException(""));
    assertThrows(ExternalTaskException.class, () -> ecloudEngineTaskClient.cancelEngineTask("topologyName", "1", ""));
  }

  @Test
  void createEngineDatasetId() throws ExternalTaskException {
    when(ecloudEngineDatasetRecordClient.createEngineDatasetId(ecloudEngineTaskSettings.getProvider(), "datasetId")).thenReturn(
        true);
    assertTrue(ecloudEngineTaskClient.createEngineDatasetId("datasetId"));
  }

  @Test
  void getRecords_withDatasetId() throws ExternalTaskException {
    Date now = new Date();
    List<Record> records = List.of(new Record());
    when(ecloudEngineDatasetRecordClient.getRecords(ecloudEngineTaskSettings.getProvider(), "datasetId", "representationName",
        "revisionName", now, 1)).thenReturn(records);
    List<Record> recordsResult = ecloudEngineTaskClient.getRecords("datasetId", "representationName", "revisionName", now, 1);
    assertEquals(records, recordsResult);
  }

  @Test
  void getRecords_fromIds() throws ExternalTaskException {
    Date now = new Date();
    List<Record> records = List.of(new Record());
    when(ecloudEngineDatasetRecordClient.getRecords(ecloudEngineTaskSettings.getProvider(), List.of("recordId1"), "revisionName",
        now)).thenReturn(records);
    assertEquals(records, ecloudEngineTaskClient.getRecords(List.of("recordId1"), "revisionName", now));
  }

  @Test
  void getRecord() throws ExternalTaskException {
    Date now = new Date();
    Record records = new Record();
    when(ecloudEngineDatasetRecordClient.getRecord(ecloudEngineTaskSettings.getProvider(), "recordId", "revisionName",
        now)).thenReturn(records);
    assertEquals(records, ecloudEngineTaskClient.getRecord("recordId", "revisionName", now));
  }

  @Test
  void close() {
    ecloudEngineTaskClient.close();
    verify(dpsClient).close();
  }

}