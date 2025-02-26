package eu.europeana.metis.core.service;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.WireMockServer;
import eu.europeana.metis.core.dao.DatasetDao;
import eu.europeana.metis.core.dao.DatasetXsltDao;
import eu.europeana.metis.core.dao.ScheduledWorkflowDao;
import eu.europeana.metis.core.dao.WorkflowDao;
import eu.europeana.metis.core.dao.WorkflowExecutionDao;
import eu.europeana.metis.core.dataset.Dataset;
import eu.europeana.metis.core.dataset.DatasetSearchView;
import eu.europeana.metis.core.dataset.DatasetXslt;
import eu.europeana.metis.core.exceptions.DatasetAlreadyExistsException;
import eu.europeana.metis.core.exceptions.NoDatasetFoundException;
import eu.europeana.metis.core.exceptions.NoXsltFoundException;
import eu.europeana.metis.core.rest.Record;
import eu.europeana.metis.core.utils.TestObjectFactory;
import eu.europeana.metis.exception.BadContentException;
import eu.europeana.metis.exception.GenericMetisException;
import eu.europeana.metis.network.NetworkUtil;
import eu.europeana.metis.utils.RestEndpoints;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

class TestDatasetService {

  private static int portForWireMock = 9999;

  static {
    try {
      portForWireMock = new NetworkUtil().getAvailableLocalPort();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static WireMockServer wireMockServer;

  private DatasetDao datasetDao;
  private DatasetXsltDao datasetXsltDao;
  private WorkflowExecutionDao workflowExecutionDao;
  private ScheduledWorkflowDao scheduledWorkflowDao;
  private DatasetService datasetService;
  private RedissonClient redissonClient;

  private static final String DATASET_CREATION_LOCK = "datasetCreationLock";

  @BeforeAll
  static void setUp() {
    wireMockServer = new WireMockServer(wireMockConfig().port(portForWireMock));
    wireMockServer.start();
  }

  @AfterAll
  static void destroy() {
    wireMockServer.stop();
  }

  private static void expectException(Class<? extends GenericMetisException> exceptionType,
      TestAction action) throws GenericMetisException {
    try {
      action.test();
      fail("");
    } catch (GenericMetisException e) {
      if (!e.getClass().equals(exceptionType)) {
        throw e;
      }
    }
  }

  private interface TestAction {

    void test() throws GenericMetisException;
  }

  @BeforeEach
  void prepare() {
    datasetDao = mock(DatasetDao.class);
    datasetXsltDao = mock(DatasetXsltDao.class);
    WorkflowDao workflowDao = mock(WorkflowDao.class);
    workflowExecutionDao = mock(WorkflowExecutionDao.class);
    scheduledWorkflowDao = mock(ScheduledWorkflowDao.class);
    redissonClient = mock(RedissonClient.class);

    datasetService = new DatasetService(datasetDao, datasetXsltDao, workflowDao, workflowExecutionDao, scheduledWorkflowDao,
        redissonClient);
    datasetService.setMetisCoreUrl(String.format("http://localhost:%d", portForWireMock));
  }

  @Test
  void testCreateDataset() throws Exception {
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    RLock rlock = mock(RLock.class);
    when(redissonClient.getFairLock(DATASET_CREATION_LOCK)).thenReturn(rlock);
    when(datasetDao.getDatasetByOrganizationIdAndDatasetName(dataset.getOrganizationId(), dataset.getDatasetName())).thenReturn(
        null);
    when(datasetDao.findNextInSequenceDatasetId()).thenReturn(1);
    datasetService.createDataset(TestObjectFactory.USER_ID, dataset);
    ArgumentCaptor<Dataset> datasetArgumentCaptor = ArgumentCaptor.forClass(Dataset.class);
    verify(datasetDao, times(1)).create(datasetArgumentCaptor.capture());
    verify(datasetDao, times(1)).create(any(Dataset.class));
    assertEquals(dataset.getDatasetName(), datasetArgumentCaptor.getValue().getDatasetName());
    assertEquals(TestObjectFactory.USER_ID, datasetArgumentCaptor.getValue().getCreatedByUserId());
    assertEquals(DatasetDao.ORGANIZATION_ID, datasetArgumentCaptor.getValue().getOrganizationId());
    assertEquals(DatasetDao.ORGANIZATION_NAME, datasetArgumentCaptor.getValue().getOrganizationName());
  }

  @Test
  void testCreateDatasetAlreadyExists() throws Exception {
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);

    RLock rlock = mock(RLock.class);
    when(redissonClient.getFairLock(DATASET_CREATION_LOCK)).thenReturn(rlock);
    when(datasetDao.getDatasetByOrganizationIdAndDatasetName(DatasetDao.ORGANIZATION_ID, dataset.getDatasetName())).thenReturn(dataset);
    expectException(DatasetAlreadyExistsException.class, () -> datasetService.createDataset(TestObjectFactory.USER_ID, dataset));
    verify(datasetDao, times(0)).create(any(Dataset.class));
    verify(datasetDao, times(0)).getById(null);
  }

  @Test
  void testUpdateDataset() throws Exception {
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    dataset.setProvider("newProvider");
    Dataset storedDataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    storedDataset.setUpdatedDate(new Date(-1000));
    storedDataset.setOrganizationId(DatasetDao.ORGANIZATION_ID);
    when(workflowExecutionDao.existsAndNotCompleted(dataset.getDatasetId())).thenReturn(null);
    when(datasetDao.getDatasetOrThrow(dataset.getDatasetId())).thenReturn(storedDataset);
    when(datasetXsltDao.create(any(DatasetXslt.class))).thenReturn(TestObjectFactory.DATASET_XSLT);
    datasetService.updateDataset(dataset,
        TestObjectFactory.createXslt(TestObjectFactory.createDataset(dataset.getDatasetName())).getXslt());

    ArgumentCaptor<Dataset> dataSetArgumentCaptor = ArgumentCaptor.forClass(Dataset.class);
    verify(datasetDao, times(1)).update(dataSetArgumentCaptor.capture());
    assertEquals(dataset.getProvider(), dataSetArgumentCaptor.getValue().getProvider());
    assertEquals(dataset.getUpdatedDate(), dataSetArgumentCaptor.getValue().getUpdatedDate());
    assertEquals(storedDataset.getCreatedByUserId(), dataSetArgumentCaptor.getValue().getCreatedByUserId());
    assertNotEquals(storedDataset.getUpdatedDate(), dataSetArgumentCaptor.getValue().getUpdatedDate());
  }

  @Test
  void testUpdateDatasetNonNullXslt() throws Exception {
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    dataset.setProvider("newProvider");
    Dataset storedDataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    storedDataset.setUpdatedDate(new Date(-1000));
    storedDataset.setOrganizationId(DatasetDao.ORGANIZATION_ID);
    when(workflowExecutionDao.existsAndNotCompleted(dataset.getDatasetId())).thenReturn(null);
    when(datasetDao.getDatasetOrThrow(dataset.getDatasetId())).thenReturn(storedDataset);
    when(datasetXsltDao.create(any(DatasetXslt.class))).thenReturn(TestObjectFactory.DATASET_XSLT);
    datasetService.updateDataset(dataset, null);

    ArgumentCaptor<Dataset> dataSetArgumentCaptor = ArgumentCaptor.forClass(Dataset.class);
    verify(datasetDao, times(1)).update(dataSetArgumentCaptor.capture());
    assertEquals(dataset.getProvider(), dataSetArgumentCaptor.getValue().getProvider());
    assertEquals(dataset.getUpdatedDate(), dataSetArgumentCaptor.getValue().getUpdatedDate());
    assertEquals(storedDataset.getCreatedByUserId(), dataSetArgumentCaptor.getValue().getCreatedByUserId());
    assertNotEquals(storedDataset.getUpdatedDate(), dataSetArgumentCaptor.getValue().getUpdatedDate());
  }

  @Test
  void testUpdateDatasetDatasetAlreadyExistsException() throws NoDatasetFoundException {
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    dataset.setOrganizationId(DatasetDao.ORGANIZATION_ID);
    Dataset storedDataset = TestObjectFactory.createDataset(String.format("%s%s", TestObjectFactory.DATASETNAME, 10));
    storedDataset.setOrganizationId(DatasetDao.ORGANIZATION_ID);
    when(datasetDao.getDatasetOrThrow(dataset.getDatasetId())).thenReturn(storedDataset);
    when(datasetDao.getDatasetByOrganizationIdAndDatasetName(dataset.getOrganizationId(), dataset.getDatasetName())).thenReturn(
        new Dataset());
    assertThrows(DatasetAlreadyExistsException.class, () -> datasetService.updateDataset(dataset, null));
  }

  @Test
  void testUpdateDatasetDatasetExecutionIsActive() throws NoDatasetFoundException {
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    dataset.setOrganizationId(DatasetDao.ORGANIZATION_ID);
    when(datasetDao.getDatasetOrThrow(dataset.getDatasetId())).thenReturn(dataset);
    when(workflowExecutionDao.existsAndNotCompleted(dataset.getDatasetId())).thenReturn("ObjectId");
    assertThrows(BadContentException.class, () -> datasetService.updateDataset(dataset, null));
  }

  @Test
  void testUpdateDatasetNoDatasetFoundException() throws NoDatasetFoundException {
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    when(datasetDao.getDatasetOrThrow(dataset.getDatasetId())).thenThrow(new NoDatasetFoundException(dataset.getDatasetId()));
    assertThrows(NoDatasetFoundException.class, () -> datasetService.updateDataset(dataset, null));
  }

  @Test
  void testDeleteDatasetByDatasetId() throws Exception {
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    dataset.setOrganizationId(DatasetDao.ORGANIZATION_ID);
    when(workflowExecutionDao.existsAndNotCompleted(Integer.toString(TestObjectFactory.DATASETID))).thenReturn(null);
    datasetService.deleteDatasetByDatasetId(Integer.toString(TestObjectFactory.DATASETID));
    verify(datasetDao, times(1)).deleteByDatasetId(Integer.toString(TestObjectFactory.DATASETID));
    verify(workflowExecutionDao, times(1)).deleteAllByDatasetId(Integer.toString(TestObjectFactory.DATASETID));
    verify(scheduledWorkflowDao, times(1)).deleteAllByDatasetId(Integer.toString(TestObjectFactory.DATASETID));
  }

  @Test
  void testDeleteDatasetByDatasetIdNoDatasetFoundException() throws Exception {
    final String datasetId = Integer.toString(TestObjectFactory.DATASETID);
    when(datasetDao.getDatasetOrThrow(datasetId)).thenThrow(new NoDatasetFoundException(datasetId));
    expectException(NoDatasetFoundException.class, () -> datasetService.deleteDatasetByDatasetId(datasetId));
    verify(datasetDao, times(0)).deleteByDatasetId(datasetId);
  }

  @Test
  void testDeleteDatasetDatasetExecutionIsActive() {
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    dataset.setOrganizationId(DatasetDao.ORGANIZATION_ID);
    when(workflowExecutionDao.existsAndNotCompleted(Integer.toString(TestObjectFactory.DATASETID)))
        .thenReturn("ObjectId");
    assertThrows(BadContentException.class, () -> datasetService
        .deleteDatasetByDatasetId(Integer.toString(TestObjectFactory.DATASETID)));
  }

  @Test
  void testGetDatasetByDatasetName() throws Exception {
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    dataset.setOrganizationId(DatasetDao.ORGANIZATION_ID);
    when(datasetDao.getDatasetByDatasetName(dataset.getDatasetName())).thenReturn(dataset);
    Dataset returnedDataset = datasetService.getDatasetByDatasetName(TestObjectFactory.DATASETNAME);
    assertNotNull(returnedDataset);
  }

  @Test
  void testGetDatasetByDatasetNameNoDatasetFoundException() {
    assertThrows(NoDatasetFoundException.class,
        () -> datasetService.getDatasetByDatasetName(TestObjectFactory.DATASETNAME));
  }

  @Test
  void testGetDatasetByDatasetId() throws Exception {
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    dataset.setOrganizationId(DatasetDao.ORGANIZATION_ID);
    when(datasetDao.getDatasetOrThrow(dataset.getDatasetId())).thenReturn(dataset);
    Dataset returnedDataset = datasetService.getDatasetByDatasetId(dataset.getDatasetId());
    assertNotNull(returnedDataset);
  }

  @Test
  void testGetDatasetByDatasetIdNoDatasetFoundException() throws NoDatasetFoundException {
    final String datasetId = Integer.toString(TestObjectFactory.DATASETID);
    when(datasetDao.getDatasetOrThrow(datasetId)).thenThrow(new NoDatasetFoundException(datasetId));
    assertThrows(NoDatasetFoundException.class, () -> datasetService.getDatasetByDatasetId(datasetId));
  }

  @Test
  void getDatasetXsltByDatasetId() throws Exception {
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    dataset.setOrganizationId(DatasetDao.ORGANIZATION_ID);
    dataset.setXsltId(new ObjectId());
    DatasetXslt datasetXslt = TestObjectFactory.createXslt(dataset);
    when(datasetDao.getDatasetOrThrow(dataset.getDatasetId())).thenReturn(dataset);
    when(datasetXsltDao.getById(dataset.getXsltId().toString())).thenReturn(datasetXslt);

    DatasetXslt datasetXsltByDatasetId = datasetService.getDatasetXsltByDatasetId(dataset.getDatasetId());
    assertEquals(datasetXslt.getXslt(), datasetXsltByDatasetId.getXslt());
    assertEquals(datasetXslt.getDatasetId(), datasetXsltByDatasetId.getDatasetId());
  }

  @Test
  void getDatasetXsltByDatasetIdNoXsltFoundException() throws NoDatasetFoundException {
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    dataset.setOrganizationId(DatasetDao.ORGANIZATION_ID);
    when(datasetDao.getDatasetOrThrow(dataset.getDatasetId())).thenReturn(dataset);
    when(datasetXsltDao.getById(anyString())).thenReturn(null);
    assertThrows(NoXsltFoundException.class, () -> datasetService.getDatasetXsltByDatasetId(dataset.getDatasetId()));
  }

  @Test
  void getDatasetXsltByDatasetIdNoDatasetFoundException() throws NoDatasetFoundException {
    final String datasetId = Integer.toString(TestObjectFactory.DATASETID);
    when(datasetDao.getDatasetOrThrow(datasetId)).thenThrow(new NoDatasetFoundException(datasetId));
    assertThrows(NoDatasetFoundException.class, () -> datasetService.getDatasetXsltByDatasetId(datasetId));
  }

  @Test
  void getDatasetXsltByXsltId() throws Exception {
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    DatasetXslt datasetXslt = TestObjectFactory.createXslt(dataset);
    when(datasetXsltDao.getById(TestObjectFactory.DATASET_XSLT.getId().toString())).thenReturn(datasetXslt);

    DatasetXslt datasetXsltByDatasetId = datasetService
        .getDatasetXsltByXsltId(TestObjectFactory.DATASET_XSLT.getId().toString());
    assertEquals(datasetXslt.getXslt(), datasetXsltByDatasetId.getXslt());
    assertEquals(datasetXslt.getDatasetId(), datasetXsltByDatasetId.getDatasetId());
  }

  @Test
  void getDatasetXsltByXsltIdNoXsltFoundException() {
    when(datasetXsltDao.getById(TestObjectFactory.DATASET_XSLT.getId().toString())).thenReturn(null);
    assertThrows(NoXsltFoundException.class,
        () -> datasetService.getDatasetXsltByXsltId(TestObjectFactory.DATASET_XSLT.getId().toString()));
  }

  @Test
  void createDefaultXslt() {
    DatasetXslt datasetXslt = TestObjectFactory
        .createXslt(TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME));
    datasetXslt.setDatasetId("-1");
    when(datasetXsltDao.create(any(DatasetXslt.class))).thenReturn(datasetXslt);
    DatasetXslt defaultDatasetXslt = datasetService
        .createDefaultXslt(datasetXslt.getXslt());
    assertEquals(datasetXslt.getDatasetId(), defaultDatasetXslt.getDatasetId());
  }

