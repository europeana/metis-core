package eu.europeana.metis.core.engine.ecloud;

import static java.lang.String.format;

import eu.europeana.cloud.client.uis.rest.CloudException;
import eu.europeana.cloud.client.uis.rest.UISClient;
import eu.europeana.cloud.common.model.File;
import eu.europeana.cloud.common.model.Representation;
import eu.europeana.cloud.common.response.ResultSlice;
import eu.europeana.cloud.mcs.driver.DataSetServiceClient;
import eu.europeana.cloud.mcs.driver.FileServiceClient;
import eu.europeana.cloud.service.mcs.exception.MCSException;
import eu.europeana.cloud.service.uis.exception.RecordDoesNotExistException;
import eu.europeana.metis.core.rest.Record;
import eu.europeana.metis.core.workflow.plugins.MetisPlugin;
import eu.europeana.metis.exception.ExternalTaskException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

/**
 * A client for managing interactions with eCloud Engine dataset records, including creation, retrieval, and verification.
 */
@Slf4j
public class EcloudEngineDatasetRecordClient {

  private final DataSetServiceClient dataSetServiceClient;
  private final FileServiceClient fileServiceClient;
  private final UISClient uisClient;

  /**
   * Constructor.
   *
   * @param dataSetServiceClient The client used for dataset-related operations.
   * @param fileServiceClient The client used for file-management operations.
   * @param uisClient The client used for user interaction operations.
   */
  public EcloudEngineDatasetRecordClient(
      DataSetServiceClient dataSetServiceClient, FileServiceClient fileServiceClient, UISClient uisClient) {
    this.dataSetServiceClient = dataSetServiceClient;
    this.fileServiceClient = fileServiceClient;
    this.uisClient = uisClient;
  }

  /**
   * Retrieves a list of records for a dataset.
   *
   * @param providerId The ID of the provider requesting the records.
   * @param numberOfRecords The maximum number of records to retrieve.
   * @return A list of retrieved records based on the specified parameters.
   * @throws ExternalTaskException If an issue occurs while fetching the records from external services.
   */
  public List<Record> getRecords(String providerId, String engineBatchId, int numberOfRecords) throws ExternalTaskException {
    ResultSlice<Representation> dataSetRepresentationsChunk;
    try {
      dataSetRepresentationsChunk =
          dataSetServiceClient.getDataSetRepresentationsChunk(providerId, engineBatchId, true, null, numberOfRecords);
    } catch (MCSException e) {
      throw new ExternalTaskException(
          format("Error fetching dataset representations for provider %s and batch %s", providerId, engineBatchId), e);
    }

    List<Record> records = new ArrayList<>();
    for (Representation representation : dataSetRepresentationsChunk.getResults()) {
      records.add(getRecord(representation));
    }
    return records;
  }

  /**
   * Retrieves a list of records given their ids.
   *
   * @param providerId The ID of the provider.
   * @param recordIds The list of record IDs to retrieve.
   * @param engineBatchId
   * @return A list of records that match the provided criteria.
   * @throws ExternalTaskException If an error occurs while retrieving the records.
   */
  public List<Record> getRecords(String providerId, List<String> recordIds, String engineBatchId)
      throws ExternalTaskException {

    final List<Record> records = new ArrayList<>(recordIds.size());
    for (String recordId : recordIds) {
      records.add(getRecord(providerId, recordId, engineBatchId));
    }

    return records;
  }

  /**
   * Retrieves a record given its id.
   *
   * @param providerId The unique identifier for the data provider.
   * @param recordId The unique identifier for the record.
   * @return The retrieved Record object, or null if no matching record is found.
   * @throws ExternalTaskException If an issue occurs while fetching the record.
   */
  public Record getRecord(String providerId, String recordId, String engineBatchId) throws ExternalTaskException {
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

    final List<Representation> representations;
    try {
      representations = dataSetServiceClient.getDataSetRepresentations(providerId, engineBatchId, ecloudId,
          MetisPlugin.getRepresentationName());
    } catch (MCSException e) {
      throw new ExternalTaskException(
          format("Failed to get ecloud record for providerId: %s, engineBatchId: %s, ecloudId: %s, representationName: %s", providerId,
              engineBatchId, ecloudId, MetisPlugin.getRepresentationName()), e);
    }

    if (representations == null || representations.isEmpty()) {
      throw new ExternalTaskException(format("No representations found for ecloudId: %s", ecloudId));
    }
    return getRecord(representations.getFirst());
  }

  private Record getRecord(Representation representation) throws ExternalTaskException {
    // Perform checks on the file lists.
    if (representation.getFiles() == null || representation.getFiles().isEmpty()) {
      throw new ExternalTaskException(format(
          "Expecting one file in the representation, but received none. ecloudId: %s, representation: %s",
          representation.getCloudId(), representation));
    }
    final File file = representation.getFiles().getFirst();

    // Get the file contents belonging to this representation version.
    try {
      final InputStream inputStream = fileServiceClient.getFile(file.getContentUri().toString());
      return new Record(representation.getCloudId(), IOUtils.toString(inputStream, StandardCharsets.UTF_8.name()));
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
      log.warn("Could not verify existence of eCloud ID: {}", potentialEcloudId, e);
      // TODO currently we can't distinguish between a connection issue and a non-existing eCloud ID.
      //  The client should be changed to allow for this. We assume here that there is not a connection
      //  issue because, where this method is called, we just did a successful call to the UIS service.
      return null;
    }
  }
}
