package eu.europeana.metis.core.engine.base;

import static eu.europeana.metis.core.engine.base.ProcessingEngineTask.InputDataType.EXTERNAL_REPOSITORY;
import static eu.europeana.metis.core.engine.base.ProcessingEngineTask.InputDataType.INTERNAL_DATASET;
import static eu.europeana.metis.core.engine.base.ProcessingEngineTaskKeys.DATASET_IDS_TO_REDIRECT_FROM;
import static eu.europeana.metis.core.engine.base.ProcessingEngineTaskKeys.DEPUBLICATION_REASON;
import static eu.europeana.metis.core.engine.base.ProcessingEngineTaskKeys.GENERATE_STATS;
import static eu.europeana.metis.core.engine.base.ProcessingEngineTaskKeys.HARVEST_DATE;
import static eu.europeana.metis.core.engine.base.ProcessingEngineTaskKeys.INCREMENTAL_HARVEST;
import static eu.europeana.metis.core.engine.base.ProcessingEngineTaskKeys.INCREMENTAL_INDEXING;
import static eu.europeana.metis.core.engine.base.ProcessingEngineTaskKeys.MAXIMUM_PARALLELIZATION;
import static eu.europeana.metis.core.engine.base.ProcessingEngineTaskKeys.METIS_DATASET_COUNTRY;
import static eu.europeana.metis.core.engine.base.ProcessingEngineTaskKeys.METIS_DATASET_ID;
import static eu.europeana.metis.core.engine.base.ProcessingEngineTaskKeys.METIS_DATASET_LANGUAGE;
import static eu.europeana.metis.core.engine.base.ProcessingEngineTaskKeys.METIS_DATASET_NAME;
import static eu.europeana.metis.core.engine.base.ProcessingEngineTaskKeys.NEW_REPRESENTATION_NAME;
import static eu.europeana.metis.core.engine.base.ProcessingEngineTaskKeys.OUTPUT_DATA_SETS;
import static eu.europeana.metis.core.engine.base.ProcessingEngineTaskKeys.PERFORM_REDIRECTS;
import static eu.europeana.metis.core.engine.base.ProcessingEngineTaskKeys.PRESERVE_TIMESTAMPS;
import static eu.europeana.metis.core.engine.base.ProcessingEngineTaskKeys.PREVIOUS_TASK_ID;
import static eu.europeana.metis.core.engine.base.ProcessingEngineTaskKeys.PROVIDER_ID;
import static eu.europeana.metis.core.engine.base.ProcessingEngineTaskKeys.RECORD_DATE;
import static eu.europeana.metis.core.engine.base.ProcessingEngineTaskKeys.RECORD_IDS_TO_DEPUBLISH;
import static eu.europeana.metis.core.engine.base.ProcessingEngineTaskKeys.REPRESENTATION_NAME;
import static eu.europeana.metis.core.engine.base.ProcessingEngineTaskKeys.REVISION_NAME;
import static eu.europeana.metis.core.engine.base.ProcessingEngineTaskKeys.REVISION_PROVIDER;
import static eu.europeana.metis.core.engine.base.ProcessingEngineTaskKeys.REVISION_TIMESTAMP;
import static eu.europeana.metis.core.engine.base.ProcessingEngineTaskKeys.ROOT_LOCATION;
import static eu.europeana.metis.core.engine.base.ProcessingEngineTaskKeys.SAMPLE_SIZE;
import static eu.europeana.metis.core.engine.base.ProcessingEngineTaskKeys.SCHEMATRON_LOCATION;
import static eu.europeana.metis.core.engine.base.ProcessingEngineTaskKeys.SCHEMA_NAME;
import static eu.europeana.metis.core.engine.base.ProcessingEngineTaskKeys.TARGET_INDEXING_DATABASE;
import static eu.europeana.metis.core.engine.base.ProcessingEngineTaskKeys.XSLT_URL;

import eu.europeana.metis.core.common.RecordIdUtils;
import eu.europeana.metis.core.workflow.plugins.MetisPlugin;
import eu.europeana.metis.core.workflow.plugins.PluginType;
import eu.europeana.metis.utils.CommonStringValues;
import eu.europeana.metis.utils.RestEndpoints;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import org.springframework.util.CollectionUtils;

public class ProcessingEngineTaskConfigurator {

