package eu.europeana.metis.core.execution;

import eu.europeana.metis.core.dao.WorkflowExecutionDao;
import eu.europeana.metis.core.engine.base.EngineTask;
import eu.europeana.metis.core.engine.base.EngineTaskClient;
import eu.europeana.metis.core.engine.base.EngineTaskSettings;
import lombok.Getter;
import lombok.Setter;

/**
 * These are settings that are all related to the actual execution of workflows, and used mostly by the classes
 * {@link WorkflowExecutor}.
 *
 * @param <S> The type representing the task settings required for the engine tasks.
 * @param <T> The type representing the tasks to be managed by the engine.
 */
@Setter
@Getter
public class WorkflowExecutorSettings<S extends EngineTaskSettings, T extends EngineTask> {

  private static final int DEFAULT_MONITOR_CHECK_INTERVAL_IN_SECS = 5;
  private static final int DEFAULT_PERIOD_OF_NO_PROCESSED_RECORDS_CHANGE_IN_MINUTES = 30;
  private int dpsMonitorCheckIntervalInSecs = DEFAULT_MONITOR_CHECK_INTERVAL_IN_SECS;
  private int periodOfNoProcessedRecordsChangeInMinutes = DEFAULT_PERIOD_OF_NO_PROCESSED_RECORDS_CHANGE_IN_MINUTES;

  private final SemaphoresPerPluginManager semaphoresPerPluginManager;
  private final WorkflowExecutionDao workflowExecutionDao;
  private final WorkflowPostProcessor workflowPostProcessor;
  private final EngineTaskClient<S, T> engineTaskClient;

  /**
   * Constructor.
   *
   * @param semaphoresPerPluginManager the semaphores manager for controlling access to plugin types
   * @param workflowExecutionDao the data access object for workflow execution operations
   * @param workflowPostProcessor the post-processor responsible for post-execution actions
   * @param engineTaskClient the client to manage engine tasks with specified settings and task types
   */
  public WorkflowExecutorSettings(
      SemaphoresPerPluginManager semaphoresPerPluginManager, WorkflowExecutionDao workflowExecutionDao,
      WorkflowPostProcessor workflowPostProcessor, EngineTaskClient<S, T> engineTaskClient) {
    this.semaphoresPerPluginManager = semaphoresPerPluginManager;
    this.workflowExecutionDao = workflowExecutionDao;
    this.workflowPostProcessor = workflowPostProcessor;
    this.engineTaskClient = engineTaskClient;
  }
}
