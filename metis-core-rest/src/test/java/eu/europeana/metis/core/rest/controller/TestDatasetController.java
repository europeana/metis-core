package eu.europeana.metis.core.rest.controller;

import static eu.europeana.metis.core.rest.utils.TestJwtUtils.BEARER;
import static eu.europeana.metis.core.rest.utils.TestJwtUtils.MOCK_INVALID_TOKEN;
import static eu.europeana.metis.core.rest.utils.TestJwtUtils.MOCK_VALID_TOKEN;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
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
import eu.europeana.metis.core.common.Language;
import eu.europeana.metis.core.dataset.Dataset;
import eu.europeana.metis.core.dataset.DatasetSearchView;
import eu.europeana.metis.core.dataset.DatasetXslt;
import eu.europeana.metis.core.dataset.DatasetXsltStringWrapper;
import eu.europeana.metis.core.exceptions.DatasetAlreadyExistsException;
import eu.europeana.metis.core.exceptions.NoDatasetFoundException;
import eu.europeana.metis.core.exceptions.NoXsltFoundException;
import eu.europeana.metis.core.rest.Record;
import eu.europeana.metis.core.rest.config.SecurityConfig;
import eu.europeana.metis.core.rest.config.properties.SecurityConfigurationProperties;
import eu.europeana.metis.core.rest.exception.RestResponseExceptionHandler;
import eu.europeana.metis.core.rest.utils.TestJwtUtils;
import eu.europeana.metis.core.rest.utils.TestObjectFactory;
import eu.europeana.metis.core.rest.utils.TestUtils;
import eu.europeana.metis.core.service.DatasetService;
import eu.europeana.metis.exception.BadContentException;
import eu.europeana.metis.utils.Country;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@WebMvcTest(DatasetController.class)
@ContextConfiguration(classes = {DatasetController.class, SecurityConfig.class, RestResponseExceptionHandler.class})
class TestDatasetController {

  @MockBean
  private DatasetService datasetService;

  @MockBean
  private JwtDecoder jwtDecoder;

  private static MockMvc mockMvc;

  private final TestJwtUtils testJwtUtils;

  @Autowired
  public TestDatasetController(SecurityConfigurationProperties securityConfigurationProperties) {
    testJwtUtils = new TestJwtUtils(securityConfigurationProperties.getResourceNames());
  }

  @BeforeAll
  static void setup(WebApplicationContext context) {
    mockMvc = MockMvcBuilders.webAppContextSetup(context)
                             .apply(SecurityMockMvcConfigurers.springSecurity())
                             .defaultRequest(get("/"))
                             .build();
  }

  @BeforeEach
  void cleanUp() {
    reset(datasetService);
    reset(jwtDecoder);
  }

  @Test
  void createDataset() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    when(datasetService.createDataset(any(String.class), any(Dataset.class))).thenReturn(dataset);

