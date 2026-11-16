package eu.europeana.metis.core.engine.ecloud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import eu.europeana.cloud.client.uis.rest.CloudException;
import eu.europeana.cloud.client.uis.rest.UISClient;
import eu.europeana.cloud.common.model.CloudId;
import eu.europeana.cloud.common.model.File;
import eu.europeana.cloud.common.model.LocalId;
import eu.europeana.cloud.common.model.Representation;
import eu.europeana.cloud.common.response.ErrorInfo;
import eu.europeana.cloud.common.response.ResultSlice;
import eu.europeana.cloud.mcs.driver.DataSetServiceClient;
import eu.europeana.cloud.mcs.driver.FileServiceClient;
import eu.europeana.cloud.service.mcs.exception.MCSException;
import eu.europeana.cloud.service.mcs.exception.RecordNotExistsException;
import eu.europeana.cloud.service.mcs.exception.RepresentationNotExistsException;
import eu.europeana.cloud.service.uis.exception.CloudIdDoesNotExistException;
import eu.europeana.cloud.service.uis.exception.RecordDoesNotExistException;
import eu.europeana.metis.core.rest.Record;
import eu.europeana.metis.core.workflow.plugins.MetisPlugin;
import eu.europeana.metis.exception.ExternalTaskException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TestEcloudEngineDatasetRecordClient {

  private static final String PROVIDER_ID = "providerId";
  private static final String BATCH_ID = "batchId";
  private static final String RECORD_ID = "recordId";
  private static final String ECLOUD_ID = "ecloudId";
  private static final String RECORD_CONTENT = "recordContent";
  private static final URI FILE_URI = URI.create("file://fake/path/to/file.xml");

  private DataSetServiceClient dataSetServiceClient;
  private FileServiceClient fileServiceClient;
  private UISClient uisClient;

  private EcloudEngineDatasetRecordClient ecloudEngineDatasetRecordClient;

  @BeforeEach
  void setUp() {
    dataSetServiceClient = mock(DataSetServiceClient.class);
    fileServiceClient = mock(FileServiceClient.class);
    uisClient = mock(UISClient.class);

    ecloudEngineDatasetRecordClient = new EcloudEngineDatasetRecordClient(dataSetServiceClient, fileServiceClient, uisClient);
  }

  @Test
  void getRecordsFromDataset() throws Exception {
    Representation representation = createRepresentation(ECLOUD_ID, BATCH_ID);
    ResultSlice<Representation> resultSlice = new ResultSlice<>(null, List.of(representation));
    when(dataSetServiceClient.getDataSetRepresentationsChunk(PROVIDER_ID, BATCH_ID, true, null, 1)).thenReturn(resultSlice);
    mockFileContent();
    List<Record> records = ecloudEngineDatasetRecordClient.getRecords(PROVIDER_ID, BATCH_ID, 1);

    assertEquals(1, records.size());
    assertEquals(ECLOUD_ID, records.getFirst().ecloudId());
    assertEquals(RECORD_CONTENT, records.getFirst().xmlRecord());
  }

  @Test
  void getRecordsFromDatasetWhenDatasetClientThrows() throws Exception {
    when(dataSetServiceClient.getDataSetRepresentationsChunk(PROVIDER_ID, BATCH_ID, true, null, 1))
        .thenThrow(new MCSException());
    assertThrows(ExternalTaskException.class, () -> ecloudEngineDatasetRecordClient.getRecords(PROVIDER_ID, BATCH_ID, 1));
  }

  @Test
  void getRecordsFromDatasetWhenRepresentationHasNullFiles() throws Exception {
    Representation representation = mock(Representation.class);
    when(representation.getCloudId()).thenReturn(ECLOUD_ID);
    when(representation.getFiles()).thenReturn(null);

    when(dataSetServiceClient.getDataSetRepresentationsChunk(PROVIDER_ID, BATCH_ID, true, null, 1))
        .thenReturn(new ResultSlice<>(null, List.of(representation)));

    assertThrows(ExternalTaskException.class, () -> ecloudEngineDatasetRecordClient.getRecords(PROVIDER_ID, BATCH_ID, 1));
  }

  @Test
  void getRecordsFromDatasetWhenRepresentationHasNoFiles() throws Exception {
    Representation representation = mock(Representation.class);
    when(representation.getCloudId()).thenReturn(ECLOUD_ID);
    when(representation.getFiles()).thenReturn(List.of());

    when(dataSetServiceClient.getDataSetRepresentationsChunk(PROVIDER_ID, BATCH_ID, true, null, 1))
        .thenReturn(new ResultSlice<>(null, List.of(representation)));

    assertThrows(ExternalTaskException.class, () -> ecloudEngineDatasetRecordClient.getRecords(PROVIDER_ID, BATCH_ID, 1));
  }

  @Test
  void getRecordsFromDatasetWhenFileClientThrows() throws Exception {
    Representation representation = createRepresentation(ECLOUD_ID, BATCH_ID);

    when(dataSetServiceClient.getDataSetRepresentationsChunk(PROVIDER_ID, BATCH_ID, true, null, 1))
        .thenReturn(new ResultSlice<>(null, List.of(representation)));

    when(fileServiceClient.getFile(FILE_URI.toString())).thenThrow(new MCSException());

    assertThrows(ExternalTaskException.class, () -> ecloudEngineDatasetRecordClient.getRecords(PROVIDER_ID, BATCH_ID, 1));
  }

  @Test
  void getRecordsFromDatasetWhenInputStreamThrows() throws Exception {
    Representation representation = createRepresentation(ECLOUD_ID, BATCH_ID);

    when(dataSetServiceClient.getDataSetRepresentationsChunk(PROVIDER_ID, BATCH_ID, true, null, 1))
        .thenReturn(new ResultSlice<>(null, List.of(representation)));

    InputStream throwingInputStream = mock(InputStream.class);
    when(throwingInputStream.read(any(byte[].class), anyInt(), anyInt())).thenThrow(new IOException());

    when(fileServiceClient.getFile(FILE_URI.toString())).thenReturn(throwingInputStream);

    assertThrows(ExternalTaskException.class, () -> ecloudEngineDatasetRecordClient.getRecords(PROVIDER_ID, BATCH_ID, 1));
  }

  @Test
  void getRecordsByRecordIds() throws Exception {
    CloudId cloudId = new CloudId(ECLOUD_ID, new LocalId(PROVIDER_ID, RECORD_ID));
    when(uisClient.getCloudId(PROVIDER_ID, RECORD_ID)).thenReturn(cloudId);

    Representation representation = createRepresentation(ECLOUD_ID, BATCH_ID);
    mockDatasetRepresentations(List.of(representation));
    mockFileContent();

    List<Record> records = ecloudEngineDatasetRecordClient.getRecords(PROVIDER_ID, List.of(RECORD_ID), BATCH_ID);

    assertEquals(1, records.size());
    assertEquals(ECLOUD_ID, records.getFirst().ecloudId());
    assertEquals(RECORD_CONTENT, records.getFirst().xmlRecord());
  }

  @Test
  void getRecordUsingLocalRecordId() throws Exception {
    CloudId cloudId = new CloudId(ECLOUD_ID, new LocalId(PROVIDER_ID, RECORD_ID));
    when(uisClient.getCloudId(PROVIDER_ID, RECORD_ID)).thenReturn(cloudId);

    Representation representation = createRepresentation(ECLOUD_ID, BATCH_ID);
    mockDatasetRepresentations(List.of(representation));
    mockFileContent();

    Record record = ecloudEngineDatasetRecordClient.getRecord(PROVIDER_ID, RECORD_ID, BATCH_ID);

    assertEquals(ECLOUD_ID, record.ecloudId());
    assertEquals(RECORD_CONTENT, record.xmlRecord());
    verify(dataSetServiceClient).getDataSetRepresentations(PROVIDER_ID, BATCH_ID, ECLOUD_ID, MetisPlugin.getRepresentationName());
  }

  @Test
  void getRecordSelectsRepresentationMatchingBatchId() throws Exception {
    CloudId cloudId = new CloudId(ECLOUD_ID, new LocalId(PROVIDER_ID, RECORD_ID));
    when(uisClient.getCloudId(PROVIDER_ID, RECORD_ID)).thenReturn(cloudId);
    Representation otherBatchRepresentation = createRepresentation(ECLOUD_ID, "otherBatch");
    Representation requestedBatchRepresentation = createRepresentation(ECLOUD_ID, BATCH_ID);
    mockDatasetRepresentations(List.of(otherBatchRepresentation, requestedBatchRepresentation));
    mockFileContent();

    Record record = ecloudEngineDatasetRecordClient.getRecord(PROVIDER_ID, RECORD_ID, BATCH_ID);
    assertEquals(ECLOUD_ID, record.ecloudId());
    assertEquals(RECORD_CONTENT, record.xmlRecord());
  }

  @Test
  void getRecordWhenUisLookupFailsForConnectivityReason() throws Exception {
    when(uisClient.getCloudId(PROVIDER_ID, RECORD_ID)).thenThrow(new CloudException("", new IllegalStateException()));
    assertThrows(ExternalTaskException.class,
        () -> ecloudEngineDatasetRecordClient.getRecord(PROVIDER_ID, RECORD_ID, BATCH_ID));
  }

  @Test
  void getRecordFallsBackToSuppliedEcloudId() throws Exception {
    when(uisClient.getCloudId(PROVIDER_ID, ECLOUD_ID))
        .thenThrow(new CloudException("", new RecordDoesNotExistException(new ErrorInfo())));
    CloudId matchingCloudId = new CloudId(ECLOUD_ID, new LocalId(PROVIDER_ID, RECORD_ID));
    when(uisClient.getRecordId(ECLOUD_ID)).thenReturn(new ResultSlice<>(null, List.of(matchingCloudId)));

    Representation representation = createRepresentation(ECLOUD_ID, BATCH_ID);
    mockDatasetRepresentations(List.of(representation));
    mockFileContent();

    Record record = ecloudEngineDatasetRecordClient.getRecord(PROVIDER_ID, ECLOUD_ID, BATCH_ID);

    assertEquals(ECLOUD_ID, record.ecloudId());
    assertEquals(RECORD_CONTENT, record.xmlRecord());
    verify(dataSetServiceClient).getDataSetRepresentations(PROVIDER_ID, BATCH_ID, ECLOUD_ID, MetisPlugin.getRepresentationName());
  }

  @Test
  void getRecordWhenEcloudRecordHasNullRepresentations() throws Exception {
    CloudId cloudId = new CloudId(ECLOUD_ID, new LocalId(PROVIDER_ID, RECORD_ID));
    when(uisClient.getCloudId(PROVIDER_ID, RECORD_ID)).thenReturn(cloudId);

    assertNull(ecloudEngineDatasetRecordClient.getRecord(PROVIDER_ID, RECORD_ID, BATCH_ID));
  }

  @Test
  void getRecordWhenDatasetHasNoRepresentations() throws Exception {
    when(uisClient.getCloudId(PROVIDER_ID, RECORD_ID))
        .thenReturn(new CloudId(ECLOUD_ID, new LocalId(PROVIDER_ID, RECORD_ID)));
    mockDatasetRepresentations(List.of());
    assertNull(ecloudEngineDatasetRecordClient.getRecord(PROVIDER_ID, RECORD_ID, BATCH_ID));
  }

  @Test
  void getRecordWhenPotentialEcloudIdDoesNotExist() throws Exception {
    mockMissingLocalId();
    when(uisClient.getRecordId(RECORD_ID)).thenReturn(new ResultSlice<>(null, List.of()));

    assertNull(ecloudEngineDatasetRecordClient.getRecord(PROVIDER_ID, RECORD_ID, BATCH_ID));
    verifyNoInteractions(dataSetServiceClient, fileServiceClient);
  }

  @Test
  void getRecordWhenPotentialEcloudIdVerificationReportsNotFound() throws Exception {
    mockMissingLocalId();
    when(uisClient.getRecordId(RECORD_ID))
        .thenThrow(new CloudException("notFound", new CloudIdDoesNotExistException(new ErrorInfo())));

    assertNull(ecloudEngineDatasetRecordClient.getRecord(PROVIDER_ID, RECORD_ID, BATCH_ID));
    verifyNoInteractions(dataSetServiceClient, fileServiceClient);
  }

  @Test
  void getRecordWhenPotentialEcloudIdVerificationFailsForConnectivityReason() throws Exception {
    mockMissingLocalId();
    CloudException connectivityException = new CloudException("connectionFailed", new IOException());
    when(uisClient.getRecordId(RECORD_ID)).thenThrow(connectivityException);

    ExternalTaskException externalTaskException = assertThrows(ExternalTaskException.class,
        () -> ecloudEngineDatasetRecordClient.getRecord(PROVIDER_ID, RECORD_ID, BATCH_ID));

    assertSame(connectivityException, externalTaskException.getCause());
    verifyNoInteractions(dataSetServiceClient, fileServiceClient);
  }

  @Test
  void getRecordWhenDatasetClientReportsMissingRecord() throws Exception {
    when(uisClient.getCloudId(PROVIDER_ID, RECORD_ID))
        .thenReturn(new CloudId(ECLOUD_ID, new LocalId(PROVIDER_ID, RECORD_ID)));
    when(dataSetServiceClient.getDataSetRepresentations(PROVIDER_ID, BATCH_ID, ECLOUD_ID,
        MetisPlugin.getRepresentationName())).thenThrow(new RecordNotExistsException());

    assertNull(ecloudEngineDatasetRecordClient.getRecord(PROVIDER_ID, RECORD_ID, BATCH_ID));
    verifyNoInteractions(fileServiceClient);
  }

  @Test
  void getRecordWhenDatasetClientReportsMissingRepresentation() throws Exception {
    when(uisClient.getCloudId(PROVIDER_ID, RECORD_ID))
        .thenReturn(new CloudId(ECLOUD_ID, new LocalId(PROVIDER_ID, RECORD_ID)));
    when(dataSetServiceClient.getDataSetRepresentations(PROVIDER_ID, BATCH_ID, ECLOUD_ID,
        MetisPlugin.getRepresentationName())).thenThrow(new RepresentationNotExistsException());

    assertNull(ecloudEngineDatasetRecordClient.getRecord(PROVIDER_ID, RECORD_ID, BATCH_ID));
    verifyNoInteractions(fileServiceClient);
  }

  @Test
  void getRecordWhenDatasetClientFails() throws Exception {
    when(uisClient.getCloudId(PROVIDER_ID, RECORD_ID))
        .thenReturn(new CloudId(ECLOUD_ID, new LocalId(PROVIDER_ID, RECORD_ID)));
    MCSException mcsException = new MCSException();
    when(dataSetServiceClient.getDataSetRepresentations(PROVIDER_ID, BATCH_ID, ECLOUD_ID,
        MetisPlugin.getRepresentationName())).thenThrow(mcsException);

    ExternalTaskException externalTaskException = assertThrows(ExternalTaskException.class,
        () -> ecloudEngineDatasetRecordClient.getRecord(PROVIDER_ID, RECORD_ID, BATCH_ID));
    assertSame(mcsException, externalTaskException.getCause());
  }

  @Test
  void getRecordsByRecordIdsOmitsMissingRecords() throws Exception {
    mockMissingLocalId();
    when(uisClient.getRecordId(RECORD_ID)).thenReturn(new ResultSlice<>(null, List.of()));
    when(uisClient.getCloudId(PROVIDER_ID, "existingRecordId"))
        .thenReturn(new CloudId(ECLOUD_ID, new LocalId(PROVIDER_ID, "existingRecordId")));
    mockDatasetRepresentations(List.of(createRepresentation(ECLOUD_ID, BATCH_ID)));
    mockFileContent();

    List<Record> records = ecloudEngineDatasetRecordClient.getRecords(
        PROVIDER_ID, List.of(RECORD_ID, "existingRecordId"), BATCH_ID);
    assertEquals(List.of(new Record(ECLOUD_ID, RECORD_CONTENT)), records);
  }

  private void mockMissingLocalId() throws CloudException {
    when(uisClient.getCloudId(PROVIDER_ID, RECORD_ID))
        .thenThrow(new CloudException("notFound", new RecordDoesNotExistException(new ErrorInfo())));
  }

  private Representation createRepresentation(String ecloudId, String datasetId) {

    File file = mock(File.class);
    when(file.getContentUri()).thenReturn(FILE_URI);

    Representation representation = mock(Representation.class);
    when(representation.getCloudId()).thenReturn(ecloudId);
    when(representation.getDatasetIds()).thenReturn(Set.of(datasetId));
    when(representation.getFiles()).thenReturn(List.of(file));

    return representation;
  }

  private void mockDatasetRepresentations(List<Representation> representations) throws MCSException {
    when(dataSetServiceClient.getDataSetRepresentations(PROVIDER_ID, BATCH_ID, ECLOUD_ID,
        MetisPlugin.getRepresentationName())).thenReturn(representations);
  }

  private void mockFileContent() throws MCSException {
    when(fileServiceClient.getFile(FILE_URI.toString()))
        .thenReturn(new ByteArrayInputStream(RECORD_CONTENT.getBytes(StandardCharsets.UTF_8)));
  }
}