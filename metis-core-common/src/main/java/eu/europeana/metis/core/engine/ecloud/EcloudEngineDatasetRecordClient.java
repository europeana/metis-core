package eu.europeana.metis.core.engine.ecloud;

import static java.lang.String.format;

import eu.europeana.cloud.client.uis.rest.CloudException;
import eu.europeana.cloud.client.uis.rest.UISClient;
import eu.europeana.cloud.common.model.File;
import eu.europeana.cloud.common.model.Representation;
import eu.europeana.cloud.common.model.Revision;
import eu.europeana.cloud.common.response.CloudTagsResponse;
import eu.europeana.cloud.mcs.driver.DataSetServiceClient;
import eu.europeana.cloud.mcs.driver.FileServiceClient;
import eu.europeana.cloud.mcs.driver.RecordServiceClient;
import eu.europeana.cloud.service.mcs.exception.MCSException;
import eu.europeana.cloud.service.uis.exception.RecordDoesNotExistException;
import eu.europeana.metis.core.rest.Record;
import eu.europeana.metis.core.workflow.plugins.MetisPlugin;
import eu.europeana.metis.exception.ExternalTaskException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A client for managing interactions with eCloud Engine dataset records, including creation,
 * retrieval, and verification.
 */
public class EcloudEngineDatasetRecordClient {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final DateFormat pluginDateFormatForEcloud = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US);
  private final DataSetServiceClient dataSetServiceClient;
  private final RecordServiceClient recordServiceClient;
  private final FileServiceClient fileServiceClient;
  private final UISClient uisClient;

  /**
   * Constructor.
   *
   * @param dataSetServiceClient The client used for dataset-related operations.
   * @param recordServiceClient The client used for record-related operations.
   * @param fileServiceClient The client used for file-management operations.
   * @param uisClient The client used for user interaction operations.
   */
  public EcloudEngineDatasetRecordClient(DataSetServiceClient dataSetServiceClient, RecordServiceClient recordServiceClient,
      FileServiceClient fileServiceClient, UISClient uisClient) {
    this.dataSetServiceClient = dataSetServiceClient;
    this.recordServiceClient = recordServiceClient;
    this.fileServiceClient = fileServiceClient;
    this.uisClient = uisClient;
  }

  /**
   * Creates a dataset ID for the engine using the provided dataset and provider IDs.
   *
   * @param providerId The unique identifier for the dataset provider.
   * @param datasetId The unique identifier for the dataset.
   * @return True if the dataset ID is created successfully.
   * @throws ExternalTaskException If an error occurs during the dataset creation process.
   */
  public boolean createEngineDatasetId(String providerId, String datasetId) throws ExternalTaskException {
    try {
      dataSetServiceClient.createDataSet(providerId, datasetId, "Metis generated dataset id");
    } catch (MCSException e) {
      throw new ExternalTaskException("An error has occurred during ecloud dataset creation.", e);
    }
    return true;
  }

  /**
   * Retrieves a list of records for a dataset.
   *
   * @param providerId The ID of the provider requesting the records.
   * @param datasetId The ID of the dataset containing the records.
   * @param representationName The name of the representation associated with the records.
   * @param revisionName The name of the revision to filter the records by.
   * @param revisionTimestamp The timestamp of the revision to filter the records by.
   * @param numberOfRecords The maximum number of records to retrieve.
   * @return A list of retrieved records based on the specified parameters.
   * @throws ExternalTaskException If an issue occurs while fetching the records from external services.
   */
  public List<Record> getRecords(String providerId, String datasetId, String representationName, String revisionName,
      Date revisionTimestamp, int numberOfRecords) throws ExternalTaskException {
    final List<CloudTagsResponse> cloudIdsWithDeletedFlagSetToFalse;
    try {
      cloudIdsWithDeletedFlagSetToFalse = dataSetServiceClient.getRevisionsWithDeletedFlagSetToFalse(
          providerId, datasetId, representationName, revisionName,
          providerId, pluginDateFormatForEcloud.format(revisionTimestamp), numberOfRecords);
    } catch (MCSException e) {
      throw new ExternalTaskException(format(
          "Getting record list with file content failed. datasetId: %s, representationName: %s, revisionName: %s, revisionTimestamp: %s",
          datasetId, representationName, revisionName, revisionTimestamp),
          e);
    }

    // Get the records themselves.
    final List<Record> records = new ArrayList<>(cloudIdsWithDeletedFlagSetToFalse.size());
    for (CloudTagsResponse cloudTagsResponse : cloudIdsWithDeletedFlagSetToFalse) {
      final Record eloudXmlRecord = getRecordByEcloudIdAndRevision(providerId, cloudTagsResponse.getCloudId(), revisionName,
          revisionTimestamp);
      if (eloudXmlRecord == null) {
        throw new IllegalStateException(format("Could not get record for ecloudId: %s", cloudTagsResponse.getCloudId()));
      }
      records.add(eloudXmlRecord);
    }

    return records;
  }

  /**
   * Retrieves a list of records given their ids.
   *
   * @param providerId The ID of the provider.
   * @param recordIds The list of record IDs to retrieve.
   * @param revisionName The name of the revision.
   * @param revisionTimestamp The timestamp of the revision.
   * @return A list of records that match the provided criteria.
   * @throws ExternalTaskException If an error occurs while retrieving the records.
   */
  public List<Record> getRecords(String providerId, List<String> recordIds, String revisionName, Date revisionTimestamp)
      throws ExternalTaskException {

    final List<Record> records = new ArrayList<>(recordIds.size());
    for (String recordId : recordIds) {
      Optional.ofNullable(getRecordByEcloudIdAndRevision(providerId, recordId, revisionName, revisionTimestamp))
              .ifPresent(records::add);
    }

    return records;
  }

  /**
   * Retrieves a record given its id.
   *
   * @param providerId The unique identifier for the data provider.
   * @param recordId The unique identifier for the record.
   * @param revisionName The name of the revision to retrieve.
   * @param revisionTimestamp The timestamp of the specified revision.
   * @return The retrieved Record object, or null if no matching record is found.
   * @throws ExternalTaskException If an issue occurs while fetching the record.
   */
  public Record getRecord(String providerId, String recordId, String revisionName, Date revisionTimestamp)
      throws ExternalTaskException {
    String ecloudId = null;
    try {
      if (recordId != null) {
        ecloudId = uisClient.getCloudId(providerId, recordId).getId();
      }
    } catch (CloudException e) {
      if (e.getCause() instanceof RecordDoesNotExistException) {
        // The record ID does not exist. Check whether the ID is already an eCloud ID.
        ecloudId = verifyExistenceOfEcloudId(recordId);
      } else {
        // Some other connectivity issue.
        throw new ExternalTaskException(format("Failed to lookup cloudId for idToSearch: %s", recordId), e);
      }
    }

    // Try to retrieve the record. Note: we need to know if the eCloud ID exists at this point
    // because getRecord() cannot detect non-existing eCloud IDs.
    return ecloudId == null ? null : getRecordByEcloudIdAndRevision(providerId, ecloudId, revisionName, revisionTimestamp);
  }

  private Record getRecordByEcloudIdAndRevision(String providerId, String ecloudId, String revisionName, Date revisionTimestamp)
      throws ExternalTaskException {

    // Get the representation(s) for the given combination of plugin and record ID.
    final List<Representation> representations;
    final Revision revision = new Revision(revisionName, providerId, revisionTimestamp);
    try {
      representations = recordServiceClient
          .getRepresentationsByRevision(ecloudId, MetisPlugin.getRepresentationName(), revision);
    } catch (MCSException e) {
      throw new ExternalTaskException(format(
          "Getting representation list with failed. ecloudId: %s, revision: %s", ecloudId, revision), e);
    }

    // If no representation is found, return null.
    if (representations == null || representations.isEmpty()) {
      return null;
    }
    final Representation representation = representations.getFirst();

    // Perform checks on the file lists.
    if (representation.getFiles() == null || representation.getFiles().isEmpty()) {
      throw new ExternalTaskException(format(
          "Expecting one file in the representation, but received none. ecloudId: %s, representation: %s", ecloudId,
          representation));
    }
    final File file = representation.getFiles().getFirst();

    // Obtain the file contents belonging to this representation version.
    try {
      final InputStream inputStream = fileServiceClient.getFile(file.getContentUri().toString());
      return new Record(ecloudId, IOUtils.toString(inputStream, StandardCharsets.UTF_8.name()));
    } catch (MCSException e) {
      throw new ExternalTaskException(format("Getting file content failed. uri: %s", file.getContentUri()), e);
    } catch (IOException e) {
      throw new ExternalTaskException(format("Problem reading input stream. uri: %s", file.getContentUri()), e);
    }
  }

  private String verifyExistenceOfEcloudId(String potentialEcloudId) {
    try {
      return uisClient.getRecordId(potentialEcloudId).getResults().isEmpty() ? null : potentialEcloudId;
    } catch (CloudException e) {
      LOGGER.warn("Could not verify existence of eCloud ID: {}", potentialEcloudId, e);
      // TODO currently we can't distinguish between a connection issue and a non-existing eCloud ID.
      //  The client should be changed to allow for this. We assume here that there is not a connection
      //  issue because, where this method is called, we just did a successful call to the UIS service.
      return null;
    }
  }
}
