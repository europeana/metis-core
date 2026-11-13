package eu.europeana.metis.core.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import eu.europeana.cloud.service.dps.BatchInfo;
import eu.europeana.cloud.service.dps.DpsTask;
import eu.europeana.metis.core.dao.DatasetXsltDao;
import eu.europeana.metis.core.dao.WorkflowExecutionDao;
import eu.europeana.metis.core.engine.base.EngineTask;
import eu.europeana.metis.core.engine.base.EngineTaskClient;
import eu.europeana.metis.core.engine.base.EngineTaskSettings;
import eu.europeana.metis.core.engine.base.EngineType;
import eu.europeana.metis.core.engine.base.task.input.InputDataEndpoint;
import eu.europeana.metis.core.engine.ecloud.EcloudEngineTask;
import eu.europeana.metis.core.engine.ecloud.EcloudEngineTaskSettings;
import eu.europeana.metis.core.engine.sandbox.SandboxEngineTask;
import eu.europeana.metis.core.engine.sandbox.SandboxEngineTaskSettings;
import eu.europeana.metis.core.workflow.WorkflowExecution;
import eu.europeana.metis.core.workflow.plugins.OaipmhHarvestPlugin;
import eu.europeana.metis.core.workflow.plugins.OaipmhHarvestPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.PluginStatus;
import eu.europeana.metis.sandbox.common.task.input.SandboxTask;
import eu.europeana.metis.exception.ExternalTaskException;
import java.util.List;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InOrder;

class TestPluginExecutor {

