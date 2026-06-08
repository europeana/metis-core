package eu.europeana.metis.core.workflow.plugins;

import static eu.europeana.metis.core.workflow.execution.TestExecutionProgressUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import eu.europeana.metis.core.common.TestSerializationUtils;
import java.io.File;
import java.net.URL;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class TestExecutionProgress {

  private void assertExecutionProgress(ExecutionProgress executionProgress) {
    assertEquals(EXPECTED_RECORDS_VALUE, executionProgress.getExpectedRecords());
    assertEquals(PROCESSED_RECORDS_VALUE, executionProgress.getProcessedRecords());
    assertEquals(PROGRESS_PERCENTAGE_VALUE, executionProgress.getProgressPercentage());
    assertEquals(STATUS_VALUE, executionProgress.getStatus());
    assertEquals(TOTAL_DATABASE_RECORDS_VALUE, executionProgress.getTotalDatabaseRecords());
    assertEquals(SUCCESS_RECORDS_VALUE, executionProgress.getSuccessRecords());
    assertEquals(FAIL_RECORDS_VALUE, executionProgress.getFailRecords());
    assertEquals(WARNING_RECORDS_VALUE, executionProgress.getWarningRecords());
    assertEquals(DUPLICATE_RECORDS_VALUE, executionProgress.getDuplicateRecords());
    assertEquals(UNCHANGED_RECORDS_VALUE, executionProgress.getUnchangedRecords());
    assertEquals(EXPECTED_DEPUBLISH_RECORDS_VALUE, executionProgress.getExpectedDepublishRecords());
    assertEquals(SUCCESS_DEPUBLISH_RECORDS_VALUE, executionProgress.getSuccessDepublishRecords());
    assertEquals(FAIL_DEPUBLISH_RECORDS_VALUE, executionProgress.getFailDepublishRecords());
    assertEquals(PROCESSED_DEPUBLISH_RECORDS_VALUE, executionProgress.getProcessedDepublishRecords());
  }

  private void assertExecutionProgress(String jsonOutput) {
    TestSerializationUtils.assertFieldEquals(jsonOutput, EXPECTED_RECORDS, EXPECTED_RECORDS_VALUE);
    TestSerializationUtils.assertFieldEquals(jsonOutput, PROCESSED_RECORDS, PROCESSED_RECORDS_VALUE);
    TestSerializationUtils.assertFieldEquals(jsonOutput, PROGRESS_PERCENTAGE, PROGRESS_PERCENTAGE_VALUE);
    TestSerializationUtils.assertFieldEquals(jsonOutput, STATUS, STATUS_VALUE);
    TestSerializationUtils.assertFieldEquals(jsonOutput, TOTAL_DATABASE_RECORDS, TOTAL_DATABASE_RECORDS_VALUE);
    TestSerializationUtils.assertFieldEquals(jsonOutput, SUCCESS_RECORDS, SUCCESS_RECORDS_VALUE);
    TestSerializationUtils.assertFieldEquals(jsonOutput, FAIL_RECORDS, FAIL_RECORDS_VALUE);
    TestSerializationUtils.assertFieldEquals(jsonOutput, WARNING_RECORDS, WARNING_RECORDS_VALUE);
    TestSerializationUtils.assertFieldEquals(jsonOutput, DUPLICATE_RECORDS, DUPLICATE_RECORDS_VALUE);
    TestSerializationUtils.assertFieldEquals(jsonOutput, UNCHANGED_RECORDS, UNCHANGED_RECORDS_VALUE);
    TestSerializationUtils.assertFieldEquals(jsonOutput, EXPECTED_DEPUBLISH_RECORDS, EXPECTED_DEPUBLISH_RECORDS_VALUE);
    TestSerializationUtils.assertFieldEquals(jsonOutput, SUCCESS_DEPUBLISH_RECORDS, SUCCESS_DEPUBLISH_RECORDS_VALUE);
    TestSerializationUtils.assertFieldEquals(jsonOutput, FAIL_DEPUBLISH_RECORDS, FAIL_DEPUBLISH_RECORDS_VALUE);
    TestSerializationUtils.assertFieldEquals(jsonOutput, PROCESSED_DEPUBLISH_RECORDS, PROCESSED_DEPUBLISH_RECORDS_VALUE);
  }

  @Test
  void testGetters() {
    ExecutionProgress executionProgress = getExecutionProgressUsingSetters();
    assertExecutionProgress(executionProgress);
  }

  @Test
  void testSerialization() {
    ExecutionProgress executionProgress = getExecutionProgressUsingSetters();

    ObjectMapper objectMapper = JsonMapper.builder()
                                          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                                          .build();
    String jsonOutput = objectMapper.writeValueAsString(executionProgress);

    assertExecutionProgress(jsonOutput);
  }

  @Test
  void testDeserialization() {
    ObjectMapper objectMapper = JsonMapper.builder()
                                          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                                          .build();
    URL resource = getClass().getClassLoader().getResource("executionProgress.json");
    Objects.requireNonNull(resource);
    File jsonFile = new File(resource.getFile());
    ExecutionProgress deserializedExecutionProgress = objectMapper.readValue(jsonFile, ExecutionProgress.class);

    assertNotNull(deserializedExecutionProgress);
    assertExecutionProgress(deserializedExecutionProgress);
  }
}
