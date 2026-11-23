package eu.europeana.metis.core.execution;

import eu.europeana.metis.core.dao.WorkflowExecutionClaimDao;
import eu.europeana.metis.core.engine.base.EngineTask;
import eu.europeana.metis.core.engine.base.EngineTaskSettings;
import eu.europeana.metis.core.workflow.WorkflowExecution;
import eu.europeana.metis.core.workflow.plugins.ExecutablePluginType;
import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadPoolExecutor;
import lombok.extern.slf4j.Slf4j;

/**
 * The WorkflowExecutionDispatcher is responsible for managing the lifecycle of workflow executions. It handles the polling,
 * execution submission, and cleanup of workflow executions using a thread pool.
 * <p>
 * Tracks each in-flight execution against the {@link ExecutablePluginType} whose semaphore permit was acquired for it. The entry
 * is registered <em>before</em> the task is handed to the executor, so it is always visible by the time the task can complete.
 * This lets {@link #cleanup()} release the permit for a finished task in every case - whether it completed normally or failed.
 *
 * @param <S> The type representing the task settings required for the engine tasks.
 * @param <T> The type representing the tasks to be executed.
 */
@Slf4j
public class WorkflowExecutionDispatcher<S extends EngineTaskSettings, T extends EngineTask> {

  private final WorkflowExecutorSettings<S, T> workflowExecutorSettings;
  private final WorkflowExecutionClaimDao workflowExecutionClaimDao;
  private final SemaphoresPerPluginManager semaphoresPerPluginManager;
  private final ThreadPoolExecutor threadPoolExecutor;
  private final Map<Future<WorkflowExecution>, ExecutablePluginType> pluginTypeByFuture = new ConcurrentHashMap<>();
  private final Duration failsafeLeniency;

  /**
   * Constructor.
   *
   * @param workflowExecutorSettings the settings that manage the execution of workflows, including semaphores, workflow execution
   * DAO, workflow post-processor, and engine task client
   * @param threadPoolExecutor the thread pool executor used to manage concurrent execution of workflows
   * @param workflowExecutionClaimDao the DAO responsible for claiming workflow executions for processing
   * @param failsafeLeniency the duration allowed for failsafe operations to complete without interruption
   */
  public WorkflowExecutionDispatcher(WorkflowExecutorSettings<S, T> workflowExecutorSettings,
      ThreadPoolExecutor threadPoolExecutor,
      WorkflowExecutionClaimDao workflowExecutionClaimDao, Duration failsafeLeniency) {
    this.semaphoresPerPluginManager = workflowExecutorSettings.semaphoresPerPluginManager();
    this.workflowExecutorSettings = workflowExecutorSettings;
    this.workflowExecutionClaimDao = workflowExecutionClaimDao;
    this.failsafeLeniency = failsafeLeniency;
    this.threadPoolExecutor = threadPoolExecutor;
  }

  /**
   * Polls for the next eligible workflow execution task and submits it for processing.
   * <p>
   * This method iterates over all available {@link ExecutablePluginType} values and performs the following steps:
   * <ul>
   *   <li>Attempts to acquire the semaphore for the specific plugin type using {@code semaphoresPerPluginManager} to ensure concurrency limits.</li>
   *   <li>Claims the next workflow execution from the {@link WorkflowExecutionClaimDao} for the plugin type.</li>
   *   <li>If no workflow execution is claimed, releases the semaphore for the plugin type.</li>
   *   <li>If a workflow execution is successfully claimed, submits the workflow execution task for processing.</li>
   * </ul>
   */
  public void pollAndSubmit() {
    for (ExecutablePluginType pluginType : ExecutablePluginType.values()) {
      if (semaphoresPerPluginManager.tryAcquireForExecutablePluginType(pluginType)) {
        WorkflowExecution workflowExecution = workflowExecutionClaimDao.claimNextExecution(failsafeLeniency, pluginType);
        if (workflowExecution == null) {
          semaphoresPerPluginManager.releaseForPluginType(pluginType);
        } else {
          submitExecution(workflowExecution, pluginType);
        }
      }
    }
  }

  private void submitExecution(WorkflowExecution workflowExecution, ExecutablePluginType executablePluginType) {
    FutureTask<WorkflowExecution> future = null;
    try {
      WorkflowExecutor<S, T> workflowExecutor = createExecutor(workflowExecution, workflowExecutorSettings);
      future = new FutureTask<>(workflowExecutor);
      // Register the permit owner before starting the task, so cleanup() can always find it once the task completes.
      pluginTypeByFuture.put(future, executablePluginType);
      threadPoolExecutor.execute(future);
    } catch (RuntimeException e) {
      // The task never started (or was rejected): drop any registration and release the permit here.
      if (future != null) {
        pluginTypeByFuture.remove(future);
      }
      semaphoresPerPluginManager.releaseForPluginType(executablePluginType);
      throw e;
    }
  }

  WorkflowExecutor<S, T> createExecutor(
      WorkflowExecution workflowExecution,
      WorkflowExecutorSettings<S, T> workflowExecutorSettings) {
    return new WorkflowExecutor<>(workflowExecution, workflowExecutorSettings);
  }

  /**
   * Cleans up completed workflow execution tasks.
   * <p>
   * This method scans the in-flight tasks and, for each one that has finished:
   * <ul>
   *   <li>Removes it from the tracking map.</li>
   *   <li>Gets its result, logging its completion or any execution exception.</li>
   *   <li>Releases the semaphore permit for its plugin type - always, regardless of the task outcome - so new tasks for that
   *   type can be dispatched.</li>
   * </ul>
   * <p>
   * Safe to call multiple times (idempotent). Tasks still running are left untouched for a later invocation.
   *
   * @throws InterruptedException if the thread is interrupted while retrieving the result of a finished task.
   */
  public void cleanup() throws InterruptedException {
    Iterator<Entry<Future<WorkflowExecution>, ExecutablePluginType>> futuresIterator = pluginTypeByFuture.entrySet().iterator();
    while (futuresIterator.hasNext()) {
      Entry<Future<WorkflowExecution>, ExecutablePluginType> entry = futuresIterator.next();
      Future<WorkflowExecution> future = entry.getKey();
      if (!future.isDone()) {
        continue;
      }
      ExecutablePluginType pluginType = entry.getValue();
      futuresIterator.remove();
      try {
        WorkflowExecution execution = future.get();
        log.info("workflowExecutionId: {} - Task finished", execution.getId());
      } catch (ExecutionException | CancellationException e) {
        log.warn("Exception occurred in Future task for pluginType {}", pluginType, e);
      } finally {
        semaphoresPerPluginManager.releaseForPluginType(pluginType);
      }
    }
  }

}
