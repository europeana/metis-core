package eu.europeana.metis.core.rest.controller;

import eu.europeana.metis.core.exceptions.NoDatasetFoundException;
import eu.europeana.metis.core.exceptions.NoScheduledWorkflowFoundException;
import eu.europeana.metis.core.exceptions.NoWorkflowFoundException;
import eu.europeana.metis.core.exceptions.ScheduledWorkflowAlreadyExistsException;
import eu.europeana.metis.core.rest.config.SecurityConfig;
import eu.europeana.metis.core.rest.config.properties.SecurityConfigurationProperties;
import eu.europeana.metis.core.rest.exception.RestResponseExceptionHandler;
import eu.europeana.metis.core.rest.utils.TestJwtUtils;
import eu.europeana.metis.core.rest.utils.TestUtils;
import eu.europeana.metis.core.service.ScheduleWorkflowService;
import eu.europeana.metis.core.service.UserService;
import eu.europeana.metis.core.workflow.ScheduleFrequence;
import eu.europeana.metis.core.workflow.ScheduledWorkflow;
import eu.europeana.metis.exception.BadContentException;
import eu.europeana.metis.utils.RestEndpoints;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static com.jayway.jsonassert.impl.matcher.IsCollectionWithSize.hasSize;
import static eu.europeana.metis.core.rest.utils.TestJwtUtils.BEARER;
import static eu.europeana.metis.core.rest.utils.TestJwtUtils.MOCK_INVALID_TOKEN;
import static eu.europeana.metis.core.rest.utils.TestJwtUtils.MOCK_VALID_TOKEN;
import static eu.europeana.metis.core.rest.utils.TestObjectFactory.DATASETID;
import static eu.europeana.metis.core.rest.utils.TestObjectFactory.createListOfScheduledWorkflows;
import static eu.europeana.metis.core.rest.utils.TestObjectFactory.createScheduledWorkflowObject;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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

@WebMvcTest(ScheduleWorkflowController.class)
@ContextConfiguration(classes = {ScheduleWorkflowController.class, SecurityConfig.class, RestResponseExceptionHandler.class})
class TestScheduleWorkflowController {

  @MockBean
  private ScheduleWorkflowService scheduleWorkflowService;

  @MockBean
  private JwtDecoder jwtDecoder;

  @MockBean
  private UserService userService;

  private static MockMvc mockMvc;
  private final TestJwtUtils testJwtUtils;

  @Autowired
  public TestScheduleWorkflowController(SecurityConfigurationProperties securityConfigurationProperties) {
    testJwtUtils = new TestJwtUtils(securityConfigurationProperties.resourceNames());
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
    reset(scheduleWorkflowService);
    reset(jwtDecoder);
    reset(userService);
  }