  private static final String ENGINE_TASK_ID = "123";
  private static final String ENGINE_BATCH_ID = "batchId";
  private WorkflowExecutionDao workflowExecutionDao;
  private EngineTaskClient<EngineTaskSettings, EngineTask> engineTaskClient;
  private PluginExecutor<EngineTaskSettings, EngineTask> pluginExecutor;
  private WorkflowExecution workflowExecution;
  private OaipmhHarvestPlugin oaipmhHarvestPlugin;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    workflowExecutionDao = mock(WorkflowExecutionDao.class);
    engineTaskClient = mock(EngineTaskClient.class);
    pluginExecutor = new PluginExecutor<>(engineTaskClient, workflowExecutionDao, mock(DatasetXsltDao.class));
    oaipmhHarvestPlugin = new OaipmhHarvestPlugin();
    oaipmhHarvestPlugin.setPluginMetadata(new OaipmhHarvestPluginMetadata());
    workflowExecution = new WorkflowExecution();
    workflowExecution.setId(new ObjectId());
    workflowExecution.setDatasetId("dataset");
    workflowExecution.setEngineDatasetId("engineDatasetId");
    workflowExecution.setMetisPlugins(List.of(oaipmhHarvestPlugin));
  }

  @ParameterizedTest
  @EnumSource(EngineType.class)
  void persistsCreatedIdentifiersBeforeStarting(EngineType engineType) throws Exception {
    EngineTask engineTask = prepareCreatedTask(engineType);
    when(workflowExecutionDao.updateWorkflowPluginsIfOwned(workflowExecution)).thenAnswer(_ -> {
      assertEquals(ENGINE_TASK_ID, oaipmhHarvestPlugin.getEngineTaskId());
      assertEquals(ENGINE_BATCH_ID, oaipmhHarvestPlugin.getEngineBatchId());
      assertEquals(PluginStatus.RUNNING, oaipmhHarvestPlugin.getPluginStatus());
      assertNotNull(oaipmhHarvestPlugin.getStartedDate());
      return true;
    });

    assertEquals(PluginExecutor.ExecutionResult.SUCCESS, pluginExecutor.execute(oaipmhHarvestPlugin, workflowExecution));

    InOrder submissionOrder = inOrder(engineTaskClient, workflowExecutionDao);
    submissionOrder.verify(engineTaskClient).createEngineTask(anyMap(), any(InputDataEndpoint.class), anyString());
    submissionOrder.verify(workflowExecutionDao).updateWorkflowPluginsIfOwned(workflowExecution);
    submissionOrder.verify(engineTaskClient).submitEngineTask(engineTask, oaipmhHarvestPlugin.getTopologyName());
    submissionOrder.verify(workflowExecutionDao).updateWorkflowPluginsIfOwned(workflowExecution);
  }

  @ParameterizedTest
  @EnumSource(EngineType.class)
  void doesNotStartWhenPersistenceMatchesNoOwnedWorkflow(EngineType engineType) throws Exception {
    prepareCreatedTask(engineType);
    when(workflowExecutionDao.updateWorkflowPluginsIfOwned(workflowExecution)).thenReturn(false);

    assertEquals(PluginExecutor.ExecutionResult.OWNERSHIP_LOST, pluginExecutor.execute(oaipmhHarvestPlugin, workflowExecution));

    verify(engineTaskClient, never()).submitEngineTask(any(EngineTask.class), anyString());
    assertEquals(PluginStatus.RUNNING, oaipmhHarvestPlugin.getPluginStatus());
    assertEquals(ENGINE_TASK_ID, oaipmhHarvestPlugin.getEngineTaskId());
    assertEquals(ENGINE_BATCH_ID, oaipmhHarvestPlugin.getEngineBatchId());
  }

  @ParameterizedTest
  @EnumSource(EngineType.class)
  void doesNotStartWhenPersistenceThrows(EngineType engineType) throws Exception {
    prepareCreatedTask(engineType);
    when(workflowExecutionDao.updateWorkflowPluginsIfOwned(workflowExecution))
        .thenThrow(new IllegalStateException("Database update failed")).thenReturn(true);

    assertEquals(PluginExecutor.ExecutionResult.FAILED, pluginExecutor.execute(oaipmhHarvestPlugin, workflowExecution));

    verify(engineTaskClient, never()).submitEngineTask(any(EngineTask.class), anyString());
    verify(workflowExecutionDao, times(2)).updateWorkflowPluginsIfOwned(workflowExecution);
    assertEquals(PluginStatus.FAILED, oaipmhHarvestPlugin.getPluginStatus());
  }

  @ParameterizedTest
  @EnumSource(EngineType.class)
  void startFailureKeepsPersistedIdentifiers(EngineType engineType) throws Exception {
    EngineTask engineTask = prepareCreatedTask(engineType);
    when(workflowExecutionDao.updateWorkflowPluginsIfOwned(workflowExecution)).thenReturn(true);
    doAnswer(_ -> {
      assertEquals(ENGINE_TASK_ID, oaipmhHarvestPlugin.getEngineTaskId());
      assertEquals(ENGINE_BATCH_ID, oaipmhHarvestPlugin.getEngineBatchId());
      throw new ExternalTaskException("Start failed");
    }).when(engineTaskClient).submitEngineTask(engineTask, oaipmhHarvestPlugin.getTopologyName());

    assertEquals(PluginExecutor.ExecutionResult.FAILED, pluginExecutor.execute(oaipmhHarvestPlugin, workflowExecution));

    InOrder submissionOrder = inOrder(workflowExecutionDao, engineTaskClient);
    submissionOrder.verify(workflowExecutionDao).updateWorkflowPluginsIfOwned(workflowExecution);
    submissionOrder.verify(engineTaskClient).submitEngineTask(engineTask, oaipmhHarvestPlugin.getTopologyName());
    submissionOrder.verify(workflowExecutionDao).updateWorkflowPluginsIfOwned(workflowExecution);
    assertEquals(ENGINE_TASK_ID, oaipmhHarvestPlugin.getEngineTaskId());
    assertEquals(ENGINE_BATCH_ID, oaipmhHarvestPlugin.getEngineBatchId());
  }

  private EngineTask prepareCreatedTask(EngineType engineType) throws Exception {
    EngineTask engineTask;
    EngineTaskSettings engineTaskSettings;
    if (engineType == EngineType.ECLOUD) {
      DpsTask dpsTask = new DpsTask();
      dpsTask.setTaskId(Long.parseLong(ENGINE_TASK_ID));
      dpsTask.setResultsBatch(new BatchInfo("provider", ENGINE_BATCH_ID));
      engineTask = new EcloudEngineTask(dpsTask);
      engineTaskSettings = new EcloudEngineTaskSettings("http://cloud", "provider", "http://core", null);
    } else {
      SandboxTask sandboxTask = new SandboxTask();
      sandboxTask.setTaskId(ENGINE_TASK_ID);
      sandboxTask.setBatchId(ENGINE_BATCH_ID);
      engineTask = new SandboxEngineTask(sandboxTask);
      engineTaskSettings = new SandboxEngineTaskSettings("http://sandbox", "provider", "http://core", null);
    }
    when(engineTaskClient.getEngineTaskSettings()).thenReturn(engineTaskSettings);
    when(engineTaskClient.createEngineTask(anyMap(), any(InputDataEndpoint.class), anyString())).thenReturn(engineTask);
    return engineTask;
  }
}
