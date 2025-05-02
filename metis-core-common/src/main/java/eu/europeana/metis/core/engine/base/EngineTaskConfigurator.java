package eu.europeana.metis.core.engine.base;

import static eu.europeana.metis.core.engine.base.EngineTask.InputDataType.EXTERNAL_REPOSITORY;
import static eu.europeana.metis.core.engine.base.EngineTask.InputDataType.INTERNAL_DATASET;
import static eu.europeana.metis.core.engine.base.EngineTaskKey.DATASET_IDS_TO_REDIRECT_FROM;
import static eu.europeana.metis.core.engine.base.EngineTaskKey.DEPUBLICATION_REASON;
import static eu.europeana.metis.core.engine.base.EngineTaskKey.GENERATE_STATS;
import static eu.europeana.metis.core.engine.base.EngineTaskKey.HARVEST_DATE;
import static eu.europeana.metis.core.engine.base.EngineTaskKey.INCREMENTAL_HARVEST;
import static eu.europeana.metis.core.engine.base.EngineTaskKey.INCREMENTAL_INDEXING;
import static eu.europeana.metis.core.engine.base.EngineTaskKey.MAXIMUM_PARALLELIZATION;
import static eu.europeana.metis.core.engine.base.EngineTaskKey.METIS_DATASET_COUNTRY;
import static eu.europeana.metis.core.engine.base.EngineTaskKey.METIS_DATASET_ID;
import static eu.europeana.metis.core.engine.base.EngineTaskKey.METIS_DATASET_LANGUAGE;
import static eu.europeana.metis.core.engine.base.EngineTaskKey.METIS_DATASET_NAME;
import static eu.europeana.metis.core.engine.base.EngineTaskKey.NEW_REPRESENTATION_NAME;
import static eu.europeana.metis.core.engine.base.EngineTaskKey.OUTPUT_DATA_SETS;
import static eu.europeana.metis.core.engine.base.EngineTaskKey.PERFORM_REDIRECTS;
import static eu.europeana.metis.core.engine.base.EngineTaskKey.PRESERVE_TIMESTAMPS;
import static eu.europeana.metis.core.engine.base.EngineTaskKey.PREVIOUS_TASK_ID;
import static eu.europeana.metis.core.engine.base.EngineTaskKey.PROVIDER_ID;
import static eu.europeana.metis.core.engine.base.EngineTaskKey.RECORD_DATE;
import static eu.europeana.metis.core.engine.base.EngineTaskKey.RECORD_IDS_TO_DEPUBLISH;
import static eu.europeana.metis.core.engine.base.EngineTaskKey.REPRESENTATION_NAME;
import static eu.europeana.metis.core.engine.base.EngineTaskKey.REVISION_NAME;
import static eu.europeana.metis.core.engine.base.EngineTaskKey.REVISION_PROVIDER;
import static eu.europeana.metis.core.engine.base.EngineTaskKey.REVISION_TIMESTAMP;
import static eu.europeana.metis.core.engine.base.EngineTaskKey.ROOT_LOCATION;
import static eu.europeana.metis.core.engine.base.EngineTaskKey.SAMPLE_SIZE;
import static eu.europeana.metis.core.engine.base.EngineTaskKey.SCHEMATRON_LOCATION;
import static eu.europeana.metis.core.engine.base.EngineTaskKey.SCHEMA_NAME;
import static eu.europeana.metis.core.engine.base.EngineTaskKey.TARGET_INDEXING_DATABASE;
import static eu.europeana.metis.core.engine.base.EngineTaskKey.XSLT_URL;

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

public class EngineTaskConfigurator {