  @Test
  void getLatestDefaultXslt() throws Exception {
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    DatasetXslt datasetXslt = TestObjectFactory.createXslt(dataset);
    when(datasetXsltDao.getLatestDefaultXslt()).thenReturn(datasetXslt);

    DatasetXslt datasetXsltByDatasetId = datasetService.getLatestDefaultXslt();
    assertEquals(datasetXslt.getXslt(), datasetXsltByDatasetId.getXslt());
    assertEquals(datasetXslt.getDatasetId(), datasetXsltByDatasetId.getDatasetId());
  }

  @Test
  void getLatestDefaultXsltNoXsltFoundException() {
    when(datasetXsltDao.create(TestObjectFactory.DATASET_XSLT)).thenReturn(null);
    assertThrows(NoXsltFoundException.class, () -> datasetService.getLatestDefaultXslt());
  }

  @Test
  void transformRecordsUsingLatestDefaultXslt() throws Exception {
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    dataset.setOrganizationId(DatasetDao.ORGANIZATION_ID);
    DatasetXslt datasetXslt = TestObjectFactory.createXslt(dataset);
    when(datasetDao.getDatasetOrThrow(dataset.getDatasetId())).thenReturn(dataset);
    when(datasetXsltDao.getLatestDefaultXslt()).thenReturn(datasetXslt);
    List<Record> listOfRecords = TestObjectFactory.createListOfRecords(5);
    listOfRecords.getFirst().setXmlRecord("invalid xml");

    String xsltUrl = RestEndpoints.resolve(RestEndpoints.DATASETS_XSLT_XSLTID,
        Collections.singletonList(datasetXslt.getId().toString()));
    wireMockServer.stubFor(get(urlEqualTo(xsltUrl))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "text/plain")
            .withBody(datasetXslt.getXslt())));

    List<Record> records = datasetService.transformRecordsUsingLatestDefaultXslt(dataset.getDatasetId(), listOfRecords);
    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
    Document doc;
    assertFalse(records.getFirst().getXmlRecord().contains("edm:ProvidedCHO")); //First record is invalid
    for (int i = 1; i < records.size(); i++) {
      doc = dBuilder.parse(new InputSource(new StringReader(records.get(i).getXmlRecord())));
      assertEquals(1, doc.getElementsByTagName("edm:ProvidedCHO").getLength());
      assertTrue(doc.getElementsByTagName("edm:ProvidedCHO").item(0).getAttributes()
                    .getNamedItem("rdf:about").getTextContent().contains(Integer.toString(i)));
    }
  }

  @Test
  void transformRecordsUsingLatestDefaultXslt_NoXsltFoundException() {
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    dataset.setOrganizationId(DatasetDao.ORGANIZATION_ID);
    when(datasetDao.getDatasetByDatasetId(dataset.getDatasetId())).thenReturn(dataset);
    when(datasetXsltDao.getLatestDefaultXslt()).thenReturn(null);
    List<Record> listOfRecords = TestObjectFactory.createListOfRecords(1);
    assertThrows(NoXsltFoundException.class, () -> datasetService
        .transformRecordsUsingLatestDefaultXslt(dataset.getDatasetId(), listOfRecords));
  }

  @Test
  void transformRecordsUsingLatestDatasetXslt() throws Exception {
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    dataset.setOrganizationId(DatasetDao.ORGANIZATION_ID);
    dataset.setXsltId(new ObjectId());
    DatasetXslt datasetXslt = TestObjectFactory.createXslt(dataset);
    when(datasetDao.getDatasetOrThrow(dataset.getDatasetId())).thenReturn(dataset);
    when(datasetXsltDao.getById(dataset.getXsltId().toString())).thenReturn(datasetXslt);
    List<Record> listOfRecords = TestObjectFactory.createListOfRecords(5);

    String xsltUrl = RestEndpoints.resolve(RestEndpoints.DATASETS_XSLT_XSLTID,
        Collections.singletonList(datasetXslt.getId().toString()));
    wireMockServer.stubFor(get(urlEqualTo(xsltUrl))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "text/plain")
            .withBody(datasetXslt.getXslt())));

    List<Record> records = datasetService.transformRecordsUsingLatestDatasetXslt(dataset.getDatasetId(), listOfRecords);
    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
    Document doc;
    for (int i = 0; i < records.size(); i++) {
      doc = dBuilder.parse(new InputSource(new StringReader(records.get(i).getXmlRecord())));
      assertEquals(1, doc.getElementsByTagName("edm:ProvidedCHO").getLength());
      assertTrue(doc.getElementsByTagName("edm:ProvidedCHO").item(0).getAttributes()
                    .getNamedItem("rdf:about").getTextContent().contains(Integer.toString(i)));
    }
  }

  @Test
  void transformRecordsUsingLatestDatasetXslt_NoXsltFoundException() throws NoDatasetFoundException {
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    dataset.setOrganizationId(DatasetDao.ORGANIZATION_ID);
    when(datasetDao.getDatasetOrThrow(dataset.getDatasetId())).thenReturn(dataset);
    List<Record> listOfRecords = TestObjectFactory.createListOfRecords(1);
    assertThrows(NoXsltFoundException.class,
        () -> datasetService.transformRecordsUsingLatestDatasetXslt(dataset.getDatasetId(), listOfRecords));
  }

  @Test
  void testGetAllDatasetsByProvider() {
    List<Dataset> list = new ArrayList<>();
    String provider = "myProvider";
    int nextPage = 1;
    when(datasetDao.getAllDatasetsByProvider(provider, nextPage)).thenReturn(list);
    List<Dataset> retList = datasetService.getAllDatasetsByProvider(provider, nextPage);
    assertSame(list, retList);
  }

  @Test
  void testGetAllDatasetsByIntermediateProvider() {
    List<Dataset> list = new ArrayList<>();
    String provider = "myProvider";
    int nextPage = 1;
    when(datasetDao.getAllDatasetsByIntermediateProvider(provider, nextPage)).thenReturn(list);
    List<Dataset> retList = datasetService
        .getAllDatasetsByIntermediateProvider(provider, nextPage);
    assertSame(list, retList);
  }

  @Test
  void testGetAllDatasetsByDataProvider() {
    List<Dataset> list = new ArrayList<>();
    String provider = "myProvider";
    int nextPage = 1;
    when(datasetDao.getAllDatasetsByDataProvider(provider, nextPage)).thenReturn(list);
    List<Dataset> retList = datasetService
        .getAllDatasetsByDataProvider(provider, nextPage);
    assertSame(list, retList);
  }

  @Test
  void testGetAllDatasetsByOrganizationId() {
    List<Dataset> list = new ArrayList<>();
    String organizationId = "organizationId";
    int nextPage = 1;
    when(datasetDao.getAllDatasetsByOrganizationId(organizationId, nextPage)).thenReturn(list);
    List<Dataset> retList = datasetService
        .getAllDatasetsByOrganizationId(organizationId, nextPage);
    assertSame(list, retList);
  }

  @Test
  void testGetAllDatasetsByOrganizationName() {
    List<Dataset> list = new ArrayList<>();
    String organizationName = "organizationName";
    int nextPage = 1;
    when(datasetDao.getAllDatasetsByOrganizationName(organizationName, nextPage))
        .thenReturn(list);
    List<Dataset> retList = datasetService
        .getAllDatasetsByOrganizationName(organizationName, nextPage);
    assertSame(list, retList);
  }

  @Test
  void testSearchDatasetsBasedOnSearchString() throws Exception {
    List<Dataset> list = new ArrayList<>();
    String searchString = "test 0";
    int nextPage = 1;
    when(datasetDao.searchDatasetsBasedOnSearchString(Collections.singletonList("0"),
        Collections.singletonList("0"), nextPage)).thenReturn(list);
    List<DatasetSearchView> retList = datasetService
        .searchDatasetsBasedOnSearchString(searchString, nextPage);
    assertEquals(list.size(), retList.size());
  }

  @Test
  void testSearchDatasetsBasedOnSearchString_BadContentException() throws Exception {
    String searchString = "";
    int nextPage = 1;
    expectException(BadContentException.class,
        () -> datasetService.searchDatasetsBasedOnSearchString(searchString, nextPage));
  }

  @Test
  void testGetDatasetsPerRequestLimit() {
    when(datasetDao.getDatasetsPerRequest()).thenReturn(5);
    assertEquals(5, datasetService.getDatasetsPerRequestLimit());
  }
}

