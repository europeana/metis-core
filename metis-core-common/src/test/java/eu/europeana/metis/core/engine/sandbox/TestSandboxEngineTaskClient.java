package eu.europeana.metis.core.engine.sandbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_XML;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withAccepted;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import eu.europeana.metis.core.engine.base.EngineTaskKey;
import eu.europeana.metis.core.engine.base.task.input.HttpHarvestInputDataEndpoint;
import eu.europeana.metis.core.rest.Record;
import eu.europeana.metis.core.workflow.plugins.ExecutablePluginType;
import eu.europeana.metis.exception.ExternalTaskException;
import eu.europeana.metis.sandbox.common.task.input.HttpHarvestInputMetadataRequest;
import eu.europeana.metis.sandbox.common.task.input.SandboxTask;
import eu.europeana.metis.sandbox.common.task.input.SandboxTaskKey;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

class TestSandboxEngineTaskClient {

  private static final String BASE_URL = "http://sandbox.test";
  private static final String TASK_ID = "taskId";
  private static final String BATCH_ID = "batchId";
  private static final String DATASET_ID = "datasetId";
  private static final String PROVIDER_ID = "providerId";
  private static final String TOPOLOGY_NAME = "topologyName";
  private static final String INPUT_URL = "http://input.test/records.xml";
  private static final int STEP_SIZE = 100;

  private final ObjectMapper objectMapper = new ObjectMapper();
  private MockRestServiceServer mockServer;
  private SandboxEngineTaskClient sandboxEngineTaskClient;

  @BeforeEach
  void setup() {
    RestClient.Builder restClientBuilder = RestClient.builder().baseUrl(BASE_URL);
    mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
    sandboxEngineTaskClient = new SandboxEngineTaskClient(
        new SandboxEngineTaskSettings(BASE_URL, null, null, null),
        restClientBuilder.build());
  }

  @Test
  void createEngineTaskUsesRequestAndReturnsCreatedTask() throws Exception {
    SandboxTask sandboxTask = createSandboxTask();

    mockServer.expect(once(), requestTo(BASE_URL + "/task/create"))
              .andExpect(method(POST))
              .andExpect(content().contentType(APPLICATION_JSON))
              .andExpect(jsonPath("$.parameters.ENGINE_DATASET_ID").value(DATASET_ID))
              .andExpect(jsonPath("$.parameters.PROVIDER_ID").doesNotExist())
              .andExpect(jsonPath("$.inputMetadataRequest.type").value("HTTP"))
              .andExpect(jsonPath("$.inputMetadataRequest.url").value(INPUT_URL))
              .andExpect(jsonPath("$.inputMetadataRequest.stepSize").value(STEP_SIZE))
              .andRespond(withSuccess(objectMapper.writeValueAsString(sandboxTask), APPLICATION_JSON));

    SandboxEngineTask engineTask = sandboxEngineTaskClient.createEngineTask(
        Map.of(
            EngineTaskKey.ENGINE_DATASET_ID, DATASET_ID,
            EngineTaskKey.PROVIDER_ID, PROVIDER_ID),
        new HttpHarvestInputDataEndpoint(INPUT_URL, STEP_SIZE), TOPOLOGY_NAME);

    assertEquals(TASK_ID, engineTask.getEngineTaskId());
    assertEquals(BATCH_ID, engineTask.getEngineBatchId());
    mockServer.verify();
  }

  @Test
  void submitEngineTaskSendsUnwrappedSandboxTask() throws Exception {
    SandboxTask sandboxTask = createSandboxTask();

    mockServer.expect(once(), requestTo(BASE_URL + "/task/submit"))
              .andExpect(method(POST))
              .andExpect(jsonPath("$.sandboxTask").doesNotExist())
              .andExpect(jsonPath("$.taskId").value(TASK_ID))
              .andExpect(jsonPath("$.batchId").value(BATCH_ID))
              .andRespond(withAccepted());

    sandboxEngineTaskClient.submitEngineTask(new SandboxEngineTask(sandboxTask), TOPOLOGY_NAME);

    mockServer.verify();
  }

  @Test
  void getRecordReturnsRecordContent() throws Exception {
    mockServer.expect(requestTo(BASE_URL + "/dataset/" + DATASET_ID + "/record?recordId=recordId&step=NORMALIZE"))
        .andExpect(method(GET))
        .andRespond(withSuccess("<record/>", APPLICATION_XML));

    Record record = sandboxEngineTaskClient.getRecord(DATASET_ID, "recordId", BATCH_ID,
        ExecutablePluginType.NORMALIZATION);
    assertEquals(new Record("recordId", "<record/>"), record);
    mockServer.verify();
  }

  @Test
  void getRecordReturnsNullWhenNotFound() throws Exception {
    mockServer.expect(requestTo(BASE_URL + "/dataset/" + DATASET_ID + "/record?recordId=recordId&step=NORMALIZE"))
        .andRespond(withStatus(HttpStatus.NOT_FOUND));

    assertNull(sandboxEngineTaskClient.getRecord(DATASET_ID, "recordId", BATCH_ID,
        ExecutablePluginType.NORMALIZATION));
    mockServer.verify();
  }

  @ParameterizedTest
  @EnumSource(value = HttpStatus.class, names = {"BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"})
  void getRecordThrowsForOtherHttpFailures(HttpStatus httpStatus) {
    mockServer.expect(requestTo(BASE_URL + "/dataset/" + DATASET_ID + "/record?recordId=recordId&step=NORMALIZE"))
        .andRespond(withStatus(httpStatus));

    assertThrows(ExternalTaskException.class, () -> sandboxEngineTaskClient.getRecord(
        DATASET_ID, "recordId", BATCH_ID, ExecutablePluginType.NORMALIZATION));
    mockServer.verify();
  }

  @Test
  void getRecordThrowsForConnectivityFailure() {
    mockServer.expect(requestTo(BASE_URL + "/dataset/" + DATASET_ID + "/record?recordId=recordId&step=NORMALIZE"))
        .andRespond(withException(new IOException("connectionFailed")));

    assertThrows(ExternalTaskException.class, () -> sandboxEngineTaskClient.getRecord(
        DATASET_ID, "recordId", BATCH_ID, ExecutablePluginType.NORMALIZATION));
    mockServer.verify();
  }

  private SandboxTask createSandboxTask() {
    SandboxTask sandboxTask = new SandboxTask();
    sandboxTask.setParameters(Map.of(SandboxTaskKey.ENGINE_DATASET_ID, DATASET_ID));
    sandboxTask.setInputMetadataRequest(new HttpHarvestInputMetadataRequest(INPUT_URL, STEP_SIZE));
    sandboxTask.setTaskId(TASK_ID);
    sandboxTask.setBatchId(BATCH_ID);
    return sandboxTask;
  }
}
