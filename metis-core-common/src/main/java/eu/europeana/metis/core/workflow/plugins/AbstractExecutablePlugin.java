package eu.europeana.metis.core.workflow.plugins;

import static eu.europeana.metis.core.engine.base.ProcessingEngineTask.InputDataType.EXTERNAL_REPOSITORY;
import static eu.europeana.metis.core.engine.base.ProcessingEngineTask.InputDataType.INTERNAL_DATASET;

import eu.europeana.cloud.common.model.dps.TaskInfo;
import eu.europeana.cloud.common.model.dps.TaskState;
import eu.europeana.cloud.service.dps.PluginParameterKeys;
import eu.europeana.metis.core.engine.base.DataRevision;
import eu.europeana.metis.core.engine.base.ProcessingEngineTask;
import eu.europeana.metis.core.engine.base.ProcessingEngineTaskClient;
import eu.europeana.metis.core.engine.base.ProcessingEngineTaskSettings;
import eu.europeana.metis.core.engine.base.report.task.ProcessingEngineTaskProgress;
import eu.europeana.metis.core.engine.base.report.task.ProcessingEngineTaskState;
import eu.europeana.metis.core.workflow.execution.SystemId;
import eu.europeana.metis.exception.ExternalTaskException;
import eu.europeana.metis.exception.UnrecoverableExternalTaskException;
import eu.europeana.metis.utils.CommonStringValues;
import java.lang.invoke.MethodHandles;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
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

  //NEW
  private DataRevision createDataRevisionOutput(String ecloudProvider) {
    return new DataRevision(getPluginType().name(), ecloudProvider, getStartedDate(), false);
  }

  // NEW
  private <T extends ProcessingEngineTask> T createExternalTaskForPluginWithExistingDataset(Map<String, String> parameters,
      ProcessingEngineTaskSettings<T> processingEngineTaskSettings) {
    T externalTask = processingEngineTaskSettings.getTaskCreator().get();
    final String inputDataLocation =
        String.format(CommonStringValues.S_DATA_PROVIDERS_S_DATA_SETS_S_TEMPLATE, processingEngineTaskSettings.getBaseUrl(),
            processingEngineTaskSettings.getProvider(), processingEngineTaskSettings.getDatasetId());

    externalTask.setInputDataLocation(INTERNAL_DATASET, inputDataLocation);
    externalTask.setParameters(parameters);
    externalTask.setOutputRevision(createDataRevisionOutput(processingEngineTaskSettings.getProvider()));
    return externalTask;
  }

  //NEW
  <T extends ProcessingEngineTask> T createExternalTaskForHarvestPlugin(
      ProcessingEngineTaskSettings<T> processingEngineTaskSettings,
      Map<String, String> extraParameters, String targetUrl, boolean incrementalProcessing) {
    T externalTask = processingEngineTaskSettings.getTaskCreator().get();
    externalTask.setInputDataLocation(EXTERNAL_REPOSITORY, targetUrl);

    Map<String, String> parameters = new HashMap<>();
    if (extraParameters != null) {
      parameters.putAll(extraParameters);
    }

    final DateFormat dateFormat = new SimpleDateFormat(CommonStringValues.DATE_FORMAT_Z, Locale.US);
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    parameters.put("INCREMENTAL_HARVEST", String.valueOf(incrementalProcessing));
    parameters.put("HARVEST_DATE", dateFormat.format(getStartedDate()));
    parameters.put("PROVIDER_ID", processingEngineTaskSettings.getProvider());
    parameters.put("OUTPUT_DATA_SETS", String
        .format(CommonStringValues.S_DATA_PROVIDERS_S_DATA_SETS_S_TEMPLATE, processingEngineTaskSettings.getBaseUrl(),
            processingEngineTaskSettings.getProvider(), processingEngineTaskSettings.getDatasetId()));
    parameters.put(PluginParameterKeys.NEW_REPRESENTATION_NAME, MetisPlugin.getRepresentationName());

    externalTask.setParameters(parameters);
    externalTask.setOutputRevision(createDataRevisionOutput(processingEngineTaskSettings.getProvider()));
    return externalTask;
  }

  //NEW
  <T extends ProcessingEngineTask> T createExternalTaskForProcessPlugin(
      ProcessingEngineTaskSettings<T> processingEngineTaskSettings,
      Map<String, String> extraParameters) {
    Map<String, String> parameters = new HashMap<>();
    if (extraParameters != null) {
      parameters.putAll(extraParameters);
    }
    parameters.put("REPRESENTATION_NAME", MetisPlugin.getRepresentationName());
    parameters.put("REVISION_NAME", getPluginMetadata().getRevisionNamePreviousPlugin());
    parameters.put("REVISION_PROVIDER", processingEngineTaskSettings.getProvider());
    DateFormat dateFormat = new SimpleDateFormat(CommonStringValues.DATE_FORMAT_Z, Locale.US);
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    parameters.put("REVISION_TIMESTAMP", dateFormat.format(getPluginMetadata().getRevisionTimestampPreviousPlugin()));
    parameters.put("PREVIOUS_TASK_ID", processingEngineTaskSettings.getPreviousTaskId());
    parameters.put("NEW_REPRESENTATION_NAME", MetisPlugin.getRepresentationName());
    parameters.put("OUTPUT_DATA_SETS", String
        .format(CommonStringValues.S_DATA_PROVIDERS_S_DATA_SETS_S_TEMPLATE, processingEngineTaskSettings.getBaseUrl(),
            processingEngineTaskSettings.getProvider(), processingEngineTaskSettings.getDatasetId()));
    return createExternalTaskForPluginWithExistingDataset(parameters, processingEngineTaskSettings);
  }

  <T extends ProcessingEngineTask> T createExternalTaskForIndexPlugin(
      ProcessingEngineTaskSettings<T> processingEngineTaskSettings, String datasetId,
      AbstractIndexPluginMetadata abstractIndexPluginMetadata, String targetDatabase) {
    final DateFormat dateFormat = new SimpleDateFormat(CommonStringValues.DATE_FORMAT_Z, Locale.US);
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    final Map<String, String> extraParameters = new HashMap<>();
    extraParameters.put(PluginParameterKeys.METIS_DATASET_ID, datasetId);
    extraParameters.put(PluginParameterKeys.INCREMENTAL_INDEXING,
        String.valueOf(abstractIndexPluginMetadata.isIncrementalIndexing()));
    extraParameters.put(PluginParameterKeys.HARVEST_DATE, dateFormat.format(abstractIndexPluginMetadata.getHarvestDate()));
    extraParameters.put(PluginParameterKeys.METIS_TARGET_INDEXING_DATABASE, targetDatabase);
    extraParameters.put(PluginParameterKeys.METIS_RECORD_DATE, dateFormat.format(getStartedDate()));
    extraParameters.put(PluginParameterKeys.METIS_PRESERVE_TIMESTAMPS,
        String.valueOf(abstractIndexPluginMetadata.isPreserveTimestamps()));
    extraParameters.put(PluginParameterKeys.DATASET_IDS_TO_REDIRECT_FROM,
        String.join(",", abstractIndexPluginMetadata.getDatasetIdsToRedirectFrom()));
    extraParameters.put(PluginParameterKeys.PERFORM_REDIRECTS, String.valueOf(abstractIndexPluginMetadata.isPerformRedirects()));
    return createExternalTaskForProcessPlugin(processingEngineTaskSettings, extraParameters);
  }

  Map<String, String> createParametersForValidationExternal(String urlOfSchemasZip, String schemaRootPath,
      String schematronRootPath) {
    final Map<String, String> parametersForValidation = createParametersForValidation(urlOfSchemasZip, schemaRootPath,
        schematronRootPath);
    parametersForValidation.put(PluginParameterKeys.GENERATE_STATS, Boolean.TRUE.toString());
    return parametersForValidation;
  }

  Map<String, String> createParametersForValidationInternal(String urlOfSchemasZip, String schemaRootPath,
      String schematronRootPath) {
    final Map<String, String> parametersForValidation = createParametersForValidation(urlOfSchemasZip, schemaRootPath,
        schematronRootPath);
    parametersForValidation.put(PluginParameterKeys.GENERATE_STATS, Boolean.FALSE.toString());
    return parametersForValidation;
  }

  private Map<String, String> createParametersForValidation(String urlOfSchemasZip, String schemaRootPath,
      String schematronRootPath) {
    Map<String, String> extraParameters = new HashMap<>();
    extraParameters.put(PluginParameterKeys.SCHEMA_NAME, urlOfSchemasZip);
    extraParameters.put(PluginParameterKeys.ROOT_LOCATION, schemaRootPath);
    extraParameters.put(PluginParameterKeys.SCHEMATRON_LOCATION, schematronRootPath);
    return extraParameters;
  }

  abstract <T extends ProcessingEngineTask> T prepareExternalTask(String datasetId, ProcessingEngineTaskSettings<T> processingEngineTaskSettings);

  @Override
  public <T extends ProcessingEngineTask> void execute(String datasetId, ProcessingEngineTaskClient<T> processingEngineTaskClient,
      ProcessingEngineTaskSettings<T> processingEngineTaskSettings)
  //  public void execute(String datasetId, DpsClient dpsClient, DpsTaskSettings dpsTaskSettings)
      throws ExternalTaskException {
    String pluginTypeName = getPluginType().name();
    LOGGER.info("Starting execution of {} plugin for externalDatasetId {}", pluginTypeName, processingEngineTaskSettings.getDatasetId());
    T externalTask = prepareExternalTask(datasetId, processingEngineTaskSettings);

    try {
      setExternalTaskId(Long.toString(processingEngineTaskClient.submitTask(externalTask, getTopologyName())));
      setDataStatus(DataStatus.VALID);
    } catch (ExternalTaskException | RuntimeException e) {
      throw new ExternalTaskException("Submitting task failed", e);
    }
    LOGGER.info("Submitted task with externalTaskId: {}", getExternalTaskId());
  }

  @Override
  public <T extends ProcessingEngineTask> MonitorResult monitor(ProcessingEngineTaskClient<T> processingEngineTaskClient)
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
  public <T extends ProcessingEngineTask> void cancel(ProcessingEngineTaskClient<T> processingEngineTaskClient, String cancelledById)
      throws ExternalTaskException {
    LOGGER.info("Cancel execution for externalTaskId: {}", getExternalTaskId());
    processingEngineTaskClient.cancel(getTopologyName(), Long.parseLong(getExternalTaskId()),
        SystemId.SYSTEM_MINUTE_CAP_EXPIRE.name().equals(cancelledById) ? "Cancelled By System" : "Cancelled By User");
  }
}
