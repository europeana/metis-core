package eu.europeana.metis.core.execution;

import eu.europeana.metis.core.engine.base.ProcessingEngineTask;
import eu.europeana.metis.core.engine.base.ProcessingEngineTaskClient;
import eu.europeana.metis.core.engine.base.ProcessingEngineTaskSettings;
import eu.europeana.metis.core.engine.base.report.task.ProcessingEngineTaskProgress;
import eu.europeana.metis.core.workflow.plugins.AbstractExecutablePlugin;
import eu.europeana.metis.core.workflow.plugins.AbstractHarvestPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.AbstractIndexPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.ExecutionProgress;
import eu.europeana.metis.exception.ExternalTaskException;
import eu.europeana.metis.exception.UnrecoverableExternalTaskException;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PluginMonitor {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final AbstractExecutablePlugin<?> plugin;

  public PluginMonitor(AbstractExecutablePlugin<?> plugin) {
    this.plugin = plugin;
  }

  public <S extends ProcessingEngineTaskSettings, T extends ProcessingEngineTask>
  ProcessingEngineTaskProgress monitor(ProcessingEngineTaskClient<S, T> processingEngineTaskClient)
      throws ExternalTaskException, UnrecoverableExternalTaskException {
    LOGGER.info("Requesting progress information for externalTaskId: {}", plugin.getExternalTaskId());
    ProcessingEngineTaskProgress processingEngineTaskProgress = processingEngineTaskClient.getTaskProgress(
        plugin.getTopologyName(), Long.parseLong(plugin.getExternalTaskId()));
    LOGGER.info("Task information received for externalTaskId: {}", plugin.getExternalTaskId());
    updateExecutionProgress(processingEngineTaskProgress);
    return processingEngineTaskProgress;
  }

  void updateExecutionProgress(ProcessingEngineTaskProgress processingEngineTaskProgress) {

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
        expectedRecordCount = processingEngineTaskProgress.getExpectedRecords();
        processedRecordCount =
            processingEngineTaskProgress.getProcessedRecords() + processingEngineTaskProgress.getIgnoredRecords();
        deletedRecordCount = processingEngineTaskProgress.getDeletedRecords();
      }
      case AbstractHarvestPluginMetadata abstractHarvestPluginMetadata -> {
        //Full Harvest
        //expectedPostProcessedRecordsNumber, postProcessedRecordsCount and ignoredRecordsCount not used
        //deletedRecordsCount is always 0
        expectedRecordCount = processingEngineTaskProgress.getExpectedRecords();
        processedRecordCount = processingEngineTaskProgress.getProcessedRecords();
        deletedRecordCount = processingEngineTaskProgress.getDeletedRecords();
      }
      case AbstractIndexPluginMetadata abstractIndexPluginMetadata when !abstractIndexPluginMetadata.isIncrementalIndexing() -> {
        //Full Indexing
        //ignoredRecordsCount never used
        //expectedPostProcessedRecordsNumber and postProcessedRecordsCount represent deleted records
        //The deletedRecordsCount is always 0
        expectedRecordCount = processingEngineTaskProgress.getExpectedRecords();
        processedRecordCount = processingEngineTaskProgress.getProcessedRecords();
        deletedRecordCount = processingEngineTaskProgress.getDeletedRecords();
      }
      case null, default -> {
        //Other plugins including incremental indexing
        //expectedPostProcessedRecordsNumber, postProcessedRecordsCount and ignoredRecordsCount not used
        expectedRecordCount =
            processingEngineTaskProgress.getExpectedRecords() - processingEngineTaskProgress.getDeletedRecords();
        processedRecordCount = processingEngineTaskProgress.getProcessedRecords();
        deletedRecordCount = processingEngineTaskProgress.getDeletedRecords();
      }
    }

    int errorCount = processingEngineTaskProgress.getProcessedErrors() + processingEngineTaskProgress.getDeletedErrors();
    // Update the execution progress.
    ExecutionProgress executionProgress = plugin.getExecutionProgress();
    executionProgress.setExpectedRecords(expectedRecordCount);
    executionProgress.setProcessedRecords(processedRecordCount);
    executionProgress.setDeletedRecords(deletedRecordCount);
    executionProgress.setIgnoredRecords(processingEngineTaskProgress.getIgnoredRecords());
    executionProgress.setErrors(errorCount);
    executionProgress.recalculateProgressPercentage();
    executionProgress.setStatus(processingEngineTaskProgress.getProcessingEngineTaskState().name());
  }
}
