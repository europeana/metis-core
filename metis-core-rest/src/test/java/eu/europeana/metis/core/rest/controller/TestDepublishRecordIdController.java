package eu.europeana.metis.core.rest.controller;

import static eu.europeana.metis.core.rest.utils.TestJwtUtils.BEARER;
import static eu.europeana.metis.core.rest.utils.TestJwtUtils.MOCK_INVALID_TOKEN;
import static eu.europeana.metis.core.rest.utils.TestJwtUtils.MOCK_VALID_TOKEN;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.europeana.metis.core.exceptions.NoDatasetFoundException;
import eu.europeana.metis.core.exceptions.NoWorkflowFoundException;
import eu.europeana.metis.core.exceptions.PluginExecutionNotAllowed;
import eu.europeana.metis.core.rest.DepublishRecordIdView;
import eu.europeana.metis.core.rest.ResponseListWrapper;
import eu.europeana.metis.core.rest.config.SecurityConfig;
import eu.europeana.metis.core.rest.config.properties.SecurityConfigurationProperties;
import eu.europeana.metis.core.rest.exception.RestResponseExceptionHandler;
import eu.europeana.metis.core.rest.utils.TestJwtUtils;
import eu.europeana.metis.core.service.DepublishRecordIdService;
import eu.europeana.metis.core.workflow.WorkflowExecution;
import eu.europeana.metis.exception.BadContentException;
import eu.europeana.metis.exception.ExternalTaskException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@WebMvcTest(DepublishRecordIdController.class)
@ContextConfiguration(classes = {DepublishRecordIdController.class, SecurityConfig.class,
    RestResponseExceptionHandler.class})
class TestDepublishRecordIdController {

  @MockBean
  private DepublishRecordIdService depublishRecordIdService;

  @MockBean
  private JwtDecoder jwtDecoder;

  private static MockMvc mockMvc;

  private final TestJwtUtils testJwtUtils;

  @Autowired
  public TestDepublishRecordIdController(SecurityConfigurationProperties securityConfigurationProperties) {
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
    reset(depublishRecordIdService);
    reset(jwtDecoder);
  }

  @Test
  void createRecordIdsToBeDepublishedString() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    String datasetId = "dataset123";
    String recordIds = "1\n2\n3";
    when(depublishRecordIdService.addRecordIdsToBeDepublished(datasetId, recordIds)).thenReturn(3);

