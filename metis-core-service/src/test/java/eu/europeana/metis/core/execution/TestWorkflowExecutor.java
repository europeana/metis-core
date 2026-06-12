package eu.europeana.metis.core.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europeana.metis.core.dao.DatasetXsltDao;
import eu.europeana.metis.core.dao.WorkflowExecutionDao;
import eu.europeana.metis.core.engine.base.DataRevision;
import eu.europeana.metis.core.engine.base.EngineTask;
import eu.europeana.metis.core.engine.base.EngineTaskClient;
import eu.europeana.metis.core.engine.base.EngineTaskSettings;
import eu.europeana.metis.core.engine.base.task.input.InputDataEndpoint;
import eu.europeana.metis.core.engine.base.task.report.EngineTaskProgress;
import eu.europeana.metis.core.engine.base.task.report.EngineTaskState;
import eu.europeana.metis.core.utils.TestObjectFactory;
import eu.europeana.metis.core.workflow.WorkflowExecution;
import eu.europeana.metis.core.workflow.WorkflowStatus;
import eu.europeana.metis.core.workflow.execution.SystemId;
import eu.europeana.metis.core.workflow.plugins.AbstractMetisPlugin;
import eu.europeana.metis.core.workflow.plugins.ExecutablePluginType;
import eu.europeana.metis.core.workflow.plugins.OaipmhHarvestPlugin;
import eu.europeana.metis.core.workflow.plugins.OaipmhHarvestPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.PluginStatus;
import eu.europeana.metis.exception.ExternalTaskException;
import eu.europeana.metis.exception.UnrecoverableExternalTaskException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;

class TestWorkflowExecutor {

  private static WorkflowExecutionDao workflowExecutionDao;
  private static WorkflowPostProcessor workflowPostProcessor;
  private static DatasetXsltDao datasetXsltDao;
  private static EngineTaskClient<EngineTaskSettings, EngineTask> engineTaskClient;
  private static WorkflowExecutorSettings<EngineTaskSettings, EngineTask> workflowExecutorSettings;

  @BeforeAll
  static void prepare() {
    workflowExecutionDao = Mockito.mock(WorkflowExecutionDao.class);
    workflowPostProcessor = Mockito.mock(WorkflowPostProcessor.class);
    datasetXsltDao = Mockito.mock(DatasetXsltDao.class);
    engineTaskClient = mock(EngineTaskClient.class);

    TestWorkflowExecutor.workflowExecutorSettings = new WorkflowExecutorSettings<>(
        Duration.ofMillis(1), Duration.ofMinutes(10),
        new SemaphoresPerPluginManager(2), workflowExecutionDao,
        workflowPostProcessor, datasetXsltDao, engineTaskClient);
  }

  @BeforeEach
  void cleanUp() {
    Mockito.reset(workflowExecutionDao);
    Mockito.reset(workflowPostProcessor);
    Mockito.reset(engineTaskClient);

    EngineTask engineTask = mock(EngineTask.class);
    when(engineTaskClient.createEngineTask(anyMap(), any(InputDataEndpoint.class), any(DataRevision.class)))
        .thenReturn(engineTask);
    EngineTaskSettings engineTaskSettings = mock(EngineTaskSettings.class);
    when(engineTaskClient.getEngineTaskSettings()).thenReturn(engineTaskSettings);
  }

