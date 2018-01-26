package eu.europeana.metis.core.rest;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import eu.europeana.metis.CommonStringValues;
import eu.europeana.metis.authentication.rest.client.AuthenticationClient;
import eu.europeana.metis.authentication.user.AccountRole;
import eu.europeana.metis.authentication.user.MetisUser;
import eu.europeana.metis.core.common.Country;
import eu.europeana.metis.core.common.Language;
import eu.europeana.metis.core.dataset.Dataset;
import eu.europeana.metis.core.exceptions.BadContentException;
import eu.europeana.metis.core.exceptions.DatasetAlreadyExistsException;
import eu.europeana.metis.core.exceptions.NoDatasetFoundException;
import eu.europeana.metis.core.rest.exception.RestResponseExceptionHandler;
import eu.europeana.metis.core.service.DatasetService;
import eu.europeana.metis.core.test.utils.TestObjectFactory;
import eu.europeana.metis.core.test.utils.TestUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

public class TestDatasetController {

  private DatasetService datasetServiceMock;
  private AuthenticationClient authenticationClient;
  private MockMvc datasetControllerMock;

  @Before
  public void setUp() {
    datasetServiceMock = mock(DatasetService.class);
    authenticationClient = mock(AuthenticationClient.class);
    DatasetController datasetController = new DatasetController(datasetServiceMock,
        authenticationClient);
    datasetControllerMock = MockMvcBuilders
        .standaloneSetup(datasetController)
        .setControllerAdvice(new RestResponseExceptionHandler())
        .build();
  }

  @Test
  public void createDataset() throws Exception {
    MetisUser metisUser = TestObjectFactory.createMetisUser(TestObjectFactory.EMAIL);
    metisUser.setAccountRole(AccountRole.EUROPEANA_DATA_OFFICER);
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    when(authenticationClient.getUserByAccessTokenInHeader(TestObjectFactory.AUTHORIZATION_HEADER))
        .thenReturn(metisUser);
    when(datasetServiceMock.createDataset(any(MetisUser.class), any(Dataset.class)))
        .thenReturn(dataset);

    datasetControllerMock.perform(post("/datasets")
        .header("Authorization", TestObjectFactory.AUTHORIZATION_HEADER)
        .contentType(MediaType.APPLICATION_JSON_UTF8)
        .accept(MediaType.APPLICATION_JSON_UTF8)
        .content(TestUtils.convertObjectToJsonBytes(dataset)))
        .andExpect(status().is(201))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
        .andExpect(jsonPath("$.datasetName", is(TestObjectFactory.DATASETNAME)));
    verify(datasetServiceMock, times(1)).createDataset(any(MetisUser.class), any(Dataset.class));
  }

  @Test
  public void createDatasetInvalidUser() throws Exception {
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    when(authenticationClient.getUserByAccessTokenInHeader(TestObjectFactory.AUTHORIZATION_HEADER))
        .thenThrow(new BadContentException(CommonStringValues.WRONG_ACCESS_TOKEN));

    datasetControllerMock.perform(post("/datasets")
        .header("Authorization", TestObjectFactory.AUTHORIZATION_HEADER)
        .contentType(MediaType.APPLICATION_JSON_UTF8)
        .accept(MediaType.APPLICATION_JSON_UTF8)
        .content(TestUtils.convertObjectToJsonBytes(dataset)))
        .andExpect(status().is(406))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
        .andExpect(jsonPath("$.errorMessage", is(CommonStringValues.WRONG_ACCESS_TOKEN)));
    verify(datasetServiceMock, times(0)).createDataset(any(MetisUser.class), any(Dataset.class));
  }

  @Test
  public void createDataset_DatasetAlreadyExistsException_Returns409() throws Exception {
    MetisUser metisUser = TestObjectFactory.createMetisUser(TestObjectFactory.EMAIL);
    metisUser.setAccountRole(AccountRole.EUROPEANA_DATA_OFFICER);
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);

    when(authenticationClient.getUserByAccessTokenInHeader(TestObjectFactory.AUTHORIZATION_HEADER))
        .thenReturn(metisUser);
    doThrow(new DatasetAlreadyExistsException("Conflict"))
        .when(datasetServiceMock).createDataset(any(MetisUser.class), any(Dataset.class));

