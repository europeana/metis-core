package eu.europeana.metis.core.rest.controller;

import static eu.europeana.metis.core.common.AccountRole.ADMIN;
import static eu.europeana.metis.core.common.AccountRole.DATA_OFFICER;
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
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
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
import eu.europeana.metis.core.rest.exception.RestResponseExceptionHandler;
import eu.europeana.metis.core.rest.utils.TestObjectFactory;
import eu.europeana.metis.core.rest.utils.TestUtils;
import eu.europeana.metis.core.service.SecuredDatasetService;
import eu.europeana.metis.exception.BadContentException;
import eu.europeana.metis.utils.Country;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(SecuredDatasetController.class)
@ContextConfiguration(classes = {SecuredDatasetController.class, SecurityConfig.class, RestResponseExceptionHandler.class})
@TestPropertySource(properties = "spring.security.oauth2.resourceserver.jwt.resourceNames=secured-service")
class TestSecuredDatasetController {

  @MockBean
  private SecuredDatasetService securedDatasetService;

  @MockBean
  private JwtDecoder jwtDecoder;

  @Autowired
  private MockMvc mockMvc;

  private static final String BEARER = "Bearer ";
  private static final String MOCK_VALID_TOKEN = "xxx.yyy.zzz";
  private static final Jwt JWT_DATA_OFFICER = getJwt(MOCK_VALID_TOKEN, List.of(DATA_OFFICER.name()));
  private static final Jwt JWT_ADMIN = getJwt(MOCK_VALID_TOKEN, List.of(ADMIN.name()));
  private static final String MOCK_INVALID_TOKEN = "invalidToken";
  private static final Jwt JWT_INVALID_ROLE = getJwt(MOCK_INVALID_TOKEN, List.of("INVALID"));

  private static @NotNull Jwt getJwt(String token, List<String> resourceAccessRoles) {
    return Jwt.withTokenValue(token)
              .header("alg", "none")
              .claim("resource_access", Map.of("secured-service", Map.of("roles", resourceAccessRoles)))
              .claim("email", "user@example.com")
              .build();
  }


  @AfterEach
  void cleanUp() {
    reset(securedDatasetService);
    reset(jwtDecoder);
  }

  @Test
  void createDataset() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(JWT_DATA_OFFICER);

    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    when(securedDatasetService.createDataset(any(String.class), any(Dataset.class))).thenReturn(dataset);