  @Test
  void callNonMockedFieldValue() throws Exception {
    OaipmhHarvestPlugin oaipmhHarvestPlugin = Mockito.spy(OaipmhHarvestPlugin.class);
    OaipmhHarvestPluginMetadata oaipmhHarvestPluginMetadata = new OaipmhHarvestPluginMetadata();
    oaipmhHarvestPlugin.setPluginMetadata(oaipmhHarvestPluginMetadata);
    ArrayList<AbstractMetisPlugin<?>> abstractMetisPlugins = new ArrayList<>();
    abstractMetisPlugins.add(oaipmhHarvestPlugin);

    WorkflowExecution workflowExecution = TestObjectFactory.createWorkflowExecutionObject();
    workflowExecution.setId(new ObjectId());
    workflowExecution.setWorkflowStatus(WorkflowStatus.INQUEUE);
    workflowExecution.setMetisPlugins(abstractMetisPlugins);
    workflowExecution.setStartedDate(new Date());

    doReturn(oaipmhHarvestPluginMetadata).when(oaipmhHarvestPlugin).getPluginMetadata();

    EngineTaskProgress currentlyProcessingProgress = new EngineTaskProgress();
    currentlyProcessingProgress.setEngineTaskState(EngineTaskState.CURRENTLY_PROCESSING);
    EngineTaskProgress processedProgress = new EngineTaskProgress();
    processedProgress.setEngineTaskState(EngineTaskState.PROCESSED);
    String topologyName = oaipmhHarvestPlugin.getTopologyName();
    when(engineTaskClient.getEngineTaskProgress(eq(topologyName), any(), any()))
        .thenReturn(currentlyProcessingProgress)
        .thenReturn(processedProgress);

    when(workflowExecutionDao.updateMonitorInformation(workflowExecution)).thenReturn(true);
    when(workflowExecutionDao.isCancelling(workflowExecution.getId())).thenReturn(false);

    doNothing().when(workflowExecutionDao).updateWorkflowPlugins(workflowExecution);
    when(workflowExecutionDao.update(workflowExecution))
        .thenReturn(workflowExecution.getId().toString());
    when(workflowExecutionDao.getById(anyString())).thenReturn(workflowExecution);

    WorkflowExecutor<EngineTaskSettings, EngineTask> workflowExecutor = new WorkflowExecutor<>(workflowExecution,
        workflowExecutorSettings);
    workflowExecutor.call();

    verify(workflowExecutionDao, times(2)).updateMonitorInformation(workflowExecution);
    verify(workflowExecutionDao, times(1)).update(workflowExecution);

    InOrder inOrderForPlugin = inOrder(oaipmhHarvestPlugin);
    inOrderForPlugin.verify(oaipmhHarvestPlugin, times(2))
                    .setPluginStatusAndResetFailMessage(PluginStatus.RUNNING);
    inOrderForPlugin.verify(oaipmhHarvestPlugin)
                    .setPluginStatusAndResetFailMessage(PluginStatus.FINISHED);
    verify(oaipmhHarvestPlugin, atMost(5)).setPluginStatusAndResetFailMessage(any());
    verify(oaipmhHarvestPlugin, never()).setFailMessage(anyString());
  }

  @Test
  void callNonMockedFieldValue_DROPPEDExternalTaskButNotCancelled() throws Exception {
    OaipmhHarvestPlugin oaipmhHarvestPlugin = Mockito.spy(OaipmhHarvestPlugin.class);
    OaipmhHarvestPluginMetadata oaipmhHarvestPluginMetadata = new OaipmhHarvestPluginMetadata();
    oaipmhHarvestPlugin.setPluginMetadata(oaipmhHarvestPluginMetadata);
    ArrayList<AbstractMetisPlugin<?>> abstractMetisPlugins = new ArrayList<>();
    abstractMetisPlugins.add(oaipmhHarvestPlugin);

    WorkflowExecution workflowExecution = TestObjectFactory.createWorkflowExecutionObject();
    workflowExecution.setId(new ObjectId());
    workflowExecution.setWorkflowStatus(WorkflowStatus.INQUEUE);
    workflowExecution.setMetisPlugins(abstractMetisPlugins);
    workflowExecution.setStartedDate(new Date());

    EngineTaskProgress currentlyProcessingProgress = new EngineTaskProgress();
    currentlyProcessingProgress.setEngineTaskState(EngineTaskState.CURRENTLY_PROCESSING);
    EngineTaskProgress droppedProgress = new EngineTaskProgress();
    droppedProgress.setEngineTaskState(EngineTaskState.DROPPED);
    String topologyName = oaipmhHarvestPlugin.getTopologyName();
    when(engineTaskClient.getEngineTaskProgress(eq(topologyName), any(), any()))
        .thenReturn(currentlyProcessingProgress)
        .thenReturn(droppedProgress);

    when(workflowExecutionDao.updateMonitorInformation(workflowExecution)).thenReturn(true);
    when(workflowExecutionDao.isCancelling(workflowExecution.getId())).thenReturn(false);

    doNothing().when(workflowExecutionDao).updateWorkflowPlugins(workflowExecution);
    when(workflowExecutionDao.update(workflowExecution))
        .thenReturn(workflowExecution.getId().toString());
    when(workflowExecutionDao.getById(anyString())).thenReturn(workflowExecution);

    WorkflowExecutor<EngineTaskSettings, EngineTask> workflowExecutor = new WorkflowExecutor<>(workflowExecution,
        workflowExecutorSettings);
    workflowExecutor.call();

    verify(workflowExecutionDao, times(2)).updateMonitorInformation(workflowExecution);
    verify(workflowExecutionDao, times(1)).update(workflowExecution);

    InOrder inOrderForPlugin = inOrder(oaipmhHarvestPlugin);
    inOrderForPlugin.verify(oaipmhHarvestPlugin, times(2))
                    .setPluginStatusAndResetFailMessage(PluginStatus.RUNNING);
    inOrderForPlugin.verify(oaipmhHarvestPlugin)
                    .setPluginStatusAndResetFailMessage(PluginStatus.FAILED);
    verify(oaipmhHarvestPlugin, atMost(5)).setPluginStatusAndResetFailMessage(any());
    verify(oaipmhHarvestPlugin).setFailMessage(notNull());
    verify(oaipmhHarvestPlugin, times(1)).setFailMessage(anyString());
  }

