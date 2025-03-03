package eu.europeana.metis.core.rest.controller;

import static eu.europeana.metis.core.rest.utils.TestJwtUtils.BEARER;
import static eu.europeana.metis.core.rest.utils.TestJwtUtils.MOCK_INVALID_TOKEN;
import static eu.europeana.metis.core.rest.utils.TestJwtUtils.MOCK_VALID_TOKEN;
import static eu.europeana.metis.utils.RestEndpoints.ORCHESTRATOR_WORKFLOWS_DATASETID;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
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

import eu.europeana.metis.core.common.DaoFieldNames;
import eu.europeana.metis.core.dataset.DatasetExecutionInformation;
import eu.europeana.metis.core.exceptions.NoDatasetFoundException;
import eu.europeana.metis.core.exceptions.NoWorkflowExecutionFoundException;
import eu.europeana.metis.core.exceptions.NoWorkflowFoundException;
import eu.europeana.metis.core.exceptions.WorkflowAlreadyExistsException;
import eu.europeana.metis.core.exceptions.WorkflowExecutionAlreadyExistsException;
import eu.europeana.metis.core.rest.ExecutionHistory;
import eu.europeana.metis.core.rest.ExecutionHistory.Execution;
import eu.europeana.metis.core.rest.PluginsWithDataAvailability;
import eu.europeana.metis.core.rest.PluginsWithDataAvailability.PluginWithDataAvailability;
import eu.europeana.metis.core.rest.ResponseListWrapper;
import eu.europeana.metis.core.rest.VersionEvolution;
import eu.europeana.metis.core.rest.VersionEvolution.VersionEvolutionStep;
import eu.europeana.metis.core.rest.config.SecurityConfig;
import eu.europeana.metis.core.rest.config.properties.SecurityConfigurationProperties;
import eu.europeana.metis.core.rest.exception.RestResponseExceptionHandler;
import eu.europeana.metis.core.rest.execution.details.WorkflowExecutionView;
import eu.europeana.metis.core.rest.execution.overview.ExecutionAndDatasetView;
import eu.europeana.metis.core.rest.utils.TestJwtUtils;
import eu.europeana.metis.core.rest.utils.TestObjectFactory;
import eu.europeana.metis.core.rest.utils.TestUtils;
import eu.europeana.metis.core.service.OrchestratorService;
import eu.europeana.metis.core.workflow.Workflow;
import eu.europeana.metis.core.workflow.WorkflowExecution;
import eu.europeana.metis.core.workflow.WorkflowStatus;
import eu.europeana.metis.core.workflow.plugins.AbstractExecutablePlugin;
import eu.europeana.metis.core.workflow.plugins.ExecutablePluginFactory;
import eu.europeana.metis.core.workflow.plugins.ExecutablePluginType;
import eu.europeana.metis.core.workflow.plugins.PluginType;
import eu.europeana.metis.core.workflow.plugins.ValidationExternalPluginMetadata;
import eu.europeana.metis.utils.RestEndpoints;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.TimeZone;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
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

@WebMvcTest(OrchestratorController.class)
@ContextConfiguration(classes = {OrchestratorController.class, SecurityConfig.class, RestResponseExceptionHandler.class})
class TestOrchestratorController {

  @MockBean
  private OrchestratorService orchestratorService;

  @MockBean
  private JwtDecoder jwtDecoder;

  private static MockMvc mockMvc;
  private final TestJwtUtils testJwtUtils;

  @Autowired
  public TestOrchestratorController(SecurityConfigurationProperties securityConfigurationProperties) {
    testJwtUtils = new TestJwtUtils(securityConfigurationProperties.getResourceNames());
  }

  private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
      "yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

  static {
    simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
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
    reset(orchestratorService);
    reset(jwtDecoder);
  }

