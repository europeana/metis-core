package eu.europeana.metis.core.engine.ecloud;

import static eu.europeana.cloud.common.model.dps.EngineTaskState.PROCESSED;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
import eu.europeana.cloud.service.dps.DpsTask;
import eu.europeana.cloud.service.dps.exception.AccessDeniedOrObjectDoesNotExistException;
import eu.europeana.cloud.service.dps.exception.DpsException;
import eu.europeana.cloud.service.dps.metis.indexing.TargetIndexingDatabase;
import eu.europeana.metis.core.dataset.Dataset;
import eu.europeana.metis.core.engine.base.DataRevision;
import eu.europeana.metis.core.engine.base.EngineTaskKey;
import eu.europeana.metis.core.engine.base.IndexDatabase;
import eu.europeana.metis.core.engine.base.item.report.DataItemState;
import eu.europeana.metis.core.engine.base.item.report.DataItemStatus;
import eu.europeana.metis.core.engine.base.task.input.InputDataEndpoint;
import eu.europeana.metis.core.engine.base.task.input.SimpleIntermediateInputDataEndpoint;
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
import io.micrometer.common.util.StringUtils;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TestEcloudEngineTaskClient {

  private static final String TOPOLOGY_NAME = "topologyName";
  private static final String DATASET_ID = "datasetId";
  private static final String REPRESENTATION_NAME = "representationName";
  private static final String REVISION_NAME = "revisionName";
  private static final String X_PATH = "xPath";

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
    InputDataEndpoint inputDataEndpoint = new SimpleIntermediateInputDataEndpoint("http://internal.url", "",
        new DataRevision("name", "provider", Instant.now(), false));
    DataRevision outputDataRevision = new DataRevision("name", "provider", Instant.now(), false);
    EcloudEngineTask ecloudEngineTask = ecloudEngineTaskClient.createEngineTask(parameters, inputDataEndpoint,
        outputDataRevision);
    assertNotNull(ecloudEngineTask);
  }

  @Test
  void createEngineTask_throws() {
    Map<EngineTaskKey, String> parameters = Map.of();
    InputDataEndpoint inputDataEndpoint = new SimpleIntermediateInputDataEndpoint("http://internal.url", "",
        new DataRevision("name", "provider", Instant.now(), false));
    DataRevision outputDataRevision = new DataRevision("name", "provider", Instant.now(), false);
    assertThrows(NullPointerException.class, () -> ecloudEngineTaskClient.createEngineTask(parameters, null, outputDataRevision));
    assertThrows(NullPointerException.class, () -> ecloudEngineTaskClient.createEngineTask(parameters, inputDataEndpoint, null));
  }

  @Test
  void submitEngineTask() throws Exception {
    EcloudEngineTask ecloudEngineTask = mock(EcloudEngineTask.class);
    when(ecloudEngineTask.toDpsTask()).thenReturn(new DpsTask());
    when(dpsClient.submitTask(any(), eq(TOPOLOGY_NAME))).thenReturn(123L);
    String taskId = ecloudEngineTaskClient.submitEngineTask(ecloudEngineTask, TOPOLOGY_NAME);
    assertEquals("123", taskId);
  }

  @Test
  void submitEngineTask_throws() throws DpsException {
    EcloudEngineTask ecloudEngineTask = mock(EcloudEngineTask.class);
    when(ecloudEngineTask.toDpsTask()).thenReturn(new DpsTask());
    when(dpsClient.submitTask(any(), any())).thenThrow(new DpsException(""));
    assertThrows(ExternalTaskException.class, () -> ecloudEngineTaskClient.submitEngineTask(ecloudEngineTask, TOPOLOGY_NAME));
  }

  @Test
  void submitEngineTask_throwsRuntimeException() throws DpsException {
    EcloudEngineTask ecloudEngineTask = mock(EcloudEngineTask.class);
    when(ecloudEngineTask.toDpsTask()).thenReturn(new DpsTask());
    when(dpsClient.submitTask(any(), any())).thenThrow(new RuntimeException(""));
    assertThrows(ExternalTaskException.class, () -> ecloudEngineTaskClient.submitEngineTask(ecloudEngineTask, TOPOLOGY_NAME));
  }

  @Test
  void getEngineTaskProgress() throws Exception {
    TaskInfo taskInfo = new TaskInfo();
    taskInfo.setExpectedRecords(100);
    taskInfo.setSuccessRecords(100);
    taskInfo.setSuccessDepublishRecords(1);
    taskInfo.setUnchangedRecords(2);
    taskInfo.setFailRecords(3);
    taskInfo.setFailDepublishRecords(5);
    taskInfo.setEngineTaskState(PROCESSED);
    taskInfo.setEngineTaskStateInfo("stateDescription");

    when(dpsClient.getTaskProgress(TOPOLOGY_NAME, 1L)).thenReturn(taskInfo);

    EngineTaskProgress engineTaskProgress = ecloudEngineTaskClient.getEngineTaskProgress(TOPOLOGY_NAME, "1", null);

    assertEquals(taskInfo.getExpectedRecords(), engineTaskProgress.getExpectedRecords());
    assertEquals(taskInfo.getProcessedRecords(), engineTaskProgress.getProcessedRecords());
    assertEquals(taskInfo.getSuccessDepublishRecords(), engineTaskProgress.getSuccessDepublishRecords());
    assertEquals(taskInfo.getUnchangedRecords(), engineTaskProgress.getUnchangedRecords());
    assertEquals(taskInfo.getFailRecords(), engineTaskProgress.getFailRecords());
    assertEquals(taskInfo.getFailDepublishRecords(), engineTaskProgress.getFailDepublishRecords());
    assertEquals(EngineTaskState.valueOf(taskInfo.getEngineTaskState().name()), engineTaskProgress.getEngineTaskState());
    assertEquals(taskInfo.getEngineTaskStateInfo(), engineTaskProgress.getEngineTaskStateInfo());
  }

  @Test
  void getEngineTaskProgress_throwsAccessDeniedOrObjectDoesNotExistException() throws Exception {
    when(dpsClient.getTaskProgress(TOPOLOGY_NAME, 1L)).thenThrow(new AccessDeniedOrObjectDoesNotExistException());

    ExternalTaskException externalTaskException = assertThrows(ExternalTaskException.class,
        () -> ecloudEngineTaskClient.getEngineTaskProgress(TOPOLOGY_NAME, "1",null));

    Throwable cause = externalTaskException.getCause();
    assertNotNull(cause);
    assertInstanceOf(UnrecoverableExternalTaskException.class, cause);
  }

  @Test
  void getEngineTaskProgress_throwsDpsException() throws Exception {
    when(dpsClient.getTaskProgress(TOPOLOGY_NAME, 1L)).thenThrow(new DpsException(""));

    assertThrows(ExternalTaskException.class,
        () -> ecloudEngineTaskClient.getEngineTaskProgress(TOPOLOGY_NAME, "1", null));

  }

  @Test
  void getEngineTaskProgress_throwsRuntimeException() throws DpsException {
    when(dpsClient.getTaskProgress(TOPOLOGY_NAME, 1L)).thenThrow(new RuntimeException(""));
    assertThrows(ExternalTaskException.class,
        () -> ecloudEngineTaskClient.getEngineTaskProgress(TOPOLOGY_NAME, "1",  null));
  }

  @Test
  void getTotalIndexedRecords() throws Exception {
    when(dpsClient.getTotalMetisDatabaseRecords(DATASET_ID, TargetIndexingDatabase.PUBLISH)).thenReturn(100L);
    long totalIndexedRecords = ecloudEngineTaskClient.getTotalIndexedRecords(DATASET_ID, IndexDatabase.PUBLISH);
    assertEquals(100L, totalIndexedRecords);
  }

  @Test
  void getTotalIndexedRecords_throws() throws DpsException {
    when(dpsClient.getTotalMetisDatabaseRecords(DATASET_ID, TargetIndexingDatabase.PUBLISH)).thenThrow(new DpsException(""));
    assertThrows(ExternalTaskException.class,
        () -> ecloudEngineTaskClient.getTotalIndexedRecords(DATASET_ID, IndexDatabase.PUBLISH));
  }

  @Test
  void getPublishedRecords() throws Exception {
    List<String> recordIds = List.of("record1", "record2");
    when(dpsClient.searchPublishedDatasetRecords(DATASET_ID, recordIds)).thenReturn(recordIds);
    assertEquals(recordIds, ecloudEngineTaskClient.getPublishedRecords(DATASET_ID, recordIds));
  }

  @Test
  void getPublishedRecords_throws() throws DpsException {
    List<String> recordIds = List.of("record1", "record2");
    when(dpsClient.searchPublishedDatasetRecords(DATASET_ID, recordIds)).thenThrow(new DpsException(""));
    assertThrows(ExternalTaskException.class, () -> ecloudEngineTaskClient.getPublishedRecords(DATASET_ID, recordIds));
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

    when(dpsClient.getDetailedTaskReportBetweenChunks(TOPOLOGY_NAME, 1L, 0, 1)).thenReturn(singletonList(subTaskInfo));
    List<DataItemStatus> dataItemStatuses = ecloudEngineTaskClient.getDataItemStatuses(TOPOLOGY_NAME, "1", 0, 1);
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
    when(dpsClient.getDetailedTaskReportBetweenChunks(TOPOLOGY_NAME, 1L, 0, 1)).thenThrow(new DpsException(""));
    assertThrows(ExternalTaskException.class, () -> ecloudEngineTaskClient.getDataItemStatuses(TOPOLOGY_NAME, "1", 0, 1));
  }

  @Test
  void hasEngineTaskErrorReport() throws Exception {
    when(dpsClient.checkIfErrorReportExists(TOPOLOGY_NAME, 1L)).thenReturn(true);
    assertTrue(ecloudEngineTaskClient.hasEngineTaskErrorReport(TOPOLOGY_NAME, "1"));
  }

  @Test
  void hasEngineTaskErrorReport_throws() throws DpsException {
    when(dpsClient.checkIfErrorReportExists(TOPOLOGY_NAME, 1L)).thenThrow(new DpsException(""));
    assertThrows(ExternalTaskException.class, () -> ecloudEngineTaskClient.hasEngineTaskErrorReport(TOPOLOGY_NAME, "1"));
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

    when(dpsClient.getTaskErrorsReport(TOPOLOGY_NAME, 1L, null, 100)).thenReturn(taskErrorsInfo);
    EngineTaskErrors engineTaskErrors = ecloudEngineTaskClient.getEngineTaskErrors(TOPOLOGY_NAME, "1", 100);

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
    when(dpsClient.getTaskErrorsReport(TOPOLOGY_NAME, 1L, null, 100)).thenThrow(new DpsException(""));
    assertThrows(ExternalTaskException.class, () -> ecloudEngineTaskClient.getEngineTaskErrors(TOPOLOGY_NAME, "1", 100));
  }

  @Test
  void getEngineTaskContentRecordStatistics() throws Exception {
    StatisticsReport statisticsReport = new StatisticsReport();

    when(dpsClient.getTaskStatisticsReport(TOPOLOGY_NAME, 1L)).thenReturn(statisticsReport);
    RecordStatisticsDTO recordStatisticsDTO = ecloudEngineTaskClient.getEngineTaskContentRecordStatistics(TOPOLOGY_NAME, "1");
    assertEquals("0", recordStatisticsDTO.taskId());
    assertEquals(0, recordStatisticsDTO.nodePathStatistics().size());
  }

  @Test
  void getEngineTaskContentRecordStatistics_throws() throws DpsException {
    when(dpsClient.getTaskStatisticsReport(TOPOLOGY_NAME, 1L)).thenThrow(new DpsException(""));
    assertThrows(ExternalTaskException.class,
        () -> ecloudEngineTaskClient.getEngineTaskContentRecordStatistics(TOPOLOGY_NAME, "1"));
  }

  @Test
  void getEngineTaskContentNodePathStatistics() throws Exception {
    List<NodeReport> nodeReports = List.of();
    when(dpsClient.getElementReport(TOPOLOGY_NAME, 1L, X_PATH)).thenReturn(nodeReports);
    NodePathStatisticsDTO nodePathStatisticsDTO = ecloudEngineTaskClient.getEngineTaskContentNodePathStatistics(
        TOPOLOGY_NAME, "1", X_PATH);
    assertEquals(X_PATH, nodePathStatisticsDTO.xPath());
    assertEquals(0, nodePathStatisticsDTO.nodeValueStatistics().size());
  }

  @Test
  void getEngineTaskContentNodePathStatistics_throws() throws DpsException {
    when(dpsClient.getElementReport(TOPOLOGY_NAME, 1L, X_PATH)).thenThrow(new DpsException(""));
    assertThrows(ExternalTaskException.class, () -> ecloudEngineTaskClient.getEngineTaskContentNodePathStatistics(
        TOPOLOGY_NAME, "1", X_PATH));
  }

  @Test
  void cancelEngineTask_success() throws Exception {
    ecloudEngineTaskClient.cancelEngineTask(TOPOLOGY_NAME, "1", "", null);
    verify(dpsClient).killTask(TOPOLOGY_NAME, 1L, "");
  }

  @Test
  void cancelEngineTask_throws() throws DpsException {
    when(dpsClient.killTask(TOPOLOGY_NAME, 1L, "")).thenThrow(new DpsException(""));
    assertThrows(ExternalTaskException.class, () -> ecloudEngineTaskClient.cancelEngineTask(TOPOLOGY_NAME, "1", "", null));
  }

  @Test
  void cancelEngineTask_throwsRuntimeException() throws DpsException {
    when(dpsClient.killTask(TOPOLOGY_NAME, 1L, "")).thenThrow(new RuntimeException(""));
    assertThrows(ExternalTaskException.class, () -> ecloudEngineTaskClient.cancelEngineTask(TOPOLOGY_NAME, "1", "", null));
  }

  @Test
  void createEngineDatasetId() throws ExternalTaskException {
    when(ecloudEngineDatasetRecordClient.createEngineDatasetId(eq(ecloudEngineTaskSettings.getProvider()), anyString())).thenReturn(
        true);
    assertTrue(StringUtils.isNotBlank(ecloudEngineTaskClient.createEngineDatasetId(new Dataset())));
  }

  @Test
  void getRecords_withDatasetId() throws ExternalTaskException {
    Instant now = Instant.now();
    List<Record> records = List.of(new Record(null, null));
    when(ecloudEngineDatasetRecordClient.getRecords(ecloudEngineTaskSettings.getProvider(), DATASET_ID, REPRESENTATION_NAME,
        REVISION_NAME, now, 1)).thenReturn(records);
    List<Record> recordsResult = ecloudEngineTaskClient.getRecords(DATASET_ID, REPRESENTATION_NAME, REVISION_NAME, now, 1);
    assertEquals(records, recordsResult);
  }

  @Test
  void getRecords_fromIds() throws ExternalTaskException {
    Instant now = Instant.now();
    List<Record> records = List.of(new Record(null, null));
    when(ecloudEngineDatasetRecordClient.getRecords(ecloudEngineTaskSettings.getProvider(), List.of("recordId1"), REVISION_NAME,
        now)).thenReturn(records);
    assertEquals(records, ecloudEngineTaskClient.getRecords(List.of("recordId1"), REVISION_NAME, now));
  }

  @Test
  void getRecord() throws ExternalTaskException {
    Instant now = Instant.now();
    Record records = new Record(null, null);
    when(ecloudEngineDatasetRecordClient.getRecord(ecloudEngineTaskSettings.getProvider(), "recordId", REVISION_NAME,
        now)).thenReturn(records);
    assertEquals(records, ecloudEngineTaskClient.getRecord(DATASET_ID, "recordId", REVISION_NAME, now, null));
  }

  @Test
  void close() {
    ecloudEngineTaskClient.close();
    verify(dpsClient).close();
  }

}