  @Test
  void callNonMockedFieldValue_ConsecutiveMonitorFailures() throws Exception {
    OaipmhHarvestPlugin oaipmhHarvestPlugin = Mockito.spy(OaipmhHarvestPlugin.class);
    OaipmhHarvestPluginMetadata oaipmhHarvestPluginMetadata = new OaipmhHarvestPluginMetadata();
    oaipmhHarvestPlugin.setPluginMetadata(oaipmhHarvestPluginMetadata);
    ArrayList<AbstractMetisPlugin<?>> abstractMetisPlugins = new ArrayList<>();
    abstractMetisPlugins.add(oaipmhHarvestPlugin);

    WorkflowExecution workflowExecution = TestObjectFactory.createWorkflowExecutionObject();
    workflowExecution.setId(new ObjectId());
    workflowExecution.setWorkflowStatus(WorkflowStatus.INQUEUE);
    workflowExecution.setMetisPlugins(abstractMetisPlugins);
    workflowExecution.setStartedDate(new Date());

    doReturn(oaipmhHarvestPluginMetadata).when(oaipmhHarvestPlugin).getPluginMetadata();

    Throwable[] engineTaskException100Times = new Throwable[100];
    Arrays.setAll(engineTaskException100Times, index -> new ExternalTaskException("Some error"));

    EngineTaskProgress processedProgress = new EngineTaskProgress();
    processedProgress.setEngineTaskState(EngineTaskState.PROCESSED);
    String topologyName = oaipmhHarvestPlugin.getTopologyName();
    when(engineTaskClient.getEngineTaskProgress(eq(topologyName), any(), any()))
        .thenThrow(engineTaskException100Times)
        .thenReturn(processedProgress);

    when(workflowExecutionDao.updateMonitorInformation(workflowExecution)).thenReturn(true);
    when(workflowExecutionDao.isCancelling(workflowExecution.getId())).thenReturn(false);

    doNothing().when(workflowExecutionDao).updateWorkflowPlugins(workflowExecution);
    when(workflowExecutionDao.update(workflowExecution))
        .thenReturn(workflowExecution.getId().toString());
    when(workflowExecutionDao.getById(anyString())).thenReturn(workflowExecution);

    WorkflowExecutor<EngineTaskSettings, EngineTask> workflowExecutor = new WorkflowExecutor<>(workflowExecution,
        workflowExecutorSettings);
    workflowExecutor.call();

    verify(workflowExecutionDao, times(1)).update(workflowExecution);

    verify(oaipmhHarvestPlugin).setPluginStatusAndResetFailMessage(PluginStatus.FINISHED);
    verify(oaipmhHarvestPlugin, atLeastOnce()).setPluginStatusAndResetFailMessage(PluginStatus.PENDING);
    verify(oaipmhHarvestPlugin, never()).setFailMessage(notNull());
  }

