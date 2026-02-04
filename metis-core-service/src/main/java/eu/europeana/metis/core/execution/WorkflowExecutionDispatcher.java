package eu.europeana.metis.core.execution;

import eu.europeana.metis.core.dao.WorkflowExecutionClaimDao;
import eu.europeana.metis.core.engine.base.EngineTask;
import eu.europeana.metis.core.engine.base.EngineTaskSettings;
import eu.europeana.metis.core.workflow.WorkflowExecution;
import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The WorkflowExecutionDispatcher is responsible for managing the lifecycle of workflow executions. It handles the polling,
 * execution submission, and cleanup of workflow executions using a thread pool and completion service.
 *
 * @param <S> The type representing the task settings required for the engine tasks.
 * @param <T> The type representing the tasks to be executed.
 */
public class WorkflowExecutionDispatcher<S extends EngineTaskSettings, T extends EngineTask> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final int SHORT_COMPLETION_POLL_TIMEOUT_MILLIS = 50;

  private final WorkflowExecutorManager<S, T> workflowExecutorManager;
  private final WorkflowExecutionClaimDao workflowExecutionClaimDao;
  private final ThreadPoolExecutor threadPoolExecutor;
  private final ExecutorCompletionService<Pair<WorkflowExecution, Boolean>> completionService;
  private final Duration failsafeLeniency;

  /**
   * Constructor.
   *
   * @param workflowExecutorManager Manager responsible for managing the execution of workflows and interacting with the
   * distributed workflow queue.
   * @param threadPoolExecutor Thread pool executor used to manage worker threads for executing workflows.
   * @param workflowExecutionClaimDao Data access object for claiming workflow executions and ensuring proper ownership during
   * processing.
   * @param failsafeLeniency Duration specifying the leniency period allowed for handling failsafe operations in the workflow
   * execution process.
   */
  public WorkflowExecutionDispatcher(WorkflowExecutorManager<S, T> workflowExecutorManager, ThreadPoolExecutor threadPoolExecutor,
      WorkflowExecutionClaimDao workflowExecutionClaimDao, Duration failsafeLeniency) {
    this.workflowExecutorManager = workflowExecutorManager;
    this.workflowExecutionClaimDao = workflowExecutionClaimDao;
    this.threadPoolExecutor = threadPoolExecutor;
    this.failsafeLeniency = failsafeLeniency;
    this.completionService = new ExecutorCompletionService<>(threadPoolExecutor);
  }

  /**
   * Polls for workflow executions and submits them for processing.
   * <p>
   * This method interacts with the {@code workflowExecutionDao} to claim the next available workflow execution. It claims up to a
   * maximum of {@code MAX_CLAIM_BATCH} executions in a single invocation. Each claimed execution is submitted for processing
   * using the {@code submitExecution} method.
   */
  public void pollAndSubmit() {
    int availableSlots = getAvailableSlots();

    while (availableSlots > 0) {
      WorkflowExecution execution = workflowExecutionClaimDao.claimNextExecution(failsafeLeniency);
      if (execution == null) {
        return;
      }
      submitExecution(execution);
      availableSlots--;
    }
  }

  private int getAvailableSlots() {
    return threadPoolExecutor.getMaximumPoolSize() - threadPoolExecutor.getActiveCount();
  }

  private void submitExecution(WorkflowExecution workflowExecution) {
    WorkflowExecutor<S, T> workflowExecutor = createExecutor(workflowExecution, workflowExecutorManager);
    completionService.submit(workflowExecutor);
  }

  WorkflowExecutor<S, T> createExecutor(WorkflowExecution workflowExecution,
      WorkflowExecutorManager<S, T> workflowExecutorManager) {
    return new WorkflowExecutor<>(workflowExecution, workflowExecutorManager);
  }

  /**
   * Cleans up completed tasks from the internal completion service and updates the thread counter.
   * <p>
   * This method polls the {@code completionService} for tasks that have completed execution. For each completed task, the thread
   * counter is decremented, and the task's result is processed using the {@code checkCollectedWorkflowExecution} method. Any
   * resulting exceptions during the processing of tasks are logged.
   *
   * @throws InterruptedException if the thread is interrupted while waiting for task completion.
   */
  public void cleanup() throws InterruptedException {
    Future<Pair<WorkflowExecution, Boolean>> userWorkflowExecutionFuture;
    while ((userWorkflowExecutionFuture = completionService.poll(SHORT_COMPLETION_POLL_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS))
        != null) {
      try {
        Pair<WorkflowExecution, Boolean> result = userWorkflowExecutionFuture.get();
        checkCollectedWorkflowExecution(result);
      } catch (ExecutionException e) {
        LOGGER.warn("Exception occurred in Future task", e);
      }
    }
  }

  private void checkCollectedWorkflowExecution(
      Pair<WorkflowExecution, Boolean> workflowExecutionRanFlagPair) {
    final WorkflowExecution workflowExecution = workflowExecutionRanFlagPair.getLeft();
    if (workflowExecution != null) {
      boolean wasExecutionClaimedAndPluginRan = workflowExecutionRanFlagPair.getRight();
      //If a plugin did not run, we are sending it back to queue so another instance can pick it up
      if (wasExecutionClaimedAndPluginRan) {
        LOGGER.info("workflowExecutionId: {} - Task finished", workflowExecution.getId());
      } else {
        LOGGER.info("workflowExecutionId: {} - Sent to queue because execution could "
            + "not be claimed or plugin could not run in this instance", workflowExecution.getId());
        if (!workflowExecutionClaimDao.requeue(workflowExecution)) {
          LOGGER.warn("Could not requeue workflowExecutionId: {}", workflowExecution.getId());
        }
      }
    }
  }
}

