package eu.europeana.metis.core.engine.ecloud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import eu.europeana.cloud.client.uis.rest.CloudException;
import eu.europeana.cloud.client.uis.rest.UISClient;
import eu.europeana.cloud.common.model.CloudId;
import eu.europeana.cloud.common.model.File;
import eu.europeana.cloud.common.model.LocalId;
import eu.europeana.cloud.common.model.Representation;
import eu.europeana.cloud.common.model.Revision;
import eu.europeana.cloud.common.response.CloudTagsResponse;
import eu.europeana.cloud.common.response.ErrorInfo;
import eu.europeana.cloud.common.response.ResultSlice;
import eu.europeana.cloud.mcs.driver.DataSetServiceClient;
import eu.europeana.cloud.mcs.driver.FileServiceClient;
import eu.europeana.cloud.mcs.driver.RecordServiceClient;
import eu.europeana.cloud.service.mcs.exception.MCSException;
import eu.europeana.cloud.service.uis.exception.RecordDoesNotExistException;
import eu.europeana.metis.core.rest.Record;
import eu.europeana.metis.core.workflow.plugins.MetisPlugin;
import eu.europeana.metis.exception.ExternalTaskException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TestEcloudEngineDatasetRecordClient {

  private static final String DATASET_ID = "datasetId";
  private static final String PROVIDER_ID = "providerId";
  private static final String REPRESENTATION_NAME = "representationName";
  private static final String REVISION_NAME = "revisionName";

  private DataSetServiceClient dataSetServiceClient;
  private RecordServiceClient recordServiceClient;
  private FileServiceClient fileServiceClient;
  private UISClient uisClient;

  private EcloudEngineDatasetRecordClient ecloudEngineDatasetRecordClient;

  @BeforeEach
  void setUp() {
    dataSetServiceClient = mock(DataSetServiceClient.class);
    recordServiceClient = mock(RecordServiceClient.class);
    fileServiceClient = mock(FileServiceClient.class);
    uisClient = mock(UISClient.class);
    ecloudEngineDatasetRecordClient = new EcloudEngineDatasetRecordClient(dataSetServiceClient, recordServiceClient,
        fileServiceClient, uisClient);
  }

  @Test
  void createEngineDatasetId() throws Exception {
    when(dataSetServiceClient.createDataSet(anyString(), anyString(), anyString())).thenReturn(null);
    assertTrue(ecloudEngineDatasetRecordClient.createEngineDatasetId(PROVIDER_ID, DATASET_ID));
  }

  @Test
  void createEngineDatasetId_throws() throws Exception {
    when(dataSetServiceClient.createDataSet(anyString(), anyString(), anyString())).thenThrow(new MCSException(""));
    assertThrows(ExternalTaskException.class,
        () -> ecloudEngineDatasetRecordClient.createEngineDatasetId(PROVIDER_ID, DATASET_ID));
  }

  @Test
  void getRecords_withDatasetId() throws Exception {
    CloudTagsResponse cloudTagsResponse = new CloudTagsResponse("cloudId", false);
    final List<CloudTagsResponse> cloudTagsResponses = List.of(cloudTagsResponse);
    when(dataSetServiceClient.getRevisionsWithDeletedFlagSetToFalse(
        eq(PROVIDER_ID), eq(DATASET_ID), eq(REPRESENTATION_NAME), eq(REVISION_NAME),
        eq(PROVIDER_ID), anyString(), eq(1))).thenReturn(cloudTagsResponses);

    File fileMock = mock(File.class);
    when(fileMock.getContentUri()).thenReturn(URI.create("file://fake/path/to/file.xml"));
    Representation representationMock = mock(Representation.class);
    when(representationMock.getFiles()).thenReturn(List.of(fileMock));

    when(recordServiceClient
        .getRepresentationsByRevision(eq(cloudTagsResponse.getCloudId()), eq(MetisPlugin.getRepresentationName()), any(
            Revision.class))).thenReturn(List.of(representationMock));

    String recordContent = "recordContent";
    when(fileServiceClient.getFile(anyString())).thenReturn(new ByteArrayInputStream(recordContent.getBytes()));

    List<Record> records =
        ecloudEngineDatasetRecordClient.getRecords(PROVIDER_ID, DATASET_ID, REPRESENTATION_NAME, REVISION_NAME, new Date(), 1);
    assertEquals(1, records.size());
    assertEquals(cloudTagsResponse.getCloudId(), records.getFirst().getEcloudId());
    assertEquals(recordContent, records.getFirst().getXmlRecord());
  }

  @Test
  void getRecords_withDatasetId_getFile_throws_MCSException() throws Exception {
    CloudTagsResponse cloudTagsResponse = new CloudTagsResponse("cloudId", false);
    final List<CloudTagsResponse> cloudTagsResponses = List.of(cloudTagsResponse);
    when(dataSetServiceClient.getRevisionsWithDeletedFlagSetToFalse(
        eq(PROVIDER_ID), eq(DATASET_ID), eq(REPRESENTATION_NAME), eq(REVISION_NAME),
        eq(PROVIDER_ID), anyString(), eq(1))).thenReturn(cloudTagsResponses);

    File fileMock = mock(File.class);
    when(fileMock.getContentUri()).thenReturn(URI.create("file://fake/path/to/file.xml"));
    Representation representationMock = mock(Representation.class);
    when(representationMock.getFiles()).thenReturn(List.of(fileMock));

    when(recordServiceClient
        .getRepresentationsByRevision(eq(cloudTagsResponse.getCloudId()), eq(MetisPlugin.getRepresentationName()), any(
            Revision.class))).thenReturn(List.of(representationMock));

    when(fileServiceClient.getFile(anyString())).thenThrow(new MCSException());

    assertThrows(ExternalTaskException.class,
        () -> ecloudEngineDatasetRecordClient.getRecords(PROVIDER_ID, DATASET_ID, REPRESENTATION_NAME, REVISION_NAME,
            new Date(), 1));
  }

  @Test
  void getRecords_withDatasetId_getFile_InputStream_throws_IOException() throws Exception {
    CloudTagsResponse cloudTagsResponse = new CloudTagsResponse("cloudId", false);
    final List<CloudTagsResponse> cloudTagsResponses = List.of(cloudTagsResponse);
    when(dataSetServiceClient.getRevisionsWithDeletedFlagSetToFalse(
        eq(PROVIDER_ID), eq(DATASET_ID), eq(REPRESENTATION_NAME), eq(REVISION_NAME),
        eq(PROVIDER_ID), anyString(), eq(1))).thenReturn(cloudTagsResponses);

    File fileMock = mock(File.class);
    when(fileMock.getContentUri()).thenReturn(URI.create("file://fake/path/to/file.xml"));
    Representation representationMock = mock(Representation.class);
    when(representationMock.getFiles()).thenReturn(List.of(fileMock));

    when(recordServiceClient
        .getRepresentationsByRevision(eq(cloudTagsResponse.getCloudId()), eq(MetisPlugin.getRepresentationName()), any(
            Revision.class))).thenReturn(List.of(representationMock));

    InputStream throwingStreamMock = mock(InputStream.class);
    when(throwingStreamMock.read(any(byte[].class), anyInt(), anyInt())).thenThrow(new IOException());
    when(fileServiceClient.getFile(anyString())).thenReturn(throwingStreamMock);

    assertThrows(ExternalTaskException.class,
        () -> ecloudEngineDatasetRecordClient.getRecords(PROVIDER_ID, DATASET_ID, REPRESENTATION_NAME, REVISION_NAME,
            new Date(), 1));
  }

  @Test
  void getRecords_withDatasetId_representation_getFiles_blank_throws() throws Exception {
    CloudTagsResponse cloudTagsResponse = new CloudTagsResponse("cloudId", false);
    final List<CloudTagsResponse> cloudTagsResponses = List.of(cloudTagsResponse);
    when(dataSetServiceClient.getRevisionsWithDeletedFlagSetToFalse(
        eq(PROVIDER_ID), eq(DATASET_ID), eq(REPRESENTATION_NAME), eq(REVISION_NAME),
        eq(PROVIDER_ID), anyString(), eq(1))).thenReturn(cloudTagsResponses);

    Representation representationMock = mock(Representation.class);
    when(representationMock.getFiles()).thenReturn(null).thenReturn(List.of());

    when(recordServiceClient
        .getRepresentationsByRevision(eq(cloudTagsResponse.getCloudId()), eq(MetisPlugin.getRepresentationName()), any(
            Revision.class))).thenReturn(List.of(representationMock));

    assertThrows(ExternalTaskException.class,
        () -> ecloudEngineDatasetRecordClient.getRecords(PROVIDER_ID, DATASET_ID, REPRESENTATION_NAME, REVISION_NAME,
            new Date(), 1));
    assertThrows(ExternalTaskException.class,
        () -> ecloudEngineDatasetRecordClient.getRecords(PROVIDER_ID, DATASET_ID, REPRESENTATION_NAME, REVISION_NAME,
            new Date(), 1));
  }

  @Test
  void getRecords_withDatasetId_representations_blank_throws() throws Exception {
    CloudTagsResponse cloudTagsResponse = new CloudTagsResponse("cloudId", false);
    final List<CloudTagsResponse> cloudTagsResponses = List.of(cloudTagsResponse);
    when(dataSetServiceClient.getRevisionsWithDeletedFlagSetToFalse(
        eq(PROVIDER_ID), eq(DATASET_ID), eq(REPRESENTATION_NAME), eq(REVISION_NAME),
        eq(PROVIDER_ID), anyString(), eq(1))).thenReturn(cloudTagsResponses);

    when(recordServiceClient
        .getRepresentationsByRevision(eq(cloudTagsResponse.getCloudId()), eq(MetisPlugin.getRepresentationName()), any(
            Revision.class))).thenReturn(null).thenReturn(List.of()).thenThrow(new MCSException());

    Date now = new Date();
    assertThrows(IllegalStateException.class,
        () -> ecloudEngineDatasetRecordClient.getRecords(PROVIDER_ID, DATASET_ID, REPRESENTATION_NAME, REVISION_NAME, now, 1));
    assertThrows(IllegalStateException.class,
        () -> ecloudEngineDatasetRecordClient.getRecords(PROVIDER_ID, DATASET_ID, REPRESENTATION_NAME, REVISION_NAME, now, 1));
    assertThrows(ExternalTaskException.class,
        () -> ecloudEngineDatasetRecordClient.getRecords(PROVIDER_ID, DATASET_ID, REPRESENTATION_NAME, REVISION_NAME, now, 1));
  }

  @Test
  void getRecords_withDatasetId_getRevisions_throws() throws Exception {
    when(dataSetServiceClient.getRevisionsWithDeletedFlagSetToFalse(
        eq(PROVIDER_ID), eq(DATASET_ID), eq(REPRESENTATION_NAME), eq(REVISION_NAME),
        eq(PROVIDER_ID), anyString(), eq(1))).thenThrow(new MCSException());

    assertThrows(ExternalTaskException.class,
        () -> ecloudEngineDatasetRecordClient.getRecords(PROVIDER_ID, DATASET_ID, REPRESENTATION_NAME, REVISION_NAME,
            new Date(), 1));
  }

  @Test
  void getRecords_withRecordIds() throws Exception {
    List<String> recordIds = List.of("recordId");

    File fileMock = mock(File.class);
    when(fileMock.getContentUri()).thenReturn(URI.create("file://fake/path/to/file.xml"));
    Representation representationMock = mock(Representation.class);
    when(representationMock.getFiles()).thenReturn(List.of(fileMock));

    when(recordServiceClient
        .getRepresentationsByRevision(eq(recordIds.getFirst()), eq(MetisPlugin.getRepresentationName()), any(
            Revision.class))).thenReturn(List.of(representationMock));

    String recordContent = "recordContent";
    when(fileServiceClient.getFile(anyString())).thenReturn(new ByteArrayInputStream(recordContent.getBytes()));

    List<Record> records =
        ecloudEngineDatasetRecordClient.getRecords(PROVIDER_ID, recordIds, REVISION_NAME, new Date());
    assertEquals(1, records.size());
    assertEquals(recordIds.getFirst(), records.getFirst().getEcloudId());
    assertEquals(recordContent, records.getFirst().getXmlRecord());
  }

  @Test
  void getRecords_withRecordIds_getFile_throws_MCSException() throws Exception {
    List<String> recordIds = List.of("recordId");

    File fileMock = mock(File.class);
    when(fileMock.getContentUri()).thenReturn(URI.create("file://fake/path/to/file.xml"));
    Representation representationMock = mock(Representation.class);
    when(representationMock.getFiles()).thenReturn(List.of(fileMock));

    when(recordServiceClient
        .getRepresentationsByRevision(eq(recordIds.getFirst()), eq(MetisPlugin.getRepresentationName()), any(
            Revision.class))).thenReturn(List.of(representationMock));

    when(fileServiceClient.getFile(anyString())).thenThrow(new MCSException());

    assertThrows(ExternalTaskException.class,
        () -> ecloudEngineDatasetRecordClient.getRecords(PROVIDER_ID, recordIds, REVISION_NAME, new Date()));
  }

  @Test
  void getRecords_withRecordIds_getFile_InputStream_throws_IOException() throws Exception {
    List<String> recordIds = List.of("recordId");

    File fileMock = mock(File.class);
    when(fileMock.getContentUri()).thenReturn(URI.create("file://fake/path/to/file.xml"));
    Representation representationMock = mock(Representation.class);
    when(representationMock.getFiles()).thenReturn(List.of(fileMock));

    when(recordServiceClient
        .getRepresentationsByRevision(eq(recordIds.getFirst()), eq(MetisPlugin.getRepresentationName()), any(
            Revision.class))).thenReturn(List.of(representationMock));

    InputStream throwingStreamMock = mock(InputStream.class);
    when(throwingStreamMock.read(any(byte[].class), anyInt(), anyInt())).thenThrow(new IOException());
    when(fileServiceClient.getFile(anyString())).thenReturn(throwingStreamMock);

    assertThrows(ExternalTaskException.class,
        () -> ecloudEngineDatasetRecordClient.getRecords(PROVIDER_ID, recordIds, REVISION_NAME, new Date()));
  }

  @Test
  void getRecords_withRecordIds_representation_getFiles_blank_empty_list() throws Exception {
    List<String> recordIds = List.of("recordId");
    Representation representationMock = mock(Representation.class);
    when(representationMock.getFiles()).thenReturn(null).thenReturn(List.of());

    when(recordServiceClient
        .getRepresentationsByRevision(eq(recordIds.getFirst()), eq(MetisPlugin.getRepresentationName()), any(
            Revision.class))).thenReturn(List.of(representationMock));

    assertThrows(ExternalTaskException.class,
        () -> ecloudEngineDatasetRecordClient.getRecords(PROVIDER_ID, recordIds, REVISION_NAME, new Date()));
    assertThrows(ExternalTaskException.class,
        () -> ecloudEngineDatasetRecordClient.getRecords(PROVIDER_ID, recordIds, REVISION_NAME, new Date()));
  }

  @Test
  void getRecords_withRecordIds_representations_blank_then_throws() throws Exception {
    List<String> recordIds = List.of("recordId");

    when(recordServiceClient
        .getRepresentationsByRevision(eq(recordIds.getFirst()), eq(MetisPlugin.getRepresentationName()), any(
            Revision.class))).thenReturn(null).thenReturn(List.of()).thenThrow(new MCSException());

    assertTrue(ecloudEngineDatasetRecordClient.getRecords(PROVIDER_ID, recordIds, REVISION_NAME, new Date()).isEmpty());
    assertTrue(ecloudEngineDatasetRecordClient.getRecords(PROVIDER_ID, recordIds, REVISION_NAME, new Date()).isEmpty());
    assertThrows(ExternalTaskException.class,
        () -> ecloudEngineDatasetRecordClient.getRecords(PROVIDER_ID, recordIds, REVISION_NAME, new Date()));
  }

  @Test
  void getRecord() throws Exception {
    String recordId = "recordId";

    CloudId cloudId = new CloudId(recordId, new LocalId(PROVIDER_ID, recordId));
    when(uisClient.getCloudId(PROVIDER_ID, recordId)).thenReturn(cloudId);

    File fileMock = mock(File.class);
    when(fileMock.getContentUri()).thenReturn(URI.create("file://fake/path/to/file.xml"));
    Representation representationMock = mock(Representation.class);
    when(representationMock.getFiles()).thenReturn(List.of(fileMock));
    when(recordServiceClient
        .getRepresentationsByRevision(eq(recordId), eq(MetisPlugin.getRepresentationName()), any(
            Revision.class))).thenReturn(List.of(representationMock));

    String recordContent = "recordContent";
    when(fileServiceClient.getFile(anyString())).thenReturn(new ByteArrayInputStream(recordContent.getBytes()));

    Record recordItem = ecloudEngineDatasetRecordClient.getRecord(PROVIDER_ID, recordId, REVISION_NAME, new Date());
    assertEquals(recordId, recordItem.getEcloudId());
    assertEquals(recordContent, recordItem.getXmlRecord());
  }

  @Test
  void getRecord_getFile_throws_MCSException() throws Exception {
    String recordId = "recordId";

    CloudId cloudId = new CloudId(recordId, new LocalId(PROVIDER_ID, recordId));
    when(uisClient.getCloudId(PROVIDER_ID, recordId)).thenReturn(cloudId);

    File fileMock = mock(File.class);
    when(fileMock.getContentUri()).thenReturn(URI.create("file://fake/path/to/file.xml"));
    Representation representationMock = mock(Representation.class);
    when(representationMock.getFiles()).thenReturn(List.of(fileMock));
    when(recordServiceClient
        .getRepresentationsByRevision(eq(recordId), eq(MetisPlugin.getRepresentationName()), any(
            Revision.class))).thenReturn(List.of(representationMock));

    when(fileServiceClient.getFile(anyString())).thenThrow(new MCSException());

    assertThrows(ExternalTaskException.class,
        () -> ecloudEngineDatasetRecordClient.getRecord(PROVIDER_ID, recordId, REVISION_NAME, new Date()));
  }

  @Test
  void getRecord_getFile_InputStream_throws_IOException() throws Exception {
    String recordId = "recordId";

    CloudId cloudId = new CloudId(recordId, new LocalId(PROVIDER_ID, recordId));
    when(uisClient.getCloudId(PROVIDER_ID, recordId)).thenReturn(cloudId);

    File fileMock = mock(File.class);
    when(fileMock.getContentUri()).thenReturn(URI.create("file://fake/path/to/file.xml"));
    Representation representationMock = mock(Representation.class);
    when(representationMock.getFiles()).thenReturn(List.of(fileMock));
    when(recordServiceClient
        .getRepresentationsByRevision(eq(recordId), eq(MetisPlugin.getRepresentationName()), any(
            Revision.class))).thenReturn(List.of(representationMock));

    InputStream throwingStreamMock = mock(InputStream.class);
    when(throwingStreamMock.read(any(byte[].class), anyInt(), anyInt())).thenThrow(new IOException());
    when(fileServiceClient.getFile(anyString())).thenReturn(throwingStreamMock);

    assertThrows(ExternalTaskException.class,
        () -> ecloudEngineDatasetRecordClient.getRecord(PROVIDER_ID, recordId, REVISION_NAME, new Date()));
  }

  @Test
  void getRecord_representation_getFiles_blank_empty_list() throws Exception {
    String recordId = "recordId";

    CloudId cloudId = new CloudId(recordId, new LocalId(PROVIDER_ID, recordId));
    when(uisClient.getCloudId(PROVIDER_ID, recordId)).thenReturn(cloudId);

    Representation representationMock = mock(Representation.class);
    when(representationMock.getFiles()).thenReturn(null).thenReturn(List.of());

    when(recordServiceClient
        .getRepresentationsByRevision(eq(recordId), eq(MetisPlugin.getRepresentationName()), any(
            Revision.class))).thenReturn(List.of(representationMock));

    assertThrows(ExternalTaskException.class,
        () -> ecloudEngineDatasetRecordClient.getRecord(PROVIDER_ID, recordId, REVISION_NAME, new Date()));
    assertThrows(ExternalTaskException.class,
        () -> ecloudEngineDatasetRecordClient.getRecord(PROVIDER_ID, recordId, REVISION_NAME, new Date()));
  }

  @Test
  void getRecord_representations_blank_then_throws() throws Exception {
    String recordId = "recordId";

    CloudId cloudId = new CloudId(recordId, new LocalId(PROVIDER_ID, recordId));
    when(uisClient.getCloudId(PROVIDER_ID, recordId)).thenReturn(cloudId);

    when(recordServiceClient
        .getRepresentationsByRevision(eq(recordId), eq(MetisPlugin.getRepresentationName()), any(
            Revision.class))).thenReturn(null).thenReturn(List.of()).thenThrow(new MCSException());

    assertNull(ecloudEngineDatasetRecordClient.getRecord(PROVIDER_ID, recordId, REVISION_NAME, new Date()));
    assertNull(ecloudEngineDatasetRecordClient.getRecord(PROVIDER_ID, recordId, REVISION_NAME, new Date()));
    assertThrows(ExternalTaskException.class,
        () -> ecloudEngineDatasetRecordClient.getRecord(PROVIDER_ID, recordId, REVISION_NAME, new Date()));
  }

  @Test
  void getRecord_uisClient_throws() throws Exception {
    String recordId = "recordId";

    when(uisClient.getCloudId(PROVIDER_ID, recordId)).thenThrow(new CloudException("", new IllegalStateException()));
    assertThrows(ExternalTaskException.class,
        () -> ecloudEngineDatasetRecordClient.getRecord(PROVIDER_ID, recordId, REVISION_NAME, new Date()));
  }

  @Test
  void getRecord_uisClient_throws_with_cause_RecordDoesNotExistException_verify_success() throws Exception {
    String recordId = "recordId";

    when(uisClient.getCloudId(PROVIDER_ID, recordId)).thenThrow(
        new CloudException("", new RecordDoesNotExistException(new ErrorInfo())));

    CloudId cloudId = new CloudId(recordId, new LocalId(PROVIDER_ID, recordId));
    ResultSlice<CloudId> resultSlice = new ResultSlice<>("nextSlice", List.of(cloudId));
    when(uisClient.getRecordId(recordId)).thenReturn(resultSlice);

    File fileMock = mock(File.class);
    when(fileMock.getContentUri()).thenReturn(URI.create("file://fake/path/to/file.xml"));
    Representation representationMock = mock(Representation.class);
    when(representationMock.getFiles()).thenReturn(List.of(fileMock));
    when(recordServiceClient
        .getRepresentationsByRevision(eq(recordId), eq(MetisPlugin.getRepresentationName()), any(
            Revision.class))).thenReturn(List.of(representationMock));

    String recordContent = "recordContent";
    when(fileServiceClient.getFile(anyString())).thenReturn(new ByteArrayInputStream(recordContent.getBytes()));

    Record recordItem = ecloudEngineDatasetRecordClient.getRecord(PROVIDER_ID, recordId, REVISION_NAME, new Date());
    assertEquals(recordId, recordItem.getEcloudId());
    assertEquals(recordContent, recordItem.getXmlRecord());
  }

  @Test
  void getRecord_uisClient_throws_with_cause_RecordDoesNotExistException_verify_fail() throws Exception {
    String recordId = "recordId";

    when(uisClient.getCloudId(PROVIDER_ID, recordId)).thenThrow(
        new CloudException("", new RecordDoesNotExistException(new ErrorInfo())));

    ResultSlice<CloudId> resultSlice = new ResultSlice<>("nextSlice", List.of());
    when(uisClient.getRecordId(recordId)).thenReturn(resultSlice).thenThrow(new CloudException("", new IllegalStateException()));

    assertNull(ecloudEngineDatasetRecordClient.getRecord(PROVIDER_ID, recordId, REVISION_NAME, new Date()));
    assertNull(ecloudEngineDatasetRecordClient.getRecord(PROVIDER_ID, recordId, REVISION_NAME, new Date()));
  }

  @Test
  void getRecord_recordId_null() throws Exception {
    assertNull(ecloudEngineDatasetRecordClient.getRecord(PROVIDER_ID, null, REVISION_NAME, new Date()));
  }
}