package eu.europeana.metis.core.execution;

import eu.europeana.metis.core.engine.base.EngineTask;
import eu.europeana.metis.core.engine.base.EngineTaskClient;
import eu.europeana.metis.core.engine.base.EngineTaskSettings;
import eu.europeana.metis.core.engine.base.task.report.EngineTaskProgress;
import eu.europeana.metis.core.workflow.execution.SystemId;
import eu.europeana.metis.core.workflow.plugins.AbstractExecutablePlugin;
import eu.europeana.metis.core.workflow.plugins.AbstractHarvestPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.AbstractIndexPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.ExecutionProgress;
import eu.europeana.metis.exception.ExternalTaskException;
import eu.europeana.metis.exception.UnrecoverableExternalTaskException;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PluginMonitor<S extends EngineTaskSettings, T extends EngineTask> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final AbstractExecutablePlugin<?> plugin;
  private final EngineTaskClient<S, T> engineTaskClient;

  public PluginMonitor(AbstractExecutablePlugin<?> plugin, EngineTaskClient<S, T> engineTaskClient) {
    this.plugin = plugin;
    this.engineTaskClient = engineTaskClient;
  }

  public EngineTaskProgress monitor()
      throws ExternalTaskException, UnrecoverableExternalTaskException {
    LOGGER.info("Requesting progress information for externalTaskId: {}", plugin.getExternalTaskId());
    EngineTaskProgress engineTaskProgress = engineTaskClient.getEngineTaskProgress(
        plugin.getTopologyName(), Long.parseLong(plugin.getExternalTaskId()));
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
      case AbstractHarvestPluginMetadata abstractHarvestPluginMetadata -> {
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

  public void cancel(String cancelledById) throws ExternalTaskException {
    LOGGER.info("Cancel execution for externalTaskId: {}", plugin.getExternalTaskId());
    engineTaskClient.cancelEngineTask(plugin.getTopologyName(), Long.parseLong(plugin.getExternalTaskId()),
        SystemId.SYSTEM_MINUTE_CAP_EXPIRE.name().equals(cancelledById) ? "Cancelled By System" : "Cancelled By User");
  }
}