  @Test
  void scheduleWorkflowExecution() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    ScheduledWorkflow scheduledWorkflow = createScheduledWorkflowObject();
    mockMvc.perform(post(RestEndpoints.ORCHESTRATOR_WORKFLOWS_SCHEDULE)
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(scheduledWorkflow)))
           .andExpect(status().isCreated())
           .andExpect(content().string(""));
    verify(scheduleWorkflowService, times(1))
        .scheduleWorkflow(any(ScheduledWorkflow.class));
  }

  @Test
  void scheduleWorkflowExecution_Unauthenticated() throws Exception {
    ScheduledWorkflow scheduledWorkflow = createScheduledWorkflowObject();
    mockMvc.perform(post(RestEndpoints.ORCHESTRATOR_WORKFLOWS_SCHEDULE)
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(scheduledWorkflow)))
           .andExpect(status().isUnauthorized());
  }

  @Test
  void scheduleWorkflowExecution_Unauthorized() throws Exception {
    when(jwtDecoder.decode(MOCK_INVALID_TOKEN)).thenReturn(testJwtUtils.getInvalidRoleJwt());
    ScheduledWorkflow scheduledWorkflow = createScheduledWorkflowObject();
    mockMvc.perform(post(RestEndpoints.ORCHESTRATOR_WORKFLOWS_SCHEDULE)
               .header("Authorization", BEARER + MOCK_INVALID_TOKEN)
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(scheduledWorkflow)))
           .andExpect(status().isForbidden());
  }

  @Test
  void scheduleWorkflowExecution_BadContentException() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    ScheduledWorkflow scheduledWorkflow = createScheduledWorkflowObject();
    doThrow(new BadContentException("Some error")).when(scheduleWorkflowService)
                                                  .scheduleWorkflow(any(ScheduledWorkflow.class));
    mockMvc.perform(post(RestEndpoints.ORCHESTRATOR_WORKFLOWS_SCHEDULE)
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(scheduledWorkflow)))
           .andExpect(status().isNotAcceptable())
           .andExpect(content().string("{\"errorMessage\":\"Some error\"}"));
  }

  @Test
  void scheduleWorkflowExecution_ScheduledWorkflowAlreadyExistsException() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    ScheduledWorkflow scheduledWorkflow = createScheduledWorkflowObject();
    doThrow(new ScheduledWorkflowAlreadyExistsException("Some error")).when(scheduleWorkflowService)
                                                                      .scheduleWorkflow(
                                                                          any(ScheduledWorkflow.class));
    mockMvc.perform(post(RestEndpoints.ORCHESTRATOR_WORKFLOWS_SCHEDULE)
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(scheduledWorkflow)))
           .andExpect(status().isConflict())
           .andExpect(content().string("{\"errorMessage\":\"Some error\"}"));
  }

  @Test
  void scheduleWorkflowExecution_NoWorkflowFoundException() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    ScheduledWorkflow scheduledWorkflow = createScheduledWorkflowObject();
    doThrow(new NoWorkflowFoundException("Some error")).when(scheduleWorkflowService)
                                                       .scheduleWorkflow(any(ScheduledWorkflow.class));
    mockMvc.perform(post(RestEndpoints.ORCHESTRATOR_WORKFLOWS_SCHEDULE)
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(scheduledWorkflow)))
           .andExpect(status().isNotFound())
           .andExpect(content().string("{\"errorMessage\":\"Some error\"}"));
  }

  @Test
  void scheduleWorkflowExecution_NoDatasetFoundException() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    ScheduledWorkflow scheduledWorkflow = createScheduledWorkflowObject();
    doThrow(new NoDatasetFoundException("Some error")).when(scheduleWorkflowService)
                                                      .scheduleWorkflow(any(ScheduledWorkflow.class));
    mockMvc.perform(post(RestEndpoints.ORCHESTRATOR_WORKFLOWS_SCHEDULE)
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(scheduledWorkflow)))
           .andExpect(status().isNotFound())
           .andExpect(content().string("{\"errorMessage\":\"Some error\"}"));
  }

  @Test
  void getScheduledWorkflow() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    ScheduledWorkflow scheduledWorkflow = createScheduledWorkflowObject();
    when(scheduleWorkflowService.getScheduledWorkflowByDatasetId(anyString()))
        .thenReturn(scheduledWorkflow);
    mockMvc.perform(get(RestEndpoints.ORCHESTRATOR_WORKFLOWS_SCHEDULE_DATASETID,
               Integer.toString(DATASETID))
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .contentType(MediaType.APPLICATION_JSON)
               .content(""))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.scheduleFrequence", is(ScheduleFrequence.ONCE.name())));
    verify(scheduleWorkflowService, times(1))
        .getScheduledWorkflowByDatasetId(anyString());
  }

  @Test
  void getAllScheduledWorkflows() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    int listSize = 2;
    List<ScheduledWorkflow> listOfScheduledWorkflows = createListOfScheduledWorkflows(listSize + 1);//To get the effect of next page

    when(scheduleWorkflowService.getScheduledWorkflowsPerRequest()).thenReturn(listSize);
    when(scheduleWorkflowService
        .getAllScheduledWorkflows(any(ScheduleFrequence.class), anyInt()))
        .thenReturn(listOfScheduledWorkflows);
    mockMvc.perform(get(RestEndpoints.ORCHESTRATOR_WORKFLOWS_SCHEDULE)
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .param("nextPage", "")
               .contentType(MediaType.APPLICATION_JSON)
               .content(""))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.results", hasSize(listSize + 1)))
           .andExpect(jsonPath("$.results[0].datasetId", is(Integer.toString(DATASETID))))
           .andExpect(jsonPath("$.results[0].scheduleFrequence", is(ScheduleFrequence.ONCE.name())))
           .andExpect(jsonPath("$.results[1].datasetId", is(Integer.toString(DATASETID + 1))))
           .andExpect(jsonPath("$.results[1].scheduleFrequence", is(ScheduleFrequence.ONCE.name())))
           .andExpect(jsonPath("$.nextPage").isNotEmpty());
  }

  @Test
  void getAllScheduledWorkflowsNegativeNextPage() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    mockMvc.perform(get(RestEndpoints.ORCHESTRATOR_WORKFLOWS_SCHEDULE)
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .param("nextPage", "-1")
               .contentType(MediaType.APPLICATION_JSON)
               .content(""))
           .andExpect(status().isNotAcceptable());
  }

  @Test
  void updateScheduledWorkflow() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    ScheduledWorkflow scheduledWorkflow = createScheduledWorkflowObject();
    mockMvc.perform(put(RestEndpoints.ORCHESTRATOR_WORKFLOWS_SCHEDULE)
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(scheduledWorkflow)))
           .andExpect(status().isNoContent())
           .andExpect(content().string(""));
    verify(scheduleWorkflowService, times(1))
        .updateScheduledWorkflow(any(ScheduledWorkflow.class));
  }

  @Test
  void updateScheduledWorkflow_Unauthenticated() throws Exception {
    ScheduledWorkflow scheduledWorkflow = createScheduledWorkflowObject();
    mockMvc.perform(put(RestEndpoints.ORCHESTRATOR_WORKFLOWS_SCHEDULE)
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(scheduledWorkflow)))
           .andExpect(status().isUnauthorized());
  }

  @Test
  void updateScheduledWorkflow_Unauthorized() throws Exception {
    when(jwtDecoder.decode(MOCK_INVALID_TOKEN)).thenReturn(testJwtUtils.getInvalidRoleJwt());
    ScheduledWorkflow scheduledWorkflow = createScheduledWorkflowObject();
    mockMvc.perform(put(RestEndpoints.ORCHESTRATOR_WORKFLOWS_SCHEDULE)
               .header("Authorization", BEARER + MOCK_INVALID_TOKEN)
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(scheduledWorkflow)))
           .andExpect(status().isForbidden());
  }

  @Test
  void updateScheduledWorkflow_NoWorkflowFoundException() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    ScheduledWorkflow scheduledWorkflow = createScheduledWorkflowObject();
    doThrow(new NoWorkflowFoundException("Some error"))
        .when(scheduleWorkflowService).updateScheduledWorkflow(any(ScheduledWorkflow.class));
    mockMvc.perform(put(RestEndpoints.ORCHESTRATOR_WORKFLOWS_SCHEDULE)
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(scheduledWorkflow)))
           .andExpect(status().isNotFound())
           .andExpect(content().string("{\"errorMessage\":\"Some error\"}"));
  }

  @Test
  void updateScheduledWorkflow_NoScheduledWorkflowFoundException() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    ScheduledWorkflow scheduledWorkflow = createScheduledWorkflowObject();
    doThrow(new NoScheduledWorkflowFoundException("Some error"))
        .when(scheduleWorkflowService).updateScheduledWorkflow(any(ScheduledWorkflow.class));
    mockMvc.perform(put(RestEndpoints.ORCHESTRATOR_WORKFLOWS_SCHEDULE)
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(scheduledWorkflow)))
           .andExpect(status().isNotFound())
           .andExpect(content().string("{\"errorMessage\":\"Some error\"}"));
  }

  @Test
  void updateScheduledWorkflow_BadContentException() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    ScheduledWorkflow scheduledWorkflow = createScheduledWorkflowObject();
    doThrow(new BadContentException("Some error"))
        .when(scheduleWorkflowService).updateScheduledWorkflow(any(ScheduledWorkflow.class));
    mockMvc.perform(put(RestEndpoints.ORCHESTRATOR_WORKFLOWS_SCHEDULE)
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(scheduledWorkflow)))
           .andExpect(status().isNotAcceptable())
           .andExpect(content().string("{\"errorMessage\":\"Some error\"}"));
  }

  @Test
  void deleteScheduledWorkflowExecution() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    mockMvc.perform(delete(RestEndpoints.ORCHESTRATOR_WORKFLOWS_SCHEDULE_DATASETID,
               Integer.toString(DATASETID))
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .contentType(MediaType.APPLICATION_JSON)
               .content(""))
           .andExpect(status().isNoContent())
           .andExpect(content().string(""));
    verify(scheduleWorkflowService, times(1))
        .deleteScheduledWorkflow(anyString());
  }


  @Test
  void deleteScheduledWorkflowExecution_Unauthenticated() throws Exception {
    mockMvc.perform(delete(RestEndpoints.ORCHESTRATOR_WORKFLOWS_SCHEDULE_DATASETID,
               Integer.toString(DATASETID))
               .contentType(MediaType.APPLICATION_JSON)
               .content(""))
           .andExpect(status().isUnauthorized());
  }

  @Test
  void deleteScheduledWorkflowExecution_Unauthorized() throws Exception {
    when(jwtDecoder.decode(MOCK_INVALID_TOKEN)).thenReturn(testJwtUtils.getInvalidRoleJwt());
    mockMvc.perform(delete(RestEndpoints.ORCHESTRATOR_WORKFLOWS_SCHEDULE_DATASETID,
               Integer.toString(DATASETID))
               .header("Authorization", BEARER + MOCK_INVALID_TOKEN)
               .contentType(MediaType.APPLICATION_JSON)
               .content(""))
           .andExpect(status().isForbidden());
  }
}