  @Test
  void callNonMockedFieldValue_MonitorFailsOnUnrecoverableExternalTaskException() throws Exception {
    OaipmhHarvestPlugin oaipmhHarvestPlugin = Mockito.spy(OaipmhHarvestPlugin.class);
    OaipmhHarvestPluginMetadata oaipmhHarvestPluginMetadata = new OaipmhHarvestPluginMetadata();
    oaipmhHarvestPlugin.setPluginMetadata(oaipmhHarvestPluginMetadata);
    ArrayList<AbstractMetisPlugin<?>> abstractMetisPlugins = new ArrayList<>();
    abstractMetisPlugins.add(oaipmhHarvestPlugin);

    WorkflowExecution workflowExecution = TestObjectFactory.createWorkflowExecutionObject();
    workflowExecution.setId(new ObjectId());
    workflowExecution.setWorkflowStatus(WorkflowStatus.INQUEUE);
    workflowExecution.setMetisPlugins(abstractMetisPlugins);
    workflowExecution.setStartedDate(new Date());

    doReturn(oaipmhHarvestPluginMetadata).when(oaipmhHarvestPlugin).getPluginMetadata();

    String topologyName = oaipmhHarvestPlugin.getTopologyName();
    when(engineTaskClient.getEngineTaskProgress(eq(topologyName), any(), any(ExecutablePluginType.class)))
        .thenThrow(new ExternalTaskException("",
            new UnrecoverableExternalTaskException("Check progress failed!", new Exception("Some error"))));

    when(workflowExecutionDao.updateMonitorInformation(workflowExecution)).thenReturn(true);
    when(workflowExecutionDao.isCancelling(workflowExecution.getId())).thenReturn(false);

    doNothing().when(workflowExecutionDao).updateWorkflowPlugins(workflowExecution);
    when(workflowExecutionDao.update(workflowExecution))
        .thenReturn(workflowExecution.getId().toString());
    when(workflowExecutionDao.getById(anyString())).thenReturn(workflowExecution);

    WorkflowExecutor<EngineTaskSettings, EngineTask> workflowExecutor = new WorkflowExecutor<>(workflowExecution,
        workflowExecutorSettings);
    workflowExecutor.call();

    verify(workflowExecutionDao, times(1)).update(workflowExecution);

    verify(oaipmhHarvestPlugin).setPluginStatusAndResetFailMessage(PluginStatus.FAILED);
    verify(oaipmhHarvestPlugin, atMost(1)).setPluginStatusAndResetFailMessage(any());
    verify(oaipmhHarvestPlugin).setFailMessage(notNull());
    verify(oaipmhHarvestPlugin, times(1)).setFailMessage(anyString());
  }

  @Test
  void callNonMockedFieldValue_ReachPendingState_and_then_finish() throws Exception {
    OaipmhHarvestPlugin oaipmhHarvestPlugin = Mockito.spy(OaipmhHarvestPlugin.class);
    OaipmhHarvestPluginMetadata oaipmhHarvestPluginMetadata = new OaipmhHarvestPluginMetadata();
    oaipmhHarvestPlugin.setPluginMetadata(oaipmhHarvestPluginMetadata);
    ArrayList<AbstractMetisPlugin<?>> abstractMetisPlugins = new ArrayList<>();
    abstractMetisPlugins.add(oaipmhHarvestPlugin);

    WorkflowExecution workflowExecution = TestObjectFactory.createWorkflowExecutionObject();
    workflowExecution.setId(new ObjectId());
    workflowExecution.setWorkflowStatus(WorkflowStatus.INQUEUE);
    workflowExecution.setMetisPlugins(abstractMetisPlugins);
    workflowExecution.setStartedDate(new Date());

    doReturn(oaipmhHarvestPluginMetadata).when(oaipmhHarvestPlugin).getPluginMetadata();
    final ExternalTaskException exception = new ExternalTaskException("Some error",
        new HttpServerErrorException(HttpStatus.BAD_GATEWAY));
    final ExternalTaskException[] engineTaskExceptions = new ExternalTaskException[WorkflowExecutor.MAX_CANCEL_OR_MONITOR_FAILURES];
    Arrays.fill(engineTaskExceptions, exception);

    EngineTaskProgress currentlyProcessingProgress = new EngineTaskProgress();
    currentlyProcessingProgress.setEngineTaskState(EngineTaskState.CURRENTLY_PROCESSING);
    EngineTaskProgress processedProgress = new EngineTaskProgress();
    processedProgress.setEngineTaskState(EngineTaskState.PROCESSED);
    String topologyName = oaipmhHarvestPlugin.getTopologyName();
    when(engineTaskClient.getEngineTaskProgress(eq(topologyName), any(), any()))
        .thenThrow(engineTaskExceptions)
        .thenReturn(currentlyProcessingProgress)
        .thenReturn(processedProgress);

    when(workflowExecutionDao.updateMonitorInformation(workflowExecution)).thenReturn(true);
    when(workflowExecutionDao.isCancelling(workflowExecution.getId())).thenReturn(false);

    doNothing().when(workflowExecutionDao).updateWorkflowPlugins(workflowExecution);
    when(workflowExecutionDao.getById(anyString())).thenReturn(workflowExecution);

    when(workflowExecutionDao.update(workflowExecution))
        .thenReturn(workflowExecution.getId().toString());

    WorkflowExecutor<EngineTaskSettings, EngineTask> workflowExecutor = new WorkflowExecutor<>(workflowExecution,
        workflowExecutorSettings);
    workflowExecutor.call();

    verify(workflowExecutionDao, times(1)).update(workflowExecution);

    InOrder inOrderForPlugin = inOrder(oaipmhHarvestPlugin);
    inOrderForPlugin.verify(oaipmhHarvestPlugin, times(1))
                    .setPluginStatusAndResetFailMessage(PluginStatus.PENDING);
    inOrderForPlugin.verify(oaipmhHarvestPlugin, times(2))
                    .setPluginStatusAndResetFailMessage(PluginStatus.RUNNING);
    inOrderForPlugin.verify(oaipmhHarvestPlugin, times(1))
                    .setPluginStatusAndResetFailMessage(PluginStatus.FINISHED);
    verify(oaipmhHarvestPlugin, atMost(6)).setPluginStatusAndResetFailMessage(any());
    verify(oaipmhHarvestPlugin, never()).setFailMessage(anyString());
  }


