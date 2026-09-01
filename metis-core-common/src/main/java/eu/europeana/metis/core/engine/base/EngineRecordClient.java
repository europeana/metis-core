package eu.europeana.metis.core.engine.base;

import eu.europeana.metis.core.dataset.Dataset;
import eu.europeana.metis.core.rest.Record;
import eu.europeana.metis.core.workflow.plugins.ExecutablePluginType;
import eu.europeana.metis.exception.ExternalTaskException;
import java.util.List;

/**
 * Provides methods for retrieving and managing records within the processing engine.
 */
public interface EngineRecordClient {

  /**
   * Retrieves a list of records from a specified dataset in the processing engine.
   *
   * @param engineDatasetId the identifier of the dataset to retrieve records from
   * @param engineBatchId the identifier of the batch to retrieve records from
   * @param numberOfRecords the maximum number of records to retrieve
   * @return a list of {@link java.lang.Record} objects from the specified dataset and batch
   * @throws ExternalTaskException if an error occurs while retrieving the records
   */
  List<Record> getRecords(String engineDatasetId, String engineBatchId, int numberOfRecords) throws ExternalTaskException;

  /**
   * Retrieves a list of records based on the provided record identifiers and batch information.
   *
   * @param recordIds the list of record identifiers to retrieve
   * @return a list of {@link java.lang.Record} objects corresponding to the provided record identifiers
   * @throws ExternalTaskException if an error occurs during the record retrieval process
   */
  List<Record> getRecords(List<String> recordIds, String engineBatchId) throws ExternalTaskException;

  /**
   * Retrieves a record based on the provided dataset id, record id, batch id, and plugin type.
   *
   * @param engineDatasetId the identifier of the engine dataset containing the record
   * @param recordId the unique identifier of the record to retrieve
   * @param engineBatchId the batch identifier associated with the record retrieval
   * @param pluginType the plugin type associated with the record retrieval
   * @return the retrieved Record object matching the provided criteria
   * @throws ExternalTaskException if there is an error while retrieving the record
   */
  Record getRecord(String engineDatasetId, String recordId, String engineBatchId,
      ExecutablePluginType pluginType) throws ExternalTaskException;

  /**
   * Retrieves a list of published record identifiers for a given dataset.
   *
   * @param datasetId The unique identifier of the dataset.
   * @param recordsIds A list of record identifiers to check for published status.
   * @return A list of record identifiers that are marked as published.
   * @throws ExternalTaskException If an error occurs while retrieving the records.
   */
  List<String> getPublishedRecords(String datasetId, List<String> recordsIds) throws ExternalTaskException;

  /**
   * Retrieves the total number of indexed records for a given dataset.
   *
   * @param datasetId the unique identifier of the dataset
   * @param indexDatabase the index database where records are stored
   * @return the total count of indexed records
   * @throws ExternalTaskException if an error occurs during the retrieval process
   */
  long getTotalIndexedRecords(String datasetId, IndexDatabase indexDatabase) throws ExternalTaskException;

  /**
   * Generates a unique engine dataset identifier for the given dataset to be used in the processing engine.
   * <p>
   * NOTE: This is now NOT required for ecloud, but it is STILL required for metis-sandbox.
   *
   * @param dataset the dataset object for which the engine dataset identifier is to be generated
   * @return the generated engine dataset identifier
   * @throws ExternalTaskException if an error occurs during the identifier generation process
   */
  String createEngineDatasetId(Dataset dataset) throws ExternalTaskException;

}
