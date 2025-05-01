package eu.europeana.metis.core.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europeana.metis.core.dao.WorkflowExecutionDao;
import eu.europeana.metis.core.engine.base.ProcessingEngineTask;
import eu.europeana.metis.core.engine.base.ProcessingEngineTaskClient;
import eu.europeana.metis.core.engine.base.ProcessingEngineTaskSettings;
import eu.europeana.metis.core.engine.base.report.task.ProcessingEngineTaskProgress;
import eu.europeana.metis.core.engine.base.report.task.ProcessingEngineTaskState;
import eu.europeana.metis.core.utils.TestObjectFactory;
import eu.europeana.metis.core.workflow.WorkflowExecution;
import eu.europeana.metis.core.workflow.WorkflowStatus;
import eu.europeana.metis.core.workflow.execution.SystemId;
import eu.europeana.metis.core.workflow.plugins.AbstractMetisPlugin;
import eu.europeana.metis.core.workflow.plugins.OaipmhHarvestPlugin;
import eu.europeana.metis.core.workflow.plugins.OaipmhHarvestPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.PluginStatus;
import eu.europeana.metis.exception.ExternalTaskException;
import eu.europeana.metis.exception.UnrecoverableExternalTaskException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.function.Supplier;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;

/**
 * @author Simon Tzanakis (Simon.Tzanakis@europeana.eu)
 * @since 2017-10-17
 */
class TestWorkflowExecutor {

  private static WorkflowExecutionDao workflowExecutionDao;
  private static WorkflowPostProcessor workflowPostProcessor;
  private static ProcessingEngineTaskClient<ProcessingEngineTaskSettings, ProcessingEngineTask> processingEngineTaskClient;
  private static WorkflowExecutionMonitor workflowExecutionMonitor;
  private static WorkflowExecutorManager workflowExecutorManager;
  private static WorkflowExecutionSettings workflowExecutionSettings;

  @BeforeAll
  static void prepare() {
    workflowExecutionDao = Mockito.mock(WorkflowExecutionDao.class);
    workflowPostProcessor = Mockito.mock(WorkflowPostProcessor.class);
    processingEngineTaskClient = mock(ProcessingEngineTaskClient.class);
    workflowExecutionMonitor = Mockito.mock(WorkflowExecutionMonitor.class);
    workflowExecutorManager = new WorkflowExecutorManager(new SemaphoresPerPluginManager(2), workflowExecutionDao,
        workflowPostProcessor, null, null, null, processingEngineTaskClient);
    workflowExecutionSettings = Mockito.mock(WorkflowExecutionSettings.class);
    when(workflowExecutionSettings.getPeriodOfNoProcessedRecordsChangeInMinutes()).thenReturn(10);
  }

  @BeforeEach
  void cleanUp() {
    Mockito.reset(workflowExecutionDao);
    Mockito.reset(workflowPostProcessor);
    Mockito.reset(workflowExecutionMonitor);
    Mockito.reset(processingEngineTaskClient);

    ProcessingEngineTask processingEngineTask = mock(ProcessingEngineTask.class);
    Supplier<ProcessingEngineTask> taskCreator = () -> processingEngineTask;
    when(processingEngineTaskClient.getTaskCreator()).thenReturn(taskCreator);
    ProcessingEngineTaskSettings processingEngineTaskSettings = mock(ProcessingEngineTaskSettings.class);
    when(processingEngineTaskClient.getProcessingEngineTaskSettings()).thenReturn(processingEngineTaskSettings);
  }