  @Test
  void callNonMockedFieldValueCancellingState() throws Exception {
    OaipmhHarvestPlugin oaipmhHarvestPlugin = Mockito.spy(OaipmhHarvestPlugin.class);
    OaipmhHarvestPluginMetadata oaipmhHarvestPluginMetadata = new OaipmhHarvestPluginMetadata();
    oaipmhHarvestPlugin.setPluginMetadata(oaipmhHarvestPluginMetadata);
    ArrayList<AbstractMetisPlugin<?>> abstractMetisPlugins = new ArrayList<>();
    abstractMetisPlugins.add(oaipmhHarvestPlugin);

    WorkflowExecution workflowExecution = TestObjectFactory.createWorkflowExecutionObject();
    workflowExecution.setId(new ObjectId());
    workflowExecution.setWorkflowStatus(WorkflowStatus.INQUEUE);
    workflowExecution.setMetisPlugins(abstractMetisPlugins);
    workflowExecution.setStartedDate(new Date());

    when(oaipmhHarvestPlugin.getPluginMetadata()).thenReturn(oaipmhHarvestPluginMetadata);

    EngineTaskProgress currentlyProcessingProgress = new EngineTaskProgress();
    currentlyProcessingProgress.setEngineTaskState(EngineTaskState.CURRENTLY_PROCESSING);
    EngineTaskProgress processedProgress = new EngineTaskProgress();
    processedProgress.setEngineTaskState(EngineTaskState.PROCESSED);
    String topologyName = oaipmhHarvestPlugin.getTopologyName();
    when(engineTaskClient.getEngineTaskProgress(eq(topologyName), any(), any()))
        .thenReturn(currentlyProcessingProgress)
        .thenReturn(processedProgress);
    String message = SystemId.SYSTEM_MINUTE_CAP_EXPIRE.name();
    doNothing().when(engineTaskClient).cancelEngineTask(eq(topologyName), any(), eq(message), isNull());

    when(workflowExecutionDao.updateMonitorInformation(workflowExecution)).thenReturn(true);
    when(workflowExecutionDao.isCancelling(workflowExecution.getId())).thenReturn(false)
                                                                      .thenReturn(true);
    when(workflowExecutionDao.getById(workflowExecution.getId().toString()))
        .thenReturn(workflowExecution);

    doNothing().when(workflowExecutionDao).updateWorkflowPlugins(workflowExecution);
    when(workflowExecutionDao.update(workflowExecution))
        .thenReturn(workflowExecution.getId().toString());

    WorkflowExecutor<EngineTaskSettings, EngineTask> workflowExecutor = new WorkflowExecutor<>(workflowExecution,
        workflowExecutorSettings);
    workflowExecutor.call();

    verify(workflowExecutionDao, times(2)).updateMonitorInformation(workflowExecution);
    verify(workflowExecutionDao, times(1)).update(workflowExecution);

    verify(oaipmhHarvestPlugin, never()).setFailMessage(anyString());
  }