  public static <S extends EngineTaskSettings, T extends EngineTask>
  Map<EngineTaskKey, String> createDefaultTaskParameters(
      String externalDatasetId,
      String previousTaskId,
      String revisionNamePreviousPlugin,
      Date revisionTimestampPreviousPlugin,
      EngineTaskClient<S, T> engineTaskClient) {
    S engineTaskSettings = engineTaskClient.getEngineTaskSettings();
    final Map<EngineTaskKey, String> parameters = new EnumMap<>(EngineTaskKey.class);
    parameters.put(REPRESENTATION_NAME, MetisPlugin.getRepresentationName());
    parameters.put(REVISION_NAME, revisionNamePreviousPlugin);
    parameters.put(REVISION_PROVIDER, engineTaskSettings.provider());
    parameters.put(REVISION_TIMESTAMP, formatUtcDate(revisionTimestampPreviousPlugin));
    parameters.put(PREVIOUS_TASK_ID, previousTaskId);
    parameters.put(NEW_REPRESENTATION_NAME, MetisPlugin.getRepresentationName());
    parameters.put(OUTPUT_DATA_SETS, String
        .format(CommonStringValues.S_DATA_PROVIDERS_S_DATA_SETS_S_TEMPLATE, engineTaskSettings.baseUrl(),
            engineTaskSettings.provider(), externalDatasetId));
    return parameters;
  }

  public static <S extends EngineTaskSettings, T extends EngineTask>
  Map<EngineTaskKey, String> createDefaultTaskParametersHarvest(
      String datasetId, boolean incrementalHarvest, Date startedDate,
      EngineTaskClient<S, T> engineTaskClient) {
    S engineTaskSettings = engineTaskClient.getEngineTaskSettings();
    final Map<EngineTaskKey, String> parameters = new EnumMap<>(EngineTaskKey.class);
    parameters.put(METIS_DATASET_ID, datasetId);
    parameters.put(INCREMENTAL_HARVEST, String.valueOf(incrementalHarvest));
    parameters.put(HARVEST_DATE, formatUtcDate(startedDate));
    parameters.put(PROVIDER_ID, engineTaskSettings.provider());
    parameters.put(OUTPUT_DATA_SETS, String
        .format(CommonStringValues.S_DATA_PROVIDERS_S_DATA_SETS_S_TEMPLATE, engineTaskSettings.baseUrl(),
            engineTaskSettings.provider(), datasetId));
    parameters.put(NEW_REPRESENTATION_NAME, MetisPlugin.getRepresentationName());
    return parameters;
  }

  public static <S extends EngineTaskSettings, T extends EngineTask>
  T createHarvestEngineTask(
      String targetUrl,
      PluginType pluginType,
      Date pluginStartedDate,
      Map<EngineTaskKey, String> parameters,
      EngineTaskClient<S, T> engineTaskClient,
      OaiHarvestParameters oaiHarvestParameters) {
    S engineTaskSettings = engineTaskClient.getEngineTaskSettings();
    T engineTask = engineTaskClient.getEngineTaskCreator().get();
    engineTask.setInputDataLocation(EXTERNAL_REPOSITORY, targetUrl);
    engineTask.setParameters(parameters);
    engineTask.setOutputRevision(
        createDataRevisionOutput(pluginType, pluginStartedDate, engineTaskSettings.provider()));
    if (oaiHarvestParameters != null) {
      engineTask.setOaiHarvestParameters(oaiHarvestParameters);
    }
    return engineTask;
  }

  public static <S extends EngineTaskSettings, T extends EngineTask>
  T createEngineTask(
      String externalDatasetId,
      PluginType pluginType,
      Date pluginStartedDate,
      Map<EngineTaskKey, String> parameters,
      EngineTaskClient<S, T> engineTaskClient) {
    S engineTaskSettings = engineTaskClient.getEngineTaskSettings();
    T engineTask = engineTaskClient.getEngineTaskCreator().get();
    final String inputDataLocation =
        String.format(CommonStringValues.S_DATA_PROVIDERS_S_DATA_SETS_S_TEMPLATE, engineTaskSettings.baseUrl(),
            engineTaskSettings.provider(), externalDatasetId);

    engineTask.setInputDataLocation(INTERNAL_DATASET, inputDataLocation);
    engineTask.setParameters(parameters);
    engineTask.setOutputRevision(
        createDataRevisionOutput(pluginType, pluginStartedDate, engineTaskSettings.provider()));
    return engineTask;
  }

  public static <S extends EngineTaskSettings, T extends EngineTask>
  T createDepublishEngineTask(
      Map<EngineTaskKey, String> parameters,
      EngineTaskClient<S, T> engineTaskClient) {
    T engineTask = engineTaskClient.getEngineTaskCreator().get();
    engineTask.setParameters(parameters);
    return engineTask;
  }

