package eu.europeana.metis.core.engine.base;

import static eu.europeana.metis.core.engine.base.EngineTaskKey.DATASET_IDS_TO_REDIRECT_FROM;
import static eu.europeana.metis.core.engine.base.EngineTaskKey.DEPUBLICATION_REASON;
import static eu.europeana.metis.core.engine.base.EngineTaskKey.ENGINE_DATASET_ID;
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
import static lombok.AccessLevel.PRIVATE;

import eu.europeana.metis.core.common.RecordIdUtils;
import eu.europeana.metis.core.workflow.plugins.MetisPlugin;
import eu.europeana.metis.core.workflow.plugins.PluginType;
import eu.europeana.metis.utils.CommonStringValues;
import eu.europeana.metis.utils.RestEndpoints;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.NoArgsConstructor;
import org.springframework.util.CollectionUtils;

/**
 * Configures task parameters for the engine tasks in various contexts, such as harvesting, validation, transformation, and
 * indexing.
 */
@NoArgsConstructor(access = PRIVATE)
public final class EngineTaskParametersConfigurator {

  private static final DateTimeFormatter UTC_DATE_FORMAT =
      DateTimeFormatter.ofPattern(CommonStringValues.DATE_FORMAT_Z, Locale.ROOT)
                       .withZone(ZoneOffset.UTC);

  /**
   * Creates a default set of task parameters used for configuring an engine task.
   *
   * @param engineDatasetId the identifier of the engine dataset
   * @param datasetId the identifier of the dataset
   * @param previousTaskId the identifier of the previous task
   * @param inputDataRevision the revision of input data
   * @param dataLocation the location of the output data sets
   * @return a map of {@link EngineTaskKey} keys to their corresponding parameter values
   */
  public static Map<EngineTaskKey, String> createDefaultTaskParameters(
      String engineDatasetId, String datasetId, String previousTaskId, DataRevision inputDataRevision, String dataLocation) {
    final Map<EngineTaskKey, String> parameters = new EnumMap<>(EngineTaskKey.class);
    parameters.put(ENGINE_DATASET_ID, engineDatasetId);
    parameters.put(METIS_DATASET_ID, datasetId);
    parameters.put(REPRESENTATION_NAME, MetisPlugin.getRepresentationName());
    parameters.put(REVISION_NAME, inputDataRevision.name());
    parameters.put(REVISION_PROVIDER, inputDataRevision.providerId());
    parameters.put(REVISION_TIMESTAMP, formatUtcDate(inputDataRevision.creationTimeStamp()));
    parameters.put(PREVIOUS_TASK_ID, previousTaskId);
    parameters.put(NEW_REPRESENTATION_NAME, MetisPlugin.getRepresentationName());
    parameters.put(OUTPUT_DATA_SETS, dataLocation);
    return parameters;
  }

  /**
   * Creates a map of default task parameters for a harvest operation.
   *
   * @param engineDatasetId the identifier of the engine dataset
   * @param datasetId the identifier of the dataset to be harvested
   * @param incrementalHarvest a flag indicating if the harvest should be incremental
   * @param startedDate the starting date of the harvest operation
   * @param dataLocation the location of the output data sets
   * @param providerId the identifier of the data provider
   * @return a map of {@link EngineTaskKey} keys to their corresponding parameter values
   */
  public static Map<EngineTaskKey, String> createDefaultTaskParametersHarvest(
      String engineDatasetId, String datasetId, boolean incrementalHarvest, Instant startedDate, String dataLocation,
      String providerId) {
    final Map<EngineTaskKey, String> parameters = new EnumMap<>(EngineTaskKey.class);
    parameters.put(ENGINE_DATASET_ID, engineDatasetId);
    parameters.put(METIS_DATASET_ID, datasetId);
    parameters.put(INCREMENTAL_HARVEST, String.valueOf(incrementalHarvest));
    parameters.put(HARVEST_DATE, formatUtcDate(startedDate));
    parameters.put(PROVIDER_ID, providerId);
    parameters.put(OUTPUT_DATA_SETS, dataLocation);
    parameters.put(NEW_REPRESENTATION_NAME, MetisPlugin.getRepresentationName());
    return parameters;
  }