  @Test
  void callNonMockedFieldValue() throws Exception {
    OaipmhHarvestPlugin oaipmhHarvestPlugin = Mockito.spy(OaipmhHarvestPlugin.class);
    OaipmhHarvestPluginMetadata oaipmhHarvestPluginMetadata = new OaipmhHarvestPluginMetadata();
    oaipmhHarvestPlugin.setPluginMetadata(oaipmhHarvestPluginMetadata);
    ArrayList<AbstractMetisPlugin> abstractMetisPlugins = new ArrayList<>();
    abstractMetisPlugins.add(oaipmhHarvestPlugin);

    WorkflowExecution workflowExecution = TestObjectFactory.createWorkflowExecutionObject();
    workflowExecution.setId(new ObjectId());
    workflowExecution.setWorkflowStatus(WorkflowStatus.INQUEUE);
    workflowExecution.setMetisPlugins(abstractMetisPlugins);
    workflowExecution.setStartedDate(new Date());

    doReturn(oaipmhHarvestPluginMetadata).when(oaipmhHarvestPlugin).getPluginMetadata();

    ProcessingEngineTaskProgress currentlyProcessingProgress = new ProcessingEngineTaskProgress();
    currentlyProcessingProgress.setProcessingEngineTaskState(ProcessingEngineTaskState.CURRENTLY_PROCESSING);
    ProcessingEngineTaskProgress processedProgress = new ProcessingEngineTaskProgress();
    processedProgress.setProcessingEngineTaskState(ProcessingEngineTaskState.PROCESSED);
    when(processingEngineTaskClient.getTaskProgress(eq(oaipmhHarvestPlugin.getTopologyName()), anyLong()))
        .thenReturn(currentlyProcessingProgress)
        .thenReturn(processedProgress);

    doNothing().when(workflowExecutionDao).updateMonitorInformation(workflowExecution);
    when(workflowExecutionDao.isCancelling(workflowExecution.getId())).thenReturn(false);

    doNothing().when(workflowExecutionDao).updateWorkflowPlugins(workflowExecution);
    when(workflowExecutionDao.update(workflowExecution))
        .thenReturn(workflowExecution.getId().toString());
    when(workflowExecutionDao.getById(anyString())).thenReturn(workflowExecution);

    WorkflowExecutor workflowExecutor = new WorkflowExecutor(workflowExecution, workflowExecutorManager,
        workflowExecutionSettings);
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
  void callNonMockedFieldValue_DROPPEDExeternalTaskButNotCancelled() throws Exception {
    OaipmhHarvestPlugin oaipmhHarvestPlugin = Mockito.spy(OaipmhHarvestPlugin.class);
    OaipmhHarvestPluginMetadata oaipmhHarvestPluginMetadata = new OaipmhHarvestPluginMetadata();
    oaipmhHarvestPlugin.setPluginMetadata(oaipmhHarvestPluginMetadata);
    ArrayList<AbstractMetisPlugin> abstractMetisPlugins = new ArrayList<>();
    abstractMetisPlugins.add(oaipmhHarvestPlugin);

    WorkflowExecution workflowExecution = TestObjectFactory.createWorkflowExecutionObject();
    workflowExecution.setId(new ObjectId());
    workflowExecution.setWorkflowStatus(WorkflowStatus.INQUEUE);
    workflowExecution.setMetisPlugins(abstractMetisPlugins);
    workflowExecution.setStartedDate(new Date());

    ProcessingEngineTaskProgress currentlyProcessingProgress = new ProcessingEngineTaskProgress();
    currentlyProcessingProgress.setProcessingEngineTaskState(ProcessingEngineTaskState.CURRENTLY_PROCESSING);
    ProcessingEngineTaskProgress droppedProgress = new ProcessingEngineTaskProgress();
    droppedProgress.setProcessingEngineTaskState(ProcessingEngineTaskState.DROPPED);
    when(processingEngineTaskClient.getTaskProgress(eq(oaipmhHarvestPlugin.getTopologyName()), anyLong()))
        .thenReturn(currentlyProcessingProgress)
        .thenReturn(droppedProgress);

    doNothing().when(workflowExecutionDao).updateMonitorInformation(workflowExecution);
    when(workflowExecutionDao.isCancelling(workflowExecution.getId())).thenReturn(false);

    doNothing().when(workflowExecutionDao).updateWorkflowPlugins(workflowExecution);
    when(workflowExecutionDao.update(workflowExecution))
        .thenReturn(workflowExecution.getId().toString());
    when(workflowExecutionDao.getById(anyString())).thenReturn(workflowExecution);

    WorkflowExecutor workflowExecutor = new WorkflowExecutor(workflowExecution, workflowExecutorManager,
        workflowExecutionSettings);
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
    ArrayList<AbstractMetisPlugin> abstractMetisPlugins = new ArrayList<>();
    abstractMetisPlugins.add(oaipmhHarvestPlugin);

    WorkflowExecution workflowExecution = TestObjectFactory.createWorkflowExecutionObject();
    workflowExecution.setId(new ObjectId());
    workflowExecution.setWorkflowStatus(WorkflowStatus.INQUEUE);
    workflowExecution.setMetisPlugins(abstractMetisPlugins);
    workflowExecution.setStartedDate(new Date());

    doReturn(oaipmhHarvestPluginMetadata).when(oaipmhHarvestPlugin).getPluginMetadata();

    Throwable[] externalTaskException100Times = new Throwable[100];
    Arrays.setAll(externalTaskException100Times, index -> new ExternalTaskException("Some error"));

    ProcessingEngineTaskProgress processedProgress = new ProcessingEngineTaskProgress();
    processedProgress.setProcessingEngineTaskState(ProcessingEngineTaskState.PROCESSED);
    when(processingEngineTaskClient.getTaskProgress(eq(oaipmhHarvestPlugin.getTopologyName()), anyLong()))
        .thenThrow(externalTaskException100Times)
        .thenReturn(processedProgress);

    doNothing().when(workflowExecutionDao).updateMonitorInformation(workflowExecution);
    when(workflowExecutionDao.isCancelling(workflowExecution.getId())).thenReturn(false);

    doNothing().when(workflowExecutionDao).updateWorkflowPlugins(workflowExecution);
    when(workflowExecutionDao.update(workflowExecution))
        .thenReturn(workflowExecution.getId().toString());
    when(workflowExecutionDao.getById(anyString())).thenReturn(workflowExecution);
    when(workflowExecutionSettings.getDpsMonitorCheckIntervalInSecs()).thenReturn(0);
    WorkflowExecutor workflowExecutor = new WorkflowExecutor(workflowExecution, workflowExecutorManager,
        workflowExecutionSettings);
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
    ArrayList<AbstractMetisPlugin> abstractMetisPlugins = new ArrayList<>();
    abstractMetisPlugins.add(oaipmhHarvestPlugin);

    WorkflowExecution workflowExecution = TestObjectFactory.createWorkflowExecutionObject();
    workflowExecution.setId(new ObjectId());
    workflowExecution.setWorkflowStatus(WorkflowStatus.INQUEUE);
    workflowExecution.setMetisPlugins(abstractMetisPlugins);
    workflowExecution.setStartedDate(new Date());

    doReturn(oaipmhHarvestPluginMetadata).when(oaipmhHarvestPlugin).getPluginMetadata();

    when(processingEngineTaskClient.getTaskProgress(eq(oaipmhHarvestPlugin.getTopologyName()), anyLong()))
        .thenThrow(new UnrecoverableExternalTaskException("Check progress failed!", new Exception("Some error")));

    doNothing().when(workflowExecutionDao).updateMonitorInformation(workflowExecution);
    when(workflowExecutionDao.isCancelling(workflowExecution.getId())).thenReturn(false);

    doNothing().when(workflowExecutionDao).updateWorkflowPlugins(workflowExecution);
    when(workflowExecutionDao.update(workflowExecution))
        .thenReturn(workflowExecution.getId().toString());
    when(workflowExecutionDao.getById(anyString())).thenReturn(workflowExecution);

    WorkflowExecutor workflowExecutor = new WorkflowExecutor(workflowExecution, workflowExecutorManager,
        workflowExecutionSettings);
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
    ArrayList<AbstractMetisPlugin> abstractMetisPlugins = new ArrayList<>();
    abstractMetisPlugins.add(oaipmhHarvestPlugin);

    WorkflowExecution workflowExecution = TestObjectFactory.createWorkflowExecutionObject();
    workflowExecution.setId(new ObjectId());
    workflowExecution.setWorkflowStatus(WorkflowStatus.INQUEUE);
    workflowExecution.setMetisPlugins(abstractMetisPlugins);
    workflowExecution.setStartedDate(new Date());

    doReturn(oaipmhHarvestPluginMetadata).when(oaipmhHarvestPlugin).getPluginMetadata();
    final ExternalTaskException exception = new ExternalTaskException("Some error",
        new HttpServerErrorException(HttpStatus.BAD_GATEWAY));
    final ExternalTaskException[] externalTaskExceptions = new ExternalTaskException[WorkflowExecutor.MAX_CANCEL_OR_MONITOR_FAILURES];
    Arrays.fill(externalTaskExceptions, exception);

    ProcessingEngineTaskProgress currentlyProcessingProgress = new ProcessingEngineTaskProgress();
    currentlyProcessingProgress.setProcessingEngineTaskState(ProcessingEngineTaskState.CURRENTLY_PROCESSING);
    ProcessingEngineTaskProgress processedProgress = new ProcessingEngineTaskProgress();
    processedProgress.setProcessingEngineTaskState(ProcessingEngineTaskState.PROCESSED);
    when(processingEngineTaskClient.getTaskProgress(eq(oaipmhHarvestPlugin.getTopologyName()), anyLong()))
        .thenThrow(externalTaskExceptions)
        .thenReturn(currentlyProcessingProgress)
        .thenReturn(processedProgress);

    doNothing().when(workflowExecutionDao).updateMonitorInformation(workflowExecution);
    when(workflowExecutionDao.isCancelling(workflowExecution.getId())).thenReturn(false);

    doNothing().when(workflowExecutionDao).updateWorkflowPlugins(workflowExecution);
    when(workflowExecutionDao.getById(anyString())).thenReturn(workflowExecution);

    when(workflowExecutionDao.update(workflowExecution))
        .thenReturn(workflowExecution.getId().toString());

    WorkflowExecutor workflowExecutor = new WorkflowExecutor(workflowExecution, workflowExecutorManager,
        workflowExecutionSettings);
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
    ArrayList<AbstractMetisPlugin> abstractMetisPlugins = new ArrayList<>();
    abstractMetisPlugins.add(oaipmhHarvestPlugin);

    WorkflowExecution workflowExecution = TestObjectFactory.createWorkflowExecutionObject();
    workflowExecution.setId(new ObjectId());
    workflowExecution.setWorkflowStatus(WorkflowStatus.INQUEUE);
    workflowExecution.setMetisPlugins(abstractMetisPlugins);
    workflowExecution.setStartedDate(new Date());

    when(oaipmhHarvestPlugin.getPluginMetadata()).thenReturn(oaipmhHarvestPluginMetadata);

    ProcessingEngineTaskProgress currentlyProcessingProgress = new ProcessingEngineTaskProgress();
    currentlyProcessingProgress.setProcessingEngineTaskState(ProcessingEngineTaskState.CURRENTLY_PROCESSING);
    ProcessingEngineTaskProgress processedProgress = new ProcessingEngineTaskProgress();
    processedProgress.setProcessingEngineTaskState(ProcessingEngineTaskState.PROCESSED);
    when(processingEngineTaskClient.getTaskProgress(eq(oaipmhHarvestPlugin.getTopologyName()), anyLong()))
        .thenReturn(currentlyProcessingProgress)
        .thenReturn(processedProgress);

    String topologyName = oaipmhHarvestPlugin.getTopologyName();
    String message = SystemId.SYSTEM_MINUTE_CAP_EXPIRE.name();
    doNothing().when(processingEngineTaskClient).cancel(eq(topologyName), anyLong(), eq(message));

    doNothing().when(workflowExecutionDao).updateMonitorInformation(workflowExecution);
    when(workflowExecutionDao.isCancelling(workflowExecution.getId())).thenReturn(false)
                                                                      .thenReturn(true);
    when(workflowExecutionDao.getById(workflowExecution.getId().toString()))
        .thenReturn(workflowExecution);

    doNothing().when(workflowExecutionDao).updateWorkflowPlugins(workflowExecution);
    when(workflowExecutionDao.update(workflowExecution))
        .thenReturn(workflowExecution.getId().toString());

    WorkflowExecutor workflowExecutor = new WorkflowExecutor(workflowExecution, workflowExecutorManager,
        workflowExecutionSettings);
    workflowExecutor.call();

    verify(workflowExecutionDao, times(2)).updateMonitorInformation(workflowExecution);
    verify(workflowExecutionDao, times(1)).update(workflowExecution);

    verify(oaipmhHarvestPlugin, never()).setFailMessage(anyString());
  }

  @Test
  void callExecutionInRUNNINGState() throws ExternalTaskException, UnrecoverableExternalTaskException {
    OaipmhHarvestPlugin oaipmhHarvestPlugin = Mockito.spy(OaipmhHarvestPlugin.class);
    oaipmhHarvestPlugin.setPluginStatus(PluginStatus.FINISHED);
    OaipmhHarvestPluginMetadata oaipmhHarvestPluginMetadata = new OaipmhHarvestPluginMetadata();
    oaipmhHarvestPlugin.setPluginMetadata(oaipmhHarvestPluginMetadata);
    oaipmhHarvestPlugin.setStartedDate(new Date());
    ArrayList<AbstractMetisPlugin> abstractMetisPlugins = new ArrayList<>();
    abstractMetisPlugins.add(oaipmhHarvestPlugin);

    WorkflowExecution workflowExecution = TestObjectFactory.createWorkflowExecutionObject();
    workflowExecution.setId(new ObjectId());
    workflowExecution.setWorkflowStatus(WorkflowStatus.RUNNING);
    workflowExecution.setMetisPlugins(abstractMetisPlugins);
    workflowExecution.setStartedDate(oaipmhHarvestPlugin.getStartedDate());

    when(oaipmhHarvestPlugin.getPluginMetadata()).thenReturn(oaipmhHarvestPluginMetadata);

    ProcessingEngineTaskProgress currentlyProcessingProgress = new ProcessingEngineTaskProgress();
    currentlyProcessingProgress.setProcessingEngineTaskState(ProcessingEngineTaskState.CURRENTLY_PROCESSING);
    when(processingEngineTaskClient.getTaskProgress(eq(oaipmhHarvestPlugin.getTopologyName()), anyLong()))
        .thenReturn(currentlyProcessingProgress);

    doNothing().when(workflowExecutionDao).updateMonitorInformation(workflowExecution);
    when(workflowExecutionDao.isCancelling(workflowExecution.getId())).thenReturn(false);

    ProcessingEngineTaskProgress processedProgress = new ProcessingEngineTaskProgress();
    processedProgress.setProcessingEngineTaskState(ProcessingEngineTaskState.PROCESSED);
    when(processingEngineTaskClient.getTaskProgress(eq(oaipmhHarvestPlugin.getTopologyName()), anyLong()))
        .thenReturn(currentlyProcessingProgress)
        .thenReturn(processedProgress);

    doNothing().when(workflowExecutionDao).updateWorkflowPlugins(workflowExecution);
    when(workflowExecutionDao.update(workflowExecution))
        .thenReturn(workflowExecution.getId().toString());
    when(workflowExecutionDao.getById(anyString())).thenReturn(workflowExecution);

    WorkflowExecutor workflowExecutor = new WorkflowExecutor(workflowExecution, workflowExecutorManager,
        workflowExecutionSettings);
    workflowExecutor.call();

    assertEquals(WorkflowStatus.FINISHED, workflowExecution.getWorkflowStatus());
    assertNotNull(workflowExecution.getStartedDate());
    assertNotNull(workflowExecution.getUpdatedDate());
    assertNotNull(workflowExecution.getFinishedDate());
    assertNotNull(workflowExecution.getMetisPlugins().getFirst().getFinishedDate());
  }

  @Test
  void callCancellingStateINQUEUE() throws ExternalTaskException, UnrecoverableExternalTaskException {
    OaipmhHarvestPlugin oaipmhHarvestPlugin = Mockito.spy(new OaipmhHarvestPlugin());
    OaipmhHarvestPluginMetadata oaipmhHarvestPluginMetadata = new OaipmhHarvestPluginMetadata();
    oaipmhHarvestPlugin.setPluginMetadata(oaipmhHarvestPluginMetadata);
    ArrayList<AbstractMetisPlugin> abstractMetisPlugins = new ArrayList<>();
    abstractMetisPlugins.add(oaipmhHarvestPlugin);
    final ObjectId objectId = new ObjectId();
    WorkflowExecution workflowExecution = TestObjectFactory.createWorkflowExecutionObject();
    workflowExecution.setId(objectId);
    workflowExecution.setMetisPlugins(abstractMetisPlugins);
    workflowExecution.setCancelledBy(SystemId.SYSTEM_MINUTE_CAP_EXPIRE.name());

    when(workflowExecutionMonitor.claimExecution(workflowExecution.getId().toString()))
        .thenReturn(new ImmutablePair<>(workflowExecution, true));
    when(workflowExecutionDao.isCancelling(workflowExecution.getId())).thenReturn(true);

    String topologyName = oaipmhHarvestPlugin.getTopologyName();
    String message = SystemId.SYSTEM_MINUTE_CAP_EXPIRE.name();
    doNothing().when(processingEngineTaskClient).cancel(eq(topologyName), anyLong(), eq(message));

    ProcessingEngineTaskProgress droppedProgress = new ProcessingEngineTaskProgress();
    droppedProgress.setProcessingEngineTaskState(ProcessingEngineTaskState.DROPPED);
    when(processingEngineTaskClient.getTaskProgress(anyString(), anyLong()))
        .thenReturn(droppedProgress);

    when(workflowExecutionDao.getById(workflowExecution.getId().toString()))
        .thenReturn(workflowExecution);

    WorkflowExecutor workflowExecutor = new WorkflowExecutor(workflowExecution, workflowExecutorManager,
        workflowExecutionSettings);
    workflowExecutor.call();

    ArgumentCaptor<WorkflowExecution> workflowExecutionArgumentCaptor = ArgumentCaptor
        .forClass(WorkflowExecution.class);
    verify(workflowExecutionDao, times(1)).update(workflowExecutionArgumentCaptor.capture());
    assertEquals(WorkflowStatus.CANCELLED,
        workflowExecutionArgumentCaptor.getValue().getWorkflowStatus());
    assertEquals(SystemId.SYSTEM_MINUTE_CAP_EXPIRE.name(),
        workflowExecutionArgumentCaptor.getValue().getCancelledBy());
  }

  @Test
  void callCancellingStateRUNNING() throws ExternalTaskException, UnrecoverableExternalTaskException {
    OaipmhHarvestPlugin oaipmhHarvestPlugin = Mockito.spy(new OaipmhHarvestPlugin());
    oaipmhHarvestPlugin.setPluginStatus(PluginStatus.RUNNING);
    OaipmhHarvestPluginMetadata oaipmhHarvestPluginMetadata = new OaipmhHarvestPluginMetadata();
    oaipmhHarvestPlugin.setPluginMetadata(oaipmhHarvestPluginMetadata);
    ArrayList<AbstractMetisPlugin> abstractMetisPlugins = new ArrayList<>();
    abstractMetisPlugins.add(oaipmhHarvestPlugin);
    final ObjectId objectId = new ObjectId();
    WorkflowExecution workflowExecution = TestObjectFactory.createWorkflowExecutionObject();
    workflowExecution.setId(objectId);
    workflowExecution.setMetisPlugins(abstractMetisPlugins);
    workflowExecution.setWorkflowStatus(WorkflowStatus.RUNNING);
    workflowExecution.setCancelledBy(SystemId.SYSTEM_MINUTE_CAP_EXPIRE.name());

    when(workflowExecutionMonitor.claimExecution(workflowExecution.getId().toString()))
        .thenReturn(new ImmutablePair<>(workflowExecution, true));
    when(workflowExecutionDao.isCancelling(workflowExecution.getId())).thenReturn(true);

    String topologyName = oaipmhHarvestPlugin.getTopologyName();
    String message = SystemId.SYSTEM_MINUTE_CAP_EXPIRE.name();
    doNothing().when(processingEngineTaskClient).cancel(eq(topologyName), anyLong(), eq(message));

    ProcessingEngineTaskProgress currentlyProcessingProgress = new ProcessingEngineTaskProgress();
    currentlyProcessingProgress.setProcessingEngineTaskState(ProcessingEngineTaskState.CURRENTLY_PROCESSING);
    when(processingEngineTaskClient.getTaskProgress(anyString(), anyLong()))
        .thenReturn(currentlyProcessingProgress);

    when(workflowExecutionDao.getById(workflowExecution.getId().toString()))
        .thenReturn(workflowExecution);

    WorkflowExecutor workflowExecutor = new WorkflowExecutor(workflowExecution, workflowExecutorManager,
        workflowExecutionSettings);
    workflowExecutor.call();

    ArgumentCaptor<WorkflowExecution> workflowExecutionArgumentCaptor = ArgumentCaptor
        .forClass(WorkflowExecution.class);
    verify(workflowExecutionDao, times(1)).update(workflowExecutionArgumentCaptor.capture());
    assertEquals(WorkflowStatus.CANCELLED,
        workflowExecutionArgumentCaptor.getValue().getWorkflowStatus());
    assertEquals(SystemId.SYSTEM_MINUTE_CAP_EXPIRE.name(),
        workflowExecutionArgumentCaptor.getValue().getCancelledBy());
  }
}