  @Test
  void callExecutionInRUNNINGState() throws ExternalTaskException {
    OaipmhHarvestPlugin oaipmhHarvestPlugin = Mockito.spy(OaipmhHarvestPlugin.class);
    oaipmhHarvestPlugin.setPluginStatus(PluginStatus.RUNNING);
    OaipmhHarvestPluginMetadata oaipmhHarvestPluginMetadata = new OaipmhHarvestPluginMetadata();
    oaipmhHarvestPlugin.setPluginMetadata(oaipmhHarvestPluginMetadata);
    oaipmhHarvestPlugin.setStartedDate(new Date());
    ArrayList<AbstractMetisPlugin<?>> abstractMetisPlugins = new ArrayList<>();
    abstractMetisPlugins.add(oaipmhHarvestPlugin);

    WorkflowExecution workflowExecution = TestObjectFactory.createWorkflowExecutionObject();
    workflowExecution.setId(new ObjectId());
    workflowExecution.setWorkflowStatus(WorkflowStatus.RUNNING);
    workflowExecution.setMetisPlugins(abstractMetisPlugins);
    workflowExecution.setStartedDate(oaipmhHarvestPlugin.getStartedDate());
    workflowExecution.setNextExecutablePluginType(ExecutablePluginType.OAIPMH_HARVEST);

    when(oaipmhHarvestPlugin.getPluginMetadata()).thenReturn(oaipmhHarvestPluginMetadata);

    EngineTaskProgress currentlyProcessingProgress = new EngineTaskProgress();
    currentlyProcessingProgress.setEngineTaskState(EngineTaskState.CURRENTLY_PROCESSING);
    String topologyName = oaipmhHarvestPlugin.getTopologyName();
    when(engineTaskClient.getEngineTaskProgress(eq(topologyName), any(), any()))
        .thenReturn(currentlyProcessingProgress);

    when(workflowExecutionDao.updateMonitorInformation(workflowExecution)).thenReturn(true);
    when(workflowExecutionDao.isCancelling(workflowExecution.getId())).thenReturn(false);

    EngineTaskProgress processedProgress = new EngineTaskProgress();
    processedProgress.setEngineTaskState(EngineTaskState.PROCESSED);
    when(engineTaskClient.getEngineTaskProgress(eq(topologyName), any(), any()))
        .thenReturn(currentlyProcessingProgress)
        .thenReturn(processedProgress);

    doNothing().when(workflowExecutionDao).updateWorkflowPlugins(workflowExecution);
    when(workflowExecutionDao.update(workflowExecution))
        .thenReturn(workflowExecution.getId().toString());
    when(workflowExecutionDao.getById(anyString())).thenReturn(workflowExecution);

    WorkflowExecutor<EngineTaskSettings, EngineTask> workflowExecutor = new WorkflowExecutor<>(workflowExecution,
        workflowExecutorSettings);
    workflowExecutor.call();

    assertEquals(WorkflowStatus.FINISHED, workflowExecution.getWorkflowStatus());
    assertNotNull(workflowExecution.getStartedDate());
    assertNotNull(workflowExecution.getUpdatedDate());
    assertNotNull(workflowExecution.getFinishedDate());
    assertNotNull(workflowExecution.getMetisPlugins().getFirst().getFinishedDate());
  }

