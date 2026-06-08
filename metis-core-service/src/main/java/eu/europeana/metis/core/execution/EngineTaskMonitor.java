package eu.europeana.metis.core.execution;

import eu.europeana.metis.core.engine.base.EngineTask;
import eu.europeana.metis.core.engine.base.EngineTaskClient;
import eu.europeana.metis.core.engine.base.EngineTaskSettings;
import eu.europeana.metis.core.engine.base.task.report.EngineTaskProgress;
import eu.europeana.metis.core.workflow.execution.SystemId;
import eu.europeana.metis.core.workflow.plugins.AbstractExecutablePlugin;
import eu.europeana.metis.core.workflow.plugins.ExecutionProgress;
import eu.europeana.metis.exception.ExternalTaskException;
import lombok.extern.slf4j.Slf4j;

/**
 * Monitors and manages the execution of a task in a processing engine.
 *
 * @param <S> The type representing the task settings required for the engine tasks.
 * @param <T> The type representing the tasks to be managed by the engine.
 */
@Slf4j
public class EngineTaskMonitor<S extends EngineTaskSettings, T extends EngineTask> {

  private final AbstractExecutablePlugin<?> plugin;
  private final EngineTaskClient<S, T> engineTaskClient;

  /**
   * Constructor
   *
   * @param plugin The plugin instance to be monitored, of type {@link AbstractExecutablePlugin}.
   * @param engineTaskClient The engine task client for managing and interacting with engine tasks.
   */
  public EngineTaskMonitor(AbstractExecutablePlugin<?> plugin, EngineTaskClient<S, T> engineTaskClient) {
    this.plugin = plugin;
    this.engineTaskClient = engineTaskClient;
  }

  /**
   * Monitors and retrieves the progress of an external task associated with the plugin. Updates the execution progress based on
   * the retrieved task information.
   *
   * @return An instance of {@link EngineTaskProgress} containing the progress details of the external task.
   * @throws ExternalTaskException If an error occurs while interacting with the external resource.
   */
  public EngineTaskProgress monitor() throws ExternalTaskException {
    log.info("Requesting progress information for externalTaskId: {}", plugin.getExternalTaskId());
    EngineTaskProgress engineTaskProgress = engineTaskClient.getEngineTaskProgress(
        plugin.getTopologyName(), plugin.getExternalTaskId(), plugin.getPluginMetadata().getExecutablePluginType());
    log.info("Task information received for externalTaskId: {}", plugin.getExternalTaskId());
    updateExecutionProgress(engineTaskProgress);
    return engineTaskProgress;
  }

  void updateExecutionProgress(EngineTaskProgress engineTaskProgress) {
    //todo: We further need to update the ExecutionProgress entity to support the new counters
    ExecutionProgress executionProgress = plugin.getExecutionProgress();
    executionProgress.setExpectedRecords(engineTaskProgress.getExpectedRecords());
    executionProgress.setProcessedRecords(engineTaskProgress.getProcessedRecords());
    executionProgress.recalculateProgressPercentage();
    executionProgress.setStatus(engineTaskProgress.getEngineTaskState().name());
    executionProgress.setSuccessRecords(engineTaskProgress.getSuccessRecords());
    executionProgress.setFailRecords(engineTaskProgress.getFailRecords());
    executionProgress.setWarningRecords(engineTaskProgress.getWarningRecords());
    executionProgress.setDuplicateRecords(engineTaskProgress.getDuplicateRecords());
    executionProgress.setUnchangedRecords(engineTaskProgress.getUnchangedRecords());
    executionProgress.setExpectedDepublishRecords(engineTaskProgress.getExpectedDepublishRecords());
    executionProgress.setSuccessDepublishRecords(engineTaskProgress.getSuccessDepublishRecords());
    executionProgress.setFailDepublishRecords(engineTaskProgress.getFailDepublishRecords());
    executionProgress.setProcessedDepublishRecords(engineTaskProgress.getProcessedDepublishRecords());
  }

  /**
   * Cancels the execution of an external task associated with the plugin.
   *
   * @param cancelledById Identifier indicating who initiated the cancellation. Either a system identifier or a user identifier.
   * @throws ExternalTaskException If an error occurs while attempting to cancel the task.
   */
  public void cancel(String cancelledById) throws ExternalTaskException {
    log.info("Cancel execution for externalTaskId: {}", plugin.getExternalTaskId());
    String message = SystemId.SYSTEM_MINUTE_CAP_EXPIRE.name().equals(cancelledById) ? "Cancelled By System" : "Cancelled By User";
    engineTaskClient.cancelEngineTask(plugin.getTopologyName(), plugin.getExternalTaskId(), message,
        plugin.getPluginMetadata().getExecutablePluginType());
  }
}
