package eu.europeana.metis.core.engine.base;

import static eu.europeana.metis.core.engine.base.ProcessingEngineTask.InputDataType.EXTERNAL_REPOSITORY;
import static eu.europeana.metis.core.engine.base.ProcessingEngineTask.InputDataType.INTERNAL_DATASET;

import eu.europeana.cloud.service.dps.PluginParameterKeys;
import eu.europeana.metis.core.workflow.plugins.MetisPlugin;
import eu.europeana.metis.core.workflow.plugins.PluginType;
import eu.europeana.metis.utils.CommonStringValues;
import eu.europeana.metis.utils.RestEndpoints;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class ProcessingEngineTaskPreparator {

  public static <S extends ProcessingEngineTaskSettings, T extends ProcessingEngineTask>
  Map<String, String> createBasicTaskParameters(
      String externalDatasetId,
      String previousTaskId,
      String revisionNamePreviousPlugin,
      Date revisionTimestampPreviousPlugin,
      ProcessingEngineTaskClient<S, T> processingEngineTaskClient) {
    S processingEngineTaskSettings = processingEngineTaskClient.getProcessingEngineTaskSettings();
    Map<String, String> parameters = new HashMap<>();
    parameters.put("REPRESENTATION_NAME", MetisPlugin.getRepresentationName());
    parameters.put("REVISION_NAME", revisionNamePreviousPlugin);
    parameters.put("REVISION_PROVIDER", processingEngineTaskSettings.provider());
    DateFormat dateFormat = new SimpleDateFormat(CommonStringValues.DATE_FORMAT_Z, Locale.US);
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    parameters.put("REVISION_TIMESTAMP", dateFormat.format(revisionTimestampPreviousPlugin));
    parameters.put("PREVIOUS_TASK_ID", previousTaskId);
    parameters.put("NEW_REPRESENTATION_NAME", MetisPlugin.getRepresentationName());
    parameters.put("OUTPUT_DATA_SETS", String
        .format(CommonStringValues.S_DATA_PROVIDERS_S_DATA_SETS_S_TEMPLATE, processingEngineTaskSettings.baseUrl(),
            processingEngineTaskSettings.provider(), externalDatasetId));
    return parameters;
  }

  public static <S extends ProcessingEngineTaskSettings, T extends ProcessingEngineTask>
  Map<String, String> createBasicTaskParametersHarvest(
      String datasetId, boolean incrementalHarvest, Date startedDate,
      ProcessingEngineTaskClient<S, T> processingEngineTaskClient) {
    S processingEngineTaskSettings = processingEngineTaskClient.getProcessingEngineTaskSettings();
    final Map<String, String> parameters = new HashMap<>();
    parameters.put(PluginParameterKeys.METIS_DATASET_ID, datasetId);
    final DateFormat dateFormat = new SimpleDateFormat(CommonStringValues.DATE_FORMAT_Z, Locale.US);
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    parameters.put("INCREMENTAL_HARVEST", String.valueOf(incrementalHarvest));
    parameters.put("HARVEST_DATE", dateFormat.format(startedDate));
    parameters.put("PROVIDER_ID", processingEngineTaskSettings.provider());
    parameters.put("OUTPUT_DATA_SETS", String
        .format(CommonStringValues.S_DATA_PROVIDERS_S_DATA_SETS_S_TEMPLATE, processingEngineTaskSettings.baseUrl(),
            processingEngineTaskSettings.provider(), datasetId));
    parameters.put(PluginParameterKeys.NEW_REPRESENTATION_NAME, MetisPlugin.getRepresentationName());
    return parameters;
  }

  public static <S extends ProcessingEngineTaskSettings, T extends ProcessingEngineTask>
  T createExternalTaskForPluginWithExistingDataset(
      String externalDatasetId,
      PluginType pluginType,
      Date pluginStartedDate,
      Map<String, String> parameters,
      ProcessingEngineTaskClient<S, T> processingEngineTaskClient) {
    S processingEngineTaskSettings = processingEngineTaskClient.getProcessingEngineTaskSettings();
    T externalTask = processingEngineTaskClient.getTaskCreator().get();
    final String inputDataLocation =
        String.format(CommonStringValues.S_DATA_PROVIDERS_S_DATA_SETS_S_TEMPLATE, processingEngineTaskSettings.baseUrl(),
            processingEngineTaskSettings.provider(), externalDatasetId);

    externalTask.setInputDataLocation(INTERNAL_DATASET, inputDataLocation);
    externalTask.setParameters(parameters);
    externalTask.setOutputRevision(
        createDataRevisionOutput(pluginType, pluginStartedDate, processingEngineTaskSettings.provider()));
    return externalTask;
  }

  public static <S extends ProcessingEngineTaskSettings, T extends ProcessingEngineTask>
  T createExternalTaskForDepublish(
      Map<String, String> parameters,
      ProcessingEngineTaskClient<S, T> processingEngineTaskClient) {
    T externalTask = processingEngineTaskClient.getTaskCreator().get();
    externalTask.setParameters(parameters);
    return externalTask;
  }

  public static <S extends ProcessingEngineTaskSettings, T extends ProcessingEngineTask>
  T createExternalTaskForHarvest(
      String targetUrl,
      PluginType pluginType,
      Date pluginStartedDate,
      Map<String, String> parameters,
      ProcessingEngineTaskClient<S, T> processingEngineTaskClient,
      OaiHarvestParameters oaiHarvestParameters) {
    S processingEngineTaskSettings = processingEngineTaskClient.getProcessingEngineTaskSettings();
    T externalTask = processingEngineTaskClient.getTaskCreator().get();
    externalTask.setInputDataLocation(EXTERNAL_REPOSITORY, targetUrl);
    externalTask.setParameters(parameters);
    externalTask.setOutputRevision(
        createDataRevisionOutput(pluginType, pluginStartedDate, processingEngineTaskSettings.provider()));
    if (oaiHarvestParameters != null) {
      externalTask.setOaiHarvestParameters(oaiHarvestParameters);
    }
    return externalTask;
  }

  private static DataRevision createDataRevisionOutput(PluginType pluginType, Date pluginStartedDate, String ecloudProvider) {
    return new DataRevision(pluginType.name(), ecloudProvider, pluginStartedDate, false);
  }

  public static Map<String, String> createParametersForValidationExternal(String urlOfSchemasZip, String schemaRootPath,
      String schematronRootPath) {
    final Map<String, String> parametersForValidation = createParametersForValidation(urlOfSchemasZip, schemaRootPath,
        schematronRootPath);
    parametersForValidation.put(PluginParameterKeys.GENERATE_STATS, Boolean.TRUE.toString());
    return parametersForValidation;
  }

  public static Map<String, String> createParametersForValidationInternal(String urlOfSchemasZip, String schemaRootPath,
      String schematronRootPath) {
    final Map<String, String> parametersForValidation = createParametersForValidation(urlOfSchemasZip, schemaRootPath,
        schematronRootPath);
    parametersForValidation.put(PluginParameterKeys.GENERATE_STATS, Boolean.FALSE.toString());
    return parametersForValidation;
  }

  private static Map<String, String> createParametersForValidation(String urlOfSchemasZip, String schemaRootPath,
      String schematronRootPath) {
    Map<String, String> parameters = new HashMap<>();
    parameters.put(PluginParameterKeys.SCHEMA_NAME, urlOfSchemasZip);
    parameters.put(PluginParameterKeys.ROOT_LOCATION, schemaRootPath);
    parameters.put(PluginParameterKeys.SCHEMATRON_LOCATION, schematronRootPath);
    return parameters;
  }

  public static Map<String, String> createParametersForTransformation(String metisCoreBaseUrl, String xsltId, String datasetId,
      String datasetName, String country, String language) {
    Map<String, String> parameters = new HashMap<>();
    parameters.put(PluginParameterKeys.XSLT_URL,
        metisCoreBaseUrl + RestEndpoints
            .resolve(RestEndpoints.DATASETS_XSLT_XSLTID,
                Collections.singletonList(xsltId)));
    parameters.put(PluginParameterKeys.METIS_DATASET_ID, datasetId);
    parameters.put(PluginParameterKeys.METIS_DATASET_NAME, datasetName);
    parameters.put(PluginParameterKeys.METIS_DATASET_COUNTRY, country);
    parameters.put(PluginParameterKeys.METIS_DATASET_LANGUAGE, language);
    return parameters;
  }

  public static Map<String, String> createParametersForMedia(String maximumParallelization) {
    Map<String, String> parameters = new HashMap<>();
    parameters.put(PluginParameterKeys.MAXIMUM_PARALLELIZATION, maximumParallelization);
    return parameters;
  }

  public static Map<String, String> createParametersForLinkChecking(boolean performSampling, Integer sampleSize) {
    final Map<String, String> parameters = new HashMap<>();
    if (performSampling && sampleSize != null) {
      parameters.put(PluginParameterKeys.SAMPLE_SIZE, sampleSize.toString());
    }
    return parameters;
  }

  public static Map<String, String> createParametersIndex(
      String datasetId,
      Date pluginStartedDate,
      boolean incrementalIndexing,
      Date harvestDate,
      boolean preserveTimestamps,
      List<String> datasetIdsToRedirectFrom,
      boolean performRedirects,
      String targetIndexingDatabase) {
    final DateFormat dateFormat = new SimpleDateFormat(CommonStringValues.DATE_FORMAT_Z, Locale.US);
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    final Map<String, String> parameters = new HashMap<>();
    parameters.put(PluginParameterKeys.METIS_DATASET_ID, datasetId);
    parameters.put(PluginParameterKeys.INCREMENTAL_INDEXING,
        String.valueOf(incrementalIndexing));
    parameters.put(PluginParameterKeys.HARVEST_DATE, dateFormat.format(harvestDate));
    parameters.put(PluginParameterKeys.METIS_TARGET_INDEXING_DATABASE, targetIndexingDatabase);
    parameters.put(PluginParameterKeys.METIS_RECORD_DATE, dateFormat.format(pluginStartedDate));
    parameters.put(PluginParameterKeys.METIS_PRESERVE_TIMESTAMPS,
        String.valueOf(preserveTimestamps));
    parameters.put(PluginParameterKeys.DATASET_IDS_TO_REDIRECT_FROM,
        String.join(",", datasetIdsToRedirectFrom));
    parameters.put(PluginParameterKeys.PERFORM_REDIRECTS, String.valueOf(performRedirects));
    return parameters;
  }

}