  public static <S extends ProcessingEngineTaskSettings, T extends ProcessingEngineTask>
  Map<ProcessingEngineTaskKeys, String> createDefaultTaskParameters(
      String externalDatasetId,
      String previousTaskId,
      String revisionNamePreviousPlugin,
      Date revisionTimestampPreviousPlugin,
      ProcessingEngineTaskClient<S, T> processingEngineTaskClient) {
    S processingEngineTaskSettings = processingEngineTaskClient.getProcessingEngineTaskSettings();
    final Map<ProcessingEngineTaskKeys, String> parameters = new EnumMap<>(ProcessingEngineTaskKeys.class);
    parameters.put(REPRESENTATION_NAME, MetisPlugin.getRepresentationName());
    parameters.put(REVISION_NAME, revisionNamePreviousPlugin);
    parameters.put(REVISION_PROVIDER, processingEngineTaskSettings.provider());
    DateFormat dateFormat = new SimpleDateFormat(CommonStringValues.DATE_FORMAT_Z, Locale.US);
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    parameters.put(REVISION_TIMESTAMP, dateFormat.format(revisionTimestampPreviousPlugin));
    parameters.put(PREVIOUS_TASK_ID, previousTaskId);
    parameters.put(NEW_REPRESENTATION_NAME, MetisPlugin.getRepresentationName());
    parameters.put(OUTPUT_DATA_SETS, String
        .format(CommonStringValues.S_DATA_PROVIDERS_S_DATA_SETS_S_TEMPLATE, processingEngineTaskSettings.baseUrl(),
            processingEngineTaskSettings.provider(), externalDatasetId));
    return parameters;
  }

  public static <S extends ProcessingEngineTaskSettings, T extends ProcessingEngineTask>
  Map<ProcessingEngineTaskKeys, String> createDefaultTaskParametersHarvest(
      String datasetId, boolean incrementalHarvest, Date startedDate,
      ProcessingEngineTaskClient<S, T> processingEngineTaskClient) {
    S processingEngineTaskSettings = processingEngineTaskClient.getProcessingEngineTaskSettings();
    final Map<ProcessingEngineTaskKeys, String> parameters = new EnumMap<>(ProcessingEngineTaskKeys.class);
    parameters.put(METIS_DATASET_ID, datasetId);
    final DateFormat dateFormat = new SimpleDateFormat(CommonStringValues.DATE_FORMAT_Z, Locale.US);
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    parameters.put(INCREMENTAL_HARVEST, String.valueOf(incrementalHarvest));
    parameters.put(HARVEST_DATE, dateFormat.format(startedDate));
    parameters.put(PROVIDER_ID, processingEngineTaskSettings.provider());
    parameters.put(OUTPUT_DATA_SETS, String
        .format(CommonStringValues.S_DATA_PROVIDERS_S_DATA_SETS_S_TEMPLATE, processingEngineTaskSettings.baseUrl(),
            processingEngineTaskSettings.provider(), datasetId));
    parameters.put(NEW_REPRESENTATION_NAME, MetisPlugin.getRepresentationName());
    return parameters;
  }

