package eu.europeana.metis.core.execution;

import eu.europeana.metis.core.dao.WorkflowExecutionClaimDao;
import eu.europeana.metis.core.engine.base.EngineTask;
import eu.europeana.metis.core.engine.base.EngineTaskSettings;
import eu.europeana.metis.core.workflow.WorkflowExecution;
import eu.europeana.metis.core.workflow.plugins.ExecutablePluginType;
import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
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

  private final WorkflowExecutorSettings<S, T> workflowExecutorSettings;
  private final WorkflowExecutionClaimDao workflowExecutionClaimDao;
  private final SemaphoresPerPluginManager semaphoresPerPluginManager;
  private final ExecutorCompletionService<WorkflowExecution> completionService;
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
    this.completionService = new ExecutorCompletionService<>(threadPoolExecutor);
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
    try {
      WorkflowExecutor<S, T> workflowExecutor = createExecutor(workflowExecution, workflowExecutorSettings);
      Future<WorkflowExecution> future = completionService.submit(workflowExecutor);
      pluginTypeByFuture.put(future, executablePluginType);
    } catch (RuntimeException e) {
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
   * This method processes all completed tasks from the CompletionService queue. For each task:
   * <ul>
   *   <li>The result of the task is obtained, logging its completion or handling any execution exceptions.</li>
   *   <li>The associated plugin type is removed from the mapping of futures to plugin types.</li>
   *   <li>The associated semaphore for the respective plugin type is released to allow new tasks for that type.</li>
   * </ul>
   *
   * @throws InterruptedException if the thread is interrupted while waiting for the completion of tasks.
   */
  public void cleanup() throws InterruptedException {
    Future<WorkflowExecution> future;
    while ((future = completionService.poll()) != null) {
      ExecutablePluginType pluginType = pluginTypeByFuture.remove(future);
      try {
        WorkflowExecution execution = future.get();
        LOGGER.info("workflowExecutionId: {} - Task finished", execution.getId());
      } catch (ExecutionException e) {
        LOGGER.warn("Exception occurred in Future task for pluginType {}", pluginType, e);
      } finally {
        semaphoresPerPluginManager.releaseForPluginType(pluginType);
      }
    }
  }

}

