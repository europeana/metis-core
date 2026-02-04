package eu.europeana.metis.core.execution;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import eu.europeana.metis.core.dao.WorkflowExecutionClaimDao;
import eu.europeana.metis.core.engine.base.EngineTask;
import eu.europeana.metis.core.engine.base.EngineTaskSettings;
import eu.europeana.metis.core.utils.TestObjectFactory;
import eu.europeana.metis.core.workflow.WorkflowExecution;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import org.apache.commons.lang3.tuple.Pair;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@ExtendWith(MockitoExtension.class)
class WorkflowExecutionDispatcherTest {

  public static final int CORE_POOL_SIZE = 20;
  private static ThreadPoolTaskExecutor threadPoolTaskExecutor;
  @Mock
  private WorkflowExecutionClaimDao workflowExecutionClaimDao;

  @AfterEach
  void shutdown() {
    threadPoolTaskExecutor.shutdown();
  }

  @Test
  void createExecutor() {
    WorkflowExecutionDispatcher<EngineTaskSettings, EngineTask> workflowExecutionDispatcher = createDispatcher();
    WorkflowExecutorSettings<EngineTaskSettings, EngineTask> workflowExecutorSettings = mock(WorkflowExecutorSettings.class);

    WorkflowExecutor<EngineTaskSettings, EngineTask> executor = workflowExecutionDispatcher
        .createExecutor(mock(WorkflowExecution.class), workflowExecutorSettings);
    assertNotNull(executor);
  }

  @Test
  void pollAndSubmit_stopsWhenNoExecutionClaimed() {
    when(workflowExecutionClaimDao.claimNextExecution(any())).thenReturn(null);

    WorkflowExecutionDispatcher<EngineTaskSettings, EngineTask> workflowExecutionDispatcher =
        createDispatcher();
    workflowExecutionDispatcher.pollAndSubmit();

    verify(workflowExecutionClaimDao, times(1)).claimNextExecution(any());
    verifyNoMoreInteractions(workflowExecutionClaimDao);
  }

  @Test
  void pollAndSubmit_submitsAtMostMaxBatch20() {
    Queue<WorkflowExecutor<EngineTaskSettings, EngineTask>> executors = new ArrayDeque<>();

    List<WorkflowExecution> workflowExecutions = new ArrayList<>();
    for (int i = 0; i < 20; i++) {
      WorkflowExecution workflowExecution = TestObjectFactory.createWorkflowExecutionObject();
      workflowExecutions.add(workflowExecution);
      executors.add(mockExecutor(Pair.of(workflowExecution, true)));
    }

    var stubbing = when(workflowExecutionClaimDao.claimNextExecution(any()));
    for (WorkflowExecution workflowExecution : workflowExecutions) {
      stubbing = stubbing.thenReturn(workflowExecution);
    }
    stubbing.thenReturn(null);

    WorkflowExecutionDispatcher<EngineTaskSettings, EngineTask> workflowExecutionDispatcher =
        createDispatcherWithStub(executors);
    workflowExecutionDispatcher.pollAndSubmit();

    //No chance to claim +1 because we reached the max batch size
    verify(workflowExecutionClaimDao, times(20)).claimNextExecution(any());
  }

  @Test
  void cleanup_swallowsExecutionException() throws InterruptedException {
    Queue<WorkflowExecutor<EngineTaskSettings, EngineTask>> executors = new ArrayDeque<>();
    WorkflowExecution workflowExecution1 = TestObjectFactory.createWorkflowExecutionObject();
    WorkflowExecution workflowExecution2 = TestObjectFactory.createWorkflowExecutionObject();

    executors.add(mockExecutor(Pair.of(workflowExecution1, true)));
    executors.add(failingExecutor());

    when(workflowExecutionClaimDao.claimNextExecution(any())).thenReturn(workflowExecution1, workflowExecution2, null);

    WorkflowExecutionDispatcher<EngineTaskSettings, EngineTask> workflowExecutionDispatcher =
        createDispatcherWithStub(executors);
    workflowExecutionDispatcher.pollAndSubmit();
    workflowExecutionDispatcher.cleanup();

    //We claim +1 to exit the loop
    verify(workflowExecutionClaimDao, times(3)).claimNextExecution(any());
    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(() -> {
          workflowExecutionDispatcher.cleanup();
          verify(workflowExecutionClaimDao, never()).requeue(any());
        });
  }


  @Test
  void cleanup_requeuesWhenPluginDidNotRun() {
    Queue<WorkflowExecutor<EngineTaskSettings, EngineTask>> executors = new ArrayDeque<>();
    WorkflowExecution workflowExecution1 = TestObjectFactory.createWorkflowExecutionObject();
    WorkflowExecution workflowExecution2 = TestObjectFactory.createWorkflowExecutionObject();

    executors.add(mockExecutor(Pair.of(workflowExecution1, true)));
    executors.add(mockExecutor(Pair.of(workflowExecution1, false)));

    when(workflowExecutionClaimDao.claimNextExecution(any())).thenReturn(workflowExecution1, workflowExecution2, null);
    when(workflowExecutionClaimDao.requeue(any())).thenReturn(true);

    WorkflowExecutionDispatcher<EngineTaskSettings, EngineTask> workflowExecutionDispatcher =
        createDispatcherWithStub(executors);
    workflowExecutionDispatcher.pollAndSubmit();
    //We claim +1 to exit the loop
    verify(workflowExecutionClaimDao, times(3)).claimNextExecution(any());
    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(() -> {
          workflowExecutionDispatcher.cleanup();
          verify(workflowExecutionClaimDao, times(1)).requeue(any());
        });
  }