  private static DataRevision createDataRevisionOutput(PluginType pluginType, Date pluginStartedDate, String ecloudProvider) {
    return new DataRevision(pluginType.name(), ecloudProvider, pluginStartedDate, false);
  }

  public static Map<EngineTaskKey, String> createValidationExternalParameters(String urlOfSchemasZip,
      String schemaRootPath,
      String schematronRootPath) {
    final Map<EngineTaskKey, String> parameters = createValidationParameters(urlOfSchemasZip, schemaRootPath,
        schematronRootPath);
    parameters.put(GENERATE_STATS, Boolean.TRUE.toString());
    return parameters;
  }

  public static Map<EngineTaskKey, String> createValidationInternalParameters(String urlOfSchemasZip,
      String schemaRootPath,
      String schematronRootPath) {
    final Map<EngineTaskKey, String> parameters =
        createValidationParameters(urlOfSchemasZip, schemaRootPath, schematronRootPath);
    parameters.put(GENERATE_STATS, Boolean.FALSE.toString());
    return parameters;
  }

  private static Map<EngineTaskKey, String> createValidationParameters(String urlOfSchemasZip,
      String schemaRootPath,
      String schematronRootPath) {
    final Map<EngineTaskKey, String> parameters = new EnumMap<>(EngineTaskKey.class);
    parameters.put(SCHEMA_NAME, urlOfSchemasZip);
    parameters.put(ROOT_LOCATION, schemaRootPath);
    parameters.put(SCHEMATRON_LOCATION, schematronRootPath);
    return parameters;
  }

  public static Map<EngineTaskKey, String> createTransformationParameters(String metisCoreBaseUrl, String xsltId,
      String datasetId,
      String datasetName, String country, String language) {
    Map<EngineTaskKey, String> parameters = new HashMap<>();
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

  public static Map<EngineTaskKey, String> createMediaParameters(String maximumParallelization) {
    Map<EngineTaskKey, String> parameters = new HashMap<>();
    parameters.put(MAXIMUM_PARALLELIZATION, maximumParallelization);
    return parameters;
  }

  public static Map<EngineTaskKey, String> createLinkCheckingParameters(boolean performSampling,
      Integer sampleSize) {
    final Map<EngineTaskKey, String> parameters = new EnumMap<>(EngineTaskKey.class);
    if (performSampling && sampleSize != null) {
      parameters.put(SAMPLE_SIZE, sampleSize.toString());
    }
    return parameters;
  }

  public static Map<EngineTaskKey, String> createIndexParameters(
      String datasetId,
      Date pluginStartedDate,
      boolean incrementalIndexing,
      Date harvestDate,
      boolean preserveTimestamps,
      List<String> datasetIdsToRedirectFrom,
      boolean performRedirects,
      String targetIndexingDatabase) {
    final Map<EngineTaskKey, String> parameters = new EnumMap<>(EngineTaskKey.class);
    parameters.put(METIS_DATASET_ID, datasetId);
    parameters.put(INCREMENTAL_INDEXING, String.valueOf(incrementalIndexing));
    parameters.put(HARVEST_DATE, formatUtcDate(harvestDate));
    parameters.put(TARGET_INDEXING_DATABASE, targetIndexingDatabase);
    parameters.put(RECORD_DATE, formatUtcDate(pluginStartedDate));
    parameters.put(PRESERVE_TIMESTAMPS, String.valueOf(preserveTimestamps));
    parameters.put(DATASET_IDS_TO_REDIRECT_FROM, String.join(",", datasetIdsToRedirectFrom));
    parameters.put(PERFORM_REDIRECTS, String.valueOf(performRedirects));
    return parameters;
  }

  public static Map<EngineTaskKey, String> createDepublishParameters(
      String datasetId,
      boolean datasetDepublish,
      Set<String> recordIdsToDepublish,
      String depublicationReason) {
    final Map<EngineTaskKey, String> parameters = new EnumMap<>(EngineTaskKey.class);
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

  private static String formatUtcDate(Date date) {
    DateFormat dateFormat = new SimpleDateFormat(CommonStringValues.DATE_FORMAT_Z, Locale.US);
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    return dateFormat.format(date);
  }
}
