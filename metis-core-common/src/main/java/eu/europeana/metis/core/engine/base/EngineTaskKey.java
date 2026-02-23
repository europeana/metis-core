package eu.europeana.metis.core.engine.base;

/**
 * Enum representing various keys used to configure and manage tasks within the processing engine.
 */
public enum EngineTaskKey {

  //Sandbox
  STEP_SIZE,
  ENGINE_DATASET_ID,
  JOB_NAME,

  //Ecloud
  /**
   * URL of the XSLT transformation content
   */
  XSLT_URL,
  /**
   * ID of the previous task in the workflow
   */
  PREVIOUS_TASK_ID,
  /**
   * Unique identifier of the External dataset id.
   */
  METIS_DATASET_ID,
  /**
   * Name of the Metis dataset
   */
  METIS_DATASET_NAME,
  /**
   * Country value of the dataset's origin
   */
  METIS_DATASET_COUNTRY,
  /**
   * Language value of the dataset
   */
  METIS_DATASET_LANGUAGE,
  /**
   * Target database for indexing operations
   */
  TARGET_INDEXING_DATABASE,
  /**
   * Date associated with the record
   */
  RECORD_DATE,
  /**
   * Flag indicating whether to preserve original timestamps
   */
  PRESERVE_TIMESTAMPS,
  /**
   * List of dataset IDs from which to redirect
   */
  DATASET_IDS_TO_REDIRECT_FROM,
  /**
   * Flag indicating whether to perform redirects
   */
  PERFORM_REDIRECTS,
  /**
   * List of record IDs to be depublished
   */
  RECORD_IDS_TO_DEPUBLISH,
  /**
   * Reason for depublication of records
   */
  DEPUBLICATION_REASON,
  /**
   * ID of the data provider. This is ECloud specific.
   */
  PROVIDER_ID,
  /**
   * Name of the current representation. This is ECloud specific.
   */
  REPRESENTATION_NAME,
  /**
   * New name for the representation. This is ECloud specific.
   */
  NEW_REPRESENTATION_NAME,
  /**
   * Output datasets configuration. This is ECloud specific.
   */
  OUTPUT_DATA_SETS,
  /**
   * Size of the sample to be processed
   */
  SAMPLE_SIZE,
  /**
   * Name of the schema to be used
   */
  SCHEMA_NAME,
  /**
   * Location of the schema files
   */
  ROOT_LOCATION,
  /**
   * Location of the Schematron files
   */
  SCHEMATRON_LOCATION,
  /**
   * Flag indicating whether to generate statistics for the processed data
   */
  GENERATE_STATS,
  /**
   * Name of the revision
   */
  REVISION_NAME,
  /**
   * Provider of the revision. This is ECloud specific.
   */
  REVISION_PROVIDER,
  /**
   * Timestamp of the revision
   */
  REVISION_TIMESTAMP,
  /**
   * Flag indicating incremental harvest mode
   */
  INCREMENTAL_HARVEST,
  /**
   * Flag indicating incremental indexing mode
   */
  INCREMENTAL_INDEXING,
  /**
   * Date of the harvest operation.
   * <p>
   * Used in Harvesting to supply the harvest date. Also used in indexing to indicate the Harvested date.
   */
  HARVEST_DATE,
  /**
   * Maximum number of parallel operations allowed
   */
  MAXIMUM_PARALLELIZATION;
}
