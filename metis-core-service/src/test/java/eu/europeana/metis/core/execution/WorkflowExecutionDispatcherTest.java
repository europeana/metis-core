package eu.europeana.metis.core.execution;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import eu.europeana.metis.core.dao.WorkflowExecutionClaimDao;
import eu.europeana.metis.core.engine.base.EngineTask;
import eu.europeana.metis.core.engine.base.EngineTaskSettings;
import eu.europeana.metis.core.utils.TestObjectFactory;
import eu.europeana.metis.core.workflow.WorkflowExecution;
import eu.europeana.metis.core.workflow.plugins.ExecutablePluginType;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@ExtendWith(MockitoExtension.class)
class WorkflowExecutionDispatcherTest {

  public static final int PLUGIN_CONCURRENCY = 2;
  public static final int CORE_POOL_SIZE = ExecutablePluginType.values().length * PLUGIN_CONCURRENCY;
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
    when(workflowExecutionClaimDao.claimNextExecution(any(), any())).thenReturn(null);

    WorkflowExecutionDispatcher<EngineTaskSettings, EngineTask> workflowExecutionDispatcher = createDispatcher();
    workflowExecutionDispatcher.pollAndSubmit();

    verify(workflowExecutionClaimDao, times(ExecutablePluginType.values().length)).claimNextExecution(any(), any());
    verifyNoMoreInteractions(workflowExecutionClaimDao);
  }

  @Test
  void pollAndSubmit_submitsAtMostMaxBatch() {
    Queue<WorkflowExecutor<EngineTaskSettings, EngineTask>> executors = new ArrayDeque<>();
    CountDownLatch blockLatch = new CountDownLatch(1);
    for (ExecutablePluginType pluginType : ExecutablePluginType.values()) {
      var stubbing = when(workflowExecutionClaimDao.claimNextExecution(any(), eq(pluginType)));

      for (int i = 0; i < PLUGIN_CONCURRENCY; i++) {
        WorkflowExecution workflowExecution = TestObjectFactory.createWorkflowExecutionObject(pluginType);
        stubbing = stubbing.thenReturn(workflowExecution);
        executors.add(blockingExecutor(workflowExecution, blockLatch));
      }
      stubbing.thenReturn(null);
    }

    SemaphoresPerPluginManager semaphoresPerPluginManager = getSemaphoresPerPluginManager();
    WorkflowExecutionDispatcher<EngineTaskSettings, EngineTask> workflowExecutionDispatcher =
        createDispatcherWithStub(executors, semaphoresPerPluginManager);
    workflowExecutionDispatcher.pollAndSubmit(); //First set of plugins
    workflowExecutionDispatcher.pollAndSubmit(); //Second set of plugins (fill up the concurrency)
    workflowExecutionDispatcher.pollAndSubmit(); //Third set of plugins (should not get any more executions)
    // Wait until executors started
    await().atMost(Duration.ofSeconds(5))
           .untilAsserted(() ->
               executors.forEach(executor ->
                   verify(executor, atLeastOnce()).call()
               )
           );

    for (ExecutablePluginType pluginType : ExecutablePluginType.values()) {
      verify(workflowExecutionClaimDao, times(PLUGIN_CONCURRENCY)).claimNextExecution(any(), eq(pluginType));
    }
    verify(workflowExecutionClaimDao, times(CORE_POOL_SIZE)).claimNextExecution(any(), any());
    verifyNoMoreInteractions(workflowExecutionClaimDao);

    // Now allow executors to finish
    blockLatch.countDown();
    await().atMost(Duration.ofSeconds(5)).untilAsserted(workflowExecutionDispatcher::cleanup);

    for (ExecutablePluginType pluginType : ExecutablePluginType.values()) {
      verify(semaphoresPerPluginManager, times(PLUGIN_CONCURRENCY)).releaseForPluginType(pluginType);
    }
  }

  @Test
  void submitExecution_releasesSemaphoreWhenSubmitExecution() {
    ExecutablePluginType pluginType = ExecutablePluginType.OAIPMH_HARVEST;
    WorkflowExecution execution = TestObjectFactory.createWorkflowExecutionObject(pluginType);

    when(workflowExecutionClaimDao.claimNextExecution(any(), eq(pluginType))).thenReturn(execution);
    SemaphoresPerPluginManager semaphoresPerPluginManager = getSemaphoresPerPluginManager();
    WorkflowExecutionDispatcher<EngineTaskSettings, EngineTask> workflowExecutionDispatcher = createDispatcher(
        semaphoresPerPluginManager);

    doThrow(new RuntimeException("failure"))
        .when(workflowExecutionDispatcher)
        .createExecutor(any(), any());

    assertThrows(RuntimeException.class, workflowExecutionDispatcher::pollAndSubmit);
    verify(semaphoresPerPluginManager).releaseForPluginType(pluginType);
  }

  @Test
  void cleanup_swallowsExecutionException() throws InterruptedException {
    Queue<WorkflowExecutor<EngineTaskSettings, EngineTask>> executors = new ArrayDeque<>();

    WorkflowExecution workflowExecution1 = TestObjectFactory.createWorkflowExecutionObject(ExecutablePluginType.OAIPMH_HARVEST);
    WorkflowExecution workflowExecution2 = TestObjectFactory.createWorkflowExecutionObject(ExecutablePluginType.HTTP_HARVEST);

    executors.add(mockExecutor(workflowExecution1));
    executors.add(failingExecutor());

    when(workflowExecutionClaimDao.claimNextExecution(any(), any()))
        .thenReturn(workflowExecution1, workflowExecution2, null);
    SemaphoresPerPluginManager semaphoresPerPluginManager = getSemaphoresPerPluginManager();
    WorkflowExecutionDispatcher<EngineTaskSettings, EngineTask> workflowExecutionDispatcher =
        createDispatcherWithStub(executors, semaphoresPerPluginManager);
    workflowExecutionDispatcher.pollAndSubmit();
    workflowExecutionDispatcher.cleanup();

    verify(semaphoresPerPluginManager, times(1)).releaseForPluginType(ExecutablePluginType.OAIPMH_HARVEST);
    verify(semaphoresPerPluginManager, times(1)).releaseForPluginType(ExecutablePluginType.HTTP_HARVEST);
  }

  private WorkflowExecutionDispatcher<EngineTaskSettings, EngineTask> createDispatcherWithStub(
      Queue<WorkflowExecutor<EngineTaskSettings, EngineTask>> executors,
      SemaphoresPerPluginManager semaphoresPerPluginManager) {
    WorkflowExecutionDispatcher<EngineTaskSettings, EngineTask> workflowExecutionDispatcher = createDispatcher(
        semaphoresPerPluginManager);

    doAnswer(invocation -> executors.poll())
        .when(workflowExecutionDispatcher)
        .createExecutor(any(), any());

    return workflowExecutionDispatcher;
  }

  private WorkflowExecutionDispatcher<EngineTaskSettings, EngineTask> createDispatcher() {
    return createDispatcher(getSemaphoresPerPluginManager());
  }

  private WorkflowExecutionDispatcher<EngineTaskSettings, EngineTask> createDispatcher(
      SemaphoresPerPluginManager semaphoresPerPluginManager) {
    WorkflowExecutorSettings<EngineTaskSettings, EngineTask> workflowExecutorSettings = mock(WorkflowExecutorSettings.class);
    when(workflowExecutorSettings.semaphoresPerPluginManager()).thenReturn(semaphoresPerPluginManager);
    return spy(new WorkflowExecutionDispatcher<>(
        workflowExecutorSettings, getThreadPoolTaskExecutor().getThreadPoolExecutor(), workflowExecutionClaimDao,
        Duration.ofSeconds(10)));
  }

  private static SemaphoresPerPluginManager getSemaphoresPerPluginManager() {
    return spy(new SemaphoresPerPluginManager(PLUGIN_CONCURRENCY));
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

  private WorkflowExecutor<EngineTaskSettings, EngineTask> blockingExecutor(
      WorkflowExecution workflowExecution, CountDownLatch blockLatch) {

    @SuppressWarnings("unchecked")
    WorkflowExecutor<EngineTaskSettings, EngineTask> executor =
        mock(WorkflowExecutor.class);

    try {
      when(executor.call()).thenAnswer(invocation -> {
        blockLatch.await();
        return workflowExecution;
      });
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    return executor;
  }

  private WorkflowExecutor<EngineTaskSettings, EngineTask> mockExecutor(WorkflowExecution workflowExecution) {

    @SuppressWarnings("unchecked")
    WorkflowExecutor<EngineTaskSettings, EngineTask> workflowExecutor = mock(WorkflowExecutor.class);

    try {
      when(workflowExecutor.call()).thenReturn(workflowExecution);
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
