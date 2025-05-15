package eu.europeana.metis.core.engine.base;

import eu.europeana.metis.core.rest.Record;
import eu.europeana.metis.exception.ExternalTaskException;
import java.util.Date;
import java.util.List;

/**
 * Provides methods for retrieving and managing records within the processing engine.
 */
public interface EngineRecordClient {

  /**
   * Retrieves a list of records from a specified dataset in the processing engine.
   *
   * @param datasetId the identifier of the dataset to retrieve records from
   * @param representationName the name of the representation to retrieve records for
   * @param revisionName the name of the revision to retrieve records for
   * @param revisionTimestamp the timestamp of the revision to retrieve records for
   * @param numberOfRecords the maximum number of records to retrieve
   * @return a list of {@link java.lang.Record} objects from the specified dataset and revision
   * @throws ExternalTaskException if an error occurs while retrieving the records
   */
  List<Record> getRecords(final String datasetId, String representationName, String revisionName, Date revisionTimestamp,
      int numberOfRecords) throws ExternalTaskException;

  /**
   * Retrieves a list of records based on the provided record identifiers and revision information.
   *
   * @param recordIds the list of record identifiers to retrieve
   * @param revisionName the name of the revision to fetch the records from
   * @param revisionTimestamp the timestamp of the revision to fetch the records from
   * @return a list of {@link java.lang.Record} objects corresponding to the provided record identifiers
   * @throws ExternalTaskException if an error occurs during the record retrieval process
   */
  List<Record> getRecords(List<String> recordIds, String revisionName, Date revisionTimestamp) throws ExternalTaskException;

  /**
   * Retrieves a record based on the provided record identifier, revision name, and timestamp.
   *
   * @param recordId the unique identifier of the record to retrieve
   * @param revisionName the revision name of the record to retrieve
   * @param revisionTimestamp the timestamp of the record's specific revision
   * @return the retrieved Record object matching the provided criteria
   * @throws ExternalTaskException if there is an error while retrieving the record
   */
  Record getRecord(String recordId, String revisionName, Date revisionTimestamp) throws ExternalTaskException;

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
   * Creates a dataset identifier for use within the processing engine.
   *
   * @param datasetId the identifier of the dataset to be created
   * @return true if the dataset identifier was successfully created, false otherwise
   * @throws ExternalTaskException if an error occurs while creating the dataset identifier
   */
  boolean createEngineDatasetId(String datasetId) throws ExternalTaskException;

}