    mockMvc.perform(post("/secured/datasets")
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .contentType(MediaType.APPLICATION_JSON)
               .accept(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(dataset)))
           .andExpect(status().isCreated())
           .andExpect(content().contentType(MediaType.APPLICATION_JSON))
           .andExpect(jsonPath("$.datasetName", is(TestObjectFactory.DATASETNAME)));
    verify(datasetService, times(1)).createDataset(any(String.class), any(Dataset.class));
  }

  @Test
  void createDatasetUnauthenticated() throws Exception {
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);

    mockMvc.perform(post("/secured/datasets")
               .contentType(MediaType.APPLICATION_JSON)
               .accept(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(dataset)))
           .andExpect(status().isUnauthorized());
    verify(datasetService, times(0)).createDataset(any(String.class), any(Dataset.class));
  }

  @Test
  void createDatasetInvalidUser() throws Exception {
    when(jwtDecoder.decode(MOCK_INVALID_TOKEN)).thenReturn(testJwtUtils.getInvalidRoleJwt());
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);

    mockMvc.perform(post("/secured/datasets")
               .header("Authorization", BEARER + MOCK_INVALID_TOKEN)
               .contentType(MediaType.APPLICATION_JSON)
               .accept(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(dataset)))
           .andExpect(status().isForbidden());
    verify(datasetService, times(0)).createDataset(any(String.class), any(Dataset.class));
  }

  @Test
  void createDataset_DatasetAlreadyExistsException_Returns409() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    doThrow(new DatasetAlreadyExistsException("Conflict"))
        .when(datasetService).createDataset(any(String.class), any(Dataset.class));

    mockMvc.perform(post("/secured/datasets")
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .contentType(MediaType.APPLICATION_JSON)
               .accept(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(dataset)))

           .andExpect(status().isConflict())
           .andExpect(jsonPath("$.errorMessage", is("Conflict")));
    verify(datasetService, times(1)).createDataset(any(String.class), any(Dataset.class));
  }

  @Test
  void updateDataset_withValidData_Returns204() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    DatasetXsltStringWrapper datasetXsltStringWrapper = new DatasetXsltStringWrapper(dataset,
        "<xslt attribute:\"value\"></xslt>");
    mockMvc.perform(put("/secured/datasets")
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .accept(MediaType.APPLICATION_JSON)
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(datasetXsltStringWrapper)))
           .andExpect(status().isNoContent())
           .andExpect(content().string(""));
    verify(datasetService, times(1)).updateDataset(any(Dataset.class), anyString());
  }

  @Test
  void updateDataset_Unauthenticated() throws Exception {
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    mockMvc.perform(put("/secured/datasets")
               .accept(MediaType.APPLICATION_JSON)
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(dataset)))
           .andExpect(status().isUnauthorized());
    verify(datasetService, times(0)).updateDataset(any(Dataset.class), anyString());
  }

  @Test
  void updateDataset_InvalidUser() throws Exception {
    when(jwtDecoder.decode(MOCK_INVALID_TOKEN)).thenReturn(testJwtUtils.getInvalidRoleJwt());
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    mockMvc.perform(put("/secured/datasets")
               .header("Authorization", BEARER + MOCK_INVALID_TOKEN)
               .accept(MediaType.APPLICATION_JSON)
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(dataset)))
           .andExpect(status().isForbidden());
    verify(datasetService, times(0)).updateDataset(any(Dataset.class), anyString());
  }

  @Test
  void updateDataset_noDatasetFound_Returns404() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    DatasetXsltStringWrapper datasetXsltStringWrapper = new DatasetXsltStringWrapper(dataset,
        "<xslt attribute:\"value\"></xslt>");

    doThrow(new NoDatasetFoundException("Does not exist")).when(datasetService)
                                                          .updateDataset(any(Dataset.class), anyString());
    mockMvc.perform(put("/secured/datasets")
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .accept(MediaType.APPLICATION_JSON)
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(datasetXsltStringWrapper)))
           .andExpect(status().isNotFound())
           .andExpect(jsonPath("$.errorMessage", is("Does not exist")));
    verify(datasetService, times(1)).updateDataset(any(Dataset.class), anyString());
  }

  @Test
  void updateDataset_BadContentException_Returns406() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    DatasetXsltStringWrapper datasetXsltStringWrapper = new DatasetXsltStringWrapper(dataset,
        "<xslt attribute:\"value\"></xslt>");
    doThrow(new BadContentException("Bad Content")).when(datasetService)
                                                   .updateDataset(any(Dataset.class), anyString());
    mockMvc.perform(put("/secured/datasets")
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .accept(MediaType.APPLICATION_JSON)
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(datasetXsltStringWrapper)))
           .andExpect(status().isNotAcceptable())
           .andExpect(jsonPath("$.errorMessage", is("Bad Content")));

    verify(datasetService, times(1))
        .updateDataset(any(Dataset.class), anyString());
  }

  @Test
  void deleteDataset() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    mockMvc.perform(delete(String.format("/secured/datasets/%s", TestObjectFactory.DATASETID))
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .accept(MediaType.APPLICATION_JSON)
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(null)))
           .andExpect(status().isNoContent())
           .andExpect(content().string(""));

    ArgumentCaptor<String> datasetIdArgumentCaptor = ArgumentCaptor.forClass(String.class);
    verify(datasetService, times(1)).deleteDatasetByDatasetId(datasetIdArgumentCaptor.capture());
    assertEquals(Integer.toString(TestObjectFactory.DATASETID), datasetIdArgumentCaptor.getValue());
  }

  @Test
  void deleteDatasetUnauthenticated() throws Exception {
    mockMvc.perform(delete(String.format("/secured/datasets/%s", TestObjectFactory.DATASETID))
               .accept(MediaType.APPLICATION_JSON)
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(null)))
           .andExpect(status().isUnauthorized());
    verify(datasetService, times(0)).deleteDatasetByDatasetId(anyString());
  }

  @Test
  void deleteDatasetInvalidUser() throws Exception {
    when(jwtDecoder.decode(MOCK_INVALID_TOKEN)).thenReturn(testJwtUtils.getInvalidRoleJwt());
    mockMvc.perform(delete(String.format("/secured/datasets/%s", TestObjectFactory.DATASETID))
               .header("Authorization", BEARER + MOCK_INVALID_TOKEN)
               .accept(MediaType.APPLICATION_JSON)
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(null)))
           .andExpect(status().isForbidden());
    verify(datasetService, times(0)).deleteDatasetByDatasetId(anyString());
  }

  @Test
  void deleteDataset_BadContentException_Returns406() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    doThrow(new BadContentException("Bad Content")).when(datasetService)
                                                   .deleteDatasetByDatasetId(Integer.toString(TestObjectFactory.DATASETID));
    mockMvc.perform(delete(String.format("/secured/datasets/%s", TestObjectFactory.DATASETID))
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .accept(MediaType.APPLICATION_JSON)
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(null)))
           .andExpect(status().isNotAcceptable())
           .andExpect(jsonPath("$.errorMessage", is("Bad Content")));
  }


  @Test
  void getByDatasetId() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    when(datasetService.getDatasetByDatasetId(Integer.toString(TestObjectFactory.DATASETID))).thenReturn(dataset);
    mockMvc.perform(get(String.format("/secured/datasets/%s", TestObjectFactory.DATASETID))
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(null)))
           .andExpect(status().isOk())
           .andExpect(content().contentType(MediaType.APPLICATION_JSON))
           .andExpect(jsonPath("$.datasetName", is(TestObjectFactory.DATASETNAME)))
           .andExpect(jsonPath("$.datasetId", is(Integer.toString(TestObjectFactory.DATASETID))));

    ArgumentCaptor<String> datasetIdArgumentCaptor = ArgumentCaptor.forClass(String.class);
    verify(datasetService, times(1))
        .getDatasetByDatasetId(datasetIdArgumentCaptor.capture());
    assertEquals(Integer.toString(TestObjectFactory.DATASETID), datasetIdArgumentCaptor.getValue());
  }

  @Test
  void getByDatasetIdUnauthenticated() throws Exception {
    mockMvc.perform(get(String.format("/secured/datasets/%s", TestObjectFactory.DATASETID))
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(null)))
           .andExpect(status().isUnauthorized());
    verify(datasetService, times(0)).getDatasetByDatasetId(anyString());
  }

  @Test
  void getByDatasetIdInvalidUser() throws Exception {
    when(jwtDecoder.decode(MOCK_INVALID_TOKEN)).thenReturn(testJwtUtils.getInvalidRoleJwt());
    mockMvc.perform(get(String.format("/secured/datasets/%s", TestObjectFactory.DATASETID))
               .header("Authorization", BEARER + MOCK_INVALID_TOKEN)
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(null)))
           .andExpect(status().isForbidden());
    verify(datasetService, times(0)).getDatasetByDatasetId(anyString());
  }

  @Test
  void getByDatasetId_noDatasetFound_Returns404() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    when(datasetService.getDatasetByDatasetId(Integer.toString(TestObjectFactory.DATASETID)))
        .thenThrow(new NoDatasetFoundException("Does not exist"));
    mockMvc.perform(get(String.format("/secured/datasets/%s", TestObjectFactory.DATASETID))
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(null)))
           .andExpect(content().contentType(MediaType.APPLICATION_JSON))
           .andExpect(status().isNotFound())
           .andExpect(jsonPath("$.errorMessage", is("Does not exist")));
  }

  @Test
  void getDatasetXsltByDatasetId() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    DatasetXslt xsltObject = new DatasetXslt(dataset.getDatasetId(),
        "<xslt attribute:\"value\"></xslt>");
    when(datasetService.getDatasetXsltByDatasetId(Integer.toString(TestObjectFactory.DATASETID))).thenReturn(xsltObject);
    mockMvc.perform(get(String.format("/secured/datasets/%s/xslt", TestObjectFactory.DATASETID))
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(null)))
           .andExpect(status().isOk())
           .andExpect(content().contentType(MediaType.APPLICATION_JSON))
           .andExpect(jsonPath("$.xslt", is(xsltObject.getXslt())))
           .andExpect(jsonPath("$.datasetId", is(Integer.toString(TestObjectFactory.DATASETID))));

    ArgumentCaptor<String> datasetIdArgumentCaptor = ArgumentCaptor.forClass(String.class);
    verify(datasetService, times(1)).getDatasetXsltByDatasetId(datasetIdArgumentCaptor.capture());
    assertEquals(Integer.toString(TestObjectFactory.DATASETID), datasetIdArgumentCaptor.getValue());
  }

  @Test
  void getDatasetXsltByDatasetId_noDatasetFound_Returns404() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    when(datasetService
        .getDatasetXsltByDatasetId(Integer.toString(TestObjectFactory.DATASETID)))
        .thenThrow(new NoDatasetFoundException("Does not exist"));
    mockMvc
        .perform(
            get(String.format("/secured/datasets/%s/xslt", TestObjectFactory.DATASETID))
                .header("Authorization", BEARER + MOCK_VALID_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.convertObjectToJsonBytes(null)))
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.errorMessage", is("Does not exist")));
  }

  @Test
  void getDatasetXsltByDatasetId_noXsltFound_Returns404() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    when(datasetService.getDatasetXsltByDatasetId(Integer.toString(TestObjectFactory.DATASETID)))
        .thenThrow(new NoXsltFoundException("Does not exist"));
    mockMvc.perform(get(String.format("/secured/datasets/%s/xslt", TestObjectFactory.DATASETID))
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(null)))
           .andExpect(status().isNotFound())
           .andExpect(content().contentType(MediaType.APPLICATION_JSON))
           .andExpect(jsonPath("$.errorMessage", is("Does not exist")));
  }

  @Test
  void getDatasetXsltByDatasetIdUnauthenticated() throws Exception {
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    DatasetXslt xsltObject = new DatasetXslt(dataset.getDatasetId(),
        "<xslt attribute:\"value\"></xslt>");

    when(datasetService.getDatasetXsltByDatasetId(Integer.toString(TestObjectFactory.DATASETID)))
        .thenReturn(xsltObject);
    mockMvc.perform(get(String.format("/secured/datasets/%s/xslt", TestObjectFactory.DATASETID))
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(null)))
           .andExpect(status().isUnauthorized());
    verify(datasetService, times(0)).getDatasetXsltByDatasetId(anyString());
  }

  @Test
  void getDatasetXsltByDatasetIdInvalidUser() throws Exception {
    when(jwtDecoder.decode(MOCK_INVALID_TOKEN)).thenReturn(testJwtUtils.getInvalidRoleJwt());
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    DatasetXslt xsltObject = new DatasetXslt(dataset.getDatasetId(),
        "<xslt attribute:\"value\"></xslt>");

    when(datasetService.getDatasetXsltByDatasetId(Integer.toString(TestObjectFactory.DATASETID)))
        .thenReturn(xsltObject);
    mockMvc.perform(get(String.format("/secured/datasets/%s/xslt", TestObjectFactory.DATASETID))
               .header("Authorization", BEARER + MOCK_INVALID_TOKEN)
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(null)))
           .andExpect(status().isForbidden());
    verify(datasetService, times(0)).getDatasetXsltByDatasetId(anyString());
  }

  @Test
  void getXsltByXsltId() throws Exception {
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    DatasetXslt xsltObject = new DatasetXslt(dataset.getDatasetId(),
        "<xslt attribute:\"value\"></xslt>");

    when(datasetService.getDatasetXsltByXsltId(TestObjectFactory.XSLTID))
        .thenReturn(xsltObject);
    mockMvc.perform(get(String.format("/secured/datasets/xslt/%s", TestObjectFactory.XSLTID))
               .content(TestUtils.convertObjectToJsonBytes(null)))
           .andExpect(status().isOk())
           .andExpect(content().contentType(
               new MediaType(MediaType.TEXT_PLAIN.getType(), MediaType.TEXT_PLAIN.getSubtype(), StandardCharsets.UTF_8)))
           .andExpect(content().string(xsltObject.getXslt()));

    ArgumentCaptor<String> xsltIdArgumentCaptor = ArgumentCaptor.forClass(String.class);
    verify(datasetService, times(1))
        .getDatasetXsltByXsltId(xsltIdArgumentCaptor.capture());
    assertEquals(TestObjectFactory.XSLTID, xsltIdArgumentCaptor.getValue());
  }

  @Test
  void getXsltByXsltId_NoXsltFound_404() throws Exception {
    when(datasetService.getDatasetXsltByXsltId(TestObjectFactory.XSLTID))
        .thenThrow(new NoXsltFoundException("No xslt found"));
    mockMvc.perform(get(String.format("/secured/datasets/xslt/%s", TestObjectFactory.XSLTID))
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(null)))
           .andExpect(status().isNotFound());

    ArgumentCaptor<String> xsltIdArgumentCaptor = ArgumentCaptor.forClass(String.class);
    verify(datasetService, times(1))
        .getDatasetXsltByXsltId(xsltIdArgumentCaptor.capture());
    assertEquals(TestObjectFactory.XSLTID, xsltIdArgumentCaptor.getValue());
  }

  @Test
  void createDefaultXslt() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getAdminJwt());
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    DatasetXslt xsltObject = new DatasetXslt(dataset.getDatasetId(),
        "<xslt attribute:\"value\"></xslt>");
    xsltObject.setId(new ObjectId(TestObjectFactory.XSLTID));

    when(datasetService.createDefaultXslt(anyString())).thenReturn(xsltObject);
    mockMvc.perform(post("/secured/datasets/xslt/default", TestObjectFactory.XSLTID)
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .contentType(MediaType.TEXT_PLAIN)
               .content(TestUtils.convertObjectToJsonBytes(xsltObject.getXslt())))
           .andExpect(status().isCreated())
           .andExpect(content().contentType(MediaType.APPLICATION_JSON))
           .andExpect(jsonPath("$.xslt", is(xsltObject.getXslt())))
           .andExpect(jsonPath("$.datasetId", is(Integer.toString(TestObjectFactory.DATASETID))));
  }

  @Test
  void createDefaultXslt_Unauthenticated() throws Exception {
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    DatasetXslt xsltObject = new DatasetXslt(dataset.getDatasetId(),
        "<xslt attribute:\"value\"></xslt>");
    xsltObject.setId(new ObjectId(TestObjectFactory.XSLTID));

    mockMvc.perform(post("/secured/datasets/xslt/default", TestObjectFactory.XSLTID)
               .contentType(MediaType.TEXT_PLAIN)
               .content(TestUtils.convertObjectToJsonBytes(xsltObject.getXslt())))
           .andExpect(status().isUnauthorized());
  }

  @Test
  void createDefaultXslt_Unauthorized() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    DatasetXslt xsltObject = new DatasetXslt(dataset.getDatasetId(),
        "<xslt attribute:\"value\"></xslt>");
    xsltObject.setId(new ObjectId(TestObjectFactory.XSLTID));

    mockMvc.perform(post("/secured/datasets/xslt/default", TestObjectFactory.XSLTID)
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .contentType(MediaType.TEXT_PLAIN)
               .content(TestUtils.convertObjectToJsonBytes(xsltObject.getXslt())))
           .andExpect(status().isForbidden());
  }

  @Test
  void getLatestDefaultXslt() throws Exception {
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    DatasetXslt xsltObject = new DatasetXslt(dataset.getDatasetId(),
        "<xslt attribute:\"value\"></xslt>");

    when(datasetService.getLatestDefaultXslt()).thenReturn(xsltObject);
    mockMvc.perform(get("/secured/datasets/xslt/default")
               .content(TestUtils.convertObjectToJsonBytes(null)))
           .andExpect(status().isOk())
           .andExpect(content().contentType(
               new MediaType(MediaType.TEXT_PLAIN.getType(), MediaType.TEXT_PLAIN.getSubtype(), StandardCharsets.UTF_8)))
           .andExpect(content().string(xsltObject.getXslt()));

    verify(datasetService, times(1)).getLatestDefaultXslt();
  }

  @Test
  void getLatestDefaultXslt_NoXsltFound_404() throws Exception {
    when(datasetService.getLatestDefaultXslt())
        .thenThrow(new NoXsltFoundException("No xslt found"));
    mockMvc.perform(get("/secured/datasets/xslt/default")
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(null)))
           .andExpect(status().isNotFound());

    verify(datasetService, times(1)).getLatestDefaultXslt();
  }

  @Test
  void transformRecordsUsingLatestDatasetXslt() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    List<Record> listOfRecords = TestObjectFactory.createListOfRecords(5);
    when(datasetService.transformRecordsUsingLatestDatasetXslt(anyString(), anyList())).thenReturn(listOfRecords);
    mockMvc
        .perform(post("/secured/datasets/{datasetId}/xslt/transform", Integer.toString(TestObjectFactory.DATASETID))
            .header("Authorization", BEARER + MOCK_VALID_TOKEN)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(TestUtils.convertObjectToJsonBytes(listOfRecords)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$", hasSize(5)));
  }

  @Test
  void transformRecordsUsingLatestDatasetXslt_Unauthenticated() throws Exception {
    mockMvc.perform(post("/secured/datasets/{datasetId}/xslt/transform", Integer.toString(TestObjectFactory.DATASETID))
               .contentType(MediaType.APPLICATION_JSON_VALUE)
               .content(TestUtils.convertObjectToJsonBytes(TestObjectFactory.createListOfRecords(5))))
           .andExpect(status().isUnauthorized());
  }

  @Test
  void transformRecordsUsingLatestDatasetXslt_Unauthorized() throws Exception {
    when(jwtDecoder.decode(MOCK_INVALID_TOKEN)).thenReturn(testJwtUtils.getInvalidRoleJwt());
    mockMvc.perform(post("/secured/datasets/{datasetId}/xslt/transform", Integer.toString(TestObjectFactory.DATASETID))
               .header("Authorization", BEARER + MOCK_INVALID_TOKEN)
               .contentType(MediaType.APPLICATION_JSON_VALUE)
               .content(TestUtils.convertObjectToJsonBytes(TestObjectFactory.createListOfRecords(5))))
           .andExpect(status().isForbidden());
  }

  @Test
  void transformRecordsUsingLatestDefaultXslt() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    List<eu.europeana.metis.core.rest.Record> listOfRecords = TestObjectFactory.createListOfRecords(5);
    when(datasetService.transformRecordsUsingLatestDefaultXslt(anyString(), anyList())).thenReturn(listOfRecords);
    mockMvc.perform(post("/secured/datasets/{datasetId}/xslt/transform/default", Integer.toString(TestObjectFactory.DATASETID))
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .contentType(MediaType.APPLICATION_JSON_VALUE)
               .content(TestUtils.convertObjectToJsonBytes(listOfRecords)))
           .andExpect(status().isOk())
           .andExpect(content().contentType(MediaType.APPLICATION_JSON))
           .andExpect(jsonPath("$", hasSize(5)));
  }

  @Test
  void transformRecordsUsingLatestDefaultXslt_Unauthenticated() throws Exception {
    mockMvc.perform(post("/secured/datasets/{datasetId}/xslt/transform/default", Integer.toString(TestObjectFactory.DATASETID))
               .contentType(MediaType.APPLICATION_JSON_VALUE)
               .content(TestUtils.convertObjectToJsonBytes(TestObjectFactory.createListOfRecords(5))))
           .andExpect(status().isUnauthorized());
  }

  @Test
  void transformRecordsUsingLatestDefaultXslt_Unauthorized() throws Exception {
    when(jwtDecoder.decode(MOCK_INVALID_TOKEN)).thenReturn(testJwtUtils.getInvalidRoleJwt());
    mockMvc.perform(post("/secured/datasets/{datasetId}/xslt/transform/default", Integer.toString(TestObjectFactory.DATASETID))
               .header("Authorization", BEARER + MOCK_INVALID_TOKEN)
               .contentType(MediaType.APPLICATION_JSON_VALUE)
               .content(TestUtils.convertObjectToJsonBytes(TestObjectFactory.createListOfRecords(5))))
           .andExpect(status().isForbidden());
  }

  @Test
  void getByDatasetName() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);

    when(datasetService.getDatasetByDatasetName(TestObjectFactory.DATASETNAME)).thenReturn(dataset);
    mockMvc.perform(get(String.format("/secured/datasets/dataset_name/%s", TestObjectFactory.DATASETNAME))
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(null)))
           .andExpect(status().isOk())
           .andExpect(content().contentType(MediaType.APPLICATION_JSON))
           .andExpect(jsonPath("$.datasetName", is(TestObjectFactory.DATASETNAME)))
           .andExpect(jsonPath("$.datasetId", is(Integer.toString(TestObjectFactory.DATASETID))));

    ArgumentCaptor<String> datasetNameArgumentCaptor = ArgumentCaptor.forClass(String.class);
    verify(datasetService, times(1)).getDatasetByDatasetName(datasetNameArgumentCaptor.capture());
    assertEquals(TestObjectFactory.DATASETNAME, datasetNameArgumentCaptor.getValue());
  }

  @Test
  void getByDatasetNameUnauthenticated() throws Exception {
    mockMvc.perform(get(String.format("/secured/datasets/dataset_name/%s", TestObjectFactory.DATASETNAME))
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(null)))
           .andExpect(status().isUnauthorized());

    verify(datasetService, times(0)).getDatasetByDatasetName(anyString());
  }

  @Test
  void getByDatasetNameInvalidUser() throws Exception {
    when(jwtDecoder.decode(MOCK_INVALID_TOKEN)).thenReturn(testJwtUtils.getInvalidRoleJwt());
    mockMvc.perform(get(String.format("/secured/datasets/dataset_name/%s", TestObjectFactory.DATASETNAME))
               .header("Authorization", BEARER + MOCK_INVALID_TOKEN)
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(null)))
           .andExpect(status().isForbidden());

    verify(datasetService, times(0)).getDatasetByDatasetName(anyString());
  }

  @Test
  void getByDatasetName_noDatasetFound_Returns404() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    when(datasetService.getDatasetByDatasetName(TestObjectFactory.DATASETNAME)).thenThrow(
        new NoDatasetFoundException("Does not exist"));
    mockMvc.perform(get(String.format("/secured/datasets/dataset_name/%s", TestObjectFactory.DATASETNAME))
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(null)))
           .andExpect(content().contentType(MediaType.APPLICATION_JSON))
           .andExpect(status().isNotFound())
           .andExpect(jsonPath("$.errorMessage", is("Does not exist")));
  }

  @Test
  void getAllDatasetsByProvider() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    List<Dataset> datasetList = getDatasets();
    when(datasetService.getAllDatasetsByProvider("myProvider", 3)).thenReturn(datasetList);
    when(datasetService.getDatasetsPerRequestLimit()).thenReturn(5);

    mockMvc.perform(get("/secured/datasets/provider/myProvider")
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .param("nextPage", "3")
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(null)))
           .andExpect(status().isOk())
           .andExpect(content().contentType(MediaType.APPLICATION_JSON))
           .andExpect(jsonPath("$.results", hasSize(2)))
           .andExpect(jsonPath("$.results[0].datasetId",
               is(Integer.toString(TestObjectFactory.DATASETID + 1))))
           .andExpect(jsonPath("$.results[1].datasetId",
               is(Integer.toString(TestObjectFactory.DATASETID + 2))));

    ArgumentCaptor<String> provider = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Integer> page = ArgumentCaptor.forClass(Integer.class);
    verify(datasetService, times(1)).getAllDatasetsByProvider(provider.capture(), page.capture());

    assertEquals("myProvider", provider.getValue());
    assertEquals(3, page.getValue().intValue());
  }

  @Test
  void getAllDatasetsByProviderUnauthenticated() throws Exception {
    mockMvc.perform(get("/secured/datasets/provider/myProvider")
               .param("nextPage", "3")
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(null)))
           .andExpect(status().isUnauthorized());

    verify(datasetService, times(0)).getAllDatasetsByProvider(anyString(), anyInt());
  }

  @Test
  void getAllDatasetsByProviderInvalidUser() throws Exception {
    when(jwtDecoder.decode(MOCK_INVALID_TOKEN)).thenReturn(testJwtUtils.getInvalidRoleJwt());
    mockMvc.perform(get("/secured/datasets/provider/myProvider")
               .header("Authorization", BEARER + MOCK_INVALID_TOKEN)
               .param("nextPage", "3")
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(null)))
           .andExpect(status().isForbidden());

    verify(datasetService, times(0)).getAllDatasetsByProvider(anyString(), anyInt());
  }

  @Test
  void getAllDatasetsByIntermediateProvider() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    List<Dataset> datasetList = getDatasets();
    when(datasetService.getAllDatasetsByIntermediateProvider("myIntermediateProvider", 3)).thenReturn(datasetList);
    when(datasetService.getDatasetsPerRequestLimit()).thenReturn(5);

    mockMvc.perform(get("/secured/datasets/intermediate_provider/myIntermediateProvider")
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .param("nextPage", "3")
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(null)))
           .andExpect(status().isOk())
           .andExpect(content().contentType(MediaType.APPLICATION_JSON))
           .andExpect(jsonPath("$.results", hasSize(2)))
           .andExpect(jsonPath("$.results[0].datasetId",
               is(Integer.toString(TestObjectFactory.DATASETID + 1))))
           .andExpect(jsonPath("$.results[1].datasetId",
               is(Integer.toString(TestObjectFactory.DATASETID + 2))));

    ArgumentCaptor<String> provider = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Integer> page = ArgumentCaptor.forClass(Integer.class);
    verify(datasetService, times(1)).getAllDatasetsByIntermediateProvider(provider.capture(), page.capture());

    assertEquals("myIntermediateProvider", provider.getValue());
    assertEquals(3, page.getValue().intValue());
  }

  @Test
  void getAllDatasetsByIntermediateProviderUnauthenticated() throws Exception {
    mockMvc.perform(get("/secured/datasets/intermediate_provider/myIntermediateProvider")
               .param("nextPage", "3")
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(null)))
           .andExpect(status().isUnauthorized());

    verify(datasetService, times(0)).getAllDatasetsByIntermediateProvider(anyString(), anyInt());
  }

  @Test
  void getAllDatasetsByIntermediateProviderInvalidUser() throws Exception {
    when(jwtDecoder.decode(MOCK_INVALID_TOKEN)).thenReturn(testJwtUtils.getInvalidRoleJwt());
    mockMvc.perform(get("/secured/datasets/intermediate_provider/myIntermediateProvider")
               .header("Authorization", BEARER + MOCK_INVALID_TOKEN)
               .param("nextPage", "3")
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(null)))
           .andExpect(status().isForbidden());

    verify(datasetService, times(0)).getAllDatasetsByIntermediateProvider(anyString(), anyInt());
  }


  @Test
  void getAllDatasetsByDataProvider() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    List<Dataset> datasetList = getDatasets();
    when(datasetService.getAllDatasetsByDataProvider("myDataProvider", 3)).thenReturn(datasetList);
    when(datasetService.getDatasetsPerRequestLimit()).thenReturn(5);

    mockMvc.perform(get("/secured/datasets/data_provider/myDataProvider")
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .param("nextPage", "3")
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(null)))
           .andExpect(status().isOk())
           .andExpect(content().contentType(MediaType.APPLICATION_JSON))
           .andExpect(jsonPath("$.results", hasSize(2)))
           .andExpect(jsonPath("$.results[0].datasetId",
               is(Integer.toString(TestObjectFactory.DATASETID + 1))))
           .andExpect(jsonPath("$.results[1].datasetId",
               is(Integer.toString(TestObjectFactory.DATASETID + 2))));

    ArgumentCaptor<String> provider = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Integer> page = ArgumentCaptor.forClass(Integer.class);
    verify(datasetService, times(1))
        .getAllDatasetsByDataProvider(provider.capture(), page.capture());

    assertEquals("myDataProvider", provider.getValue());
    assertEquals(3, page.getValue().intValue());
  }

  @Test
  void getAllDatasetsByDataProviderUnauthenticated() throws Exception {
    mockMvc.perform(get("/secured/datasets/data_provider/myDataProvider")
               .param("nextPage", "3")
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(null)))
           .andExpect(status().isUnauthorized());
    verify(datasetService, times(0)).getAllDatasetsByDataProvider(anyString(), anyInt());
  }

  @Test
  void getAllDatasetsByDataProviderInvalidUser() throws Exception {
    when(jwtDecoder.decode(MOCK_INVALID_TOKEN)).thenReturn(testJwtUtils.getInvalidRoleJwt());
    mockMvc.perform(get("/secured/datasets/data_provider/myDataProvider")
               .header("Authorization", BEARER + MOCK_INVALID_TOKEN)
               .param("nextPage", "3")
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(null)))
           .andExpect(status().isForbidden());
    verify(datasetService, times(0)).getAllDatasetsByDataProvider(anyString(), anyInt());
  }

  @Test
  void getAllDatasetsByOrganizationId() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    List<Dataset> datasetList = getDatasets();
    when(datasetService.getAllDatasetsByOrganizationId("myOrganizationId", 3)).thenReturn(datasetList);
    when(datasetService.getDatasetsPerRequestLimit()).thenReturn(5);

    mockMvc.perform(get("/secured/datasets/organization_id/myOrganizationId")
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .param("nextPage", "3")
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(null)))
           .andExpect(status().isOk())
           .andExpect(content().contentType(MediaType.APPLICATION_JSON))
           .andExpect(jsonPath("$.results", hasSize(2)))
           .andExpect(jsonPath("$.results[0].datasetId",
               is(Integer.toString(TestObjectFactory.DATASETID + 1))))
           .andExpect(jsonPath("$.results[1].datasetId",
               is(Integer.toString(TestObjectFactory.DATASETID + 2))));

    ArgumentCaptor<String> provider = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Integer> page = ArgumentCaptor.forClass(Integer.class);
    verify(datasetService, times(1))
        .getAllDatasetsByOrganizationId(provider.capture(), page.capture());

    assertEquals("myOrganizationId", provider.getValue());
    assertEquals(3, page.getValue().intValue());
  }

  @Test
  void getAllDatasetsByOrganizationIdUnauthenticated() throws Exception {
    mockMvc.perform(get("/secured/datasets/organization_id/myOrganizationId")
               .param("nextPage", "3")
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(null)))
           .andExpect(status().isUnauthorized());

    verify(datasetService, times(0))
        .getAllDatasetsByOrganizationId(anyString(), anyInt());
  }

  @Test
  void getAllDatasetsByOrganizationIdInvalidUser() throws Exception {
    when(jwtDecoder.decode(MOCK_INVALID_TOKEN)).thenReturn(testJwtUtils.getInvalidRoleJwt());
    mockMvc.perform(get("/secured/datasets/organization_id/myOrganizationId")
               .header("Authorization", BEARER + MOCK_INVALID_TOKEN)
               .param("nextPage", "3")
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(null)))
           .andExpect(status().isForbidden());

    verify(datasetService, times(0))
        .getAllDatasetsByOrganizationId(anyString(), anyInt());
  }

  @Test
  void getAllDatasetsByOrganizationName() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    List<Dataset> datasetList = getDatasets();
    when(datasetService.getAllDatasetsByOrganizationName("myOrganizationName", 3))
        .thenReturn(datasetList);
    when(datasetService.getDatasetsPerRequestLimit()).thenReturn(5);

    mockMvc.perform(get("/secured/datasets/organization_name/myOrganizationName")
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .param("nextPage", "3")
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(null)))
           .andExpect(status().isOk())
           .andExpect(content().contentType(MediaType.APPLICATION_JSON))
           .andExpect(jsonPath("$.results", hasSize(2)))
           .andExpect(jsonPath("$.results[0].datasetId",
               is(Integer.toString(TestObjectFactory.DATASETID + 1))))
           .andExpect(jsonPath("$.results[1].datasetId",
               is(Integer.toString(TestObjectFactory.DATASETID + 2))));

    ArgumentCaptor<String> provider = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Integer> page = ArgumentCaptor.forClass(Integer.class);
    verify(datasetService, times(1))
        .getAllDatasetsByOrganizationName(provider.capture(), page.capture());

    assertEquals("myOrganizationName", provider.getValue());
    assertEquals(3, page.getValue().intValue());
  }

  @Test
  void getAllDatasetsByOrganizationNameUnauthenticated() throws Exception {
    mockMvc.perform(get("/secured/datasets/organization_name/myOrganizationName")
               .param("nextPage", "3")
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(null)))
           .andExpect(status().isUnauthorized());

    verify(datasetService, times(0)).getAllDatasetsByOrganizationName(anyString(), anyInt());
  }

  @Test
  void getAllDatasetsByOrganizationNameInvalidUser() throws Exception {
    when(jwtDecoder.decode(MOCK_INVALID_TOKEN)).thenReturn(testJwtUtils.getInvalidRoleJwt());
    mockMvc.perform(get("/secured/datasets/organization_name/myOrganizationName")
               .header("Authorization", BEARER + MOCK_INVALID_TOKEN)
               .param("nextPage", "3")
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(null)))
           .andExpect(status().isForbidden());

    verify(datasetService, times(0)).getAllDatasetsByOrganizationName(anyString(), anyInt());
  }

  @Test
  void getDatasetsCountries() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    MvcResult mvcResult = mockMvc.perform(get("/secured/datasets/countries")
                                     .header("Authorization", BEARER + MOCK_VALID_TOKEN)
                                     .contentType(MediaType.APPLICATION_JSON)
                                     .content(""))
                                 .andExpect(status().isOk())
                                 .andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

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
  void getDatasetsCountriesUnauthenticated() throws Exception {
    mockMvc.perform(get("/secured/datasets/countries")
               .contentType(MediaType.APPLICATION_JSON)
               .content(""))
           .andExpect(status().isUnauthorized());
  }

  @Test
  void getDatasetsCountriesInvalidUser() throws Exception {
    when(jwtDecoder.decode(MOCK_INVALID_TOKEN)).thenReturn(testJwtUtils.getInvalidRoleJwt());
    mockMvc.perform(get("/secured/datasets/countries")
               .header("Authorization", BEARER + MOCK_INVALID_TOKEN)
               .contentType(MediaType.APPLICATION_JSON)
               .content(""))
           .andExpect(status().isForbidden());
  }

  @Test
  void getDatasetsLanguages() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    MvcResult mvcResult = mockMvc.perform(get("/secured/datasets/languages")
                                     .header("Authorization", BEARER + MOCK_VALID_TOKEN)
                                     .contentType(MediaType.APPLICATION_JSON)
                                     .content(""))
                                 .andExpect(status().isOk())
                                 .andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

    String resultListOfLanguages = mvcResult.getResponse().getContentAsString();
    Object document = Configuration.defaultConfiguration().jsonProvider()
                                   .parse(resultListOfLanguages);

    List<Map<String, Object>> mapListOfLanguages = JsonPath.read(document, "$[*]");
    assertEquals(Language.values().length, mapListOfLanguages.size());
    assertEquals(mapListOfLanguages.get(10).get("enum"), Language.getLanguageListSortedByName().get(10).name());
    assertEquals(mapListOfLanguages.get(10).get("name"), Language.getLanguageListSortedByName().get(10).getName());
  }

  @Test
  void getDatasetsLanguagesUnauthenticated() throws Exception {
    mockMvc.perform(get("/secured/datasets/languages")
               .contentType(MediaType.APPLICATION_JSON)
               .content(""))
           .andExpect(status().isUnauthorized());
  }

  @Test
  void getDatasetsLanguagesInvalidUser() throws Exception {
    when(jwtDecoder.decode(MOCK_INVALID_TOKEN)).thenReturn(testJwtUtils.getInvalidRoleJwt());
    mockMvc.perform(get("/secured/datasets/languages")
               .header("Authorization", BEARER + MOCK_INVALID_TOKEN)
               .contentType(MediaType.APPLICATION_JSON)
               .content(""))
           .andExpect(status().isForbidden());
  }

  @Test
  void getDatasetSearch() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    when(datasetService.searchDatasetsBasedOnSearchString("test", 3)).thenReturn(getDatasetSearchViews());
    when(datasetService.getDatasetsPerRequestLimit()).thenReturn(5);

    mockMvc.perform(get("/secured/datasets/search")
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .param("searchString", "test")
               .param("nextPage", "3")
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(null)))
           .andExpect(status().isOk())
           .andExpect(content().contentType(MediaType.APPLICATION_JSON))
           .andExpect(jsonPath("$.results", hasSize(2)))
           .andExpect(jsonPath("$.results[0].datasetId",
               is(Integer.toString(TestObjectFactory.DATASETID + 1))))
           .andExpect(jsonPath("$.results[1].datasetId",
               is(Integer.toString(TestObjectFactory.DATASETID + 2))));

    ArgumentCaptor<String> searchString = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Integer> page = ArgumentCaptor.forClass(Integer.class);
    verify(datasetService, times(1))
        .searchDatasetsBasedOnSearchString(searchString.capture(), page.capture());

    assertEquals("test", searchString.getValue());
    assertEquals(3, page.getValue().intValue());
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "/secured/datasets/provider/myProvider",
      "/secured/datasets/intermediate_provider/myIntermediateProvider",
      "/secured/datasets/data_provider/myDataProvider",
      "/secured/datasets/organization_id/myOrganizationId",
      "/secured/datasets/organization_name/myOrganizationName"
  })
  void getWithNegativeNextPage(String endpoint) throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    mockMvc.perform(get(endpoint)
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .param("nextPage", "-1")
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(null)))
           .andExpect(status().isNotAcceptable());
  }

  private List<DatasetSearchView> getDatasetSearchViews() {
    List<DatasetSearchView> datasetSearchViews = new ArrayList<>(2);
    final DatasetSearchView datasetSearchView1 = new DatasetSearchView();
    datasetSearchView1.setDatasetId(Integer.toString(TestObjectFactory.DATASETID + 1));
    datasetSearchView1.setDatasetName(TestObjectFactory.DATASETNAME + 1);
    datasetSearchView1.setProvider("provider1");
    datasetSearchView1.setDataProvider("dataProvider1");
    datasetSearchView1.setLastExecutionDate(new Date());
    datasetSearchViews.add(datasetSearchView1);

    final DatasetSearchView datasetSearchView2 = new DatasetSearchView();
    datasetSearchView2.setDatasetId(Integer.toString(TestObjectFactory.DATASETID + 2));
    datasetSearchView2.setDatasetName(TestObjectFactory.DATASETNAME + 2);
    datasetSearchView2.setProvider("provider2");
    datasetSearchView2.setDataProvider("dataProvider2");
    datasetSearchView2.setLastExecutionDate(new Date());
    datasetSearchViews.add(datasetSearchView2);

    return datasetSearchViews;
  }

  private List<Dataset> getDatasets() {
    List<Dataset> datasetList = new ArrayList<>();
    Dataset dataset1 = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    dataset1.setDatasetId(Integer.toString(TestObjectFactory.DATASETID + 1));
    datasetList.add(dataset1);

    Dataset dataset2 = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    dataset2.setDatasetId(Integer.toString(TestObjectFactory.DATASETID + 2));
    datasetList.add(dataset2);

    return datasetList;
  }


}
