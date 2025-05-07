package eu.europeana.metis.core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import eu.europeana.cloud.common.model.dps.TaskErrorsInfo;
import eu.europeana.metis.core.dao.DatasetDao;
import eu.europeana.metis.core.dao.WorkflowExecutionDao;
import eu.europeana.metis.core.engine.base.EngineTaskClient;
import eu.europeana.metis.core.engine.base.item.report.DataItemStatus;
import eu.europeana.metis.core.engine.base.task.report.EngineTaskErrors;
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
import eu.europeana.metis.core.workflow.plugins.MetisPlugin;
import eu.europeana.metis.core.workflow.plugins.PluginType;
import eu.europeana.metis.core.workflow.plugins.Topology;
import eu.europeana.metis.exception.ExternalTaskException;
import eu.europeana.metis.exception.GenericMetisException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TestProxiesService {

  private static final long EXTERNAL_TASK_ID = 2070373127078497810L;

  private static ProxiesService proxiesService;
  private static WorkflowExecutionDao workflowExecutionDao;
  private static EngineTaskClient<?, ?> engineTaskClient;

  @BeforeAll
  static void prepare() {
    workflowExecutionDao = mock(WorkflowExecutionDao.class);
    DatasetDao datasetDao = mock(DatasetDao.class);
    engineTaskClient = mock(EngineTaskClient.class);

    proxiesService = spy(new ProxiesService(engineTaskClient, workflowExecutionDao, datasetDao));
  }

  @AfterEach
  void cleanUp() {
    reset(workflowExecutionDao);
    reset(engineTaskClient);
    reset(proxiesService);
  }

  @Test
  void getExternalTaskLogs() throws Exception {
    List<DataItemStatus> dataItemStatusList = TestObjectFactory.createExternalRecordStatusList();

    when(engineTaskClient.getDataItemStatuses(Topology.OAIPMH_HARVEST.getTopologyName(),
            EXTERNAL_TASK_ID, 1, 100)).thenReturn(dataItemStatusList);
    final WorkflowExecution workflowExecution = TestObjectFactory.createWorkflowExecutionObject();
    when(workflowExecutionDao.getByExternalTaskId(EXTERNAL_TASK_ID)).thenReturn(workflowExecution);
    proxiesService.getExternalTaskLogs(Topology.OAIPMH_HARVEST.getTopologyName(),
        EXTERNAL_TASK_ID, 1, 100);
    assertEquals(2, dataItemStatusList.size());
  }

  @Test
  void getExternalTaskLogs_NoExecutionException() {
    when(workflowExecutionDao.getByExternalTaskId(EXTERNAL_TASK_ID)).thenReturn(null);
    assertThrows(NoWorkflowExecutionFoundException.class, () -> proxiesService
        .getExternalTaskLogs(Topology.OAIPMH_HARVEST.getTopologyName(),
            EXTERNAL_TASK_ID, 1, 100));
  }

  @Test
  void getExternalTaskLogs_ExternalTaskException() throws Exception {
    when(engineTaskClient
        .getDataItemStatuses(Topology.OAIPMH_HARVEST.getTopologyName(),
            EXTERNAL_TASK_ID, 1, 100)).thenThrow(new ExternalTaskException(""));
    final WorkflowExecution workflowExecution = TestObjectFactory.createWorkflowExecutionObject();
    when(workflowExecutionDao.getByExternalTaskId(EXTERNAL_TASK_ID)).thenReturn(workflowExecution);
    assertThrows(ExternalTaskException.class, () -> proxiesService
        .getExternalTaskLogs(Topology.OAIPMH_HARVEST.getTopologyName(),
            EXTERNAL_TASK_ID, 1, 100));
  }

  @Test
  void existsExternalTaskReport() throws Exception {

    when(engineTaskClient.hasEngineTaskErrorReport(Topology.OAIPMH_HARVEST.getTopologyName(),
        TestObjectFactory.EXTERNAL_TASK_ID)).thenReturn(true).thenThrow(ExternalTaskException.class);
    final WorkflowExecution workflowExecution = TestObjectFactory.createWorkflowExecutionObject();
    when(workflowExecutionDao.getByExternalTaskId(EXTERNAL_TASK_ID)).thenReturn(workflowExecution);

    final boolean existsExternalTaskReport = proxiesService.existsExternalTaskReport(Topology.OAIPMH_HARVEST.getTopologyName(),
        TestObjectFactory.EXTERNAL_TASK_ID);

    assertTrue(existsExternalTaskReport);
    assertThrows(ExternalTaskException.class,
        () -> proxiesService.existsExternalTaskReport(Topology.OAIPMH_HARVEST.getTopologyName(),
            TestObjectFactory.EXTERNAL_TASK_ID));

  }

  @Test
  void getExternalTaskReport() throws Exception {
    TaskErrorsInfo taskErrorsInfo = TestObjectFactory.createTaskErrorsInfoListWithoutIdentifiers(2);
    EngineTaskErrors taskErrorsInfoWithIdentifiers = TestObjectFactory
        .createTaskErrorsInfoWithIdentifiersExternal(taskErrorsInfo.getErrors().getFirst().getErrorType(),
            taskErrorsInfo.getErrors().getFirst().getMessage());

    when(engineTaskClient.getEngineTaskErrors(Topology.OAIPMH_HARVEST.getTopologyName(),
        TestObjectFactory.EXTERNAL_TASK_ID, 10))
        .thenReturn(taskErrorsInfoWithIdentifiers);
    final WorkflowExecution workflowExecution = TestObjectFactory.createWorkflowExecutionObject();
    when(workflowExecutionDao.getByExternalTaskId(EXTERNAL_TASK_ID)).thenReturn(workflowExecution);

    EngineTaskErrors engineTaskErrors = proxiesService.getExternalTaskReport(
        Topology.OAIPMH_HARVEST.getTopologyName(),
        TestObjectFactory.EXTERNAL_TASK_ID, 10);

    assertEquals(1, engineTaskErrors.errors().size());
    assertFalse(engineTaskErrors.errors().getFirst().errorDetails().isEmpty());
  }

  @Test
  void getExternalTaskReport_NoExecutionException() {
    when(workflowExecutionDao.getByExternalTaskId(EXTERNAL_TASK_ID)).thenReturn(null);
    assertThrows(NoWorkflowExecutionFoundException.class, () -> proxiesService
        .getExternalTaskReport(Topology.OAIPMH_HARVEST.getTopologyName(),
            TestObjectFactory.EXTERNAL_TASK_ID, 10));
  }

  @Test
  void getExternalTaskReport_ExternalTaskException() throws Exception {
    when(engineTaskClient.getEngineTaskErrors(Topology.OAIPMH_HARVEST.getTopologyName(),
        TestObjectFactory.EXTERNAL_TASK_ID,
        10)).thenThrow(new ExternalTaskException(""));
    final WorkflowExecution workflowExecution = TestObjectFactory.createWorkflowExecutionObject();
    when(workflowExecutionDao.getByExternalTaskId(EXTERNAL_TASK_ID)).thenReturn(workflowExecution);
    assertThrows(ExternalTaskException.class, () -> proxiesService
        .getExternalTaskReport(Topology.OAIPMH_HARVEST.getTopologyName(),
            TestObjectFactory.EXTERNAL_TASK_ID, 10));
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
  void getExternalTaskStatistics_NoExecutionException() {
    when(workflowExecutionDao.getByExternalTaskId(EXTERNAL_TASK_ID)).thenReturn(null);
    assertThrows(NoWorkflowExecutionFoundException.class, () -> proxiesService
        .getExternalTaskStatistics(Topology.OAIPMH_HARVEST.getTopologyName(),
            TestObjectFactory.EXTERNAL_TASK_ID));
  }

  @Test
  void getExternalTaskStatistics_ExternalTaskException() throws Exception {
    when(engineTaskClient.getEngineTaskContentRecordStatistics(Topology.OAIPMH_HARVEST.getTopologyName(),
        TestObjectFactory.EXTERNAL_TASK_ID)).thenThrow(new ExternalTaskException(""));
    final WorkflowExecution workflowExecution = TestObjectFactory.createWorkflowExecutionObject();
    when(workflowExecutionDao.getByExternalTaskId(EXTERNAL_TASK_ID)).thenReturn(workflowExecution);
    assertThrows(ExternalTaskException.class,
        () -> proxiesService.getExternalTaskStatistics(Topology.OAIPMH_HARVEST.getTopologyName(),
            TestObjectFactory.EXTERNAL_TASK_ID));
  }

  @Test
  void getAdditionalNodeStatistics() throws Exception {
    final String nodePath = "node path";
    final NodePathStatisticsDTO nodePathStatisticsDTO = new NodePathStatisticsDTO(nodePath, List.of());
    when(engineTaskClient.getEngineTaskContentNodePathStatistics(Topology.OAIPMH_HARVEST.getTopologyName(),
        TestObjectFactory.EXTERNAL_TASK_ID, nodePath)).thenReturn(nodePathStatisticsDTO);
    final WorkflowExecution workflowExecution = TestObjectFactory.createWorkflowExecutionObject();
    when(workflowExecutionDao.getByExternalTaskId(EXTERNAL_TASK_ID)).thenReturn(workflowExecution);
    final NodePathStatisticsDTO result = proxiesService.getAdditionalNodeStatistics(Topology.OAIPMH_HARVEST.getTopologyName(),
        TestObjectFactory.EXTERNAL_TASK_ID, nodePath);
    assertSame(nodePathStatisticsDTO, result);
  }

  @Test
  void getAdditionalNodeStatistics_NoExecutionException() {
    when(workflowExecutionDao.getByExternalTaskId(EXTERNAL_TASK_ID)).thenReturn(null);
    assertThrows(NoWorkflowExecutionFoundException.class, () -> proxiesService
        .getAdditionalNodeStatistics(Topology.OAIPMH_HARVEST.getTopologyName(),
            TestObjectFactory.EXTERNAL_TASK_ID, "node path"));
  }

  @Test
  void getAdditionalNodeStatistics_ExternalTaskException() throws Exception {
    final String nodePath = "node path";
    when(engineTaskClient.getEngineTaskContentNodePathStatistics(Topology.OAIPMH_HARVEST.getTopologyName(),
        TestObjectFactory.EXTERNAL_TASK_ID, nodePath)).thenThrow(new ExternalTaskException(""));
    final WorkflowExecution workflowExecution = TestObjectFactory.createWorkflowExecutionObject();
    when(workflowExecutionDao.getByExternalTaskId(EXTERNAL_TASK_ID)).thenReturn(workflowExecution);
    assertThrows(ExternalTaskException.class, () -> proxiesService
        .getAdditionalNodeStatistics(Topology.OAIPMH_HARVEST.getTopologyName(),
            TestObjectFactory.EXTERNAL_TASK_ID, nodePath));
  }

  @Test
  void getExternalTaskStatistics() throws Exception {
    final RecordStatisticsDTO recordStatisticsDTO = new RecordStatisticsDTO(0, List.of());
    when(engineTaskClient.getEngineTaskContentRecordStatistics(Topology.OAIPMH_HARVEST.getTopologyName(),
        TestObjectFactory.EXTERNAL_TASK_ID)).thenReturn(recordStatisticsDTO);
    final WorkflowExecution workflowExecution = TestObjectFactory.createWorkflowExecutionObject();
    when(workflowExecutionDao.getByExternalTaskId(EXTERNAL_TASK_ID)).thenReturn(workflowExecution);
    final RecordStatisticsDTO result = proxiesService.getExternalTaskStatistics(Topology.OAIPMH_HARVEST.getTopologyName(),
        TestObjectFactory.EXTERNAL_TASK_ID);
    assertSame(recordStatisticsDTO, result);
  }

  // TODO: add tests for searchRecordByIdFromPluginExecution

  @Test
  void getListOfFileContentsFromPluginExecution() throws Exception {

    // Create execution and plugin
    final WorkflowExecution execution = TestObjectFactory.createWorkflowExecutionObject();
    execution.getMetisPlugins()
             .forEach(abstractMetisPlugin -> abstractMetisPlugin.setStartedDate(new Date()));
    final AbstractExecutablePlugin<?> plugin = getUsedAndUnusedPluginType(execution).getLeft();
    doReturn(new ImmutablePair<>(execution, plugin)).when(proxiesService)
                                                    .getExecutionAndPlugin(TestObjectFactory.EXECUTIONID,
                                                        plugin.getPluginMetadata().getExecutablePluginType());
    when(workflowExecutionDao.getById(TestObjectFactory.EXECUTIONID)).thenReturn(execution);

    // Mock getting the records from eCloud.
    final String ecloudId = "ECLOUDID1";

    int numberOfRecords = 5;
    // Mock obtaining the actual record.
    final Record record = new Record(ecloudId, "test content");
    doReturn(List.of(record)).when(engineTaskClient)
                             .getRecords(execution.getEcloudDatasetId(), MetisPlugin.getRepresentationName(),
                                 plugin.getPluginType().name(),
                                 plugin.getStartedDate(), numberOfRecords);

    final ExecutablePluginType executablePluginType = plugin.getPluginMetadata().getExecutablePluginType();
    // Execute the call.
    PaginatedRecordsResponse listOfFileContentsFromPluginExecution = proxiesService
        .getListOfFileContentsFromPluginExecution(TestObjectFactory.EXECUTIONID,
            executablePluginType, null, numberOfRecords);
    assertEquals(record.getXmlRecord(),
        listOfFileContentsFromPluginExecution.getRecords().getFirst().getXmlRecord());
    assertEquals(ecloudId, listOfFileContentsFromPluginExecution.getRecords().getFirst().getEcloudId());

    // Test exception.
    doThrow(ExternalTaskException.class).when(engineTaskClient)
                                        .getRecords(execution.getEcloudDatasetId(), MetisPlugin.getRepresentationName(),
                                            plugin.getPluginType().name(),
                                            plugin.getStartedDate(), numberOfRecords);
    assertThrows(ExternalTaskException.class, () -> proxiesService
        .getListOfFileContentsFromPluginExecution(TestObjectFactory.EXECUTIONID, executablePluginType
            , null, numberOfRecords));
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
  void testGetListOfFileContentsFromPluginExecution() throws GenericMetisException {

    // Create execution and plugin and mock relevant method getting them.
    final WorkflowExecution execution = TestObjectFactory.createWorkflowExecutionObject();
    execution.getMetisPlugins()
             .forEach(abstractMetisPlugin -> abstractMetisPlugin.setStartedDate(new Date()));
    final AbstractExecutablePlugin<?> plugin = getUsedAndUnusedPluginType(execution).getLeft();
    doReturn(new ImmutablePair<>(execution, plugin)).when(proxiesService)
                                                    .getExecutionAndPlugin(TestObjectFactory.EXECUTIONID,
                                                        plugin.getPluginMetadata().getExecutablePluginType());

    // Create the test records and the list of IDs.
    ListOfIds listOfIds = new ListOfIds();
    listOfIds.setIds(List.of("ID 1", "ID 2", "ID 3"));
    final Record record1 = new Record("ID 1", "test content 1");
    final Record record2 = new Record("ID 2", "test content 2");
    final Record record3 = new Record("ID 3", "test content 3");
    final List<Record> recordList = List.of(record1, record2, record3);

    // Mock the method for getting records
    doReturn(recordList).when(engineTaskClient)
                        .getRecords(listOfIds.getIds(), plugin.getPluginType().name(), plugin.getStartedDate());

    final RecordsResponse result = proxiesService
        .getListOfFileContentsFromPluginExecution(TestObjectFactory.EXECUTIONID,
            plugin.getPluginMetadata().getExecutablePluginType(), listOfIds);

    // Verify that the result contains the record in the right order
    assertNotNull(result);
    assertNotNull(result.getRecords());
    assertEquals(listOfIds.getIds().size(), result.getRecords().size());
    assertEquals(listOfIds.getIds(), result.getRecords().stream().map(Record::getEcloudId).toList());

    // Check that the call also works for an empty list
    listOfIds.setIds(Collections.emptyList());
    final RecordsResponse emptyResult = proxiesService
        .getListOfFileContentsFromPluginExecution(TestObjectFactory.EXECUTIONID,
            plugin.getPluginMetadata().getExecutablePluginType(), listOfIds);
    assertNotNull(emptyResult);
    assertNotNull(emptyResult.getRecords());
    assertTrue(emptyResult.getRecords().isEmpty());

    // Check that if a record cannot be retrieved, the method fails.
    doThrow(ExternalTaskException.class).when(engineTaskClient)
                                        .getRecords(listOfIds.getIds(), plugin.getPluginType().name(), plugin.getStartedDate());
    assertThrows(ExternalTaskException.class, () -> proxiesService
        .getListOfFileContentsFromPluginExecution(TestObjectFactory.EXECUTIONID,
            plugin.getPluginMetadata().getExecutablePluginType(), listOfIds));
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
  void testGetExecutionAndPlugin() throws GenericMetisException {

    // Create a workflowExecution and get the plugin types
    final WorkflowExecution execution = TestObjectFactory.createWorkflowExecutionObject();
    final Pair<AbstractExecutablePlugin<?>, ExecutablePluginType> pluginAndUnusedType = getUsedAndUnusedPluginType(
        execution);
    final ExecutablePluginType unusedPluginType = pluginAndUnusedType.getRight();
    final AbstractExecutablePlugin<?> plugin = pluginAndUnusedType.getLeft();

    // Create a user and mock the dependency methods.
    when(workflowExecutionDao.getById(TestObjectFactory.EXECUTIONID)).thenReturn(execution);

    // Test happy flow with result
    final Pair<WorkflowExecution, ExecutablePlugin> result = proxiesService
        .getExecutionAndPlugin(TestObjectFactory.EXECUTIONID,
            plugin.getPluginMetadata().getExecutablePluginType());
    assertNotNull(result);
    assertEquals(execution, result.getLeft());
    assertNotNull(result.getRight());
    assertSame(plugin, result.getRight());

    // Test happy flow without result
    assertNull(proxiesService.getExecutionAndPlugin(TestObjectFactory.EXECUTIONID, unusedPluginType));

    // Test execution not found
    when(workflowExecutionDao.getById(TestObjectFactory.EXECUTIONID)).thenReturn(null);
    assertThrows(NoWorkflowExecutionFoundException.class, () -> proxiesService
        .getExecutionAndPlugin(TestObjectFactory.EXECUTIONID,
            plugin.getPluginMetadata().getExecutablePluginType()));
    when(workflowExecutionDao.getById(TestObjectFactory.EXECUTIONID)).thenReturn(execution);
  }
}
