package eu.europeana.metis.core.execution;

import eu.europeana.metis.core.dao.WorkflowExecutionDao;
import eu.europeana.metis.core.engine.base.EngineTask;
import eu.europeana.metis.core.engine.base.EngineTaskClient;
import eu.europeana.metis.core.engine.base.EngineTaskSettings;

/**
 * Manager class for adding executions in the distributed queue.
 *
 * @param <S> The type representing the task settings required for the engine tasks.
 * @param <T> The type representing the tasks to be managed by the engine.
 */
public class WorkflowExecutorManager<S extends EngineTaskSettings, T extends EngineTask> {

  private final WorkflowExecutorManagerSettings workflowExecutorManagerSettings;
  private final SemaphoresPerPluginManager semaphoresPerPluginManager;
  private final WorkflowExecutionDao workflowExecutionDao;
  private final WorkflowPostProcessor workflowPostProcessor;
  private final EngineTaskClient<S, T> engineTaskClient;

  /**
   * Constructor for WorkflowExecutorManager that initializes various dependencies for managing
   * workflow execution and processing.
   *
   * @param workflowExecutorManagerSettings Settings related to workflow execution.
   * @param semaphoresPerPluginManager Manager handling semaphores for different plugin types.
   * @param workflowExecutionDao Data access object for workflow execution data.
   * @param workflowPostProcessor Processor for post-workflow execution tasks.
   * @param engineTaskClient Client for executing engine tasks.
   */
  public WorkflowExecutorManager(
      WorkflowExecutorManagerSettings workflowExecutorManagerSettings, SemaphoresPerPluginManager semaphoresPerPluginManager,
      WorkflowExecutionDao workflowExecutionDao, WorkflowPostProcessor workflowPostProcessor, EngineTaskClient<S, T> engineTaskClient) {
    this.workflowExecutorManagerSettings = workflowExecutorManagerSettings;
    this.semaphoresPerPluginManager = semaphoresPerPluginManager;
    this.workflowExecutionDao = workflowExecutionDao;
    this.workflowPostProcessor = workflowPostProcessor;
    this.engineTaskClient = engineTaskClient;
  }

  public WorkflowExecutorManagerSettings getWorkflowExecutionSettings() {
    return workflowExecutorManagerSettings;
  }

  public SemaphoresPerPluginManager getSemaphoresPerPluginManager() {
    return semaphoresPerPluginManager;
  }

  public WorkflowExecutionDao getWorkflowExecutionDao() {
    return workflowExecutionDao;
  }

  public WorkflowPostProcessor getWorkflowPostProcessor() {
    return workflowPostProcessor;
  }

  public EngineTaskClient<S, T> getEngineTaskClient() {
    return engineTaskClient;
  }
}
