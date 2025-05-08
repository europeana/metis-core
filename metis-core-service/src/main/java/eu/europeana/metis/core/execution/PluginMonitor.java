package eu.europeana.metis.core.execution;

import eu.europeana.metis.core.engine.base.AbstractEngineTask;
import eu.europeana.metis.core.engine.base.AbstractEngineTaskSettings;
import eu.europeana.metis.core.engine.base.EngineTaskClient;
import eu.europeana.metis.core.engine.base.task.report.EngineTaskProgress;
import eu.europeana.metis.core.workflow.execution.SystemId;
import eu.europeana.metis.core.workflow.plugins.AbstractExecutablePlugin;
import eu.europeana.metis.core.workflow.plugins.AbstractHarvestPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.AbstractIndexPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.ExecutionProgress;
import eu.europeana.metis.exception.ExternalTaskException;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Monitors and manages the execution of a plugin task in a processing engine.
 *
 * @param <S> The type representing the task settings required for the engine tasks.
 * @param <T> The type representing the tasks to be managed by the engine.
 */
public class PluginMonitor<S extends AbstractEngineTaskSettings, T extends AbstractEngineTask> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final AbstractExecutablePlugin<?> plugin;
  private final EngineTaskClient<S, T> engineTaskClient;

  /**
   * Constructs a PluginMonitor to monitor and interact with a plugin and its associated engine tasks.
   *
   * @param plugin The plugin instance to be monitored, of type {@link AbstractExecutablePlugin}.
   * @param engineTaskClient The engine task client for managing and interacting with engine tasks.
   */
  public PluginMonitor(AbstractExecutablePlugin<?> plugin, EngineTaskClient<S, T> engineTaskClient) {
    this.plugin = plugin;
    this.engineTaskClient = engineTaskClient;
  }

  /**
   * Monitors and retrieves the progress of an external task associated with the plugin.
   * Updates the execution progress based on the retrieved task information.
   *
   * @return An instance of {@link EngineTaskProgress} containing the progress details of the external task.
   * @throws ExternalTaskException If an error occurs while interacting with the external resource.
   */
  public EngineTaskProgress monitor() throws ExternalTaskException {
    LOGGER.info("Requesting progress information for externalTaskId: {}", plugin.getExternalTaskId());
    EngineTaskProgress engineTaskProgress = engineTaskClient.getEngineTaskProgress(
        plugin.getTopologyName(), plugin.getExternalTaskId());
    LOGGER.info("Task information received for externalTaskId: {}", plugin.getExternalTaskId());
    updateExecutionProgress(engineTaskProgress);
    return engineTaskProgress;
  }

  void updateExecutionProgress(EngineTaskProgress engineTaskProgress) {

    // Calculate the various counts.
    // The expectedRecordsNumber we get from ecloud is dynamic and can change during execution.
    int expectedRecordCount;
    int processedRecordCount;
    int deletedRecordCount;

    switch (plugin.getPluginMetadata()) {
      case
          AbstractHarvestPluginMetadata abstractHarvestPluginMetadata when abstractHarvestPluginMetadata.isIncrementalHarvest() -> {
        //Incremental Harvest
        //deletedRecordsCount never used
        //expectedPostProcessedRecordsNumber and postProcessedRecordsCount represent deleted records
        expectedRecordCount = engineTaskProgress.getExpectedRecords();
        processedRecordCount =
            engineTaskProgress.getProcessedRecords() + engineTaskProgress.getIgnoredRecords();
        deletedRecordCount = engineTaskProgress.getDeletedRecords();
      }
      case AbstractHarvestPluginMetadata ignored -> {
        //Full Harvest
        //expectedPostProcessedRecordsNumber, postProcessedRecordsCount and ignoredRecordsCount not used
        //deletedRecordsCount is always 0
        expectedRecordCount = engineTaskProgress.getExpectedRecords();
        processedRecordCount = engineTaskProgress.getProcessedRecords();
        deletedRecordCount = engineTaskProgress.getDeletedRecords();
      }
      case AbstractIndexPluginMetadata abstractIndexPluginMetadata when !abstractIndexPluginMetadata.isIncrementalIndexing() -> {
        //Full Indexing
        //ignoredRecordsCount never used
        //expectedPostProcessedRecordsNumber and postProcessedRecordsCount represent deleted records
        //The deletedRecordsCount is always 0
        expectedRecordCount = engineTaskProgress.getExpectedRecords();
        processedRecordCount = engineTaskProgress.getProcessedRecords();
        deletedRecordCount = engineTaskProgress.getDeletedRecords();
      }
      case null, default -> {
        //Other plugins including incremental indexing
        //expectedPostProcessedRecordsNumber, postProcessedRecordsCount and ignoredRecordsCount not used
        expectedRecordCount =
            engineTaskProgress.getExpectedRecords() - engineTaskProgress.getDeletedRecords();
        processedRecordCount = engineTaskProgress.getProcessedRecords();
        deletedRecordCount = engineTaskProgress.getDeletedRecords();
      }
    }

    int errorCount = engineTaskProgress.getProcessedErrors() + engineTaskProgress.getDeletedErrors();
    // Update the execution progress.
    ExecutionProgress executionProgress = plugin.getExecutionProgress();
    executionProgress.setExpectedRecords(expectedRecordCount);
    executionProgress.setProcessedRecords(processedRecordCount);
    executionProgress.setDeletedRecords(deletedRecordCount);
    executionProgress.setIgnoredRecords(engineTaskProgress.getIgnoredRecords());
    executionProgress.setErrors(errorCount);
    executionProgress.recalculateProgressPercentage();
    executionProgress.setStatus(engineTaskProgress.getEngineTaskState().name());
  }

  /**
   * Cancels the execution of an external task associated with the plugin.
   *
   * @param cancelledById Identifier indicating who initiated the cancellation. Either a system identifier or a user identifier.
   * @throws ExternalTaskException If an error occurs while attempting to cancel the task.
   */
  public void cancel(String cancelledById) throws ExternalTaskException {
    LOGGER.info("Cancel execution for externalTaskId: {}", plugin.getExternalTaskId());
    String message = SystemId.SYSTEM_MINUTE_CAP_EXPIRE.name().equals(cancelledById) ? "Cancelled By System" : "Cancelled By User";
    engineTaskClient.cancelEngineTask(plugin.getTopologyName(), plugin.getExternalTaskId(), message);
  }
}