  /**
   * Creates a new instance of {@link DataRevision}.
   *
   * @param pluginType the type of the plugin initiating the data revision
   * @param pluginStartedDate the start date of the plugin associated with the data revision
   * @param ecloudProvider the identifier of the eCloud provider associated with the revision
   * @return a map of {@link EngineTaskKey} keys to their corresponding parameter values
   */
  public static DataRevision createDataRevision(PluginType pluginType, Instant pluginStartedDate, String ecloudProvider) {
    return new DataRevision(pluginType.name(), ecloudProvider, pluginStartedDate, false);
  }

  /**
   * Creates a map of validation external parameters.
   *
   * @param urlOfSchemasZip the URL pointing to the zip file containing schemas
   * @param schemaRootPath the root path for the extracted schemas
   * @param schematronRootPath the root path for the extracted schematrons
   * @return a map of {@link EngineTaskKey} keys to their corresponding parameter values
   */
  public static Map<EngineTaskKey, String> createValidationExternalParameters(
      String urlOfSchemasZip, String schemaRootPath, String schematronRootPath) {
    final Map<EngineTaskKey, String> parameters = createValidationParameters(urlOfSchemasZip, schemaRootPath,
        schematronRootPath);
    parameters.put(GENERATE_STATS, Boolean.TRUE.toString());
    return parameters;
  }

  /**
   * Creates a map of validation internal parameters.
   *
   * @param urlOfSchemasZip the URL pointing to the zip file containing schema definitions
   * @param schemaRootPath the root path where schema files are located
   * @param schematronRootPath the root path where schematron files are located
   * @return a map of {@link EngineTaskKey} keys to their corresponding parameter values
   */
  public static Map<EngineTaskKey, String> createValidationInternalParameters(
      String urlOfSchemasZip, String schemaRootPath, String schematronRootPath) {
    final Map<EngineTaskKey, String> parameters =
        createValidationParameters(urlOfSchemasZip, schemaRootPath, schematronRootPath);
    parameters.put(GENERATE_STATS, Boolean.FALSE.toString());
    return parameters;
  }

  /**
   * Creates a map of validation parameters required for the engine tasks.
   *
   * @param urlOfSchemasZip the URL pointing to the ZIP file containing schemas
   * @param schemaRootPath the root path for schema validation
   * @param schematronRootPath the root path for schematron validation
   * @return a map of {@link EngineTaskKey} keys to their corresponding parameter values
   */
  private static Map<EngineTaskKey, String> createValidationParameters(
      String urlOfSchemasZip, String schemaRootPath, String schematronRootPath) {
    final Map<EngineTaskKey, String> parameters = new EnumMap<>(EngineTaskKey.class);
    parameters.put(SCHEMA_NAME, urlOfSchemasZip);
    parameters.put(ROOT_LOCATION, schemaRootPath);
    parameters.put(SCHEMATRON_LOCATION, schematronRootPath);
    return parameters;
  }

  /**
   * Creates a map of transformation external parameters.
   *
   * @param metisCoreBaseUrl the base URL for the Metis core API
   * @param xsltId the identifier of the XSLT transformation
   * @return a map of {@link EngineTaskKey} keys to their corresponding parameter values
   */
  public static Map<EngineTaskKey, String> createTransformationExternalParameters(
      String metisCoreBaseUrl, String xsltId) {
    Map<EngineTaskKey, String> parameters = new EnumMap<>(EngineTaskKey.class);
    parameters.put(XSLT_URL,
        metisCoreBaseUrl + RestEndpoints.resolve(RestEndpoints.DATASETS_XSLT_XSLTID, Collections.singletonList(xsltId)));
    return parameters;
  }

  /**
   * Creates a map of transformation parameters.
   *
   * @param metisCoreBaseUrl the base URL for the Metis core API
   * @param xsltId the identifier of the XSLT transformation
   * @param datasetName the name of the dataset
   * @param country the country associated with the dataset
   * @param language the language associated with the dataset
   * @return a map of {@link EngineTaskKey} keys to their corresponding parameter values
   */
  public static Map<EngineTaskKey, String> createTransformationInternalParameters(
      String metisCoreBaseUrl, String xsltId, String datasetName, String country, String language) {
    Map<EngineTaskKey, String> parameters = new EnumMap<>(EngineTaskKey.class);
    parameters.put(XSLT_URL,
        metisCoreBaseUrl + RestEndpoints.resolve(RestEndpoints.DATASETS_XSLT_XSLTID, Collections.singletonList(xsltId)));
    parameters.put(METIS_DATASET_NAME, datasetName);
    parameters.put(METIS_DATASET_COUNTRY, country);
    parameters.put(METIS_DATASET_LANGUAGE, language);
    return parameters;
  }

