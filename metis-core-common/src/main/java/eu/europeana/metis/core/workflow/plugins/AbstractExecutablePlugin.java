package eu.europeana.metis.core.workflow.plugins;

import eu.europeana.cloud.common.model.dps.TaskInfo;
import eu.europeana.cloud.common.model.dps.TaskState;
import eu.europeana.metis.core.engine.base.ProcessingEngineTask;
import eu.europeana.metis.core.engine.base.ProcessingEngineTaskClient;
import eu.europeana.metis.core.engine.base.ProcessingEngineTaskSettings;
import eu.europeana.metis.core.engine.base.report.task.ProcessingEngineTaskProgress;
import eu.europeana.metis.core.engine.base.report.task.ProcessingEngineTaskState;
import eu.europeana.metis.core.workflow.execution.SystemId;
import eu.europeana.metis.exception.ExternalTaskException;
import eu.europeana.metis.exception.UnrecoverableExternalTaskException;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This abstract class is the base implementation of {@link ExecutablePlugin} and all executable plugins should inherit from it.
 *
 * @param <M> The type of the plugin metadata that this plugin represents.
 */
public abstract class AbstractExecutablePlugin<M extends AbstractExecutablePluginMetadata>
    extends AbstractMetisPlugin<M> implements ExecutablePlugin {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private String externalTaskId;
  private ExecutionProgress executionProgress = new ExecutionProgress();

  /**
   * Required by (de)serialization in db.
   * <p>It is not to be used manually</p>
   */
  AbstractExecutablePlugin() {
    //Required by (de)serialization in db
  }

  /**
   * Constructor with provided pluginType
   *
   * @param pluginType {@link PluginType}
   */
  AbstractExecutablePlugin(PluginType pluginType) {
    super(pluginType);
  }

  /**
   * Constructor to initialize the plugin with pluginMetadata required and the pluginType.
   *
   * @param pluginType a {@link PluginType} related to the implemented plugin
   * @param pluginMetadata the plugin metadata
   */
  AbstractExecutablePlugin(PluginType pluginType, M pluginMetadata) {
    super(pluginType, pluginMetadata);
  }

  @Override
  public String getExternalTaskId() {
    return this.externalTaskId;
  }

  /**
   * @param externalTaskId String representation of the external task identifier of the execution
   */
  public void setExternalTaskId(String externalTaskId) {
    this.externalTaskId = externalTaskId;
  }

  @Override
  public ExecutionProgress getExecutionProgress() {
    return this.executionProgress;
  }

  /**
   * @param executionProgress {@link ExecutionProgress} of the external execution
   */
  public void setExecutionProgress(ExecutionProgress executionProgress) {
    this.executionProgress = executionProgress;
  }

  @Override
  public <S extends ProcessingEngineTaskSettings, T extends ProcessingEngineTask>
  MonitorResult monitor(ProcessingEngineTaskClient<S, T> processingEngineTaskClient)
      throws ExternalTaskException, UnrecoverableExternalTaskException {
    LOGGER.info("Requesting progress information for externalTaskId: {}", getExternalTaskId());
    ProcessingEngineTaskProgress processingEngineTaskProgress = processingEngineTaskClient.getTaskProgress(getTopologyName(),
        Long.parseLong(getExternalTaskId()));
    LOGGER.info("Task information received for externalTaskId: {}", getExternalTaskId());
    updateExecutionProgress(processingEngineTaskProgress);
    TaskState taskState = TaskState.valueOf(processingEngineTaskProgress.getExternalTaskState().name());
    return new MonitorResult(taskState, processingEngineTaskProgress.getExternalTaskState().getDefaultMessage());
  }

  /**
   * Update this object's {@link ExecutionProgress} based on the received {@link TaskInfo}.
   *
   * @param taskInfo {@link TaskInfo}
   */
  //OLD
  public static ProcessingEngineTaskProgress getExternalTaskProgress(TaskInfo taskInfo) {
    ProcessingEngineTaskProgress processingEngineTaskProgress = new ProcessingEngineTaskProgress();
    processingEngineTaskProgress.setExpectedRecords(taskInfo.getExpectedRecordsNumber());
    processingEngineTaskProgress.setProcessedRecords(taskInfo.getProcessedRecordsCount());
    processingEngineTaskProgress.setDeletedRecords(taskInfo.getDeletedRecordsCount());
    processingEngineTaskProgress.setIgnoredRecords(taskInfo.getIgnoredRecordsCount());
    processingEngineTaskProgress.setProcessedErrors(taskInfo.getProcessedErrorsCount());
    processingEngineTaskProgress.setDeletedErrors(taskInfo.getDeletedErrorsCount());
    ProcessingEngineTaskState processingEngineTaskState = ProcessingEngineTaskState.valueOf(taskInfo.getState().name());
    processingEngineTaskProgress.setExternalTaskState(processingEngineTaskState);
    return processingEngineTaskProgress;
  }

  //NEW
  void updateExecutionProgress(ProcessingEngineTaskProgress processingEngineTaskProgress) {

    // Calculate the various counts.
    // The expectedRecordsNumber we get from ecloud is dynamic and can change during execution.
    int expectedRecordCount;
    int processedRecordCount;
    int deletedRecordCount;

    switch (getPluginMetadata()) {
      case
          AbstractHarvestPluginMetadata abstractHarvestPluginMetadata when abstractHarvestPluginMetadata.isIncrementalHarvest() -> {
        //Incremental Harvest
        //deletedRecordsCount never used
        //expectedPostProcessedRecordsNumber and postProcessedRecordsCount represent deleted records
        expectedRecordCount = processingEngineTaskProgress.getExpectedRecords();
        processedRecordCount = processingEngineTaskProgress.getProcessedRecords() + processingEngineTaskProgress.getIgnoredRecords();
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
        expectedRecordCount = processingEngineTaskProgress.getExpectedRecords() - processingEngineTaskProgress.getDeletedRecords();
        processedRecordCount = processingEngineTaskProgress.getProcessedRecords();
        deletedRecordCount = processingEngineTaskProgress.getDeletedRecords();
      }
    }

    int errorCount = processingEngineTaskProgress.getProcessedErrors() + processingEngineTaskProgress.getDeletedErrors();
    // Update the execution progress.
    getExecutionProgress().setExpectedRecords(expectedRecordCount);
    getExecutionProgress().setProcessedRecords(processedRecordCount);
    getExecutionProgress().setDeletedRecords(deletedRecordCount);
    getExecutionProgress().setIgnoredRecords(processingEngineTaskProgress.getIgnoredRecords());
    getExecutionProgress().setErrors(errorCount);
    getExecutionProgress().recalculateProgressPercentage();
    TaskState taskState = TaskState.valueOf(processingEngineTaskProgress.getExternalTaskState().name());
    getExecutionProgress().setStatus(taskState);
  }


  @Override
  public <S extends ProcessingEngineTaskSettings, T extends ProcessingEngineTask>
  void cancel(ProcessingEngineTaskClient<S, T> processingEngineTaskClient, String cancelledById)
      throws ExternalTaskException {
    LOGGER.info("Cancel execution for externalTaskId: {}", getExternalTaskId());
    processingEngineTaskClient.cancel(getTopologyName(), Long.parseLong(getExternalTaskId()),
        SystemId.SYSTEM_MINUTE_CAP_EXPIRE.name().equals(cancelledById) ? "Cancelled By System" : "Cancelled By User");
  }
}
