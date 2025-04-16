package eu.europeana.metis.core.rest.controller;

import eu.europeana.cloud.common.model.dps.SubTaskInfo;
import eu.europeana.cloud.common.model.dps.TaskErrorsInfo;
import eu.europeana.metis.core.exceptions.NoWorkflowExecutionFoundException;
import eu.europeana.metis.core.rest.ListOfIds;
import eu.europeana.metis.core.rest.PaginatedRecordsResponse;
import eu.europeana.metis.core.rest.Record;
import eu.europeana.metis.core.rest.RecordsResponse;
import eu.europeana.metis.core.rest.config.SecurityConfig;
import eu.europeana.metis.common.config.properties.security.SecurityConfigurationProperties;
import eu.europeana.metis.core.rest.exception.RestResponseExceptionHandler;
import eu.europeana.metis.core.rest.stats.AttributeStatistics;
import eu.europeana.metis.core.rest.stats.NodePathStatistics;
import eu.europeana.metis.core.rest.stats.NodeValueStatistics;
import eu.europeana.metis.core.rest.stats.RecordStatistics;
import eu.europeana.metis.security.test.JwtUtils;
import eu.europeana.metis.core.rest.utils.TestObjectFactory;
import eu.europeana.metis.core.service.ProxiesService;
import eu.europeana.metis.core.service.UserService;
import eu.europeana.metis.core.workflow.plugins.ExecutablePluginType;
import eu.europeana.metis.core.workflow.plugins.PluginType;
import eu.europeana.metis.utils.RestEndpoints;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.hamcrest.core.IsNull;
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
import static eu.europeana.metis.security.test.JwtUtils.BEARER;
import static eu.europeana.metis.security.test.JwtUtils.MOCK_INVALID_TOKEN;
import static eu.europeana.metis.security.test.JwtUtils.MOCK_VALID_TOKEN;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProxiesController.class)
@ContextConfiguration(classes = {ProxiesController.class, SecurityConfig.class, RestResponseExceptionHandler.class})
class TestProxiesController {

  @MockBean
  private ProxiesService proxiesService;

  @MockBean
  private JwtDecoder jwtDecoder;

  @MockBean
  private UserService userService;

  private static MockMvc mockMvc;
  private final JwtUtils jwtUtils;

  @Autowired
  public TestProxiesController(SecurityConfigurationProperties securityConfigurationProperties) {
    jwtUtils = new JwtUtils(securityConfigurationProperties.resourceNames());
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
    reset(proxiesService);
    reset(jwtDecoder);
    reset(userService);
  }

  @Test
  void getExternalTaskLogs() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(jwtUtils.getDataOfficerJwt());

    int from = 1;
    int to = 100;
    List<SubTaskInfo> listOfSubTaskInfo = TestObjectFactory.createListOfSubTaskInfo();
    for (SubTaskInfo subTaskInfo : listOfSubTaskInfo) {
      subTaskInfo.setAdditionalInformations(null);
    }
    when(proxiesService.getExternalTaskLogs(TestObjectFactory.TOPOLOGY_NAME,
        TestObjectFactory.EXTERNAL_TASK_ID, from, to)).thenReturn(listOfSubTaskInfo);