  @Test
  void createWorkflow() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    Workflow workflow = TestObjectFactory.createWorkflowObject();
    mockMvc.perform(post(ORCHESTRATOR_WORKFLOWS_DATASETID, Integer.toString(TestObjectFactory.DATASETID))
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(workflow)))
           .andExpect(status().isCreated())
           .andExpect(content().string(""));

    verify(orchestratorService, times(1)).createWorkflow(anyString(), any(Workflow.class), isNull());
  }

  @Test
  void createWorkflow_Unauthenticated() throws Exception {
    Workflow workflow = TestObjectFactory.createWorkflowObject();
    mockMvc.perform(post(ORCHESTRATOR_WORKFLOWS_DATASETID, Integer.toString(TestObjectFactory.DATASETID))
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(workflow)))
           .andExpect(status().isUnauthorized());

    verify(orchestratorService, never()).createWorkflow(anyString(), any(Workflow.class), any());
  }

  @Test
  void createWorkflow_Unauthorized() throws Exception {
    when(jwtDecoder.decode(MOCK_INVALID_TOKEN)).thenReturn(testJwtUtils.getInvalidRoleJwt());
    Workflow workflow = TestObjectFactory.createWorkflowObject();
    mockMvc.perform(post(ORCHESTRATOR_WORKFLOWS_DATASETID, Integer.toString(TestObjectFactory.DATASETID))
               .header("Authorization", BEARER + MOCK_INVALID_TOKEN)
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(workflow)))
           .andExpect(status().isForbidden());

    verify(orchestratorService, never()).createWorkflow(anyString(), any(Workflow.class), any());
  }

  @Test
  void createWorkflow_WorkflowAlreadyExistsException() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    Workflow workflow = TestObjectFactory.createWorkflowObject();
    doThrow(new WorkflowAlreadyExistsException("Some error")).when(orchestratorService)
                                                             .createWorkflow(anyString(), any(Workflow.class), any());
    mockMvc.perform(post(ORCHESTRATOR_WORKFLOWS_DATASETID, Integer.toString(TestObjectFactory.DATASETID))
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(workflow)))
           .andExpect(status().isConflict())
           .andExpect(jsonPath("$.errorMessage", is("Some error")));

    verify(orchestratorService, times(1)).createWorkflow(anyString(), any(Workflow.class), isNull());
  }

  @Test
  void updateWorkflow() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    Workflow workflow = TestObjectFactory.createWorkflowObject();
    mockMvc.perform(
               put(RestEndpoints.ORCHESTRATOR_WORKFLOWS_DATASETID, Integer.toString(TestObjectFactory.DATASETID))
                   .header("Authorization", BEARER + MOCK_VALID_TOKEN)
                   .contentType(MediaType.APPLICATION_JSON)
                   .content(TestUtils.convertObjectToJsonBytes(workflow)))
           .andExpect(status().isNoContent())
           .andExpect(content().string(""));

    verify(orchestratorService, times(1)).updateWorkflow(anyString(), any(Workflow.class), isNull());
  }

  @Test
  void updateWorkflow_Unauthenticated() throws Exception {
    Workflow workflow = TestObjectFactory.createWorkflowObject();
    mockMvc.perform(put(RestEndpoints.ORCHESTRATOR_WORKFLOWS_DATASETID, Integer.toString(TestObjectFactory.DATASETID))
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(workflow)))
           .andExpect(status().isUnauthorized());
  }

  @Test
  void updateWorkflow_Unauthorized() throws Exception {
    when(jwtDecoder.decode(MOCK_INVALID_TOKEN)).thenReturn(testJwtUtils.getInvalidRoleJwt());
    Workflow workflow = TestObjectFactory.createWorkflowObject();
    mockMvc.perform(put(RestEndpoints.ORCHESTRATOR_WORKFLOWS_DATASETID,
               Integer.toString(TestObjectFactory.DATASETID))
               .header("Authorization", BEARER + MOCK_INVALID_TOKEN)
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(workflow)))
           .andExpect(status().isForbidden());
  }

  @Test
  void updateWorkflow_NoWorkflowFoundException() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    Workflow workflow = TestObjectFactory.createWorkflowObject();
    doThrow(new NoWorkflowFoundException("Some error")).when(orchestratorService)
                                                       .updateWorkflow(anyString(), any(Workflow.class), isNull());
    mockMvc.perform(put(RestEndpoints.ORCHESTRATOR_WORKFLOWS_DATASETID,
               Integer.toString(TestObjectFactory.DATASETID))
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .contentType(MediaType.APPLICATION_JSON)
               .content(TestUtils.convertObjectToJsonBytes(workflow)))
           .andExpect(status().isNotFound())
           .andExpect(jsonPath("$.errorMessage", is("Some error")));

    verify(orchestratorService, times(1)).updateWorkflow(anyString(), any(Workflow.class), isNull());
  }

  @Test
  void deleteWorkflow() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    mockMvc.perform(
               delete(RestEndpoints.ORCHESTRATOR_WORKFLOWS_DATASETID, Integer.toString(TestObjectFactory.DATASETID))
                   .header("Authorization", BEARER + MOCK_VALID_TOKEN)
                   .contentType(MediaType.APPLICATION_JSON)
                   .content(""))
           .andExpect(status().isNoContent())
           .andExpect(content().string(""));
    verify(orchestratorService, times(1)).deleteWorkflow(anyString());
  }

  @Test
  void deleteWorkflow_Unauthenticated() throws Exception {
    mockMvc.perform(delete(RestEndpoints.ORCHESTRATOR_WORKFLOWS_DATASETID, Integer.toString(TestObjectFactory.DATASETID))
                   .contentType(MediaType.APPLICATION_JSON)
                   .content(""))
           .andExpect(status().isUnauthorized());
  }

  @Test
  void deleteWorkflow_Unauthorized() throws Exception {
    when(jwtDecoder.decode(MOCK_INVALID_TOKEN)).thenReturn(testJwtUtils.getInvalidRoleJwt());
    mockMvc.perform(
               delete(RestEndpoints.ORCHESTRATOR_WORKFLOWS_DATASETID, Integer.toString(TestObjectFactory.DATASETID))
                   .header("Authorization", BEARER + MOCK_INVALID_TOKEN)
                   .contentType(MediaType.APPLICATION_JSON)
                   .content(""))
           .andExpect(status().isForbidden());
  }

  @Test
  void getWorkflow() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    Workflow workflow = TestObjectFactory.createWorkflowObject();
    when(orchestratorService.getWorkflow(anyString())).thenReturn(workflow);
    mockMvc.perform(get(RestEndpoints.ORCHESTRATOR_WORKFLOWS_DATASETID,
               Integer.toString(TestObjectFactory.DATASETID))
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .contentType(MediaType.APPLICATION_JSON)
               .content(""))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.datasetId", is(workflow.getDatasetId())));

    verify(orchestratorService, times(1)).getWorkflow(anyString());
  }

  @Test
  void addWorkflowInQueueOfWorkflowExecutions() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    WorkflowExecution workflowExecution = TestObjectFactory.createWorkflowExecutionObject();
    when(
        orchestratorService.addWorkflowInQueueOfWorkflowExecutions(anyString(), isNull(), isNull(), anyInt(), anyString()))
        .thenReturn(workflowExecution);
    mockMvc.perform(
               post(RestEndpoints.ORCHESTRATOR_WORKFLOWS_DATASETID_EXECUTE,
                   Integer.toString(TestObjectFactory.DATASETID))
                   .header("Authorization", BEARER + MOCK_VALID_TOKEN)
                   .contentType(MediaType.APPLICATION_JSON)
                   .content(""))
           .andExpect(status().isCreated())
           .andExpect(jsonPath("$.workflowStatus", is(WorkflowStatus.INQUEUE.name())));
  }

  @Test
  void addWorkflowInQueueOfWorkflowExecutions_Unauthenticated() throws Exception {
    mockMvc.perform(post(RestEndpoints.ORCHESTRATOR_WORKFLOWS_DATASETID_EXECUTE, Integer.toString(TestObjectFactory.DATASETID))
                   .contentType(MediaType.APPLICATION_JSON)
                   .content(""))
           .andExpect(status().isUnauthorized());
  }

  @Test
  void addWorkflowInQueueOfWorkflowExecutions_Unauthorized() throws Exception {
    when(jwtDecoder.decode(MOCK_INVALID_TOKEN)).thenReturn(testJwtUtils.getInvalidRoleJwt());
    mockMvc.perform(
               post(RestEndpoints.ORCHESTRATOR_WORKFLOWS_DATASETID_EXECUTE,
                   Integer.toString(TestObjectFactory.DATASETID))
                   .header("Authorization", BEARER + MOCK_INVALID_TOKEN)
                   .contentType(MediaType.APPLICATION_JSON)
                   .content(""))
           .andExpect(status().isForbidden());
  }

  @Test
  void addWorkflowInQueueOfWorkflowExecutions_WorkflowExecutionAlreadyExistsException() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    doThrow(new WorkflowExecutionAlreadyExistsException("Some error"))
        .when(orchestratorService)
        .addWorkflowInQueueOfWorkflowExecutions(anyString(), isNull(), isNull(), anyInt(), anyString());
    mockMvc.perform(
               post(RestEndpoints.ORCHESTRATOR_WORKFLOWS_DATASETID_EXECUTE,
                   Integer.toString(TestObjectFactory.DATASETID))
                   .header("Authorization", BEARER + MOCK_VALID_TOKEN)
                   .contentType(MediaType.APPLICATION_JSON)
                   .content(""))
           .andExpect(status().isConflict())
           .andExpect(jsonPath("$.errorMessage", is("Some error")));
  }

  @Test
  void addWorkflowInQueueOfWorkflowExecutions_NoDatasetFoundException()
      throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    doThrow(new NoDatasetFoundException("Some error"))
        .when(orchestratorService)
        .addWorkflowInQueueOfWorkflowExecutions(anyString(), isNull(), isNull(), anyInt(), anyString());
    mockMvc.perform(
               post(RestEndpoints.ORCHESTRATOR_WORKFLOWS_DATASETID_EXECUTE,
                   Integer.toString(TestObjectFactory.DATASETID))
                   .header("Authorization", BEARER + MOCK_VALID_TOKEN)
                   .contentType(MediaType.APPLICATION_JSON)
                   .content(""))
           .andExpect(status().isNotFound())
           .andExpect(jsonPath("$.errorMessage", is("Some error")));
  }

  @Test
  void addWorkflowInQueueOfWorkflowExecutions_NoWorkflowFoundException()
      throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    doThrow(new NoWorkflowFoundException("Some error"))
        .when(orchestratorService)
        .addWorkflowInQueueOfWorkflowExecutions(anyString(), isNull(), isNull(), anyInt(), anyString());
    mockMvc.perform(
               post(RestEndpoints.ORCHESTRATOR_WORKFLOWS_DATASETID_EXECUTE,
                   Integer.toString(TestObjectFactory.DATASETID))
                   .header("Authorization", BEARER + MOCK_VALID_TOKEN)
                   .contentType(MediaType.APPLICATION_JSON)
                   .content(""))
           .andExpect(status().isNotFound())
           .andExpect(jsonPath("$.errorMessage", is("Some error")));
  }

  @Test
  void cancelWorkflowExecution() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    doNothing().when(orchestratorService).cancelWorkflowExecution(anyString(), anyString());
    mockMvc.perform(
               delete(RestEndpoints.ORCHESTRATOR_WORKFLOWS_EXECUTIONS_EXECUTIONID, TestObjectFactory.EXECUTIONID)
                   .header("Authorization", BEARER + MOCK_VALID_TOKEN)
                   .contentType(MediaType.APPLICATION_JSON)
                   .content(""))
           .andExpect(status().isNoContent())
           .andExpect(content().string(""));
  }

  @Test
  void cancelWorkflowExecution_Unauthenticated() throws Exception {
    mockMvc.perform(delete(RestEndpoints.ORCHESTRATOR_WORKFLOWS_EXECUTIONS_EXECUTIONID, TestObjectFactory.EXECUTIONID)
               .contentType(MediaType.APPLICATION_JSON)
               .content(""))
           .andExpect(status().isUnauthorized());
  }

  @Test
  void cancelWorkflowExecution_Unauthorized() throws Exception {
    when(jwtDecoder.decode(MOCK_INVALID_TOKEN)).thenReturn(testJwtUtils.getInvalidRoleJwt());
    mockMvc.perform(delete(RestEndpoints.ORCHESTRATOR_WORKFLOWS_EXECUTIONS_EXECUTIONID,
               TestObjectFactory.EXECUTIONID)
               .header("Authorization", BEARER + MOCK_INVALID_TOKEN)
               .contentType(MediaType.APPLICATION_JSON)
               .content(""))
           .andExpect(status().isForbidden());
  }

  @Test
  void cancelWorkflowExecution_NoWorkflowExecutionFoundException() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    doThrow(new NoWorkflowExecutionFoundException("Some error"))
        .when(orchestratorService).cancelWorkflowExecution(anyString(), anyString());
    mockMvc.perform(delete(RestEndpoints.ORCHESTRATOR_WORKFLOWS_EXECUTIONS_EXECUTIONID,
               TestObjectFactory.EXECUTIONID)
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .contentType(MediaType.APPLICATION_JSON)
               .content(""))
           .andExpect(status().isNotFound())
           .andExpect(jsonPath("$.errorMessage", is("Some error")));
  }

  @Test
  void getWorkflowExecutionByExecutionId() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    WorkflowExecution workflowExecution = TestObjectFactory
        .createWorkflowExecutionObject();
    workflowExecution.setWorkflowStatus(WorkflowStatus.RUNNING);
    when(orchestratorService.getWorkflowExecutionByExecutionId(anyString())).thenReturn(workflowExecution);
    mockMvc.perform(get(RestEndpoints.ORCHESTRATOR_WORKFLOWS_EXECUTIONS_EXECUTIONID,
               TestObjectFactory.EXECUTIONID)
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .contentType(MediaType.APPLICATION_JSON)
               .content(""))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.workflowStatus", is(WorkflowStatus.RUNNING.name())));
  }

  @Test
  void getLatestFinishedPluginWorkflowExecutionByDatasetIdIfPluginTypeAllowedForExecution() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    AbstractExecutablePlugin plugin = ExecutablePluginFactory.createPlugin(new ValidationExternalPluginMetadata());
    plugin.setId("validation_external_id");
    when(orchestratorService.getLatestFinishedPluginByDatasetIdIfPluginTypeAllowedForExecution(
        Integer.toString(TestObjectFactory.DATASETID),
        ExecutablePluginType.VALIDATION_EXTERNAL,
        null))
        .thenReturn(plugin);

    mockMvc.perform(
               get(RestEndpoints.ORCHESTRATOR_WORKFLOWS_EXECUTIONS_DATASET_DATASETID_ALLOWED_PLUGIN,
                   Integer.toString(TestObjectFactory.DATASETID))
                   .header("Authorization", BEARER + MOCK_VALID_TOKEN)
                   .contentType(MediaType.APPLICATION_JSON)
                   .param("pluginType", "VALIDATION_EXTERNAL")
                   .content(""))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.pluginType", is(PluginType.VALIDATION_EXTERNAL.name())));
  }

  @Test
  void getLatestFinishedPluginWorkflowExecutionByDatasetIdIfPluginTypeAllowedForExecution_HarvestingPlugin()
      throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    when(orchestratorService.getLatestFinishedPluginByDatasetIdIfPluginTypeAllowedForExecution(
        Integer.toString(TestObjectFactory.DATASETID), ExecutablePluginType.OAIPMH_HARVEST, null))
        .thenReturn(null);

    mockMvc.perform(
               get(RestEndpoints.ORCHESTRATOR_WORKFLOWS_EXECUTIONS_DATASET_DATASETID_ALLOWED_PLUGIN,
                   Integer.toString(TestObjectFactory.DATASETID))
                   .header("Authorization", BEARER + MOCK_VALID_TOKEN)
                   .contentType(MediaType.APPLICATION_JSON)
                   .param("pluginType", "OAIPMH_HARVEST")
                   .content(""))
           .andExpect(status().isOk());
  }

  @Test
  void getDatasetExecutionInformation() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    DatasetExecutionInformation datasetExecutionInformation = new DatasetExecutionInformation();
    datasetExecutionInformation.setLastHarvestedDate(new Date(1000));
    datasetExecutionInformation.setLastHarvestedRecords(100);
    datasetExecutionInformation.setFirstPublishedDate(new Date(2000));
    datasetExecutionInformation.setLastPublishedDate(new Date(3000));
    datasetExecutionInformation.setLastPublishedRecords(100);
    when(orchestratorService
        .getDatasetExecutionInformation(Integer.toString(TestObjectFactory.DATASETID)))
        .thenReturn(datasetExecutionInformation);

    mockMvc.perform(get(RestEndpoints.ORCHESTRATOR_WORKFLOWS_EXECUTIONS_DATASET_DATASETID_INFORMATION,
               Integer.toString(TestObjectFactory.DATASETID))
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .contentType(MediaType.APPLICATION_JSON)
               .content(""))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.lastHarvestedDate",
               is(simpleDateFormat.format(datasetExecutionInformation.getLastHarvestedDate()))))
           .andExpect(jsonPath("$.lastHarvestedRecords",
               is(datasetExecutionInformation.getLastHarvestedRecords())))
           .andExpect(jsonPath("$.firstPublishedDate",
               is(simpleDateFormat.format(datasetExecutionInformation.getFirstPublishedDate()))))
           .andExpect(jsonPath("$.lastPublishedDate",
               is(simpleDateFormat.format(datasetExecutionInformation.getLastPublishedDate()))))
           .andExpect(jsonPath("$.lastPublishedRecords",
               is(datasetExecutionInformation.getLastPublishedRecords())));
  }

  @Test
  void getAllWorkflowExecutionsByDatasetId() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    int listSize = 2;
    ResponseListWrapper<WorkflowExecutionView> listOfWorkflowExecutions = new ResponseListWrapper<>();
    listOfWorkflowExecutions.setResultsAndLastPage(
        TestObjectFactory.createListOfWorkflowExecutions(listSize + 1),
        orchestratorService.getWorkflowExecutionsPerRequest(), 0);

    when(orchestratorService.getWorkflowExecutionsPerRequest()).thenReturn(listSize);
    when(orchestratorService.getAllWorkflowExecutions(anyString(),
        ArgumentMatchers.anySet(), any(DaoFieldNames.class), anyBoolean(), anyInt()))
        .thenReturn(listOfWorkflowExecutions);
    mockMvc.perform(get(RestEndpoints.ORCHESTRATOR_WORKFLOWS_EXECUTIONS_DATASET_DATASETID,
               Integer.toString(TestObjectFactory.DATASETID))
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .param("workflowStatus", WorkflowStatus.INQUEUE.name())
               .param("nextPage", "")
               .contentType(MediaType.APPLICATION_JSON)
               .content(""))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.results", hasSize(listSize + 1)))
           .andExpect(jsonPath("$.results[0].datasetId", is(Integer.toString(TestObjectFactory.DATASETID))))
           .andExpect(jsonPath("$.results[0].workflowStatus", is(WorkflowStatus.INQUEUE.name())))
           .andExpect(jsonPath("$.results[1].datasetId", is(Integer.toString(TestObjectFactory.DATASETID + 1))))
           .andExpect(jsonPath("$.results[1].workflowStatus", is(WorkflowStatus.INQUEUE.name())))
           .andExpect(jsonPath("$.nextPage").isNotEmpty());
  }

  @Test
  void getAllWorkflowExecutionsByDatasetIdNegativeNextPage() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    mockMvc.perform(get(RestEndpoints.ORCHESTRATOR_WORKFLOWS_EXECUTIONS_DATASET_DATASETID,
               Integer.toString(TestObjectFactory.DATASETID))
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .param("workflowStatus", WorkflowStatus.INQUEUE.name())
               .param("nextPage", "-1")
               .contentType(MediaType.APPLICATION_JSON)
               .content(""))
           .andExpect(status().isNotAcceptable());
  }

  @Test
  void getAllWorkflowExecutions() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    int listSize = 2;
    ResponseListWrapper<WorkflowExecutionView> listOfWorkflowExecutions = new ResponseListWrapper<>();
    listOfWorkflowExecutions.setResultsAndLastPage(
        TestObjectFactory.createListOfWorkflowExecutions(listSize + 1),
        orchestratorService.getWorkflowExecutionsPerRequest(), 0);

    when(orchestratorService.getWorkflowExecutionsPerRequest()).thenReturn(listSize);
    when(orchestratorService.getAllWorkflowExecutions(isNull(),
        ArgumentMatchers.anySet(), any(DaoFieldNames.class), anyBoolean(), anyInt()))
        .thenReturn(listOfWorkflowExecutions);
    mockMvc.perform(get(RestEndpoints.ORCHESTRATOR_WORKFLOWS_EXECUTIONS)
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .param("workflowStatus", WorkflowStatus.INQUEUE.name())
               .param("nextPage", "")
               .contentType(MediaType.APPLICATION_JSON)
               .content(""))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.results", hasSize(listSize + 1)))
           .andExpect(jsonPath("$.results[0].datasetId", is(Integer.toString(TestObjectFactory.DATASETID))))
           .andExpect(jsonPath("$.results[0].workflowStatus", is(WorkflowStatus.INQUEUE.name())))
           .andExpect(jsonPath("$.results[1].datasetId", is(Integer.toString(TestObjectFactory.DATASETID + 1))))
           .andExpect(jsonPath("$.results[1].workflowStatus", is(WorkflowStatus.INQUEUE.name())))
           .andExpect(jsonPath("$.nextPage").isNotEmpty());
  }

  @Test
  void getAllWorkflowExecutionsNegativeNextPage() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    mockMvc.perform(get(RestEndpoints.ORCHESTRATOR_WORKFLOWS_EXECUTIONS)
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .param("workflowStatus", WorkflowStatus.INQUEUE.name())
               .param("nextPage", "-1")
               .contentType(MediaType.APPLICATION_JSON)
               .content(""))
           .andExpect(status().isNotAcceptable());
  }

  @Test
  void getWorkflowExecutionsOverview() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    final int pageSize = 2;
    final int nextPage = 5;
    final int pageCount = 3;
    final ResponseListWrapper<ExecutionAndDatasetView> listOfWorkflowExecutionAndDatasetViews = new ResponseListWrapper<>();
    listOfWorkflowExecutionAndDatasetViews.setResultsAndLastPage(
        TestObjectFactory.createListOfExecutionOverviews(pageSize * pageCount),
        orchestratorService.getWorkflowExecutionsPerRequest(), nextPage, pageCount);

    when(orchestratorService.getWorkflowExecutionsPerRequest()).thenReturn(pageSize);
    when(orchestratorService
        .getWorkflowExecutionsOverview(isNull(), isNull(), isNull(), isNull(),
            eq(nextPage), eq(pageCount)))
        .thenReturn(listOfWorkflowExecutionAndDatasetViews);
    mockMvc.perform(get(RestEndpoints.ORCHESTRATOR_WORKFLOWS_EXECUTIONS_OVERVIEW)
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .param("nextPage", "" + nextPage)
               .param("pageCount", "" + pageCount)
               .contentType(MediaType.APPLICATION_JSON)
               .content(""))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.results", hasSize(pageSize * pageCount)))
           .andExpect(jsonPath("$.results[0].dataset.datasetId", is(Integer.toString(TestObjectFactory.DATASETID))))
           .andExpect(jsonPath("$.results[0].execution.workflowStatus", is(WorkflowStatus.INQUEUE.name())))
           .andExpect(jsonPath("$.results[1].dataset.datasetId", is(Integer.toString(TestObjectFactory.DATASETID + 1))))
           .andExpect(jsonPath("$.results[1].execution.workflowStatus", is(WorkflowStatus.INQUEUE.name())))
           .andExpect(jsonPath("$.nextPage", is(nextPage + pageCount)))
           .andExpect(jsonPath("$.listSize", is(pageSize * pageCount)));
  }

  @Test
  void getWorkflowExecutionsOverviewBadPaginationArguments() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());
    mockMvc.perform(get(RestEndpoints.ORCHESTRATOR_WORKFLOWS_EXECUTIONS_OVERVIEW)
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .param("nextPage", "-1")
               .contentType(MediaType.APPLICATION_JSON)
               .content(""))
           .andExpect(status().isNotAcceptable());
  }

  @Test
  void testGetDatasetExecutionHistory() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());

    // Create nonempty history
    final Execution execution1 = new Execution();
    execution1.setWorkflowExecutionId("execution 1");
    execution1.setStartedDate(new Date(1));
    final Execution execution2 = new Execution();
    execution2.setWorkflowExecutionId("execution 2");
    execution2.setStartedDate(new Date(2));
    final ExecutionHistory resultNonEmpty = new ExecutionHistory();
    resultNonEmpty.setExecutions(Arrays.asList(execution1, execution2));

    // Test happy flow with non-empty evolution
    when(orchestratorService.getDatasetExecutionHistory("" + TestObjectFactory.DATASETID)).thenReturn(resultNonEmpty);
    mockMvc.perform(
               get(RestEndpoints.ORCHESTRATOR_WORKFLOWS_EXECUTIONS_DATASET_DATASETID_HISTORY, TestObjectFactory.DATASETID)
                   .header("Authorization", BEARER + MOCK_VALID_TOKEN))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.executions", hasSize(2)))
           .andExpect(jsonPath("$.executions[0].workflowExecutionId", is(execution1.getWorkflowExecutionId())))
           .andExpect(jsonPath("$.executions[0].startedDate", is(simpleDateFormat.format(execution1.getStartedDate()))))
           .andExpect(jsonPath("$.executions[1].workflowExecutionId", is(execution2.getWorkflowExecutionId())))
           .andExpect(jsonPath("$.executions[1].startedDate", is(simpleDateFormat.format(execution2.getStartedDate()))));

    // Test happy flow with empty evolution
    final ExecutionHistory resultEmpty = new ExecutionHistory();
    resultEmpty.setExecutions(Collections.emptyList());
    when(orchestratorService.getDatasetExecutionHistory("" + TestObjectFactory.DATASETID)).thenReturn(resultEmpty);
    mockMvc.perform(
               get(RestEndpoints.ORCHESTRATOR_WORKFLOWS_EXECUTIONS_DATASET_DATASETID_HISTORY, TestObjectFactory.DATASETID)
                   .header("Authorization", BEARER + MOCK_VALID_TOKEN))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.executions", hasSize(0)));

    // Test for bad input
    when(orchestratorService.getDatasetExecutionHistory("" + TestObjectFactory.DATASETID)).thenThrow(
        new NoDatasetFoundException(""));
    mockMvc.perform(
               get(RestEndpoints.ORCHESTRATOR_WORKFLOWS_EXECUTIONS_DATASET_DATASETID_HISTORY, TestObjectFactory.DATASETID)
                   .header("Authorization", BEARER + MOCK_VALID_TOKEN))
           .andExpect(status().isNotFound());

    // Test for unauthorized user
    when(jwtDecoder.decode(MOCK_INVALID_TOKEN)).thenReturn(testJwtUtils.getInvalidRoleJwt());
    mockMvc.perform(
               get(RestEndpoints.ORCHESTRATOR_WORKFLOWS_EXECUTIONS_DATASET_DATASETID_HISTORY, TestObjectFactory.DATASETID)
                   .header("Authorization", BEARER + MOCK_INVALID_TOKEN))
           .andExpect(status().isForbidden());
  }

  @Test
  void testGetExecutablePluginsWithDataAvailability() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());

    // Create nonempty history
    final PluginWithDataAvailability plugin1 = new PluginWithDataAvailability();
    plugin1.setPluginType(PluginType.OAIPMH_HARVEST);
    plugin1.setCanDisplayRawXml(true);
    final PluginWithDataAvailability plugin2 = new PluginWithDataAvailability();
    plugin2.setPluginType(PluginType.ENRICHMENT);
    plugin2.setCanDisplayRawXml(false);
    final PluginsWithDataAvailability resultNonEmpty = new PluginsWithDataAvailability();
    resultNonEmpty.setPlugins(Arrays.asList(plugin1, plugin2));

    // Test happy flow with non-empty evolution
    when(orchestratorService
        .getExecutablePluginsWithDataAvailability(TestObjectFactory.EXECUTIONID))
        .thenReturn(resultNonEmpty);
    mockMvc.perform(get(RestEndpoints.ORCHESTRATOR_WORKFLOWS_EXECUTIONS_EXECUTIONID_PLUGINS_DATA_AVAILABILITY,
               TestObjectFactory.EXECUTIONID)
               .header("Authorization", BEARER + MOCK_VALID_TOKEN))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.plugins", hasSize(2)))
           .andExpect(jsonPath("$.plugins[0].pluginType", is(plugin1.getPluginType().name())))
           .andExpect(jsonPath("$.plugins[0].canDisplayRawXml", is(plugin1.isCanDisplayRawXml())))
           .andExpect(jsonPath("$.plugins[1].pluginType", is(plugin2.getPluginType().name())))
           .andExpect(jsonPath("$.plugins[1].canDisplayRawXml", is(plugin2.isCanDisplayRawXml())));

    // Test happy flow with empty evolution
    final PluginsWithDataAvailability resultEmpty = new PluginsWithDataAvailability();
    resultEmpty.setPlugins(Collections.emptyList());
    when(orchestratorService
        .getExecutablePluginsWithDataAvailability(TestObjectFactory.EXECUTIONID))
        .thenReturn(resultEmpty);
    mockMvc.perform(get(RestEndpoints.ORCHESTRATOR_WORKFLOWS_EXECUTIONS_EXECUTIONID_PLUGINS_DATA_AVAILABILITY,
               TestObjectFactory.EXECUTIONID)
               .header("Authorization", BEARER + MOCK_VALID_TOKEN))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.plugins", hasSize(0)));

    // Test for bad input
    when(orchestratorService
        .getExecutablePluginsWithDataAvailability(TestObjectFactory.EXECUTIONID))
        .thenThrow(new NoWorkflowExecutionFoundException(""));
    mockMvc.perform(get(RestEndpoints.ORCHESTRATOR_WORKFLOWS_EXECUTIONS_EXECUTIONID_PLUGINS_DATA_AVAILABILITY,
               TestObjectFactory.EXECUTIONID)
               .header("Authorization", BEARER + MOCK_VALID_TOKEN))
           .andExpect(status().isNotFound());

    // Test for unauthorized user
    when(jwtDecoder.decode(MOCK_INVALID_TOKEN)).thenReturn(testJwtUtils.getInvalidRoleJwt());
    mockMvc.perform(get(RestEndpoints.ORCHESTRATOR_WORKFLOWS_EXECUTIONS_EXECUTIONID_PLUGINS_DATA_AVAILABILITY,
               TestObjectFactory.EXECUTIONID)
               .header("Authorization", BEARER + MOCK_INVALID_TOKEN))
           .andExpect(status().isForbidden());
  }

    @Test
    void testGetRecordEvolutionForVersion() throws Exception {
      when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(testJwtUtils.getDataOfficerJwt());

      // Create nonempty evolution step
      final VersionEvolutionStep step1 = new VersionEvolutionStep();
      step1.setFinishedTime(new Date(1));
      step1.setPluginType(ExecutablePluginType.OAIPMH_HARVEST);
      step1.setWorkflowExecutionId("execution 1");
      final VersionEvolutionStep step2 = new VersionEvolutionStep();
      step2.setFinishedTime(new Date(2));
      step2.setPluginType(ExecutablePluginType.TRANSFORMATION);
      step2.setWorkflowExecutionId("execution 2");
      final VersionEvolution resultNonEmpty = new VersionEvolution();
      resultNonEmpty.setEvolutionSteps(Arrays.asList(step1, step2));

      // Test happy flow with non-empty evolution
      final PluginType pluginType = PluginType.MEDIA_PROCESS;
      when(orchestratorService.getRecordEvolutionForVersion(TestObjectFactory.EXECUTIONID, pluginType)).thenReturn(resultNonEmpty);
      mockMvc
          .perform(get(RestEndpoints.ORCHESTRATOR_WORKFLOWS_EVOLUTION, TestObjectFactory.EXECUTIONID, pluginType)
              .header("Authorization", BEARER + MOCK_VALID_TOKEN))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.evolutionSteps", hasSize(2)))
          .andExpect(jsonPath("$.evolutionSteps[0].workflowExecutionId", is(step1.getWorkflowExecutionId())))
          .andExpect(jsonPath("$.evolutionSteps[0].pluginType", is(step1.getPluginType().name())))
          .andExpect(jsonPath("$.evolutionSteps[0].finishedTime", is(simpleDateFormat.format(step1.getFinishedTime().getTime()))))
          .andExpect(jsonPath("$.evolutionSteps[1].workflowExecutionId", is(step2.getWorkflowExecutionId())))
          .andExpect(jsonPath("$.evolutionSteps[1].pluginType", is(step2.getPluginType().name())))
          .andExpect(jsonPath("$.evolutionSteps[1].finishedTime", is(simpleDateFormat.format(step2.getFinishedTime().getTime()))));

      // Test happy flow with empty evolution
      final VersionEvolution resultEmpty = new VersionEvolution();
      resultEmpty.setEvolutionSteps(Collections.emptyList());
      when(orchestratorService.getRecordEvolutionForVersion(TestObjectFactory.EXECUTIONID, pluginType)).thenReturn(resultEmpty);
      mockMvc.perform(get(RestEndpoints.ORCHESTRATOR_WORKFLOWS_EVOLUTION, TestObjectFactory.EXECUTIONID, pluginType)
              .header("Authorization", BEARER + MOCK_VALID_TOKEN))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.evolutionSteps", hasSize(0)));

      // Test for bad input
      when(orchestratorService.getRecordEvolutionForVersion(TestObjectFactory.EXECUTIONID, pluginType)).thenThrow(new NoWorkflowExecutionFoundException(""));
      mockMvc.perform(get(RestEndpoints.ORCHESTRATOR_WORKFLOWS_EVOLUTION, TestObjectFactory.EXECUTIONID, pluginType)
              .header("Authorization", BEARER + MOCK_VALID_TOKEN))
          .andExpect(status().isNotFound());

      // Test for unauthorized user
      when(jwtDecoder.decode(MOCK_INVALID_TOKEN)).thenReturn(testJwtUtils.getInvalidRoleJwt());
      mockMvc.perform(get(RestEndpoints.ORCHESTRATOR_WORKFLOWS_EVOLUTION, TestObjectFactory.EXECUTIONID, pluginType)
              .header("Authorization", BEARER + MOCK_INVALID_TOKEN))
          .andExpect(status().isForbidden());
    }
}