  /**
   * Creates a map of media parameters.
   *
   * @param maximumParallelization the maximum level of parallelization to include in the parameters
   * @return a map of {@link EngineTaskKey} keys to their corresponding parameter values
   */
  public static Map<EngineTaskKey, String> createMediaParameters(String maximumParallelization) {
    Map<EngineTaskKey, String> parameters = new EnumMap<>(EngineTaskKey.class);
    parameters.put(MAXIMUM_PARALLELIZATION, maximumParallelization);
    return parameters;
  }

  /**
   * Creates a map containing parameters for link-checking tasks.
   *
   * @param performSampling a boolean indicating whether sampling should be performed
   * @param sampleSize the number of samples to include if sampling is enabled
   * @return a map of {@link EngineTaskKey} keys to their corresponding parameter values
   */
  public static Map<EngineTaskKey, String> createLinkCheckingParameters(boolean performSampling, Integer sampleSize) {
    final Map<EngineTaskKey, String> parameters = new EnumMap<>(EngineTaskKey.class);
    if (performSampling && sampleSize != null) {
      parameters.put(SAMPLE_SIZE, sampleSize.toString());
    }
    return parameters;
  }

  /**
   * Creates a map containing parameters for indexing tasks.
   *
   * @param pluginStartedDate the start date of the plugin process
   * @param incrementalIndexing a flag indicating if indexing should be incremental
   * @param harvestDate the date when the data was harvested
   * @param preserveTimestamps a flag indicating if original timestamps should be preserved
   * @param datasetIdsToRedirectFrom a list of dataset IDs to redirect from
   * @param performRedirects a flag indicating if redirection should be performed
   * @param targetIndexingDatabase the target database for indexing
   * @return a map of {@link EngineTaskKey} keys to their corresponding parameter values
   */
  public static Map<EngineTaskKey, String> createIndexParameters(
      Instant pluginStartedDate,
      boolean incrementalIndexing,
      Instant harvestDate,
      boolean preserveTimestamps,
      List<String> datasetIdsToRedirectFrom,
      boolean performRedirects,
      String targetIndexingDatabase) {
    final Map<EngineTaskKey, String> parameters = new EnumMap<>(EngineTaskKey.class);
    parameters.put(INCREMENTAL_INDEXING, String.valueOf(incrementalIndexing));
    parameters.put(HARVEST_DATE, formatUtcDate(harvestDate));
    parameters.put(TARGET_INDEXING_DATABASE, targetIndexingDatabase);
    parameters.put(RECORD_DATE, formatUtcDate(pluginStartedDate));
    parameters.put(PRESERVE_TIMESTAMPS, String.valueOf(preserveTimestamps));
    parameters.put(DATASET_IDS_TO_REDIRECT_FROM, String.join(",", datasetIdsToRedirectFrom));
    parameters.put(PERFORM_REDIRECTS, String.valueOf(performRedirects));
    return parameters;
  }

  /**
   * Creates a map of parameters for depublishing tasks.
   *
   * @param datasetId The unique identifier of the dataset to be depublished.
   * @param datasetDepublish Flag indicating whether the entire dataset should be depublished.
   * @param recordIdsToDepublish A set of record IDs to be depublished if partial depublishing is required.
   * @param depublicationReason The reason for the depublishing operation.
   * @return a map of {@link EngineTaskKey} keys to their corresponding parameter values
   * @throws IllegalStateException If partial depublishing is requested but no record IDs are provided.
   */
  public static Map<EngineTaskKey, String> createDepublishParameters(
      String datasetId,
      boolean datasetDepublish,
      Set<String> recordIdsToDepublish,
      String depublicationReason) {
    final Map<EngineTaskKey, String> parameters = new EnumMap<>(EngineTaskKey.class);
    parameters.put(METIS_DATASET_ID, datasetId);
    parameters.put(DEPUBLICATION_REASON, depublicationReason);

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
    return parameters;
  }

  private static String formatUtcDate(Instant instant) {
    return UTC_DATE_FORMAT.format(instant);
  }
}