    datasetControllerMock.perform(post("/datasets")
        .header("Authorization", TestObjectFactory.AUTHORIZATION_HEADER)
        .accept(MediaType.APPLICATION_JSON_UTF8)
        .contentType(MediaType.APPLICATION_JSON_UTF8)
        .content(TestUtils.convertObjectToJsonBytes(dataset)))
        .andExpect(status().is(409))
        .andExpect(jsonPath("$.errorMessage", is("Conflict")));
  }

  @Test
  public void updateDataset_withValidData_Returns204() throws Exception {
    MetisUser metisUser = TestObjectFactory.createMetisUser(TestObjectFactory.EMAIL);
    metisUser.setAccountRole(AccountRole.EUROPEANA_DATA_OFFICER);
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    when(authenticationClient.getUserByAccessTokenInHeader(TestObjectFactory.AUTHORIZATION_HEADER))
        .thenReturn(metisUser);
    datasetControllerMock.perform(put("/datasets")
        .header("Authorization", TestObjectFactory.AUTHORIZATION_HEADER)
        .accept(MediaType.APPLICATION_JSON_UTF8)
        .contentType(MediaType.APPLICATION_JSON_UTF8)
        .content(TestUtils.convertObjectToJsonBytes(dataset)))
        .andExpect(status().is(204))
        .andExpect(content().string(""));

    verify(datasetServiceMock, times(1)).updateDataset(any(MetisUser.class), any(Dataset.class));
  }

  @Test
  public void updateDataset_InvalidUser() throws Exception {
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    when(authenticationClient.getUserByAccessTokenInHeader(TestObjectFactory.AUTHORIZATION_HEADER))
        .thenThrow(new BadContentException(CommonStringValues.WRONG_ACCESS_TOKEN));
    datasetControllerMock.perform(put("/datasets")
        .header("Authorization", TestObjectFactory.AUTHORIZATION_HEADER)
        .accept(MediaType.APPLICATION_JSON_UTF8)
        .contentType(MediaType.APPLICATION_JSON_UTF8)
        .content(TestUtils.convertObjectToJsonBytes(dataset)))
        .andExpect(status().is(406))
        .andExpect(jsonPath("$.errorMessage", is(CommonStringValues.WRONG_ACCESS_TOKEN)));

    verify(datasetServiceMock, times(0)).updateDataset(any(MetisUser.class), any(Dataset.class));
  }

  @Test
  public void updateDataset_noDatasetFound_Returns404() throws Exception {
    MetisUser metisUser = TestObjectFactory.createMetisUser(TestObjectFactory.EMAIL);
    metisUser.setAccountRole(AccountRole.EUROPEANA_DATA_OFFICER);
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    when(authenticationClient.getUserByAccessTokenInHeader(TestObjectFactory.AUTHORIZATION_HEADER))
        .thenReturn(metisUser);
    doThrow(new NoDatasetFoundException("Does not exist")).when(datasetServiceMock)
        .updateDataset(any(MetisUser.class), any(Dataset.class));
    datasetControllerMock.perform(put("/datasets")
        .header("Authorization", TestObjectFactory.AUTHORIZATION_HEADER)
        .accept(MediaType.APPLICATION_JSON_UTF8)
        .contentType(MediaType.APPLICATION_JSON_UTF8)
        .content(TestUtils.convertObjectToJsonBytes(dataset)))
        .andExpect(status().is(404))
        .andExpect(jsonPath("$.errorMessage", is("Does not exist")));

    verify(datasetServiceMock, times(1)).updateDataset(any(MetisUser.class), any(Dataset.class));
  }

  @Test
  public void updateDataset_BadContentException_Returns406() throws Exception {
    MetisUser metisUser = TestObjectFactory.createMetisUser(TestObjectFactory.EMAIL);
    metisUser.setAccountRole(AccountRole.EUROPEANA_DATA_OFFICER);
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    when(authenticationClient.getUserByAccessTokenInHeader(TestObjectFactory.AUTHORIZATION_HEADER))
        .thenReturn(metisUser);
    doThrow(new BadContentException("Bad Content")).when(datasetServiceMock)
        .updateDataset(any(MetisUser.class), any(Dataset.class));
    datasetControllerMock.perform(put("/datasets")
        .header("Authorization", TestObjectFactory.AUTHORIZATION_HEADER)
        .accept(MediaType.APPLICATION_JSON_UTF8)
        .contentType(MediaType.APPLICATION_JSON_UTF8)
        .content(TestUtils.convertObjectToJsonBytes(dataset)))
        .andExpect(status().is(406))
        .andExpect(jsonPath("$.errorMessage", is("Bad Content")));

    verify(datasetServiceMock, times(1)).updateDataset(any(MetisUser.class), any(Dataset.class));
  }

  @Test
  public void deleteDataset() throws Exception {
    MetisUser metisUser = TestObjectFactory.createMetisUser(TestObjectFactory.EMAIL);
    metisUser.setAccountRole(AccountRole.EUROPEANA_DATA_OFFICER);
    when(authenticationClient.getUserByAccessTokenInHeader(TestObjectFactory.AUTHORIZATION_HEADER))
        .thenReturn(metisUser);
    datasetControllerMock.perform(delete(String.format("/datasets/%s", TestObjectFactory.DATASETID))
        .header("Authorization", TestObjectFactory.AUTHORIZATION_HEADER)
        .accept(MediaType.APPLICATION_JSON_UTF8)
        .contentType(TestUtils.APPLICATION_JSON_UTF8)
        .content(TestUtils.convertObjectToJsonBytes(null)))
        .andExpect(status().is(204))
        .andExpect(content().string(""));

    ArgumentCaptor<Integer> datasetIdArgumentCaptor = ArgumentCaptor.forClass(Integer.class);

    verify(datasetServiceMock, times(1))
        .deleteDatasetByDatasetId(any(MetisUser.class), datasetIdArgumentCaptor.capture());

    assertEquals(TestObjectFactory.DATASETID, datasetIdArgumentCaptor.getValue().intValue());
  }

  @Test
  public void deleteDatasetInvalidUser() throws Exception {
    when(authenticationClient.getUserByAccessTokenInHeader(TestObjectFactory.AUTHORIZATION_HEADER))
        .thenThrow(new BadContentException(CommonStringValues.WRONG_ACCESS_TOKEN));
    datasetControllerMock.perform(delete(String.format("/datasets/%s", TestObjectFactory.DATASETID))
        .header("Authorization", TestObjectFactory.AUTHORIZATION_HEADER)
        .accept(MediaType.APPLICATION_JSON_UTF8)
        .contentType(TestUtils.APPLICATION_JSON_UTF8)
        .content(TestUtils.convertObjectToJsonBytes(null)))
        .andExpect(status().is(406))
        .andExpect(jsonPath("$.errorMessage", is(CommonStringValues.WRONG_ACCESS_TOKEN)));
    verify(datasetServiceMock, times(0))
        .deleteDatasetByDatasetId(any(MetisUser.class), anyInt());
  }

  @Test
  public void deleteDataset_BadContentException_Returns406() throws Exception {
    MetisUser metisUser = TestObjectFactory.createMetisUser(TestObjectFactory.EMAIL);
    metisUser.setAccountRole(AccountRole.EUROPEANA_DATA_OFFICER);
    when(authenticationClient.getUserByAccessTokenInHeader(TestObjectFactory.AUTHORIZATION_HEADER))
        .thenReturn(metisUser);
    doThrow(new BadContentException("Bad Content")).when(datasetServiceMock)
        .deleteDatasetByDatasetId(metisUser, TestObjectFactory.DATASETID);
    datasetControllerMock.perform(delete(String.format("/datasets/%s", TestObjectFactory.DATASETID))
        .header("Authorization", TestObjectFactory.AUTHORIZATION_HEADER)
        .accept(MediaType.APPLICATION_JSON_UTF8)
        .contentType(TestUtils.APPLICATION_JSON_UTF8)
        .content(TestUtils.convertObjectToJsonBytes(null)))
        .andExpect(status().is(406))
        .andExpect(jsonPath("$.errorMessage", is("Bad Content")));
  }


  @Test
  public void getByDatasetId() throws Exception {
    MetisUser metisUser = TestObjectFactory.createMetisUser(TestObjectFactory.EMAIL);
    metisUser.setAccountRole(AccountRole.EUROPEANA_DATA_OFFICER);
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);

    when(authenticationClient.getUserByAccessTokenInHeader(TestObjectFactory.AUTHORIZATION_HEADER))
        .thenReturn(metisUser);
    when(datasetServiceMock.getDatasetByDatasetId(metisUser, TestObjectFactory.DATASETID))
        .thenReturn(dataset);
    datasetControllerMock.perform(get(String.format("/datasets/%s", TestObjectFactory.DATASETID))
        .header("Authorization", TestObjectFactory.AUTHORIZATION_HEADER)
        .contentType(TestUtils.APPLICATION_JSON_UTF8)
        .content(TestUtils.convertObjectToJsonBytes(null)))
        .andExpect(status().is(200))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
        .andExpect(jsonPath("$.datasetName", is(TestObjectFactory.DATASETNAME)))
        .andExpect(jsonPath("$.datasetId", is(TestObjectFactory.DATASETID)));

    ArgumentCaptor<Integer> datasetIdArgumentCaptor = ArgumentCaptor.forClass(Integer.class);
    verify(datasetServiceMock, times(1))
        .getDatasetByDatasetId(any(MetisUser.class), datasetIdArgumentCaptor.capture());
    assertEquals(TestObjectFactory.DATASETID, datasetIdArgumentCaptor.getValue().longValue());
  }

  @Test
  public void getByDatasetIdInvalidUser() throws Exception {
    when(authenticationClient.getUserByAccessTokenInHeader(TestObjectFactory.AUTHORIZATION_HEADER))
        .thenThrow(new BadContentException(CommonStringValues.WRONG_ACCESS_TOKEN));
    datasetControllerMock.perform(get(String.format("/datasets/%s", TestObjectFactory.DATASETID))
        .header("Authorization", TestObjectFactory.AUTHORIZATION_HEADER)
        .contentType(TestUtils.APPLICATION_JSON_UTF8)
        .content(TestUtils.convertObjectToJsonBytes(null)))
        .andExpect(status().is(406))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
        .andExpect(jsonPath("$.errorMessage", is(CommonStringValues.WRONG_ACCESS_TOKEN)));

    verify(datasetServiceMock, times(0))
        .getDatasetByDatasetId(any(MetisUser.class), anyInt());
  }

  @Test
  public void getByDatasetId_noDatasetFound_Returns404() throws Exception {
    MetisUser metisUser = TestObjectFactory.createMetisUser(TestObjectFactory.EMAIL);
    metisUser.setAccountRole(AccountRole.EUROPEANA_DATA_OFFICER);
    when(authenticationClient.getUserByAccessTokenInHeader(TestObjectFactory.AUTHORIZATION_HEADER))
        .thenReturn(metisUser);
    when(datasetServiceMock.getDatasetByDatasetId(metisUser, TestObjectFactory.DATASETID))
        .thenThrow(new NoDatasetFoundException("Does not exist"));
    datasetControllerMock.perform(get(String.format("/datasets/%s", TestObjectFactory.DATASETID))
        .header("Authorization", TestObjectFactory.AUTHORIZATION_HEADER)
        .contentType(TestUtils.APPLICATION_JSON_UTF8)
        .content(TestUtils.convertObjectToJsonBytes(null)))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
        .andExpect(status().is(404))
        .andExpect(jsonPath("$.errorMessage", is("Does not exist")));
  }

  @Test
  public void getByDatasetName() throws Exception {
    MetisUser metisUser = TestObjectFactory.createMetisUser(TestObjectFactory.EMAIL);
    metisUser.setAccountRole(AccountRole.EUROPEANA_DATA_OFFICER);
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);

    when(authenticationClient.getUserByAccessTokenInHeader(TestObjectFactory.AUTHORIZATION_HEADER))
        .thenReturn(metisUser);
    when(datasetServiceMock.getDatasetByDatasetName(metisUser, TestObjectFactory.DATASETNAME))
        .thenReturn(dataset);
    datasetControllerMock
        .perform(get(String.format("/datasets/dataset_name/%s", TestObjectFactory.DATASETNAME))
            .header("Authorization", TestObjectFactory.AUTHORIZATION_HEADER)
            .contentType(TestUtils.APPLICATION_JSON_UTF8)
            .content(TestUtils.convertObjectToJsonBytes(null)))
        .andExpect(status().is(200))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
        .andExpect(jsonPath("$.datasetName", is(TestObjectFactory.DATASETNAME)))
        .andExpect(jsonPath("$.datasetId", is(TestObjectFactory.DATASETID)));

    ArgumentCaptor<String> datasetNameArgumentCaptor = ArgumentCaptor.forClass(String.class);
    verify(datasetServiceMock, times(1))
        .getDatasetByDatasetName(any(MetisUser.class), datasetNameArgumentCaptor.capture());
    assertEquals(TestObjectFactory.DATASETNAME, datasetNameArgumentCaptor.getValue());
  }

  @Test
  public void getByDatasetNameInvalidUser() throws Exception {
    when(authenticationClient.getUserByAccessTokenInHeader(TestObjectFactory.AUTHORIZATION_HEADER))
        .thenThrow(new BadContentException(CommonStringValues.WRONG_ACCESS_TOKEN));
    datasetControllerMock
        .perform(get(String.format("/datasets/dataset_name/%s", TestObjectFactory.DATASETNAME))
            .header("Authorization", TestObjectFactory.AUTHORIZATION_HEADER)
            .contentType(TestUtils.APPLICATION_JSON_UTF8)
            .content(TestUtils.convertObjectToJsonBytes(null)))
        .andExpect(status().is(406))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
        .andExpect(jsonPath("$.errorMessage", is(CommonStringValues.WRONG_ACCESS_TOKEN)));

    verify(datasetServiceMock, times(0))
        .getDatasetByDatasetName(any(MetisUser.class), anyString());
  }


  @Test
  public void getByDatasetName_noDatasetFound_Returns404() throws Exception {
    MetisUser metisUser = TestObjectFactory.createMetisUser(TestObjectFactory.EMAIL);
    metisUser.setAccountRole(AccountRole.EUROPEANA_DATA_OFFICER);
    when(authenticationClient.getUserByAccessTokenInHeader(TestObjectFactory.AUTHORIZATION_HEADER))
        .thenReturn(metisUser);
    when(datasetServiceMock.getDatasetByDatasetName(metisUser, TestObjectFactory.DATASETNAME))
        .thenThrow(new NoDatasetFoundException("Does not exist"));
    datasetControllerMock
        .perform(get(String.format("/datasets/dataset_name/%s", TestObjectFactory.DATASETNAME))
            .header("Authorization", TestObjectFactory.AUTHORIZATION_HEADER)
            .contentType(TestUtils.APPLICATION_JSON_UTF8)
            .content(TestUtils.convertObjectToJsonBytes(null)))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
        .andExpect(status().is(404))
        .andExpect(jsonPath("$.errorMessage", is("Does not exist")));
  }

  @Test
  public void getAllDatasetsByProvider() throws Exception {
    MetisUser metisUser = TestObjectFactory.createMetisUser(TestObjectFactory.EMAIL);
    metisUser.setAccountRole(AccountRole.EUROPEANA_DATA_OFFICER);
    List<Dataset> datasetList = getDatasets();

    when(authenticationClient.getUserByAccessTokenInHeader(TestObjectFactory.AUTHORIZATION_HEADER))
        .thenReturn(metisUser);
    when(datasetServiceMock.getAllDatasetsByProvider(metisUser, "myProvider", 3))
        .thenReturn(datasetList);
    when(datasetServiceMock.getDatasetsPerRequestLimit()).thenReturn(5);

    datasetControllerMock.perform(get("/datasets/provider/myProvider")
        .header("Authorization", TestObjectFactory.AUTHORIZATION_HEADER)
        .param("nextPage", "3")
        .contentType(TestUtils.APPLICATION_JSON_UTF8)
        .content(TestUtils.convertObjectToJsonBytes(null)))
        .andExpect(status().is(200))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
        .andExpect(jsonPath("$.results", hasSize(2)))
        .andExpect(jsonPath("$.results[0].datasetId", is(TestObjectFactory.DATASETID + 1)))
        .andExpect(jsonPath("$.results[1].datasetId", is(TestObjectFactory.DATASETID + 2)));

    ArgumentCaptor<String> provider = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Integer> page = ArgumentCaptor.forClass(Integer.class);
    verify(datasetServiceMock, times(1))
        .getAllDatasetsByProvider(any(MetisUser.class), provider.capture(), page.capture());

    assertEquals("myProvider", provider.getValue());
    assertEquals(3, page.getValue().intValue());
  }

  @Test
  public void getAllDatasetsByProviderNegativeNextPage() throws Exception {
    datasetControllerMock.perform(get("/datasets/provider/myProvider")
        .header("Authorization", TestObjectFactory.AUTHORIZATION_HEADER)
        .param("nextPage", "-1")
        .contentType(TestUtils.APPLICATION_JSON_UTF8)
        .content(TestUtils.convertObjectToJsonBytes(null)))
        .andExpect(status().is(406));
  }

  @Test
  public void getAllDatasetsByProviderInvalidUser() throws Exception {
    when(authenticationClient.getUserByAccessTokenInHeader(TestObjectFactory.AUTHORIZATION_HEADER))
        .thenThrow(new BadContentException(CommonStringValues.WRONG_ACCESS_TOKEN));

    datasetControllerMock.perform(get("/datasets/provider/myProvider")
        .header("Authorization", TestObjectFactory.AUTHORIZATION_HEADER)
        .param("nextPage", "3")
        .contentType(TestUtils.APPLICATION_JSON_UTF8)
        .content(TestUtils.convertObjectToJsonBytes(null)))
        .andExpect(status().is(406))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
        .andExpect(jsonPath("$.errorMessage", is(CommonStringValues.WRONG_ACCESS_TOKEN)));

    verify(datasetServiceMock, times(0))
        .getAllDatasetsByProvider(any(MetisUser.class), anyString(), anyInt());
  }

  @Test
  public void getAllDatasetsByIntermediateProvider() throws Exception {
    MetisUser metisUser = TestObjectFactory.createMetisUser(TestObjectFactory.EMAIL);
    metisUser.setAccountRole(AccountRole.EUROPEANA_DATA_OFFICER);
    List<Dataset> datasetList = getDatasets();

    when(authenticationClient.getUserByAccessTokenInHeader(TestObjectFactory.AUTHORIZATION_HEADER))
        .thenReturn(metisUser);
    when(datasetServiceMock
        .getAllDatasetsByIntermediateProvider(metisUser, "myIntermediateProvider", 3))
        .thenReturn(datasetList);
    when(datasetServiceMock.getDatasetsPerRequestLimit()).thenReturn(5);

    datasetControllerMock.perform(get("/datasets/intermediate_provider/myIntermediateProvider")
        .header("Authorization", TestObjectFactory.AUTHORIZATION_HEADER)
        .param("nextPage", "3")
        .contentType(TestUtils.APPLICATION_JSON_UTF8)
        .content(TestUtils.convertObjectToJsonBytes(null)))
        .andExpect(status().is(200))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
        .andExpect(jsonPath("$.results", hasSize(2)))
        .andExpect(jsonPath("$.results[0].datasetId", is(TestObjectFactory.DATASETID + 1)))
        .andExpect(jsonPath("$.results[1].datasetId", is(TestObjectFactory.DATASETID + 2)));

    ArgumentCaptor<String> provider = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Integer> page = ArgumentCaptor.forClass(Integer.class);
    verify(datasetServiceMock, times(1))
        .getAllDatasetsByIntermediateProvider(any(MetisUser.class), provider.capture(),
            page.capture());

    assertEquals("myIntermediateProvider", provider.getValue());
    assertEquals(3, page.getValue().intValue());
  }

  @Test
  public void getAllDatasetsByIntermediateProviderNegativeNextPage() throws Exception {
    datasetControllerMock.perform(get("/datasets/intermediate_provider/myIntermediateProvider")
        .header("Authorization", TestObjectFactory.AUTHORIZATION_HEADER)
        .param("nextPage", "-1")
        .contentType(TestUtils.APPLICATION_JSON_UTF8)
        .content(TestUtils.convertObjectToJsonBytes(null)))
        .andExpect(status().is(406));
  }

  @Test
  public void getAllDatasetsByIntermediateProviderInvalidUser() throws Exception {
    when(authenticationClient.getUserByAccessTokenInHeader(TestObjectFactory.AUTHORIZATION_HEADER))
        .thenThrow(new BadContentException(CommonStringValues.WRONG_ACCESS_TOKEN));

    datasetControllerMock.perform(get("/datasets/intermediate_provider/myIntermediateProvider")
        .header("Authorization", TestObjectFactory.AUTHORIZATION_HEADER)
        .param("nextPage", "3")
        .contentType(TestUtils.APPLICATION_JSON_UTF8)
        .content(TestUtils.convertObjectToJsonBytes(null)))
        .andExpect(status().is(406))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
        .andExpect(jsonPath("$.errorMessage", is(CommonStringValues.WRONG_ACCESS_TOKEN)));

    verify(datasetServiceMock, times(0))
        .getAllDatasetsByIntermediateProvider(any(MetisUser.class), anyString(), anyInt());
  }

  @Test
  public void getAllDatasetsByDataProvider() throws Exception {
    MetisUser metisUser = TestObjectFactory.createMetisUser(TestObjectFactory.EMAIL);
    metisUser.setAccountRole(AccountRole.EUROPEANA_DATA_OFFICER);
    List<Dataset> datasetList = getDatasets();

    when(authenticationClient.getUserByAccessTokenInHeader(TestObjectFactory.AUTHORIZATION_HEADER))
        .thenReturn(metisUser);
    when(datasetServiceMock.getAllDatasetsByDataProvider(metisUser, "myDataProvider", 3))
        .thenReturn(datasetList);
    when(datasetServiceMock.getDatasetsPerRequestLimit()).thenReturn(5);

    datasetControllerMock.perform(get("/datasets/data_provider/myDataProvider")
        .header("Authorization", TestObjectFactory.AUTHORIZATION_HEADER)
        .param("nextPage", "3")
        .contentType(TestUtils.APPLICATION_JSON_UTF8)
        .content(TestUtils.convertObjectToJsonBytes(null)))
        .andExpect(status().is(200))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
        .andExpect(jsonPath("$.results", hasSize(2)))
        .andExpect(jsonPath("$.results[0].datasetId", is(TestObjectFactory.DATASETID + 1)))
        .andExpect(jsonPath("$.results[1].datasetId", is(TestObjectFactory.DATASETID + 2)));

    ArgumentCaptor<String> provider = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Integer> page = ArgumentCaptor.forClass(Integer.class);
    verify(datasetServiceMock, times(1))
        .getAllDatasetsByDataProvider(any(MetisUser.class), provider.capture(), page.capture());

    assertEquals("myDataProvider", provider.getValue());
    assertEquals(3, page.getValue().intValue());
  }

  @Test
  public void getAllDatasetsByDataProviderNegativeNextPage() throws Exception {
    datasetControllerMock.perform(get("/datasets/data_provider/myDataProvider")
        .header("Authorization", TestObjectFactory.AUTHORIZATION_HEADER)
        .param("nextPage", "-1")
        .contentType(TestUtils.APPLICATION_JSON_UTF8)
        .content(TestUtils.convertObjectToJsonBytes(null)))
        .andExpect(status().is(406));
  }

  @Test
  public void getAllDatasetsByDataProviderInvalidUser() throws Exception {
    when(authenticationClient.getUserByAccessTokenInHeader(TestObjectFactory.AUTHORIZATION_HEADER))
        .thenThrow(new BadContentException(CommonStringValues.WRONG_ACCESS_TOKEN));

    datasetControllerMock.perform(get("/datasets/data_provider/myDataProvider")
        .header("Authorization", TestObjectFactory.AUTHORIZATION_HEADER)
        .param("nextPage", "3")
        .contentType(TestUtils.APPLICATION_JSON_UTF8)
        .content(TestUtils.convertObjectToJsonBytes(null)))
        .andExpect(status().is(406))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
        .andExpect(jsonPath("$.errorMessage", is(CommonStringValues.WRONG_ACCESS_TOKEN)));

    verify(datasetServiceMock, times(0))
        .getAllDatasetsByDataProvider(any(MetisUser.class), anyString(), anyInt());
  }

  @Test
  public void getAllDatasetsByOrganizationId() throws Exception {
    MetisUser metisUser = TestObjectFactory.createMetisUser(TestObjectFactory.EMAIL);
    metisUser.setAccountRole(AccountRole.EUROPEANA_DATA_OFFICER);
    List<Dataset> datasetList = getDatasets();

    when(authenticationClient.getUserByAccessTokenInHeader(TestObjectFactory.AUTHORIZATION_HEADER))
        .thenReturn(metisUser);
    when(datasetServiceMock.getAllDatasetsByOrganizationId(metisUser, "myOrganizationId", 3))
        .thenReturn(datasetList);
    when(datasetServiceMock.getDatasetsPerRequestLimit()).thenReturn(5);

    datasetControllerMock.perform(get("/datasets/organization_id/myOrganizationId")
        .header("Authorization", TestObjectFactory.AUTHORIZATION_HEADER)
        .param("nextPage", "3")
        .contentType(TestUtils.APPLICATION_JSON_UTF8)
        .content(TestUtils.convertObjectToJsonBytes(null)))
        .andExpect(status().is(200))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
        .andExpect(jsonPath("$.results", hasSize(2)))
        .andExpect(jsonPath("$.results[0].datasetId", is(TestObjectFactory.DATASETID + 1)))
        .andExpect(jsonPath("$.results[1].datasetId", is(TestObjectFactory.DATASETID + 2)));

    ArgumentCaptor<String> provider = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Integer> page = ArgumentCaptor.forClass(Integer.class);
    verify(datasetServiceMock, times(1))
        .getAllDatasetsByOrganizationId(any(MetisUser.class), provider.capture(), page.capture());

    assertEquals("myOrganizationId", provider.getValue());
    assertEquals(3, page.getValue().intValue());
  }

  @Test
  public void getAllDatasetsByOrganizationIdNegativeNextPage() throws Exception {
    datasetControllerMock.perform(get("/datasets/organization_id/myOrganizationId")
        .header("Authorization", TestObjectFactory.AUTHORIZATION_HEADER)
        .param("nextPage", "-1")
        .contentType(TestUtils.APPLICATION_JSON_UTF8)
        .content(TestUtils.convertObjectToJsonBytes(null)))
        .andExpect(status().is(406));
  }

  @Test
  public void getAllDatasetsByOrganizationIdInvalidUser() throws Exception {
    when(authenticationClient.getUserByAccessTokenInHeader(TestObjectFactory.AUTHORIZATION_HEADER))
        .thenThrow(new BadContentException(CommonStringValues.WRONG_ACCESS_TOKEN));

    datasetControllerMock.perform(get("/datasets/organization_id/myOrganizationId")
        .header("Authorization", TestObjectFactory.AUTHORIZATION_HEADER)
        .param("nextPage", "3")
        .contentType(TestUtils.APPLICATION_JSON_UTF8)
        .content(TestUtils.convertObjectToJsonBytes(null)))
        .andExpect(status().is(406))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
        .andExpect(jsonPath("$.errorMessage", is(CommonStringValues.WRONG_ACCESS_TOKEN)));

    verify(datasetServiceMock, times(0))
        .getAllDatasetsByOrganizationId(any(MetisUser.class), anyString(), anyInt());
  }

  @Test
  public void getAllDatasetsByOrganizationName() throws Exception {
    MetisUser metisUser = TestObjectFactory.createMetisUser(TestObjectFactory.EMAIL);
    metisUser.setAccountRole(AccountRole.EUROPEANA_DATA_OFFICER);
    List<Dataset> datasetList = getDatasets();

    when(authenticationClient.getUserByAccessTokenInHeader(TestObjectFactory.AUTHORIZATION_HEADER))
        .thenReturn(metisUser);
    when(datasetServiceMock.getAllDatasetsByOrganizationName(metisUser, "myOrganizationName", 3))
        .thenReturn(datasetList);
    when(datasetServiceMock.getDatasetsPerRequestLimit()).thenReturn(5);

    datasetControllerMock.perform(get("/datasets/organization_name/myOrganizationName")
        .header("Authorization", TestObjectFactory.AUTHORIZATION_HEADER)
        .param("nextPage", "3")
        .contentType(TestUtils.APPLICATION_JSON_UTF8)
        .content(TestUtils.convertObjectToJsonBytes(null)))
        .andExpect(status().is(200))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
        .andExpect(jsonPath("$.results", hasSize(2)))
        .andExpect(jsonPath("$.results[0].datasetId", is(TestObjectFactory.DATASETID + 1)))
        .andExpect(jsonPath("$.results[1].datasetId", is(TestObjectFactory.DATASETID + 2)));

    ArgumentCaptor<String> provider = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Integer> page = ArgumentCaptor.forClass(Integer.class);
    verify(datasetServiceMock, times(1))
        .getAllDatasetsByOrganizationName(any(MetisUser.class), provider.capture(), page.capture());

    assertEquals("myOrganizationName", provider.getValue());
    assertEquals(3, page.getValue().intValue());
  }

  @Test
  public void getAllDatasetsByOrganizationNameNegativeNextPage() throws Exception {
    datasetControllerMock.perform(get("/datasets/organization_name/myOrganizationName")
        .header("Authorization", TestObjectFactory.AUTHORIZATION_HEADER)
        .param("nextPage", "-1")
        .contentType(TestUtils.APPLICATION_JSON_UTF8)
        .content(TestUtils.convertObjectToJsonBytes(null)))
        .andExpect(status().is(406));
  }

  @Test
  public void getAllDatasetsByOrganizationNameInvalidUser() throws Exception {
    when(authenticationClient.getUserByAccessTokenInHeader(TestObjectFactory.AUTHORIZATION_HEADER))
        .thenThrow(new BadContentException(CommonStringValues.WRONG_ACCESS_TOKEN));

    datasetControllerMock.perform(get("/datasets/organization_name/myOrganizationName")
        .header("Authorization", TestObjectFactory.AUTHORIZATION_HEADER)
        .param("nextPage", "3")
        .contentType(TestUtils.APPLICATION_JSON_UTF8)
        .content(TestUtils.convertObjectToJsonBytes(null)))
        .andExpect(status().is(406))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
        .andExpect(jsonPath("$.errorMessage", is(CommonStringValues.WRONG_ACCESS_TOKEN)));

    verify(datasetServiceMock, times(0))
        .getAllDatasetsByOrganizationName(any(MetisUser.class), anyString(), anyInt());
  }

  @Test
  public void getDatasetsCountries() throws Exception {
    MetisUser metisUser = TestObjectFactory.createMetisUser(TestObjectFactory.EMAIL);
    when(authenticationClient.getUserByAccessTokenInHeader(TestObjectFactory.AUTHORIZATION_HEADER))
        .thenReturn(metisUser);

    MvcResult mvcResult = datasetControllerMock.perform(get("/datasets/countries")
        .header("Authorization", TestObjectFactory.AUTHORIZATION_HEADER)
        .contentType(TestUtils.APPLICATION_JSON_UTF8)
        .content(""))
        .andExpect(status().is(200))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8)).andReturn();

    String resultListOfCountries = mvcResult.getResponse().getContentAsString();
    Object document = Configuration.defaultConfiguration().jsonProvider()
        .parse(resultListOfCountries);

    List<Map<String, Object>> mapListOfCountries = JsonPath.read(document, "$[*]");
    assertEquals(Country.values().length, mapListOfCountries.size());
    assertEquals(mapListOfCountries.get(22).get("enum"), Country.values()[22].name());
    assertEquals(mapListOfCountries.get(22).get("name"), Country.values()[22].getName());
    assertEquals(mapListOfCountries.get(22).get("isoCode"), Country.values()[22].getIsoCode());
  }

  @Test
  public void getDatasetsCountriesInvalidUser() throws Exception {
    when(authenticationClient.getUserByAccessTokenInHeader(TestObjectFactory.AUTHORIZATION_HEADER))
        .thenThrow(new BadContentException(CommonStringValues.WRONG_ACCESS_TOKEN));

    datasetControllerMock.perform(get("/datasets/countries")
        .header("Authorization", TestObjectFactory.AUTHORIZATION_HEADER)
        .contentType(TestUtils.APPLICATION_JSON_UTF8)
        .content(""))
        .andExpect(status().is(406))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
        .andExpect(jsonPath("$.errorMessage", is(CommonStringValues.WRONG_ACCESS_TOKEN)));
  }

  @Test
  public void getDatasetsLanguages() throws Exception {
    MetisUser metisUser = TestObjectFactory.createMetisUser(TestObjectFactory.EMAIL);
    when(
        authenticationClient.getUserByAccessTokenInHeader(TestObjectFactory.AUTHORIZATION_HEADER))
        .thenReturn(metisUser);

    MvcResult mvcResult = datasetControllerMock.perform(get("/datasets/languages")
        .header("Authorization", TestObjectFactory.AUTHORIZATION_HEADER)
        .contentType(TestUtils.APPLICATION_JSON_UTF8)
        .content(""))
        .andExpect(status().is(200))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8)).andReturn();

    String resultListOfLanguages = mvcResult.getResponse().getContentAsString();
    Object document = Configuration.defaultConfiguration().jsonProvider()
        .parse(resultListOfLanguages);

    List<Map<String, Object>> mapListOfLanguages = JsonPath.read(document, "$[*]");
    assertEquals(Language.values().length, mapListOfLanguages.size());
    assertEquals(mapListOfLanguages.get(10).get("enum"), Language.values()[10].name());
    assertEquals(mapListOfLanguages.get(10).get("name"), Language.values()[10].getName());
  }

  @Test
  public void getDatasetsLanguagesInvalidUser() throws Exception {
    when(authenticationClient.getUserByAccessTokenInHeader(TestObjectFactory.AUTHORIZATION_HEADER))
        .thenThrow(new BadContentException(CommonStringValues.WRONG_ACCESS_TOKEN));

    datasetControllerMock.perform(get("/datasets/languages")
        .header("Authorization", TestObjectFactory.AUTHORIZATION_HEADER)
        .contentType(TestUtils.APPLICATION_JSON_UTF8)
        .content(""))
        .andExpect(status().is(406))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
        .andExpect(jsonPath("$.errorMessage", is(CommonStringValues.WRONG_ACCESS_TOKEN)));
  }

  private List<Dataset> getDatasets() {
    List<Dataset> datasetList = new ArrayList<>();
    Dataset dataset1 = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    dataset1.setDatasetId(TestObjectFactory.DATASETID + 1);
    datasetList.add(dataset1);

    Dataset dataset2 = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    dataset2.setDatasetId(TestObjectFactory.DATASETID + 2);
    datasetList.add(dataset2);

    return datasetList;
  }


}