  public static <S extends ProcessingEngineTaskSettings, T extends ProcessingEngineTask>
  T createProcessingEngineTaskForHarvest(
      String targetUrl,
      PluginType pluginType,
      Date pluginStartedDate,
      Map<ProcessingEngineTaskKeys, String> parameters,
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

  public static <S extends ProcessingEngineTaskSettings, T extends ProcessingEngineTask>
  T createProcessingEngineTask(
      String externalDatasetId,
      PluginType pluginType,
      Date pluginStartedDate,
      Map<ProcessingEngineTaskKeys, String> parameters,
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
  T createProcessingEngineTaskForDepublish(
      Map<ProcessingEngineTaskKeys, String> parameters,
      ProcessingEngineTaskClient<S, T> processingEngineTaskClient) {
    T externalTask = processingEngineTaskClient.getTaskCreator().get();
    externalTask.setParameters(parameters);
    return externalTask;
  }

  private static DataRevision createDataRevisionOutput(PluginType pluginType, Date pluginStartedDate, String ecloudProvider) {
    return new DataRevision(pluginType.name(), ecloudProvider, pluginStartedDate, false);
  }

  public static Map<ProcessingEngineTaskKeys, String> createParametersForValidationExternal(String urlOfSchemasZip,
      String schemaRootPath,
      String schematronRootPath) {
    final Map<ProcessingEngineTaskKeys, String> parameters = createParametersForValidation(urlOfSchemasZip, schemaRootPath,
        schematronRootPath);
    parameters.put(GENERATE_STATS, Boolean.TRUE.toString());
    return parameters;
  }

  public static Map<ProcessingEngineTaskKeys, String> createParametersForValidationInternal(String urlOfSchemasZip,
      String schemaRootPath,
      String schematronRootPath) {
    final Map<ProcessingEngineTaskKeys, String> parameters =
        createParametersForValidation(urlOfSchemasZip, schemaRootPath, schematronRootPath);
    parameters.put(GENERATE_STATS, Boolean.FALSE.toString());
    return parameters;
  }

  private static Map<ProcessingEngineTaskKeys, String> createParametersForValidation(String urlOfSchemasZip,
      String schemaRootPath,
      String schematronRootPath) {
    final Map<ProcessingEngineTaskKeys, String> parameters = new EnumMap<>(ProcessingEngineTaskKeys.class);
    parameters.put(SCHEMA_NAME, urlOfSchemasZip);
    parameters.put(ROOT_LOCATION, schemaRootPath);
    parameters.put(SCHEMATRON_LOCATION, schematronRootPath);
    return parameters;
  }

  public static Map<ProcessingEngineTaskKeys, String> createParametersForTransformation(String metisCoreBaseUrl, String xsltId,
      String datasetId,
      String datasetName, String country, String language) {
    Map<ProcessingEngineTaskKeys, String> parameters = new HashMap<>();
    parameters.put(XSLT_URL,
        metisCoreBaseUrl + RestEndpoints
            .resolve(RestEndpoints.DATASETS_XSLT_XSLTID,
                Collections.singletonList(xsltId)));
    parameters.put(METIS_DATASET_ID, datasetId);
    parameters.put(METIS_DATASET_NAME, datasetName);
    parameters.put(METIS_DATASET_COUNTRY, country);
    parameters.put(METIS_DATASET_LANGUAGE, language);
    return parameters;
  }

  public static Map<ProcessingEngineTaskKeys, String> createParametersForMedia(String maximumParallelization) {
    Map<ProcessingEngineTaskKeys, String> parameters = new HashMap<>();
    parameters.put(MAXIMUM_PARALLELIZATION, maximumParallelization);
    return parameters;
  }

  public static Map<ProcessingEngineTaskKeys, String> createParametersForLinkChecking(boolean performSampling,
      Integer sampleSize) {
    final Map<ProcessingEngineTaskKeys, String> parameters = new EnumMap<>(ProcessingEngineTaskKeys.class);
    if (performSampling && sampleSize != null) {
      parameters.put(SAMPLE_SIZE, sampleSize.toString());
    }
    return parameters;
  }

  public static Map<ProcessingEngineTaskKeys, String> createParametersIndex(
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
    final Map<ProcessingEngineTaskKeys, String> parameters = new EnumMap<>(ProcessingEngineTaskKeys.class);
    parameters.put(METIS_DATASET_ID, datasetId);
    parameters.put(INCREMENTAL_INDEXING, String.valueOf(incrementalIndexing));
    parameters.put(HARVEST_DATE, dateFormat.format(harvestDate));
    parameters.put(TARGET_INDEXING_DATABASE, targetIndexingDatabase);
    parameters.put(RECORD_DATE, dateFormat.format(pluginStartedDate));
    parameters.put(PRESERVE_TIMESTAMPS, String.valueOf(preserveTimestamps));
    parameters.put(DATASET_IDS_TO_REDIRECT_FROM, String.join(",", datasetIdsToRedirectFrom));
    parameters.put(PERFORM_REDIRECTS, String.valueOf(performRedirects));
    return parameters;
  }

  public static Map<ProcessingEngineTaskKeys, String> createParametersDepublish(
      String datasetId,
      boolean datasetDepublish,
      Set<String> recordIdsToDepublish,
      String depublicationReason) {
    final Map<ProcessingEngineTaskKeys, String> parameters = new EnumMap<>(ProcessingEngineTaskKeys.class);
    parameters.put(METIS_DATASET_ID, datasetId);

    //Do set the records ids parameter only if record ids depublication enabled and there are record ids
    if (!datasetDepublish) {
      if (CollectionUtils.isEmpty(recordIdsToDepublish)) {
        throw new IllegalStateException(
            "Requested record depublication but there are no records ids for depublication in the db");
      } else {
        final String recordIdList = String.join(",", RecordIdUtils
            .composeFullRecordIds(datasetId, recordIdsToDepublish));
        parameters.put(RECORD_IDS_TO_DEPUBLISH, recordIdList);
      }
    }
    parameters.put(DEPUBLICATION_REASON, depublicationReason);
    return parameters;
  }

}