    mockMvc.perform(post("/depublish/record_ids/{datasetId}", datasetId)
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .contentType(MediaType.TEXT_PLAIN)
               .content(recordIds))
           .andExpect(status().isCreated());
    verify(depublishRecordIdService, times(1)).addRecordIdsToBeDepublished(any(String.class), any(String.class));
  }

  @Test
  void createRecordIdsToBeDepublishedStringUnauthenticated() throws Exception {
    String datasetId = "dataset123";
    String recordIds = "1\n2\n3";

    mockMvc.perform(post("/depublish/record_ids/{datasetId}", datasetId)
               .contentType(MediaType.TEXT_PLAIN)
               .content(recordIds))
           .andExpect(status().isUnauthorized());
  }

  @Test
  void createRecordIdsToBeDepublishedStringInvalidUser() throws Exception {
    when(jwtDecoder.decode(MOCK_INVALID_TOKEN)).thenReturn(testJwtUtils.getInvalidRoleJwt());
    String datasetId = "dataset123";
    String recordIds = "1\n2\n3";

    mockMvc.perform(post("/depublish/record_ids/{datasetId}", datasetId)
               .header("Authorization", BEARER + MOCK_INVALID_TOKEN)
               .contentType(MediaType.TEXT_PLAIN)
               .content(recordIds))
           .andExpect(status().isForbidden());
  }

  @Test
  void createRecordIdsToBeDepublishedFile() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    String datasetId = "dataset123";
    MockMultipartFile file = new MockMultipartFile("depublicationFile", "recordIds.txt", "text/plain", "1\n2\n3".getBytes());
    when(depublishRecordIdService.addRecordIdsToBeDepublished(datasetId, "1\n2\n3")).thenReturn(3);

    mockMvc.perform(multipart("/depublish/record_ids/{datasetId}", datasetId)
               .file(file)
               .header("Authorization", BEARER + MOCK_VALID_TOKEN))
           .andExpect(status().isCreated());
    verify(depublishRecordIdService, times(1)).addRecordIdsToBeDepublished(any(String.class), any(String.class));
  }

  @Test
  void createRecordIdsToBeDepublishedFileUnauthenticated() throws Exception {
    String datasetId = "dataset123";
    MockMultipartFile file = new MockMultipartFile("depublicationFile", "recordIds.txt", "text/plain", "1\n2\n3".getBytes());

    mockMvc.perform(multipart("/depublish/record_ids/{datasetId}", datasetId).file(file))
           .andExpect(status().isUnauthorized());
  }

  @Test
  void createRecordIdsToBeDepublishedFileInvalidUser() throws Exception {
    when(jwtDecoder.decode(MOCK_INVALID_TOKEN)).thenReturn(testJwtUtils.getInvalidRoleJwt());
    String datasetId = "dataset123";
    MockMultipartFile file = new MockMultipartFile("depublicationFile", "recordIds.txt", "text/plain", "1\n2\n3".getBytes());
    when(depublishRecordIdService.addRecordIdsToBeDepublished(datasetId, "1\n2\n3")).thenReturn(3);

    mockMvc.perform(multipart("/depublish/record_ids/{datasetId}", datasetId)
               .file(file)
               .header("Authorization", BEARER + MOCK_INVALID_TOKEN))
           .andExpect(status().isForbidden());
    verify(depublishRecordIdService, times(0)).addRecordIdsToBeDepublished(any(String.class), any(String.class));
  }

  @Test
  void deletePendingRecordIds() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    String datasetId = "dataset123";
    String recordIds = "1\n2\n3";
    when(depublishRecordIdService.deletePendingRecordIds(datasetId, recordIds)).thenReturn(3L);

    mockMvc.perform(delete("/depublish/record_ids/{datasetId}", datasetId)
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .contentType(MediaType.TEXT_PLAIN)
               .content(recordIds))
           .andExpect(status().isNoContent());
    verify(depublishRecordIdService, times(1)).deletePendingRecordIds(any(String.class), any(String.class));
  }

  @Test
  void deletePendingRecordIdsUnauthenticated() throws Exception {
    String datasetId = "dataset123";
    String recordIds = "1\n2\n3";

    mockMvc.perform(delete("/depublish/record_ids/{datasetId}", datasetId)
               .contentType(MediaType.TEXT_PLAIN)
               .content(recordIds))
           .andExpect(status().isUnauthorized());
  }

  @Test
  void deletePendingRecordIdsInvalidUser() throws Exception {
    when(jwtDecoder.decode(MOCK_INVALID_TOKEN)).thenReturn(testJwtUtils.getInvalidRoleJwt());
    String datasetId = "dataset123";
    String recordIds = "1\n2\n3";

    mockMvc.perform(delete("/depublish/record_ids/{datasetId}", datasetId)
               .header("Authorization", BEARER + MOCK_INVALID_TOKEN)
               .contentType(MediaType.TEXT_PLAIN)
               .content(recordIds))
           .andExpect(status().isForbidden());
  }

  @Test
  void deletePendingRecordIds_NoDatasetFoundException() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    String datasetId = "dataset123";
    String recordIds = "1\n2\n3";
    when(depublishRecordIdService.deletePendingRecordIds(datasetId, recordIds)).thenThrow(
        new NoDatasetFoundException("Dataset not found"));

    mockMvc.perform(delete("/depublish/record_ids/{datasetId}", datasetId)
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .contentType(MediaType.TEXT_PLAIN)
               .content(recordIds))
           .andExpect(status().isNotFound())
           .andExpect(jsonPath("$.errorMessage").value("Dataset not found"));
    verify(depublishRecordIdService, times(1)).deletePendingRecordIds(any(String.class), any(String.class));
  }

  @Test
  void deletePendingRecordIds_BadContentException() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    String datasetId = "dataset123";
    String recordIds = "1\n2\n3";
    when(depublishRecordIdService.deletePendingRecordIds(datasetId, recordIds)).thenThrow(
        new BadContentException("Bad content"));

    mockMvc.perform(delete("/depublish/record_ids/{datasetId}", datasetId)
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .contentType(MediaType.TEXT_PLAIN)
               .content(recordIds))
           .andExpect(status().isNotAcceptable())
           .andExpect(jsonPath("$.errorMessage").value("Bad content"));
    verify(depublishRecordIdService, times(1)).deletePendingRecordIds(any(String.class), any(String.class));
  }

  @Test
  void getDepublishRecordIds() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    String datasetId = "dataset123";
    final ResponseListWrapper<DepublishRecordIdView> result = new ResponseListWrapper<>();
    result.setResultsAndLastPage(null, 1, 1);
    when(depublishRecordIdService.getDepublishRecordIds(anyString(), anyInt(), any(), any(), anyString())).thenReturn(
        result);
    when(depublishRecordIdService.canTriggerDepublication(anyString())).thenReturn(true);

    mockMvc.perform(get("/depublish/record_ids/{datasetId}", datasetId)
               .param("page", "0")
               .param("sortField", "RECORD_ID")
               .param("sortAscending", "true")
               .param("searchQuery", "searchQuery")
               .header("Authorization", BEARER + MOCK_VALID_TOKEN))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.depublicationTriggerable").value(true));
    verify(depublishRecordIdService, times(1)).getDepublishRecordIds(anyString(), anyInt(), any(), any(), anyString());
    verify(depublishRecordIdService, times(1)).canTriggerDepublication(anyString());
  }

  @Test
  void getDepublishRecordIds_NoDatasetFoundException() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    String datasetId = "dataset123";
    final ResponseListWrapper<DepublishRecordIdView> result = new ResponseListWrapper<>();
    result.setResultsAndLastPage(null, 1, 1);
    when(depublishRecordIdService.getDepublishRecordIds(anyString(), anyInt(), any(), any(), anyString()))
        .thenReturn(result).thenThrow(new NoDatasetFoundException("Dataset not found"));
    when(depublishRecordIdService.canTriggerDepublication(anyString())).thenThrow(
        new NoDatasetFoundException("Dataset not found")).thenReturn(true);

    mockMvc.perform(get("/depublish/record_ids/{datasetId}", datasetId)
               .param("page", "0")
               .param("sortField", "RECORD_ID")
               .param("sortAscending", "true")
               .param("searchQuery", "searchQuery")
               .header("Authorization", BEARER + MOCK_VALID_TOKEN))
           .andExpect(status().isNotFound())
           .andExpect(jsonPath("$.errorMessage").value("Dataset not found"));

    mockMvc.perform(get("/depublish/record_ids/{datasetId}", datasetId)
               .param("page", "0")
               .param("sortField", "RECORD_ID")
               .param("sortAscending", "true")
               .param("searchQuery", "searchQuery")
               .header("Authorization", BEARER + MOCK_VALID_TOKEN))
           .andExpect(status().isNotFound())
           .andExpect(jsonPath("$.errorMessage").value("Dataset not found"));
    verify(depublishRecordIdService, times(2)).getDepublishRecordIds(anyString(), anyInt(), any(), any(), anyString());
    verify(depublishRecordIdService, times(1)).canTriggerDepublication(anyString());
  }

  @Test
  void getDepublishRecordIdsUnauthenticated() throws Exception {
    String datasetId = "dataset123";
    final ResponseListWrapper<DepublishRecordIdView> result = new ResponseListWrapper<>();
    result.setResultsAndLastPage(null, 1, 1);

    mockMvc.perform(get("/depublish/record_ids/{datasetId}", datasetId)
               .param("page", "0")
               .param("sortField", "RECORD_ID")
               .param("sortAscending", "true")
               .param("searchQuery", "searchQuery"))
           .andExpect(status().isUnauthorized());
  }

  @Test
  void getDepublishRecordIdsInvalidUser() throws Exception {
    when(jwtDecoder.decode(MOCK_INVALID_TOKEN)).thenReturn(testJwtUtils.getInvalidRoleJwt());
    String datasetId = "dataset123";
    final ResponseListWrapper<DepublishRecordIdView> result = new ResponseListWrapper<>();
    result.setResultsAndLastPage(null, 1, 1);

    mockMvc.perform(get("/depublish/record_ids/{datasetId}", datasetId)
               .param("page", "0")
               .param("sortField", "RECORD_ID")
               .param("sortAscending", "true")
               .param("searchQuery", "searchQuery")
               .header("Authorization", BEARER + MOCK_INVALID_TOKEN))
           .andExpect(status().isForbidden());
  }

  @Test
  void addDepublishWorkflowInQueue() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    String datasetId = "dataset123";
    String recordIds = "1\n2\n3";
    WorkflowExecution execution = new WorkflowExecution();
    when(depublishRecordIdService.createAndAddInQueueDepublishWorkflowExecution(anyString(), anyBoolean(), anyInt(),
        anyString(), any(), any())).thenReturn(execution);

    mockMvc.perform(post("/depublish/execute/{datasetId}", datasetId)
               .param("datasetDepublish", "false")
               .param("depublicationReason", "BROKEN_MEDIA_LINKS")
               .param("priority", "0")
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .content(recordIds))
           .andExpect(status().isCreated());

    mockMvc.perform(post("/depublish/execute/{datasetId}", datasetId)
               .param("datasetDepublish", "true")
               .param("depublicationReason", "BROKEN_MEDIA_LINKS")
               .param("priority", "0")
               .header("Authorization", BEARER + MOCK_VALID_TOKEN))
           .andExpect(status().isCreated());
    verify(depublishRecordIdService, times(1))
        .createAndAddInQueueDepublishWorkflowExecution(anyString(), anyBoolean(), anyInt(), anyString(), any(), any());
    verify(depublishRecordIdService, times(1))
        .createAndAddInQueueDepublishWorkflowExecution(anyString(), anyBoolean(), anyInt(), isNull(), any(), any());
  }

  @Test
  void addDepublishWorkflowInQueue_Exception() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    String datasetId = "dataset123";
    String recordIds = "1\n2\n3";
    when(depublishRecordIdService.createAndAddInQueueDepublishWorkflowExecution(anyString(), anyBoolean(), anyInt(),
        anyString(), any(), any())).thenThrow(new NoDatasetFoundException("Dataset not found"))
                                   .thenThrow(new PluginExecutionNotAllowed("Plugin not allowed"))
                                   .thenThrow(new NoWorkflowFoundException("No workflow found"))
                                   .thenThrow(new ExternalTaskException("Bad request"))
                                   .thenThrow(new BadContentException("Bad content"));


    final MockHttpServletRequestBuilder operation = post("/depublish/execute/{datasetId}", datasetId)
        .param("datasetDepublish", "false")
        .param("depublicationReason", "BROKEN_MEDIA_LINKS")
        .param("priority", "0")
        .header("Authorization", BEARER + MOCK_VALID_TOKEN)
        .content(recordIds);
    mockMvc.perform(operation)
           .andExpect(status().isNotFound())
           .andExpect(jsonPath("$.errorMessage").value("Dataset not found"));
    mockMvc.perform(operation)
           .andExpect(status().isNotAcceptable())
           .andExpect(jsonPath("$.errorMessage").value("Plugin not allowed"));
    mockMvc.perform(operation)
           .andExpect(status().isNotFound())
           .andExpect(jsonPath("$.errorMessage").value("No workflow found"));
    mockMvc.perform(operation)
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.errorMessage").value("Bad request"));
    mockMvc.perform(operation)
           .andExpect(status().isNotAcceptable())
           .andExpect(jsonPath("$.errorMessage").value("Bad content"));
    verify(depublishRecordIdService, times(5))
        .createAndAddInQueueDepublishWorkflowExecution(anyString(), anyBoolean(), anyInt(), anyString(), any(), any());
  }

  @Test
  void addDepublishWorkflowInQueueUnauthenticated() throws Exception {
    String datasetId = "dataset123";
    String recordIds = "1\n2\n3";

    mockMvc.perform(post("/depublish/execute/{datasetId}", datasetId)
               .param("datasetDepublish", "false")
               .param("depublicationReason", "BROKEN_MEDIA_LINKS")
               .param("priority", "0")
               .content(recordIds))
           .andExpect(status().isUnauthorized());
  }

  @Test
  void addDepublishWorkflowInQueueInvalidUser() throws Exception {
    when(jwtDecoder.decode(MOCK_INVALID_TOKEN)).thenReturn(testJwtUtils.getInvalidRoleJwt());
    String datasetId = "dataset123";
    String recordIds = "1\n2\n3";

    mockMvc.perform(post("/depublish/execute/{datasetId}", datasetId)
               .param("datasetDepublish", "false")
               .param("depublicationReason", "BROKEN_MEDIA_LINKS")
               .param("priority", "0")
               .header("Authorization", BEARER + MOCK_INVALID_TOKEN)
               .content(recordIds))
           .andExpect(status().isForbidden());
  }

  @Test
  void getDepublicationReasons() throws Exception {
    mockMvc.perform(get("/depublish/reasons"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$").isArray());
  }
}