    mockMvc.perform(post("/secured/datasets")
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .contentType(MediaType.APPLICATION_JSON)
               .accept(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(dataset)))
           .andDo(print())
           .andExpect(status().isCreated())
           .andExpect(content().contentType(MediaType.APPLICATION_JSON))
           .andExpect(jsonPath("$.datasetName", is(TestObjectFactory.DATASETNAME)));
    verify(securedDatasetService, times(1)).createDataset(any(String.class), any(Dataset.class));
  }


  @Test
  void createDatasetInvalidUser() throws Exception {
    when(jwtDecoder.decode(MOCK_INVALID_TOKEN)).thenReturn(JWT_INVALID_ROLE);

    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    when(securedDatasetService.createDataset(any(String.class), any(Dataset.class))).thenReturn(dataset);

    mockMvc.perform(post("/secured/datasets")
               .header("Authorization", BEARER + MOCK_INVALID_TOKEN)
               .contentType(MediaType.APPLICATION_JSON)
               .accept(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(dataset)))
           .andDo(print())
           .andExpect(status().isForbidden());
    verify(securedDatasetService, times(0)).createDataset(any(String.class), any(Dataset.class));
  }

  @Test
  void createDataset_DatasetAlreadyExistsException_Returns409() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(JWT_DATA_OFFICER);
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    doThrow(new DatasetAlreadyExistsException("Conflict"))
        .when(securedDatasetService).createDataset(any(String.class), any(Dataset.class));

    mockMvc.perform(post("/secured/datasets")
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .contentType(MediaType.APPLICATION_JSON)
               .accept(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(dataset)))
           .andDo(print())
           .andExpect(status().isConflict())
           .andExpect(jsonPath("$.errorMessage", is("Conflict")));
    verify(securedDatasetService, times(1)).createDataset(any(String.class), any(Dataset.class));
  }

  @Test
  void updateDataset_withValidData_Returns204() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(JWT_DATA_OFFICER);
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
    verify(securedDatasetService, times(1)).updateDataset(any(Dataset.class), anyString());
  }

  @Test
  void updateDataset_InvalidUser() throws Exception {
    when(jwtDecoder.decode(MOCK_INVALID_TOKEN)).thenReturn(JWT_INVALID_ROLE);
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    mockMvc.perform(put("/secured/datasets")
               .header("Authorization", BEARER + MOCK_INVALID_TOKEN)
               .accept(MediaType.APPLICATION_JSON)
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(dataset)))
           .andExpect(status().isForbidden());
    verify(securedDatasetService, times(0)).updateDataset(any(Dataset.class), anyString());
  }

  @Test
  void updateDataset_noDatasetFound_Returns404() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(JWT_DATA_OFFICER);
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    DatasetXsltStringWrapper datasetXsltStringWrapper = new DatasetXsltStringWrapper(dataset,
        "<xslt attribute:\"value\"></xslt>");

    doThrow(new NoDatasetFoundException("Does not exist")).when(securedDatasetService)
                                                          .updateDataset(any(Dataset.class), anyString());
    mockMvc.perform(put("/secured/datasets")
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .accept(MediaType.APPLICATION_JSON)
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(datasetXsltStringWrapper)))
           .andExpect(status().isNotFound())
           .andExpect(jsonPath("$.errorMessage", is("Does not exist")));
    verify(securedDatasetService, times(1)).updateDataset(any(Dataset.class), anyString());
  }

  @Test
  void updateDataset_BadContentException_Returns406() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(JWT_DATA_OFFICER);
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    DatasetXsltStringWrapper datasetXsltStringWrapper = new DatasetXsltStringWrapper(dataset,
        "<xslt attribute:\"value\"></xslt>");
    doThrow(new BadContentException("Bad Content")).when(securedDatasetService)
                                                   .updateDataset(any(Dataset.class), anyString());
    mockMvc.perform(put("/secured/datasets")
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .accept(MediaType.APPLICATION_JSON)
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(datasetXsltStringWrapper)))
           .andExpect(status().isNotAcceptable())
           .andExpect(jsonPath("$.errorMessage", is("Bad Content")));

    verify(securedDatasetService, times(1))
        .updateDataset(any(Dataset.class), anyString());
  }

  @Test
  void deleteDataset() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(JWT_DATA_OFFICER);
    mockMvc.perform(delete(String.format("/secured/datasets/%s", TestObjectFactory.DATASETID))
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .accept(MediaType.APPLICATION_JSON)
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(null)))
           .andExpect(status().isNoContent())
           .andExpect(content().string(""));

    ArgumentCaptor<String> datasetIdArgumentCaptor = ArgumentCaptor.forClass(String.class);
    verify(securedDatasetService, times(1)).deleteDatasetByDatasetId(datasetIdArgumentCaptor.capture());
    assertEquals(Integer.toString(TestObjectFactory.DATASETID), datasetIdArgumentCaptor.getValue());
  }

  @Test
  void deleteDatasetInvalidUser() throws Exception {
    when(jwtDecoder.decode(MOCK_INVALID_TOKEN)).thenReturn(JWT_INVALID_ROLE);
    mockMvc.perform(delete(String.format("/secured/datasets/%s", TestObjectFactory.DATASETID))
               .header("Authorization", BEARER + MOCK_INVALID_TOKEN)
               .accept(MediaType.APPLICATION_JSON)
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(null)))
           .andExpect(status().isForbidden());
    verify(securedDatasetService, times(0)).deleteDatasetByDatasetId(anyString());
  }

  @Test
  void deleteDataset_BadContentException_Returns406() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(JWT_DATA_OFFICER);
    doThrow(new BadContentException("Bad Content")).when(securedDatasetService)
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
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(JWT_DATA_OFFICER);
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    when(securedDatasetService.getDatasetByDatasetId(Integer.toString(TestObjectFactory.DATASETID))).thenReturn(dataset);
    mockMvc.perform(get(String.format("/secured/datasets/%s", TestObjectFactory.DATASETID))
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(null)))
           .andExpect(status().isOk())
           .andExpect(content().contentType(MediaType.APPLICATION_JSON))
           .andExpect(jsonPath("$.datasetName", is(TestObjectFactory.DATASETNAME)))
           .andExpect(jsonPath("$.datasetId", is(Integer.toString(TestObjectFactory.DATASETID))));

    ArgumentCaptor<String> datasetIdArgumentCaptor = ArgumentCaptor.forClass(String.class);
    verify(securedDatasetService, times(1))
        .getDatasetByDatasetId(datasetIdArgumentCaptor.capture());
    assertEquals(Integer.toString(TestObjectFactory.DATASETID), datasetIdArgumentCaptor.getValue());
  }

  @Test
  void getByDatasetIdInvalidUser() throws Exception {
    when(jwtDecoder.decode(MOCK_INVALID_TOKEN)).thenReturn(JWT_INVALID_ROLE);
    mockMvc.perform(get(String.format("/secured/datasets/%s", TestObjectFactory.DATASETID))
               .header("Authorization", BEARER + MOCK_INVALID_TOKEN)
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(null)))
           .andExpect(status().isForbidden());
    verify(securedDatasetService, times(0)).getDatasetByDatasetId(anyString());
  }

  @Test
  void getByDatasetId_noDatasetFound_Returns404() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(JWT_DATA_OFFICER);
    when(securedDatasetService.getDatasetByDatasetId(Integer.toString(TestObjectFactory.DATASETID)))
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
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(JWT_DATA_OFFICER);
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    DatasetXslt xsltObject = new DatasetXslt(dataset.getDatasetId(),
        "<xslt attribute:\"value\"></xslt>");
    when(securedDatasetService.getDatasetXsltByDatasetId(Integer.toString(TestObjectFactory.DATASETID))).thenReturn(xsltObject);
    mockMvc.perform(get(String.format("/secured/datasets/%s/xslt", TestObjectFactory.DATASETID))
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(null)))
           .andExpect(status().isOk())
           .andExpect(content().contentType(MediaType.APPLICATION_JSON))
           .andExpect(jsonPath("$.xslt", is(xsltObject.getXslt())))
           .andExpect(jsonPath("$.datasetId", is(Integer.toString(TestObjectFactory.DATASETID))));

    ArgumentCaptor<String> datasetIdArgumentCaptor = ArgumentCaptor.forClass(String.class);
    verify(securedDatasetService, times(1)).getDatasetXsltByDatasetId(datasetIdArgumentCaptor.capture());
    assertEquals(Integer.toString(TestObjectFactory.DATASETID), datasetIdArgumentCaptor.getValue());
  }

  @Test
  void getDatasetXsltByDatasetId_noDatasetFound_Returns404() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(JWT_DATA_OFFICER);
    when(securedDatasetService
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
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(JWT_DATA_OFFICER);
    when(securedDatasetService.getDatasetXsltByDatasetId(Integer.toString(TestObjectFactory.DATASETID)))
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
  void getDatasetXsltByDatasetIdInvalidUser() throws Exception {
    when(jwtDecoder.decode(MOCK_INVALID_TOKEN)).thenReturn(JWT_INVALID_ROLE);
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    DatasetXslt xsltObject = new DatasetXslt(dataset.getDatasetId(),
        "<xslt attribute:\"value\"></xslt>");

    when(securedDatasetService.getDatasetXsltByDatasetId(Integer.toString(TestObjectFactory.DATASETID)))
        .thenReturn(xsltObject);
    mockMvc.perform(get(String.format("/secured/datasets/%s/xslt", TestObjectFactory.DATASETID))
               .header("Authorization", BEARER + MOCK_INVALID_TOKEN)
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(null)))
           .andExpect(status().isForbidden());
    verify(securedDatasetService, times(0)).getDatasetXsltByDatasetId(anyString());
  }

  @Test
  void getXsltByXsltId() throws Exception {
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    DatasetXslt xsltObject = new DatasetXslt(dataset.getDatasetId(),
        "<xslt attribute:\"value\"></xslt>");

    when(securedDatasetService.getDatasetXsltByXsltId(TestObjectFactory.XSLTID))
        .thenReturn(xsltObject);
    mockMvc.perform(get(String.format("/secured/datasets/xslt/%s", TestObjectFactory.XSLTID))
               .content(TestUtils.convertObjectToJsonBytes(null)))
           .andExpect(status().isOk())
           .andExpect(content().contentType(
               new MediaType(MediaType.TEXT_PLAIN.getType(), MediaType.TEXT_PLAIN.getSubtype(), StandardCharsets.UTF_8)))
           .andExpect(content().string(xsltObject.getXslt()));

    ArgumentCaptor<String> xsltIdArgumentCaptor = ArgumentCaptor.forClass(String.class);
    verify(securedDatasetService, times(1))
        .getDatasetXsltByXsltId(xsltIdArgumentCaptor.capture());
    assertEquals(TestObjectFactory.XSLTID, xsltIdArgumentCaptor.getValue());
  }

  @Test
  void getXsltByXsltId_NoXsltFound_404() throws Exception {
    when(securedDatasetService.getDatasetXsltByXsltId(TestObjectFactory.XSLTID))
        .thenThrow(new NoXsltFoundException("No xslt found"));
    mockMvc.perform(get(String.format("/secured/datasets/xslt/%s", TestObjectFactory.XSLTID))
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(null)))
           .andExpect(status().isNotFound());

    ArgumentCaptor<String> xsltIdArgumentCaptor = ArgumentCaptor.forClass(String.class);
    verify(securedDatasetService, times(1))
        .getDatasetXsltByXsltId(xsltIdArgumentCaptor.capture());
    assertEquals(TestObjectFactory.XSLTID, xsltIdArgumentCaptor.getValue());
  }

  @Test
  void createDefaultXslt() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(JWT_ADMIN);
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    DatasetXslt xsltObject = new DatasetXslt(dataset.getDatasetId(),
        "<xslt attribute:\"value\"></xslt>");
    xsltObject.setId(new ObjectId(TestObjectFactory.XSLTID));

    when(securedDatasetService.createDefaultXslt(anyString())).thenReturn(xsltObject);
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
  void createDefaultXslt_Unauthorized() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(JWT_DATA_OFFICER);
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

    when(securedDatasetService.getLatestDefaultXslt()).thenReturn(xsltObject);
    mockMvc.perform(get("/secured/datasets/xslt/default")
               .content(TestUtils.convertObjectToJsonBytes(null)))
           .andExpect(status().isOk())
           .andExpect(content().contentType(
               new MediaType(MediaType.TEXT_PLAIN.getType(), MediaType.TEXT_PLAIN.getSubtype(), StandardCharsets.UTF_8)))
           .andExpect(content().string(xsltObject.getXslt()));

    verify(securedDatasetService, times(1)).getLatestDefaultXslt();
  }

  @Test
  void getLatestDefaultXslt_NoXsltFound_404() throws Exception {
    when(securedDatasetService.getLatestDefaultXslt())
        .thenThrow(new NoXsltFoundException("No xslt found"));
    mockMvc.perform(get("/secured/datasets/xslt/default")
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(null)))
           .andExpect(status().isNotFound());

    verify(securedDatasetService, times(1)).getLatestDefaultXslt();
  }

  @Test
  void transformRecordsUsingLatestDatasetXslt() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(JWT_DATA_OFFICER);
    List<Record> listOfRecords = TestObjectFactory.createListOfRecords(5);
    when(securedDatasetService.transformRecordsUsingLatestDatasetXslt(anyString(), anyList())).thenReturn(listOfRecords);
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
  void transformRecordsUsingLatestDatasetXslt_UserUnauthorizedException() throws Exception {
    when(jwtDecoder.decode(MOCK_INVALID_TOKEN)).thenReturn(JWT_INVALID_ROLE);
    mockMvc
        .perform(post("/secured/datasets/{datasetId}/xslt/transform", Integer.toString(TestObjectFactory.DATASETID))
            .header("Authorization", BEARER + MOCK_INVALID_TOKEN)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(TestUtils.convertObjectToJsonBytes(TestObjectFactory.createListOfRecords(5))))
        .andExpect(status().isForbidden());
  }

  @Test
  void transformRecordsUsingLatestDefaultXslt() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(JWT_DATA_OFFICER);
    List<eu.europeana.metis.core.rest.Record> listOfRecords = TestObjectFactory.createListOfRecords(5);
    when(securedDatasetService.transformRecordsUsingLatestDefaultXslt(anyString(), anyList())).thenReturn(listOfRecords);
    mockMvc
        .perform(post("/secured/datasets/{datasetId}/xslt/transform/default", Integer.toString(TestObjectFactory.DATASETID))
            .header("Authorization", BEARER + MOCK_VALID_TOKEN)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(TestUtils.convertObjectToJsonBytes(listOfRecords)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$", hasSize(5)));
  }

  @Test
  void transformRecordsUsingLatestDefaultXslt_UserUnauthorizedException() throws Exception {
    when(jwtDecoder.decode(MOCK_INVALID_TOKEN)).thenReturn(JWT_INVALID_ROLE);
    mockMvc
        .perform(post("/secured/datasets/{datasetId}/xslt/transform/default", Integer.toString(TestObjectFactory.DATASETID))
            .header("Authorization", BEARER + MOCK_INVALID_TOKEN)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(TestUtils.convertObjectToJsonBytes(TestObjectFactory.createListOfRecords(5))))
        .andExpect(status().isForbidden());
  }

  @Test
  void getByDatasetName() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(JWT_DATA_OFFICER);
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);

    when(securedDatasetService.getDatasetByDatasetName(TestObjectFactory.DATASETNAME)).thenReturn(dataset);
    mockMvc.perform(get(String.format("/secured/datasets/dataset_name/%s", TestObjectFactory.DATASETNAME))
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(null)))
           .andExpect(status().isOk())
           .andExpect(content().contentType(MediaType.APPLICATION_JSON))
           .andExpect(jsonPath("$.datasetName", is(TestObjectFactory.DATASETNAME)))
           .andExpect(jsonPath("$.datasetId", is(Integer.toString(TestObjectFactory.DATASETID))));

    ArgumentCaptor<String> datasetNameArgumentCaptor = ArgumentCaptor.forClass(String.class);
    verify(securedDatasetService, times(1)).getDatasetByDatasetName(datasetNameArgumentCaptor.capture());
    assertEquals(TestObjectFactory.DATASETNAME, datasetNameArgumentCaptor.getValue());
  }

  @Test
  void getByDatasetNameInvalidUser() throws Exception {
    when(jwtDecoder.decode(MOCK_INVALID_TOKEN)).thenReturn(JWT_INVALID_ROLE);
    mockMvc.perform(get(String.format("/secured/datasets/dataset_name/%s", TestObjectFactory.DATASETNAME))
               .header("Authorization", BEARER + MOCK_INVALID_TOKEN)
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(null)))
           .andExpect(status().isForbidden());

    verify(securedDatasetService, times(0)).getDatasetByDatasetName(anyString());
  }

  @Test
  void getByDatasetName_noDatasetFound_Returns404() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(JWT_DATA_OFFICER);
    when(securedDatasetService.getDatasetByDatasetName(TestObjectFactory.DATASETNAME)).thenThrow(
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
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(JWT_DATA_OFFICER);
    List<Dataset> datasetList = getDatasets();
    when(securedDatasetService.getAllDatasetsByProvider("myProvider", 3)).thenReturn(datasetList);
    when(securedDatasetService.getDatasetsPerRequestLimit()).thenReturn(5);

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
    verify(securedDatasetService, times(1)).getAllDatasetsByProvider(provider.capture(), page.capture());

    assertEquals("myProvider", provider.getValue());
    assertEquals(3, page.getValue().intValue());
  }

  @Test
  void getAllDatasetsByProviderNegativeNextPage() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(JWT_DATA_OFFICER);
    mockMvc.perform(get("/secured/datasets/provider/myProvider")
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .param("nextPage", "-1")
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(null)))
           .andExpect(status().isNotAcceptable());
  }

  @Test
  void getAllDatasetsByProviderInvalidUser() throws Exception {
    when(jwtDecoder.decode(MOCK_INVALID_TOKEN)).thenReturn(JWT_INVALID_ROLE);
    mockMvc.perform(get("/secured/datasets/provider/myProvider")
               .header("Authorization", BEARER + MOCK_INVALID_TOKEN)
               .param("nextPage", "3")
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(null)))
           .andExpect(status().isForbidden());

    verify(securedDatasetService, times(0)).getAllDatasetsByProvider(anyString(), anyInt());
  }

  @Test
  void getAllDatasetsByIntermediateProvider() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(JWT_DATA_OFFICER);
    List<Dataset> datasetList = getDatasets();
    when(securedDatasetService.getAllDatasetsByIntermediateProvider("myIntermediateProvider", 3)).thenReturn(datasetList);
    when(securedDatasetService.getDatasetsPerRequestLimit()).thenReturn(5);

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
    verify(securedDatasetService, times(1)).getAllDatasetsByIntermediateProvider(provider.capture(), page.capture());

    assertEquals("myIntermediateProvider", provider.getValue());
    assertEquals(3, page.getValue().intValue());
  }

  @Test
  void getAllDatasetsByIntermediateProviderNegativeNextPage() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(JWT_DATA_OFFICER);
    mockMvc.perform(get("/secured/datasets/intermediate_provider/myIntermediateProvider")
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .param("nextPage", "-1")
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(null)))
           .andExpect(status().isNotAcceptable());
  }

  @Test
  void getAllDatasetsByIntermediateProviderInvalidUser() throws Exception {
    when(jwtDecoder.decode(MOCK_INVALID_TOKEN)).thenReturn(JWT_INVALID_ROLE);
    mockMvc.perform(get("/secured/datasets/intermediate_provider/myIntermediateProvider")
               .header("Authorization", BEARER + MOCK_INVALID_TOKEN)
               .param("nextPage", "3")
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(null)))
           .andExpect(status().isForbidden());

    verify(securedDatasetService, times(0)).getAllDatasetsByIntermediateProvider(anyString(), anyInt());
  }


  @Test
  void getAllDatasetsByDataProvider() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(JWT_DATA_OFFICER);
    List<Dataset> datasetList = getDatasets();
    when(securedDatasetService.getAllDatasetsByDataProvider("myDataProvider", 3)).thenReturn(datasetList);
    when(securedDatasetService.getDatasetsPerRequestLimit()).thenReturn(5);

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
    verify(securedDatasetService, times(1))
        .getAllDatasetsByDataProvider(provider.capture(), page.capture());

    assertEquals("myDataProvider", provider.getValue());
    assertEquals(3, page.getValue().intValue());
  }

  @Test
  void getAllDatasetsByDataProviderNegativeNextPage() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(JWT_DATA_OFFICER);
    mockMvc.perform(get("/secured/datasets/data_provider/myDataProvider")
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .param("nextPage", "-1")
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(null)))
           .andExpect(status().isNotAcceptable());
  }

  @Test
  void getAllDatasetsByDataProviderInvalidUser() throws Exception {
    when(jwtDecoder.decode(MOCK_INVALID_TOKEN)).thenReturn(JWT_INVALID_ROLE);
    mockMvc.perform(get("/secured/datasets/data_provider/myDataProvider")
               .header("Authorization", BEARER + MOCK_INVALID_TOKEN)
               .param("nextPage", "3")
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(null)))
           .andExpect(status().isForbidden());
    verify(securedDatasetService, times(0)).getAllDatasetsByDataProvider(anyString(), anyInt());
  }

  @Test
  void getAllDatasetsByOrganizationId() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(JWT_DATA_OFFICER);
    List<Dataset> datasetList = getDatasets();
    when(securedDatasetService.getAllDatasetsByOrganizationId("myOrganizationId", 3)).thenReturn(datasetList);
    when(securedDatasetService.getDatasetsPerRequestLimit()).thenReturn(5);

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
    verify(securedDatasetService, times(1))
        .getAllDatasetsByOrganizationId(provider.capture(), page.capture());

    assertEquals("myOrganizationId", provider.getValue());
    assertEquals(3, page.getValue().intValue());
  }

  @Test
  void getAllDatasetsByOrganizationIdNegativeNextPage() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(JWT_DATA_OFFICER);
    mockMvc.perform(get("/secured/datasets/organization_id/myOrganizationId")
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .param("nextPage", "-1")
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(null)))
           .andExpect(status().isNotAcceptable());
  }

  @Test
  void getAllDatasetsByOrganizationIdInvalidUser() throws Exception {
    when(jwtDecoder.decode(MOCK_INVALID_TOKEN)).thenReturn(JWT_INVALID_ROLE);
    mockMvc.perform(get("/secured/datasets/organization_id/myOrganizationId")
               .header("Authorization", BEARER + MOCK_INVALID_TOKEN)
               .param("nextPage", "3")
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(null)))
           .andExpect(status().isForbidden());

    verify(securedDatasetService, times(0))
        .getAllDatasetsByOrganizationId(anyString(), anyInt());
  }

  @Test
  void getAllDatasetsByOrganizationName() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(JWT_DATA_OFFICER);
    List<Dataset> datasetList = getDatasets();
    when(securedDatasetService.getAllDatasetsByOrganizationName("myOrganizationName", 3))
        .thenReturn(datasetList);
    when(securedDatasetService.getDatasetsPerRequestLimit()).thenReturn(5);

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
    verify(securedDatasetService, times(1))
        .getAllDatasetsByOrganizationName(provider.capture(), page.capture());

    assertEquals("myOrganizationName", provider.getValue());
    assertEquals(3, page.getValue().intValue());
  }

  @Test
  void getAllDatasetsByOrganizationNameNegativeNextPage() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(JWT_DATA_OFFICER);
    mockMvc.perform(get("/secured/datasets/organization_name/myOrganizationName")
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .param("nextPage", "-1")
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(null)))
           .andExpect(status().isNotAcceptable());
  }

  @Test
  void getAllDatasetsByOrganizationNameInvalidUser() throws Exception {
    when(jwtDecoder.decode(MOCK_INVALID_TOKEN)).thenReturn(JWT_INVALID_ROLE);
    mockMvc.perform(get("/secured/datasets/organization_name/myOrganizationName")
               .header("Authorization", BEARER + MOCK_INVALID_TOKEN)
               .param("nextPage", "3")
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(null)))
           .andExpect(status().isForbidden());

    verify(securedDatasetService, times(0)).getAllDatasetsByOrganizationName(anyString(), anyInt());
  }

  @Test
  void getDatasetsCountries() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(JWT_DATA_OFFICER);
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
  void getDatasetsCountriesInvalidUser() throws Exception {
    when(jwtDecoder.decode(MOCK_INVALID_TOKEN)).thenReturn(JWT_INVALID_ROLE);
    mockMvc.perform(get("/secured/datasets/countries")
               .header("Authorization", BEARER + MOCK_INVALID_TOKEN)
               .contentType(MediaType.APPLICATION_JSON)
               .content(""))
           .andExpect(status().isForbidden());
  }

  @Test
  void getDatasetsLanguages() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(JWT_DATA_OFFICER);
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
  void getDatasetsLanguagesInvalidUser() throws Exception {
    when(jwtDecoder.decode(MOCK_INVALID_TOKEN)).thenReturn(JWT_INVALID_ROLE);
    mockMvc.perform(get("/secured/datasets/languages")
               .header("Authorization", BEARER + MOCK_INVALID_TOKEN)
               .contentType(MediaType.APPLICATION_JSON)
               .content(""))
           .andExpect(status().isForbidden());
  }

    @Test
    void getDatasetSearch() throws Exception {
      when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(JWT_DATA_OFFICER);
      when(securedDatasetService.searchDatasetsBasedOnSearchString("test", 3)).thenReturn(getDatasetSearchViews());
      when(securedDatasetService.getDatasetsPerRequestLimit()).thenReturn(5);

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
      verify(securedDatasetService, times(1))
          .searchDatasetsBasedOnSearchString(searchString.capture(), page.capture());

      assertEquals("test", searchString.getValue());
      assertEquals(3, page.getValue().intValue());
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
