package eu.europeana.metis.core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europeana.cloud.common.model.dps.TaskErrorsInfo;
import eu.europeana.metis.core.common.RecordIdUtils;
import eu.europeana.metis.core.dao.DatasetDao;
import eu.europeana.metis.core.dao.WorkflowExecutionDao;
import eu.europeana.metis.core.engine.base.EngineTaskClient;
import eu.europeana.metis.core.engine.base.task.report.EngineTaskErrors;
import eu.europeana.metis.core.exceptions.NoDatasetFoundException;
import eu.europeana.metis.core.exceptions.NoWorkflowExecutionFoundException;
import eu.europeana.metis.core.rest.ListOfIds;
import eu.europeana.metis.core.rest.PaginatedRecordsResponse;
import eu.europeana.metis.core.rest.Record;
import eu.europeana.metis.core.rest.RecordsResponse;
import eu.europeana.metis.core.rest.stats.NodePathStatisticsDTO;
import eu.europeana.metis.core.rest.stats.RecordStatisticsDTO;
import eu.europeana.metis.core.utils.TestObjectFactory;
import eu.europeana.metis.core.workflow.WorkflowExecution;
import eu.europeana.metis.core.workflow.plugins.AbstractExecutablePlugin;
import eu.europeana.metis.core.workflow.plugins.AbstractMetisPlugin;
import eu.europeana.metis.core.workflow.plugins.ExecutablePlugin;
import eu.europeana.metis.core.workflow.plugins.ExecutablePluginType;
import eu.europeana.metis.core.workflow.plugins.PluginType;
import eu.europeana.metis.core.workflow.plugins.Topology;
import eu.europeana.metis.exception.ExternalTaskException;
import eu.europeana.metis.exception.GenericMetisException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class TestProxiesService {

  private static final String ENGINE_TASK_ID = "2070373127078497810";

  private static ProxiesService<?, ?> proxiesService;
  private static WorkflowExecutionDao workflowExecutionDao;
  private static DatasetDao datasetDao;
  private static EngineTaskClient<?, ?> engineTaskClient;

  @BeforeAll
  static void prepare() {
    workflowExecutionDao = mock(WorkflowExecutionDao.class);
    datasetDao = mock(DatasetDao.class);
    engineTaskClient = mock(EngineTaskClient.class);

    proxiesService = spy(
        new ProxiesService<>(engineTaskClient, workflowExecutionDao, datasetDao));
  }

  @AfterEach
  void cleanUp() {
    reset(workflowExecutionDao);
    reset(datasetDao);
    reset(engineTaskClient);
    reset(proxiesService);
  }

  @Test
  void existsExternalTaskReport() throws Exception {
    final WorkflowExecution workflowExecution =
        TestObjectFactory.createWorkflowExecutionObject();

    when(workflowExecutionDao.getByEngineTaskId(ENGINE_TASK_ID))
        .thenReturn(workflowExecution);

    when(engineTaskClient.hasEngineTaskErrorReport(
        Topology.OAIPMH_HARVEST.getTopologyName(), ENGINE_TASK_ID))
        .thenReturn(true);

    final boolean result = proxiesService.existsEngineTaskReport(
        Topology.OAIPMH_HARVEST.getTopologyName(), ENGINE_TASK_ID);

    assertTrue(result);

    verify(datasetDao).getDatasetOrThrow(workflowExecution.getDatasetId());
    verify(engineTaskClient).hasEngineTaskErrorReport(
        Topology.OAIPMH_HARVEST.getTopologyName(), ENGINE_TASK_ID);
  }

  @Test
  void existsExternalTaskReportThrowsExternalTaskException() throws Exception {
    final WorkflowExecution workflowExecution =
        TestObjectFactory.createWorkflowExecutionObject();

    when(workflowExecutionDao.getByEngineTaskId(ENGINE_TASK_ID))
        .thenReturn(workflowExecution);

    when(engineTaskClient.hasEngineTaskErrorReport(
        Topology.OAIPMH_HARVEST.getTopologyName(), ENGINE_TASK_ID))
        .thenThrow(new ExternalTaskException("Engine failure"));

    assertThrows(ExternalTaskException.class,
        () -> proxiesService.existsEngineTaskReport(
            Topology.OAIPMH_HARVEST.getTopologyName(), ENGINE_TASK_ID));

    verify(datasetDao).getDatasetOrThrow(workflowExecution.getDatasetId());
  }

  @Test
  void existsExternalTaskReportThrowsWhenExecutionDoesNotExist() throws NoDatasetFoundException {
    when(workflowExecutionDao.getByEngineTaskId(ENGINE_TASK_ID))
        .thenReturn(null);

    assertThrows(NoWorkflowExecutionFoundException.class,
        () -> proxiesService.existsEngineTaskReport(
            Topology.OAIPMH_HARVEST.getTopologyName(), ENGINE_TASK_ID));

    verify(datasetDao, never()).getDatasetOrThrow(any());
  }

  @Test
  void getExternalTaskReport() throws Exception {
    TaskErrorsInfo taskErrorsInfo = TestObjectFactory.createTaskErrorsInfoListWithoutIdentifiers(2);
    EngineTaskErrors taskErrorsInfoWithIdentifiers = TestObjectFactory
        .createTaskErrorsInfoWithIdentifiersExternal(taskErrorsInfo.getErrors().getFirst().getErrorType(),
            taskErrorsInfo.getErrors().getFirst().getMessage());

    when(engineTaskClient.getEngineTaskErrors(Topology.OAIPMH_HARVEST.getTopologyName(),
        ENGINE_TASK_ID, 10))
        .thenReturn(taskErrorsInfoWithIdentifiers);
    final WorkflowExecution workflowExecution = TestObjectFactory.createWorkflowExecutionObject();
    when(workflowExecutionDao.getByEngineTaskId(ENGINE_TASK_ID)).thenReturn(workflowExecution);

    EngineTaskErrors engineTaskErrors = proxiesService.getExternalTaskReport(
        Topology.OAIPMH_HARVEST.getTopologyName(), ENGINE_TASK_ID, 10);

    assertEquals(1, engineTaskErrors.errors().size());
    assertFalse(engineTaskErrors.errors().getFirst().errorDetails().isEmpty());
    verify(datasetDao).getDatasetOrThrow(workflowExecution.getDatasetId());
  }

  @Test
  void getExternalTaskReport_NoExecutionException() {
    when(workflowExecutionDao.getByEngineTaskId(ENGINE_TASK_ID)).thenReturn(null);
    assertThrows(NoWorkflowExecutionFoundException.class, () -> proxiesService
        .getExternalTaskReport(Topology.OAIPMH_HARVEST.getTopologyName(), ENGINE_TASK_ID, 10));
  }

  @Test
  void getExternalTaskReport_ExternalTaskException() throws Exception {
    when(engineTaskClient.getEngineTaskErrors(Topology.OAIPMH_HARVEST.getTopologyName(),
        ENGINE_TASK_ID,
        10)).thenThrow(new ExternalTaskException(""));
    final WorkflowExecution workflowExecution = TestObjectFactory.createWorkflowExecutionObject();
    when(workflowExecutionDao.getByEngineTaskId(ENGINE_TASK_ID)).thenReturn(workflowExecution);
    assertThrows(ExternalTaskException.class, () -> proxiesService
        .getExternalTaskReport(Topology.OAIPMH_HARVEST.getTopologyName(), ENGINE_TASK_ID, 10));
  }

  private Pair<AbstractExecutablePlugin<?>, ExecutablePluginType> getUsedAndUnusedPluginType(
      WorkflowExecution execution) {
    final Set<PluginType> usedPluginTypes =
        execution.getMetisPlugins().stream()
                 .map(AbstractMetisPlugin::getPluginType).collect(Collectors.toSet());
    final Map<PluginType, ExecutablePluginType> executablePluginTypes = Stream
        .of(ExecutablePluginType.values())
        .collect(Collectors.toMap(ExecutablePluginType::toPluginType, Function.identity()));

    AbstractExecutablePlugin<?> abstractExecutablePlugin = execution.getMetisPlugins().stream()
                                                                    .filter(
                                                                        plugin -> plugin instanceof AbstractExecutablePlugin<?>)
                                                                    .map(plugin -> (AbstractExecutablePlugin<?>) plugin)
                                                                    .findAny()
                                                                    .orElseThrow(IllegalStateException::new);

    final ExecutablePluginType unusedPluginType = Stream.of(PluginType.values())
                                                        .filter(type -> !usedPluginTypes.contains(type))
                                                        .map(executablePluginTypes::get).filter(
            Objects::nonNull).findAny().orElseThrow(IllegalStateException::new);
    return new ImmutablePair<>(abstractExecutablePlugin, unusedPluginType);
  }

  @Test
  void getExternalTaskStatistics() throws Exception {
    final RecordStatisticsDTO recordStatisticsDTO = new RecordStatisticsDTO("0", List.of());
    when(engineTaskClient.getEngineTaskContentRecordStatistics(Topology.OAIPMH_HARVEST.getTopologyName(),
        ENGINE_TASK_ID)).thenReturn(recordStatisticsDTO);
    final WorkflowExecution workflowExecution = TestObjectFactory.createWorkflowExecutionObject();
    when(workflowExecutionDao.getByEngineTaskId(ENGINE_TASK_ID)).thenReturn(workflowExecution);
    final RecordStatisticsDTO result = proxiesService.getExternalTaskStatistics(Topology.OAIPMH_HARVEST.getTopologyName(),
        ENGINE_TASK_ID);
    assertSame(recordStatisticsDTO, result);
    verify(datasetDao).getDatasetOrThrow(workflowExecution.getDatasetId());
  }

  @Test
  void getExternalTaskStatistics_NoExecutionException() {
    when(workflowExecutionDao.getByEngineTaskId(ENGINE_TASK_ID)).thenReturn(null);
    assertThrows(NoWorkflowExecutionFoundException.class, () -> proxiesService
        .getExternalTaskStatistics(Topology.OAIPMH_HARVEST.getTopologyName(), ENGINE_TASK_ID));
  }

  @Test
  void getExternalTaskStatistics_ExternalTaskException() throws Exception {
    when(engineTaskClient.getEngineTaskContentRecordStatistics(Topology.OAIPMH_HARVEST.getTopologyName(),
        ENGINE_TASK_ID)).thenThrow(new ExternalTaskException(""));
    final WorkflowExecution workflowExecution = TestObjectFactory.createWorkflowExecutionObject();
    when(workflowExecutionDao.getByEngineTaskId(ENGINE_TASK_ID)).thenReturn(workflowExecution);
    assertThrows(ExternalTaskException.class,
        () -> proxiesService.getExternalTaskStatistics(Topology.OAIPMH_HARVEST.getTopologyName(), ENGINE_TASK_ID));
  }

  @Test
  void getAdditionalNodeStatistics() throws Exception {
    final String nodePath = "node path";
    final NodePathStatisticsDTO nodePathStatisticsDTO = new NodePathStatisticsDTO(nodePath, List.of());
    when(engineTaskClient.getEngineTaskContentNodePathStatistics(Topology.OAIPMH_HARVEST.getTopologyName(),
        ENGINE_TASK_ID, nodePath)).thenReturn(nodePathStatisticsDTO);
    final WorkflowExecution workflowExecution = TestObjectFactory.createWorkflowExecutionObject();
    when(workflowExecutionDao.getByEngineTaskId(ENGINE_TASK_ID)).thenReturn(workflowExecution);
    final NodePathStatisticsDTO result = proxiesService.getAdditionalNodeStatistics(Topology.OAIPMH_HARVEST.getTopologyName(),
        ENGINE_TASK_ID, nodePath);
    assertSame(nodePathStatisticsDTO, result);
    verify(datasetDao).getDatasetOrThrow(workflowExecution.getDatasetId());
  }

  @Test
  void getAdditionalNodeStatistics_NoExecutionException() {
    when(workflowExecutionDao.getByEngineTaskId(ENGINE_TASK_ID)).thenReturn(null);
    assertThrows(NoWorkflowExecutionFoundException.class, () -> proxiesService
        .getAdditionalNodeStatistics(Topology.OAIPMH_HARVEST.getTopologyName(), ENGINE_TASK_ID, "node path"));
  }

  @Test
  void getAdditionalNodeStatistics_ExternalTaskException() throws Exception {
    final String nodePath = "node path";
    when(engineTaskClient.getEngineTaskContentNodePathStatistics(Topology.OAIPMH_HARVEST.getTopologyName(),
        ENGINE_TASK_ID, nodePath)).thenThrow(new ExternalTaskException(""));
    final WorkflowExecution workflowExecution = TestObjectFactory.createWorkflowExecutionObject();
    when(workflowExecutionDao.getByEngineTaskId(ENGINE_TASK_ID)).thenReturn(workflowExecution);
    assertThrows(ExternalTaskException.class, () -> proxiesService
        .getAdditionalNodeStatistics(Topology.OAIPMH_HARVEST.getTopologyName(), ENGINE_TASK_ID, nodePath));
  }

  @Test
  void searchRecordByIdFromPluginExecutionFindsEngineRecord() throws Exception {
    final WorkflowExecution execution = TestObjectFactory.createWorkflowExecutionObject();
    final AbstractExecutablePlugin<?> plugin = getUsedAndUnusedPluginType(execution).getLeft();
    final ExecutablePluginType executablePluginType = plugin.getPluginMetadata().getExecutablePluginType();

    final String engineBatchId = "batch-id";
    final String idToSearch = "engine-record-id";
    final Record expectedRecord = new Record(idToSearch, "record content");

    plugin.setEngineBatchId(engineBatchId);

    doReturn(new ImmutablePair<>(execution, plugin)).when(proxiesService)
                                                    .getExecutionAndPlugin(TestObjectFactory.EXECUTIONID, executablePluginType);

    doReturn(expectedRecord).when(engineTaskClient)
                            .getRecord(execution.getEngineDatasetId(), idToSearch, engineBatchId, executablePluginType);

    final Record result = proxiesService.searchRecordByIdFromPluginExecution(
        TestObjectFactory.EXECUTIONID, executablePluginType, idToSearch);

    assertSame(expectedRecord, result);
    verify(engineTaskClient).getRecord(execution.getEngineDatasetId(), idToSearch, engineBatchId, executablePluginType);
  }

  @Test
  void searchRecordByIdFromPluginExecutionTriesNormalizedEuropeanaId()
      throws Exception {
    final WorkflowExecution execution = TestObjectFactory.createWorkflowExecutionObject();
    final AbstractExecutablePlugin<?> plugin = getUsedAndUnusedPluginType(execution).getLeft();
    final ExecutablePluginType executablePluginType = plugin.getPluginMetadata().getExecutablePluginType();

    final String engineBatchId = "batch-id";
    final String idToSearch = "record-id";
    final String normalizedLocalId = "normalized-record-id";
    final String fullRecordId =
        "/" + execution.getDatasetId() + "/" + normalizedLocalId;

    final Record expectedRecord =
        new Record("engine-id", "record content");

    plugin.setEngineBatchId(engineBatchId);
    doReturn(new ImmutablePair<>(execution, plugin))
        .when(proxiesService).getExecutionAndPlugin(TestObjectFactory.EXECUTIONID, executablePluginType);

    doReturn(null)
        .when(engineTaskClient).getRecord(execution.getEngineDatasetId(), idToSearch, engineBatchId, executablePluginType);

    doReturn(expectedRecord)
        .when(engineTaskClient).getRecord(execution.getEngineDatasetId(), fullRecordId, engineBatchId, executablePluginType);

    try (MockedStatic<RecordIdUtils> recordIdUtils = mockStatic(RecordIdUtils.class)) {
      recordIdUtils.when(() -> RecordIdUtils.checkAndNormalizeRecordId(execution.getDatasetId(), idToSearch))
                   .thenReturn(Optional.of(normalizedLocalId));
      recordIdUtils.when(() -> RecordIdUtils.composeFullRecordId(execution.getDatasetId(), normalizedLocalId))
                   .thenReturn(fullRecordId);

      final Record result =
          proxiesService.searchRecordByIdFromPluginExecution(TestObjectFactory.EXECUTIONID, executablePluginType, idToSearch);
      assertSame(expectedRecord, result);
    }

    verify(engineTaskClient).getRecord(execution.getEngineDatasetId(), idToSearch, engineBatchId, executablePluginType);
    verify(engineTaskClient).getRecord(execution.getEngineDatasetId(), fullRecordId, engineBatchId, executablePluginType);
  }

  @Test
  void searchRecordByIdFromPluginExecutionReturnsNullWhenRecordDoesNotExist() throws Exception {
    final WorkflowExecution execution = TestObjectFactory.createWorkflowExecutionObject();
    final AbstractExecutablePlugin<?> plugin = getUsedAndUnusedPluginType(execution).getLeft();
    final ExecutablePluginType executablePluginType = plugin.getPluginMetadata().getExecutablePluginType();

    final String engineBatchId = "batch-id";
    final String idToSearch = "record-id";
    final String fullRecordId = "/dataset/record-id";

    plugin.setEngineBatchId(engineBatchId);

    doReturn(new ImmutablePair<>(execution, plugin)).when(proxiesService)
                                                    .getExecutionAndPlugin(TestObjectFactory.EXECUTIONID, executablePluginType);

    doReturn(null).when(engineTaskClient)
                  .getRecord(execution.getEngineDatasetId(), idToSearch, engineBatchId, executablePluginType);

    doReturn(null).when(engineTaskClient)
                  .getRecord(execution.getEngineDatasetId(), fullRecordId, engineBatchId, executablePluginType);

    try (MockedStatic<RecordIdUtils> recordIdUtils = mockStatic(RecordIdUtils.class)) {

      recordIdUtils.when(() ->
          RecordIdUtils.checkAndNormalizeRecordId(execution.getDatasetId(), idToSearch)).thenReturn(Optional.of("record-id"));

      recordIdUtils.when(() ->
          RecordIdUtils.composeFullRecordId(execution.getDatasetId(), "record-id")).thenReturn(fullRecordId);

      final Record result =
          proxiesService.searchRecordByIdFromPluginExecution(TestObjectFactory.EXECUTIONID, executablePluginType, idToSearch);
      assertNull(result);
    }
  }

  @Test
  void getListOfFileContentsFromPluginExecution() throws Exception {
    final WorkflowExecution execution = TestObjectFactory.createWorkflowExecutionObject();
    final AbstractExecutablePlugin<?> plugin = getUsedAndUnusedPluginType(execution).getLeft();

    final String engineBatchId = "batch-id";
    plugin.setEngineBatchId(engineBatchId);

    doReturn(new ImmutablePair<>(execution, plugin)).when(proxiesService)
                                                    .getExecutionAndPlugin(TestObjectFactory.EXECUTIONID,
                                                        plugin.getPluginMetadata().getExecutablePluginType());

    final int numberOfRecords = 5;
    final Record record = new Record("ECLOUDID1", "test content");

    doReturn(List.of(record)).when(engineTaskClient).getRecords(execution.getEngineDatasetId(), engineBatchId, numberOfRecords);

    final PaginatedRecordsResponse result =
        proxiesService.getListOfFileContentsFromPluginExecution(
            TestObjectFactory.EXECUTIONID,
            plugin.getPluginMetadata().getExecutablePluginType(),
            null,
            numberOfRecords);

    assertNotNull(result);
    assertEquals(List.of(record), result.getRecords());
    assertNull(result.getNextPage());

    verify(engineTaskClient).getRecords(execution.getEngineDatasetId(), engineBatchId, numberOfRecords);
  }

  @Test
  void getListOfFileContentsFromPluginExecutionPropagatesExternalTaskException() throws Exception {
    final WorkflowExecution execution = TestObjectFactory.createWorkflowExecutionObject();
    final AbstractExecutablePlugin<?> plugin = getUsedAndUnusedPluginType(execution).getLeft();

    final String engineBatchId = "batch-id";
    plugin.setEngineBatchId(engineBatchId);

    doReturn(new ImmutablePair<>(execution, plugin)).when(proxiesService)
                                                    .getExecutionAndPlugin(TestObjectFactory.EXECUTIONID,
                                                        plugin.getPluginMetadata().getExecutablePluginType());

    final int numberOfRecords = 5;

    doThrow(new ExternalTaskException("Engine failure"))
        .when(engineTaskClient)
        .getRecords(execution.getEngineDatasetId(), engineBatchId, numberOfRecords);

    assertThrows(ExternalTaskException.class,
        () -> proxiesService.getListOfFileContentsFromPluginExecution(
            TestObjectFactory.EXECUTIONID,
            plugin.getPluginMetadata().getExecutablePluginType(),
            null,
            numberOfRecords));
  }

  @Test
  void getListOfFileContentsFromPluginExecution_ExceptionOfDataAvailability() throws GenericMetisException {

    // If there is no execution
    final ExecutablePluginType pluginType = ExecutablePluginType.OAIPMH_HARVEST;
    doThrow(NoWorkflowExecutionFoundException.class).when(proxiesService)
                                                    .getExecutionAndPlugin(TestObjectFactory.EXECUTIONID,
                                                        pluginType);
    assertThrows(NoWorkflowExecutionFoundException.class, () -> proxiesService
        .getListOfFileContentsFromPluginExecution(TestObjectFactory.EXECUTIONID,
            pluginType, null, 5));

    // If the execution does not have the plugin an empty result should be returned.
    doReturn(null).when(proxiesService)
                  .getExecutionAndPlugin(TestObjectFactory.EXECUTIONID, pluginType);
    final PaginatedRecordsResponse result = proxiesService
        .getListOfFileContentsFromPluginExecution(TestObjectFactory.EXECUTIONID,
            pluginType, null, 5);
    assertNotNull(result);
    assertNotNull(result.getRecords());
    assertTrue(result.getRecords().isEmpty());
    assertNull(result.getNextPage());
  }

  @Test
  void getListOfFileContentsFromPluginExecutionByIds() throws Exception {
    final WorkflowExecution execution = TestObjectFactory.createWorkflowExecutionObject();
    final AbstractExecutablePlugin<?> plugin = getUsedAndUnusedPluginType(execution).getLeft();

    final String engineBatchId = "batch-id";
    plugin.setEngineBatchId(engineBatchId);

    doReturn(new ImmutablePair<>(execution, plugin)).when(proxiesService)
                                                    .getExecutionAndPlugin(TestObjectFactory.EXECUTIONID,
                                                        plugin.getPluginMetadata().getExecutablePluginType());

    final ListOfIds listOfIds = new ListOfIds();
    listOfIds.setIds(List.of("ID 1", "ID 2", "ID 3"));

    final List<Record> records = List.of(
        new Record("ID 1", "test content 1"),
        new Record("ID 2", "test content 2"),
        new Record("ID 3", "test content 3"));

    doReturn(records).when(engineTaskClient).getRecords(listOfIds.getIds(), engineBatchId);

    final RecordsResponse result =
        proxiesService.getListOfFileContentsFromPluginExecution(
            TestObjectFactory.EXECUTIONID, plugin.getPluginMetadata().getExecutablePluginType(), listOfIds);

    assertNotNull(result);
    assertEquals(records, result.getRecords());

    verify(engineTaskClient).getRecords(listOfIds.getIds(), engineBatchId);
  }

  @Test
  void testGetListOfFileContentsFromPluginExecution_ExceptionOfDataAvailability() throws GenericMetisException {

    // If there is no execution
    final ExecutablePluginType pluginType = ExecutablePluginType.OAIPMH_HARVEST;
    doThrow(NoWorkflowExecutionFoundException.class)
        .when(proxiesService).getExecutionAndPlugin(TestObjectFactory.EXECUTIONID, pluginType);
    assertThrows(NoWorkflowExecutionFoundException.class, () -> proxiesService
        .getListOfFileContentsFromPluginExecution(TestObjectFactory.EXECUTIONID,
            pluginType, new ListOfIds()));

    // If the execution does not have the plugin an empty result should be returned.
    doReturn(null).when(proxiesService).getExecutionAndPlugin(TestObjectFactory.EXECUTIONID, pluginType);
    assertThrows(NoWorkflowExecutionFoundException.class, () -> proxiesService
        .getListOfFileContentsFromPluginExecution(TestObjectFactory.EXECUTIONID,
            pluginType, new ListOfIds()));
  }

  @Test
  void getExecutionAndPlugin() throws Exception {
    final WorkflowExecution execution = TestObjectFactory.createWorkflowExecutionObject();
    final Pair<AbstractExecutablePlugin<?>, ExecutablePluginType> pluginAndUnusedType = getUsedAndUnusedPluginType(execution);
    final AbstractExecutablePlugin<?> plugin = pluginAndUnusedType.getLeft();
    final ExecutablePluginType unusedPluginType = pluginAndUnusedType.getRight();

    when(workflowExecutionDao.getById(TestObjectFactory.EXECUTIONID)).thenReturn(execution);

    final Pair<WorkflowExecution, ExecutablePlugin> result =
        proxiesService.getExecutionAndPlugin(TestObjectFactory.EXECUTIONID, plugin.getPluginMetadata().getExecutablePluginType());

    assertNotNull(result);
    assertSame(execution, result.getLeft());
    assertSame(plugin, result.getRight());
    verify(datasetDao).getDatasetOrThrow(execution.getDatasetId());
    assertNull(proxiesService.getExecutionAndPlugin(TestObjectFactory.EXECUTIONID, unusedPluginType));
    verify(datasetDao, times(2)).getDatasetOrThrow(execution.getDatasetId());
  }

  @Test
  void getExecutionAndPluginThrowsWhenExecutionDoesNotExist() throws NoDatasetFoundException {
    when(workflowExecutionDao.getById(TestObjectFactory.EXECUTIONID)).thenReturn(null);
    assertThrows(NoWorkflowExecutionFoundException.class,
        () -> proxiesService.getExecutionAndPlugin(TestObjectFactory.EXECUTIONID, ExecutablePluginType.OAIPMH_HARVEST));

    verify(datasetDao, never()).getDatasetOrThrow(any());
  }

}