    mockMvc.perform(get(RestEndpoints.ORCHESTRATOR_PROXIES_TOPOLOGY_TASK_LOGS,
               TestObjectFactory.TOPOLOGY_NAME, TestObjectFactory.EXTERNAL_TASK_ID)
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .param("from", Integer.toString(from))
               .param("to", Integer.toString(to))
               .contentType(MediaType.APPLICATION_JSON)
               .content(""))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$[0].additionalInformations", is(IsNull.nullValue())))
           .andExpect(jsonPath("$[1].additionalInformations", is(IsNull.nullValue())));
  }

  @Test
  void existsExternalTaskReport() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(jwtUtils.getDataOfficerJwt());
    when(proxiesService.existsExternalTaskReport(TestObjectFactory.TOPOLOGY_NAME,
        TestObjectFactory.EXTERNAL_TASK_ID)).thenReturn(true);

    mockMvc.perform(get(RestEndpoints.ORCHESTRATOR_PROXIES_TOPOLOGY_TASK_REPORT_EXISTS,
               TestObjectFactory.TOPOLOGY_NAME, TestObjectFactory.EXTERNAL_TASK_ID)
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .contentType(MediaType.APPLICATION_JSON)
               .content(""))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.existsExternalTaskReport", is(true)));
  }

  @Test
  void getExternalTaskReport() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(jwtUtils.getDataOfficerJwt());
    List<SubTaskInfo> listOfSubTaskInfo = TestObjectFactory.createListOfSubTaskInfo();
    for (SubTaskInfo subTaskInfo : listOfSubTaskInfo) {
      subTaskInfo.setAdditionalInformations(null);
    }

    TaskErrorsInfo taskErrorsInfo = TestObjectFactory.createTaskErrorsInfoListWithIdentifiers(2);
    when(proxiesService.getExternalTaskReport(TestObjectFactory.TOPOLOGY_NAME,
        TestObjectFactory.EXTERNAL_TASK_ID, 10)).thenReturn(taskErrorsInfo);

    mockMvc.perform(get(RestEndpoints.ORCHESTRATOR_PROXIES_TOPOLOGY_TASK_REPORT,
               TestObjectFactory.TOPOLOGY_NAME, TestObjectFactory.EXTERNAL_TASK_ID)
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .param("idsPerError", "10")
               .contentType(MediaType.APPLICATION_JSON)
               .content(""))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.id", is(TestObjectFactory.EXTERNAL_TASK_ID)))
           .andExpect(jsonPath("$.errors", hasSize(taskErrorsInfo.getErrors().size())))
           .andExpect(jsonPath("$.errors[0].errorDetails",
               hasSize(taskErrorsInfo.getErrors().get(0).getErrorDetails().size())))
           .andExpect(jsonPath("$.errors[1].errorDetails",
               hasSize(taskErrorsInfo.getErrors().get(1).getErrorDetails().size())));
  }

  @Test
  void getExternalTaskStatistics() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(jwtUtils.getDataOfficerJwt());

    // Create response object.
    final NodeValueStatistics nodeValue = new NodeValueStatistics("node value", 3, Collections.emptyList());
    final NodePathStatistics nodePath = new NodePathStatistics("node path", Collections.singletonList(nodeValue));
    final RecordStatistics recordStatistics = new RecordStatistics(TestObjectFactory.EXTERNAL_TASK_ID, Collections.singletonList(nodePath));

    // Make the call and verify the result.
    when(proxiesService.getExternalTaskStatistics(TestObjectFactory.TOPOLOGY_NAME,
        TestObjectFactory.EXTERNAL_TASK_ID)).thenReturn(recordStatistics);
    mockMvc.perform(get(RestEndpoints.ORCHESTRATOR_PROXIES_TOPOLOGY_TASK_STATISTICS,
               TestObjectFactory.TOPOLOGY_NAME, TestObjectFactory.EXTERNAL_TASK_ID)
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .contentType(MediaType.APPLICATION_JSON).content(""))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.taskId", is(TestObjectFactory.EXTERNAL_TASK_ID)))
           .andExpect(jsonPath("$.nodePathStatistics", hasSize(recordStatistics.nodePathStatistics().size())))
           .andExpect(jsonPath("$.nodePathStatistics[0].xPath", is(nodePath.xPath())))
           .andExpect(jsonPath("$.nodePathStatistics[0].nodeValueStatistics", hasSize(nodePath.nodeValueStatistics().size())))
           .andExpect(jsonPath("$.nodePathStatistics[0].nodeValueStatistics[0].value", is(nodeValue.value())))
           .andExpect(
               jsonPath("$.nodePathStatistics[0].nodeValueStatistics[0].occurrences", is((int) nodeValue.occurrences())))
           .andExpect(jsonPath("$.nodePathStatistics[0].nodeValueStatistics[0].attributeStatistics",
               hasSize(nodeValue.attributeStatistics().size())));
  }

  @Test
  void getExternalTaskNodeStatistics() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(jwtUtils.getDataOfficerJwt());

    // Create response object.
    final AttributeStatistics attribute1 = new AttributeStatistics("attribute path 1", "attribute value 1", 1);
    final AttributeStatistics attribute2 = new AttributeStatistics("attribute path 1", "attribute value 1", 1);
    final NodeValueStatistics nodeValue = new NodeValueStatistics("node value", 3, Arrays.asList(attribute1, attribute2));
    final NodePathStatistics nodePath = new NodePathStatistics("node path", Collections.singletonList(nodeValue));

    when(proxiesService.getAdditionalNodeStatistics(TestObjectFactory.TOPOLOGY_NAME,
        TestObjectFactory.EXTERNAL_TASK_ID, nodePath.xPath())).thenReturn(nodePath);

    // Make the call and verify the result.
    mockMvc.perform(get(RestEndpoints.ORCHESTRATOR_PROXIES_TOPOLOGY_TASK_NODE_STATISTICS,
               TestObjectFactory.TOPOLOGY_NAME, TestObjectFactory.EXTERNAL_TASK_ID)
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .param("nodePath", nodePath.xPath()))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.xPath", is(nodePath.xPath())))
           .andExpect(jsonPath("$.nodeValueStatistics", hasSize(nodePath.nodeValueStatistics().size())))
           .andExpect(jsonPath("$.nodeValueStatistics[0].value", is(nodeValue.value())))
           .andExpect(jsonPath("$.nodeValueStatistics[0].occurrences", is((int) nodeValue.occurrences())))
           .andExpect(
               jsonPath("$.nodeValueStatistics[0].attributeStatistics", hasSize(nodeValue.attributeStatistics().size())))
           .andExpect(jsonPath("$.nodeValueStatistics[0].attributeStatistics[0].xPath", is(attribute1.xPath())))
           .andExpect(jsonPath("$.nodeValueStatistics[0].attributeStatistics[0].value", is(attribute1.value())))
           .andExpect(
               jsonPath("$.nodeValueStatistics[0].attributeStatistics[0].occurrences", is((int) attribute1.occurrences())))
           .andExpect(jsonPath("$.nodeValueStatistics[0].attributeStatistics[1].xPath", is(attribute2.xPath())))
           .andExpect(jsonPath("$.nodeValueStatistics[0].attributeStatistics[1].value", is(attribute2.value())))
           .andExpect(
               jsonPath("$.nodeValueStatistics[0].attributeStatistics[1].occurrences", is((int) attribute2.occurrences())));
  }

  @Test
  void getListOfFileContentsFromPluginExecution() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(jwtUtils.getDataOfficerJwt());

    ArrayList<Record> records = new ArrayList<>();
    Record record1 = new Record("ECLOUDID1",
        "<rdf:RDF><edm:ProvidedCHO rdf:about=\"/some/path1\"></edm:ProvidedCHO></rdf:RDF>");
    Record record2 = new Record("ECLOUDID2",
        "<rdf:RDF><edm:ProvidedCHO rdf:about=\"/some/path2\"></edm:ProvidedCHO></rdf:RDF>");
    records.add(record1);
    records.add(record2);
    PaginatedRecordsResponse recordsResponse = new PaginatedRecordsResponse(records, null);

    when(proxiesService.getListOfFileContentsFromPluginExecution(
        TestObjectFactory.EXECUTIONID, ExecutablePluginType.TRANSFORMATION, null, 5))
        .thenReturn(recordsResponse);

    mockMvc.perform(get(RestEndpoints.ORCHESTRATOR_PROXIES_RECORDS)
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .param("workflowExecutionId", TestObjectFactory.EXECUTIONID)
               .param("pluginType", PluginType.TRANSFORMATION.name())
               .contentType(MediaType.APPLICATION_JSON)
               .content(""))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.records[0].ecloudId", is(record1.getEcloudId())))
           .andExpect(jsonPath("$.records[0].xmlRecord", is(record1.getXmlRecord())))
           .andExpect(jsonPath("$.records[1].ecloudId", is(record2.getEcloudId())))
           .andExpect(jsonPath("$.records[1].xmlRecord", is(record2.getXmlRecord())));
  }
  // TODO: add tests for lookupIdFromUISClient

  @Test
  void testGetRecordEvolutionForVersion() throws Exception {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(jwtUtils.getDataOfficerJwt());

    // Create nonempty ID list and result list.
    final Record record1 = new Record("ID 1", "content 1");
    final Record record2 = new Record("ID 2", "content 2");
    final RecordsResponse output = new RecordsResponse(Arrays.asList(record1, record2));
    final List<String> expectedInput = Stream.concat(Stream.of("UNKNOWN ID"),
        output.getRecords().stream().map(Record::getEcloudId)).toList();

    // Test happy flow with non-empty ID list
    final ExecutablePluginType pluginType = ExecutablePluginType.MEDIA_PROCESS;
    doAnswer(invocation -> {
      final ListOfIds input = invocation.getArgument(2);
      assertEquals(expectedInput, input.getIds());
      return output;
    }).when(proxiesService).getListOfFileContentsFromPluginExecution(
        eq(TestObjectFactory.EXECUTIONID), eq(pluginType), any());

    mockMvc.perform(post(RestEndpoints.ORCHESTRATOR_PROXIES_RECORDS_BY_IDS)
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .param("workflowExecutionId", TestObjectFactory.EXECUTIONID)
               .param("pluginType", pluginType.name())
               .contentType(MediaType.APPLICATION_JSON)
               .content("{\"ids\":[\"" + String.join("\",\"", expectedInput) + "\"]}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.records", hasSize(2)))
           .andExpect(jsonPath("$.records[0].ecloudId", is(record1.getEcloudId())))
           .andExpect(jsonPath("$.records[0].xmlRecord", is(record1.getXmlRecord())))
           .andExpect(jsonPath("$.records[1].ecloudId", is(record2.getEcloudId())))
           .andExpect(jsonPath("$.records[1].xmlRecord", is(record2.getXmlRecord())));

    // Test happy flow with empty ID list
    final RecordsResponse emptyOutput = new RecordsResponse(Collections.emptyList());
    doAnswer(invocation -> {
      final ListOfIds input = invocation.getArgument(2);
      assertTrue(input.getIds().isEmpty());
      return emptyOutput;
    }).when(proxiesService).getListOfFileContentsFromPluginExecution(
        eq(TestObjectFactory.EXECUTIONID), eq(pluginType), any());

    mockMvc.perform(post(RestEndpoints.ORCHESTRATOR_PROXIES_RECORDS_BY_IDS)
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .param("workflowExecutionId", TestObjectFactory.EXECUTIONID)
               .param("pluginType", pluginType.name())
               .contentType(MediaType.APPLICATION_JSON)
               .content("{\"ids\":[]}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.records", hasSize(0)));
    mockMvc.perform(post(RestEndpoints.ORCHESTRATOR_PROXIES_RECORDS_BY_IDS)
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .param("workflowExecutionId", TestObjectFactory.EXECUTIONID)
               .param("pluginType", pluginType.name())
               .contentType(MediaType.APPLICATION_JSON)
               .content("{}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.records", hasSize(0)));

    // Test for bad input
    when(proxiesService.getListOfFileContentsFromPluginExecution(
        eq(TestObjectFactory.EXECUTIONID), eq(pluginType), any()))
        .thenThrow(new NoWorkflowExecutionFoundException(""));
    mockMvc.perform(post(RestEndpoints.ORCHESTRATOR_PROXIES_RECORDS_BY_IDS)
               .header("Authorization", BEARER + MOCK_VALID_TOKEN)
               .param("workflowExecutionId", TestObjectFactory.EXECUTIONID)
               .param("pluginType", pluginType.name())
               .contentType(MediaType.APPLICATION_JSON)
               .content("{}"))
           .andExpect(status().isNotFound());

    // Test for unauthenticated user
    mockMvc.perform(post(RestEndpoints.ORCHESTRATOR_PROXIES_RECORDS_BY_IDS)
               .param("workflowExecutionId", TestObjectFactory.EXECUTIONID)
               .param("pluginType", pluginType.name())
               .contentType(MediaType.APPLICATION_JSON)
               .content("{}"))
           .andExpect(status().isUnauthorized());

    // Test for unauthorized user
    when(jwtDecoder.decode(MOCK_INVALID_TOKEN)).thenReturn(jwtUtils.getInvalidRoleJwt());
    mockMvc.perform(post(RestEndpoints.ORCHESTRATOR_PROXIES_RECORDS_BY_IDS)
               .header("Authorization", BEARER + MOCK_INVALID_TOKEN)
               .param("workflowExecutionId", TestObjectFactory.EXECUTIONID)
               .param("pluginType", pluginType.name())
               .contentType(MediaType.APPLICATION_JSON)
               .content("{}"))
           .andExpect(status().isForbidden());
  }
}