  @Test
  void callCancellingStateINQUEUE() throws ExternalTaskException {
    OaipmhHarvestPlugin oaipmhHarvestPlugin = Mockito.spy(new OaipmhHarvestPlugin());
    OaipmhHarvestPluginMetadata oaipmhHarvestPluginMetadata = new OaipmhHarvestPluginMetadata();
    oaipmhHarvestPlugin.setPluginMetadata(oaipmhHarvestPluginMetadata);
    ArrayList<AbstractMetisPlugin<?>> abstractMetisPlugins = new ArrayList<>();
    abstractMetisPlugins.add(oaipmhHarvestPlugin);
    final ObjectId objectId = new ObjectId();
    WorkflowExecution workflowExecution = TestObjectFactory.createWorkflowExecutionObject();
    workflowExecution.setId(objectId);
    workflowExecution.setMetisPlugins(abstractMetisPlugins);
    workflowExecution.setNextExecutablePluginType(ExecutablePluginType.OAIPMH_HARVEST);
    workflowExecution.setCancelledBy(SystemId.SYSTEM_MINUTE_CAP_EXPIRE.name());

    when(workflowExecutionDao.isCancelling(workflowExecution.getId())).thenReturn(true);

    String topologyName = oaipmhHarvestPlugin.getTopologyName();
    String message = SystemId.SYSTEM_MINUTE_CAP_EXPIRE.name();
    doNothing().when(engineTaskClient).cancelEngineTask(eq(topologyName), any(), eq(message), isNull());

    EngineTaskProgress droppedProgress = new EngineTaskProgress();
    droppedProgress.setEngineTaskState(EngineTaskState.DROPPED);
    when(engineTaskClient.getEngineTaskProgress(anyString(), any(), any())).thenReturn(droppedProgress);
    when(workflowExecutionDao.getById(workflowExecution.getId().toString())).thenReturn(workflowExecution);

    WorkflowExecutor<EngineTaskSettings, EngineTask> workflowExecutor =
        new WorkflowExecutor<>(workflowExecution, workflowExecutorSettings);
    workflowExecutor.call();

    ArgumentCaptor<WorkflowExecution> workflowExecutionArgumentCaptor = ArgumentCaptor.forClass(WorkflowExecution.class);
    verify(workflowExecutionDao, times(2)).update(workflowExecutionArgumentCaptor.capture());
    WorkflowExecution lastWorkflowExecutionUpdate = workflowExecutionArgumentCaptor.getAllValues().getLast();
    assertEquals(WorkflowStatus.CANCELLED, lastWorkflowExecutionUpdate.getWorkflowStatus());
    assertEquals(PluginStatus.CANCELLED, lastWorkflowExecutionUpdate.getMetisPlugins().getFirst().getPluginStatus());
    assertEquals(SystemId.SYSTEM_MINUTE_CAP_EXPIRE.name(), workflowExecutionArgumentCaptor.getValue().getCancelledBy());
  }

  @Test
  void callCancellingStateRUNNING() throws ExternalTaskException {
    OaipmhHarvestPlugin oaipmhHarvestPlugin = Mockito.spy(new OaipmhHarvestPlugin());
    oaipmhHarvestPlugin.setPluginStatus(PluginStatus.RUNNING);
    OaipmhHarvestPluginMetadata oaipmhHarvestPluginMetadata = new OaipmhHarvestPluginMetadata();
    oaipmhHarvestPlugin.setPluginMetadata(oaipmhHarvestPluginMetadata);
    oaipmhHarvestPlugin.setStartedDate(new Date());
    ArrayList<AbstractMetisPlugin<?>> abstractMetisPlugins = new ArrayList<>();
    abstractMetisPlugins.add(oaipmhHarvestPlugin);
    final ObjectId objectId = new ObjectId();
    WorkflowExecution workflowExecution = TestObjectFactory.createWorkflowExecutionObject();
    workflowExecution.setId(objectId);
    workflowExecution.setMetisPlugins(abstractMetisPlugins);
    workflowExecution.setWorkflowStatus(WorkflowStatus.RUNNING);
    workflowExecution.setNextExecutablePluginType(ExecutablePluginType.OAIPMH_HARVEST);
    workflowExecution.setCancelledBy(SystemId.SYSTEM_MINUTE_CAP_EXPIRE.name());

    when(workflowExecutionDao.isCancelling(workflowExecution.getId())).thenReturn(true);

    String topologyName = oaipmhHarvestPlugin.getTopologyName();
    String message = SystemId.SYSTEM_MINUTE_CAP_EXPIRE.name();
    doNothing().when(engineTaskClient).cancelEngineTask(eq(topologyName), any(), eq(message), isNull());

    EngineTaskProgress currentlyProcessingProgress = new EngineTaskProgress();
    currentlyProcessingProgress.setEngineTaskState(EngineTaskState.CURRENTLY_PROCESSING);
    when(engineTaskClient.getEngineTaskProgress(anyString(), any(), any())).thenReturn(currentlyProcessingProgress);
    when(workflowExecutionDao.getById(workflowExecution.getId().toString())).thenReturn(workflowExecution);

    WorkflowExecutor<EngineTaskSettings, EngineTask> workflowExecutor =
        new WorkflowExecutor<>(workflowExecution, workflowExecutorSettings);
    workflowExecutor.call();

    ArgumentCaptor<WorkflowExecution> workflowExecutionArgumentCaptor = ArgumentCaptor.forClass(WorkflowExecution.class);
    verify(workflowExecutionDao, times(2)).update(workflowExecutionArgumentCaptor.capture());
    WorkflowExecution lastWorkflowExecutionUpdate = workflowExecutionArgumentCaptor.getAllValues().getLast();
    assertEquals(WorkflowStatus.CANCELLED, lastWorkflowExecutionUpdate.getWorkflowStatus());
    assertEquals(SystemId.SYSTEM_MINUTE_CAP_EXPIRE.name(), workflowExecutionArgumentCaptor.getValue().getCancelledBy());
  }

