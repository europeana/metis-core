package eu.europeana.metis.core.execution;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkflowExecutionDispatcherTest {

  @Mock
  private WorkflowExecutorManager<EngineTaskSettings, EngineTask> workflowExecutorManager;

  @Mock
  private WorkflowExecutionClaimDao workflowExecutionClaimDao;

  private WorkflowExecutionDispatcher<EngineTaskSettings, EngineTask> workflowExecutionDispatcher;

  @BeforeEach
  void setup() {
    Mockito.reset(workflowExecutionClaimDao);
    workflowExecutionDispatcher =
        new WorkflowExecutionDispatcher<>(
            workflowExecutorManager,
            workflowExecutionClaimDao,
            Duration.ofSeconds(10));
  }

  @AfterEach
  void tearDown() {
    workflowExecutionDispatcher.close();
  }

  @Test
  void pollAndSubmit_stopsWhenNoExecutionClaimed() {
    when(workflowExecutionClaimDao.claimNextExecution(any())).thenReturn(null);

    workflowExecutionDispatcher.pollAndSubmit();

    verify(workflowExecutionClaimDao, times(1)).claimNextExecution(any());
    verifyNoMoreInteractions(workflowExecutionClaimDao);
  }

  @Test
  void pollAndSubmit_submitsAtMostMaxBatch20() {
    List<WorkflowExecution> workflowExecutions = new ArrayList<>();
    for (int i = 0; i < 20; i++) {
      workflowExecutions.add(TestObjectFactory.createWorkflowExecutionObject());
    }

    var stubbing = when(workflowExecutionClaimDao.claimNextExecution(any()));
    for (WorkflowExecution workflowExecution : workflowExecutions) {
      stubbing = stubbing.thenReturn(workflowExecution);
    }
    stubbing.thenReturn(null);

    try (MockedConstruction<WorkflowExecutor> ignored = Mockito.mockConstruction(
        WorkflowExecutor.class,
        (mock, ctx) -> {
          WorkflowExecution we = (WorkflowExecution) ctx.arguments().getFirst();
          when(mock.call()).thenReturn(Pair.of(we, true));
        })) {

      workflowExecutionDispatcher.pollAndSubmit();
      //No chance to claim +1 because we reached the max batch size
      verify(workflowExecutionClaimDao, times(20)).claimNextExecution(any());
    }
  }

  @Test
  void cleanup_swallowsExecutionException() {
    List<WorkflowExecution> workflowExecutions = new ArrayList<>();
    for (int i = 0; i < 2; i++) {
      workflowExecutions.add(TestObjectFactory.createWorkflowExecutionObject());
    }

    var stubbing = when(workflowExecutionClaimDao.claimNextExecution(any()));
    for (WorkflowExecution workflowExecution : workflowExecutions) {
      stubbing = stubbing.thenReturn(workflowExecution);
    }
    stubbing.thenReturn(null);

    AtomicInteger invocationCounter = new AtomicInteger();
    try (MockedConstruction<WorkflowExecutor> ignored = Mockito.mockConstruction(
        WorkflowExecutor.class,
        (mock, ctx) -> {
          WorkflowExecution we = (WorkflowExecution) ctx.arguments().getFirst();
          when(mock.call()).thenAnswer(inv -> {

            int callNumber = invocationCounter.incrementAndGet();

            if (callNumber == 1) {
              return Pair.of(we, true);
            }

            throw new RuntimeException("Failure");
          });
        })) {

      workflowExecutionDispatcher.pollAndSubmit();
      await().atMost(Duration.ofSeconds(2))
             .untilAsserted(() -> {
               workflowExecutionDispatcher.cleanup();
               verify(workflowExecutionClaimDao, never()).requeue(any());
             });
      //We claim +1 to exit the loop
      verify(workflowExecutionClaimDao, times(3)).claimNextExecution(any());
    }
  }


  @Test
  void cleanup_requeuesWhenPluginDidNotRun() {
    List<WorkflowExecution> workflowExecutions = new ArrayList<>();
    for (int i = 0; i < 2; i++) {
      workflowExecutions.add(TestObjectFactory.createWorkflowExecutionObject());
    }

    var stubbing = when(workflowExecutionClaimDao.claimNextExecution(any()));
    for (WorkflowExecution workflowExecution : workflowExecutions) {
      stubbing = stubbing.thenReturn(workflowExecution);
    }
    stubbing.thenReturn(null);
    when(workflowExecutionClaimDao.requeue(any())).thenReturn(true);

    AtomicInteger invocationCounter = new AtomicInteger();
    try (MockedConstruction<WorkflowExecutor> ignored = Mockito.mockConstruction(
        WorkflowExecutor.class,
        (mock, ctx) -> {
          WorkflowExecution we = (WorkflowExecution) ctx.arguments().getFirst();
          when(mock.call()).thenAnswer(inv -> {

            int callNumber = invocationCounter.incrementAndGet();

            if (callNumber == 1) {
              return Pair.of(we, true);
            }
            return Pair.of(we, false);
          });
        })) {

      workflowExecutionDispatcher.pollAndSubmit();
      await().atMost(Duration.ofSeconds(2))
             .untilAsserted(() -> {
               workflowExecutionDispatcher.cleanup();
               verify(workflowExecutionClaimDao, times(1)).requeue(any());
             });
      //We claim +1 to exit the loop
      verify(workflowExecutionClaimDao, times(3)).claimNextExecution(any());
    }
  }

  @Test
  void cleanup_requeuesWhenPluginDidNotRun_FailOnRequeue() {
    List<WorkflowExecution> workflowExecutions = new ArrayList<>();
    for (int i = 0; i < 2; i++) {
      workflowExecutions.add(TestObjectFactory.createWorkflowExecutionObject());
    }

    var stubbing = when(workflowExecutionClaimDao.claimNextExecution(any()));
    for (WorkflowExecution workflowExecution : workflowExecutions) {
      stubbing = stubbing.thenReturn(workflowExecution);
    }
    stubbing.thenReturn(null);
    when(workflowExecutionClaimDao.requeue(any())).thenReturn(false);

    AtomicInteger invocationCounter = new AtomicInteger();
    try (MockedConstruction<WorkflowExecutor> ignored = Mockito.mockConstruction(
        WorkflowExecutor.class,
        (mock, ctx) -> {
          WorkflowExecution we = (WorkflowExecution) ctx.arguments().getFirst();
          when(mock.call()).thenAnswer(inv -> {

            int callNumber = invocationCounter.incrementAndGet();

            if (callNumber == 1) {
              return Pair.of(we, true);
            }
            return Pair.of(we, false);
          });
        })) {

      workflowExecutionDispatcher.pollAndSubmit();
      await().atMost(Duration.ofSeconds(2))
             .untilAsserted(() -> {
               workflowExecutionDispatcher.cleanup();
               verify(workflowExecutionClaimDao, times(1)).requeue(any());
             });
      //We claim +1 to exit the loop
      verify(workflowExecutionClaimDao, times(3)).claimNextExecution(any());
    }
  }

  @Test
  void cleanup_Ignore_NullWorkflowExecution() {
    List<WorkflowExecution> workflowExecutions = new ArrayList<>();
    for (int i = 0; i < 2; i++) {
      workflowExecutions.add(TestObjectFactory.createWorkflowExecutionObject());
    }

    var stubbing = when(workflowExecutionClaimDao.claimNextExecution(any()));
    for (WorkflowExecution workflowExecution : workflowExecutions) {
      stubbing = stubbing.thenReturn(workflowExecution);
    }
    stubbing.thenReturn(null);

    AtomicInteger invocationCounter = new AtomicInteger();
    try (MockedConstruction<WorkflowExecutor> ignored = Mockito.mockConstruction(
        WorkflowExecutor.class,
        (mock, ctx) -> {
          WorkflowExecution we = (WorkflowExecution) ctx.arguments().getFirst();
          when(mock.call()).thenAnswer(inv -> {

            int callNumber = invocationCounter.incrementAndGet();

            if (callNumber == 1) {
              return Pair.of(we, true);
            }
            return Pair.of(null, false);
          });
        })) {

      workflowExecutionDispatcher.pollAndSubmit();
      await().atMost(Duration.ofSeconds(2))
             .untilAsserted(() -> {
               workflowExecutionDispatcher.cleanup();
               verify(workflowExecutionClaimDao, never()).requeue(any());
             });
      //We claim +1 to exit the loop
      verify(workflowExecutionClaimDao, times(3)).claimNextExecution(any());
    }
  }
}
