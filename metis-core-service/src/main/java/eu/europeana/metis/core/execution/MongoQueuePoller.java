package eu.europeana.metis.core.execution;

import eu.europeana.metis.core.dao.WorkflowExecutionDao;
import eu.europeana.metis.core.engine.base.EngineTask;
import eu.europeana.metis.core.engine.base.EngineTaskSettings;
import eu.europeana.metis.core.workflow.WorkflowExecution;
import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MongoQueuePoller<S extends EngineTaskSettings, T extends EngineTask> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final int MAX_CLAIM_BATCH = 20;

  private final WorkflowExecutorManager<S, T> workflowExecutorManager;
  private final WorkflowExecutionDao workflowExecutionDao;
  private final ExecutorService threadPool = Executors.newCachedThreadPool();
  private final ExecutorCompletionService<Pair<WorkflowExecution, Boolean>> completionService =
      new ExecutorCompletionService<>(threadPool);
  private final Duration failsafeLeniency;

  private int threadsCounter;

  public MongoQueuePoller(WorkflowExecutorManager<S, T> workflowExecutorManager, WorkflowExecutionDao workflowExecutionDao,
      Duration failsafeLeniency) {
    this.workflowExecutorManager = workflowExecutorManager;
    this.workflowExecutionDao = workflowExecutionDao;
    this.failsafeLeniency = failsafeLeniency;
  }

  public void poll() {
    int claimedExecutions = 0;
    while (claimedExecutions < MAX_CLAIM_BATCH) {
      WorkflowExecution workflowExecution = workflowExecutionDao.claimNextExecution(failsafeLeniency);
      if (workflowExecution == null) {
        return;
      }
      submitExecution(workflowExecution);
      claimedExecutions++;
    }
  }

  private void submitExecution(WorkflowExecution workflowExecution) {
    WorkflowExecutor<S, T> executor = new WorkflowExecutor<>(workflowExecution, workflowExecutorManager);
    completionService.submit(executor);
    threadsCounter++;
  }

  public void cleanup() throws InterruptedException {
    LOGGER.debug("Check if we have a task that has finished, threadsCounter: {}", threadsCounter);
    Future<Pair<WorkflowExecution, Boolean>> userWorkflowExecutionFuture = completionService.poll();
    while (userWorkflowExecutionFuture != null) {
      threadsCounter--;
      try {
        Pair<WorkflowExecution, Boolean> result = userWorkflowExecutionFuture.get();
        checkCollectedWorkflowExecution(result);
      } catch (ExecutionException e) {
        LOGGER.warn("Exception occurred in Future task", e);
      }
      userWorkflowExecutionFuture = completionService.poll();
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
        workflowExecutionDao.requeue(workflowExecution);
      }
    }
  }

  public void close() {
    //Interrupt running threads
    threadPool.shutdownNow();
  }
}