  @Test
  void call_whenSubmitThrows_thenPluginFailsImmediately_andMonitoringIsNotStarted() throws Exception {
    OaipmhHarvestPlugin plugin = Mockito.spy(new OaipmhHarvestPlugin());
    OaipmhHarvestPluginMetadata metadata = new OaipmhHarvestPluginMetadata();
    plugin.setPluginMetadata(metadata);

    ArrayList<AbstractMetisPlugin<?>> plugins = new ArrayList<>();
    plugins.add(plugin);

    WorkflowExecution workflowExecution = TestObjectFactory.createWorkflowExecutionObject();
    workflowExecution.setId(new ObjectId());
    workflowExecution.setWorkflowStatus(WorkflowStatus.INQUEUE);
    workflowExecution.setMetisPlugins(plugins);
    workflowExecution.setStartedDate(new Date());

    doThrow(new ExternalTaskException("Submit failed"))
        .when(engineTaskClient)
        .submitEngineTask(any(EngineTask.class), anyString());

    WorkflowExecutor<EngineTaskSettings, EngineTask> executor =
        new WorkflowExecutor<>(workflowExecution, workflowExecutorSettings);

    executor.call();
    verify(plugin).setPluginStatusAndResetFailMessage(PluginStatus.FAILED);
    verify(plugin).setFailMessage(anyString());
    verify(engineTaskClient, never()).getEngineTaskProgress(anyString(), anyString(), any(ExecutablePluginType.class));
    assertNotEquals(WorkflowStatus.FINISHED, workflowExecution.getWorkflowStatus());
    verify(workflowExecutionDao, times(1)).update(workflowExecution);
  }

  @Test
  void call_noNextExecutablePluginType_returnsImmediately() {
    WorkflowExecution workflowExecution = TestObjectFactory.createWorkflowExecutionObject();
    workflowExecution.setNextExecutablePluginType(null);

    WorkflowExecutor<EngineTaskSettings, EngineTask> executor =
        new WorkflowExecutor<>(workflowExecution, workflowExecutorSettings);
    executor.call();
    verify(workflowExecutionDao, never()).updateMonitorInformation(any());
    verify(workflowExecutionDao, atMost(1)).update(workflowExecution);
  }

  @Test
  void call_notMatchingNextExecutablePluginType_returnsImmediately() {
    WorkflowExecution workflowExecution = TestObjectFactory.createWorkflowExecutionObject();
    workflowExecution.setNextExecutablePluginType(ExecutablePluginType.PREVIEW);

    WorkflowExecutor<EngineTaskSettings, EngineTask> executor =
        new WorkflowExecutor<>(workflowExecution, workflowExecutorSettings);
    executor.call();
    verify(workflowExecutionDao, never()).updateMonitorInformation(any());
    verify(workflowExecutionDao, atMost(1)).update(workflowExecution);
  }

  @Test
  void sleepMonitorInterval_whenThreadAlreadyInterrupted_gracefullyExit() {
    WorkflowExecution workflowExecution = TestObjectFactory.createWorkflowExecutionObject();
    WorkflowExecutor<EngineTaskSettings, EngineTask> executor = new WorkflowExecutor<>(workflowExecution, workflowExecutorSettings);

    Thread.currentThread().interrupt();
    WorkflowExecution result = executor.call();

    assertNotNull(result);
    assertTrue(Thread.currentThread().isInterrupted());
    verify(workflowExecutionDao, atMost(1)).updateMonitorInformation(any());
    verify(workflowExecutionDao, atMost(2)).update(workflowExecution);
  }
}