  @Test
  void cleanup_requeuesWhenPluginDidNotRun_FailOnRequeue() {
    Queue<WorkflowExecutor<EngineTaskSettings, EngineTask>> executors = new ArrayDeque<>();
    WorkflowExecution workflowExecution1 = TestObjectFactory.createWorkflowExecutionObject();
    WorkflowExecution workflowExecution2 = TestObjectFactory.createWorkflowExecutionObject();
    executors.add(mockExecutor(Pair.of(workflowExecution1, true)));
    executors.add(mockExecutor(Pair.of(workflowExecution1, false)));

    when(workflowExecutionClaimDao.claimNextExecution(any())).thenReturn(workflowExecution1, workflowExecution2, null);
    when(workflowExecutionClaimDao.requeue(any())).thenReturn(false);

    WorkflowExecutionDispatcher<EngineTaskSettings, EngineTask> workflowExecutionDispatcher =
        createDispatcherWithStub(executors);
    workflowExecutionDispatcher.pollAndSubmit();
    //We claim +1 to exit the loop
    verify(workflowExecutionClaimDao, times(3)).claimNextExecution(any());
    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(() -> {
          workflowExecutionDispatcher.cleanup();
          verify(workflowExecutionClaimDao, times(1)).requeue(any());
        });
  }

  @Test
  void cleanup_Ignore_NullWorkflowExecution() throws InterruptedException {
    Queue<WorkflowExecutor<EngineTaskSettings, EngineTask>> executors = new ArrayDeque<>();
    WorkflowExecution workflowExecution1 = TestObjectFactory.createWorkflowExecutionObject();
    WorkflowExecution workflowExecution2 = TestObjectFactory.createWorkflowExecutionObject();
    executors.add(mockExecutor(Pair.of(workflowExecution1, true)));
    executors.add(mockExecutor(Pair.of(null, true)));

    when(workflowExecutionClaimDao.claimNextExecution(any())).thenReturn(workflowExecution1, workflowExecution2, null);

    WorkflowExecutionDispatcher<EngineTaskSettings, EngineTask> workflowExecutionDispatcher =
        createDispatcherWithStub(executors);
    workflowExecutionDispatcher.pollAndSubmit();
    workflowExecutionDispatcher.cleanup();
    //We claim +1 to exit the loop
    verify(workflowExecutionClaimDao, times(3)).claimNextExecution(any());
    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(() -> {
          workflowExecutionDispatcher.cleanup();
          verify(workflowExecutionClaimDao, never()).requeue(any());
        });
  }

  private WorkflowExecutionDispatcher<EngineTaskSettings, EngineTask> createDispatcherWithStub(
      Queue<WorkflowExecutor<EngineTaskSettings, EngineTask>> executors) {
    WorkflowExecutionDispatcher<EngineTaskSettings, EngineTask> workflowExecutionDispatcher = createDispatcher();

    doAnswer(invocation -> executors.poll())
        .when(workflowExecutionDispatcher)
        .createExecutor(any(), any());

    return workflowExecutionDispatcher;
  }

  private WorkflowExecutionDispatcher<EngineTaskSettings, EngineTask> createDispatcher() {
    return Mockito.spy(new WorkflowExecutionDispatcher<>(
        mock(), getThreadPoolTaskExecutor().getThreadPoolExecutor(), workflowExecutionClaimDao,
        Duration.ofSeconds(10)));
  }

  private static @NonNull ThreadPoolTaskExecutor getThreadPoolTaskExecutor() {
    threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
    threadPoolTaskExecutor.setCorePoolSize(CORE_POOL_SIZE);
    threadPoolTaskExecutor.setMaxPoolSize(CORE_POOL_SIZE);
    threadPoolTaskExecutor.setQueueCapacity(CORE_POOL_SIZE * 2);
    threadPoolTaskExecutor.setThreadNamePrefix("workflowExecutorPool-");
    threadPoolTaskExecutor.initialize();
    return threadPoolTaskExecutor;
  }

  private WorkflowExecutor<EngineTaskSettings, EngineTask> mockExecutor(Pair<WorkflowExecution, Boolean> result) {

    @SuppressWarnings("unchecked")
    WorkflowExecutor<EngineTaskSettings, EngineTask> workflowExecutor = mock(WorkflowExecutor.class);

    try {
      when(workflowExecutor.call()).thenReturn(result);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    return workflowExecutor;
  }

  private WorkflowExecutor<EngineTaskSettings, EngineTask> failingExecutor() {

    @SuppressWarnings("unchecked")
    WorkflowExecutor<EngineTaskSettings, EngineTask> workflowExecutor = mock(WorkflowExecutor.class);

    try {
      when(workflowExecutor.call()).thenThrow(new RuntimeException("Failure"));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    return workflowExecutor;
  }
}
