package eu.europeana.metis.core.workflow.execution;

import static eu.europeana.metis.core.workflow.execution.TestExecutionProgressUtils.DELETED_RECORDS;
import static eu.europeana.metis.core.workflow.execution.TestExecutionProgressUtils.DELETED_RECORDS_VALUE;
import static eu.europeana.metis.core.workflow.execution.TestExecutionProgressUtils.ERRORS;
import static eu.europeana.metis.core.workflow.execution.TestExecutionProgressUtils.ERRORS_VALUE;
import static eu.europeana.metis.core.workflow.execution.TestExecutionProgressUtils.EXPECTED_RECORDS;
import static eu.europeana.metis.core.workflow.execution.TestExecutionProgressUtils.EXPECTED_RECORDS_VALUE;
import static eu.europeana.metis.core.workflow.execution.TestExecutionProgressUtils.IGNORED_RECORDS;
import static eu.europeana.metis.core.workflow.execution.TestExecutionProgressUtils.IGNORED_RECORDS_VALUE;
import static eu.europeana.metis.core.workflow.execution.TestExecutionProgressUtils.PROCESSED_RECORDS;
import static eu.europeana.metis.core.workflow.execution.TestExecutionProgressUtils.PROCESSED_RECORDS_VALUE;
import static eu.europeana.metis.core.workflow.execution.TestExecutionProgressUtils.PROGRESS_PERCENTAGE;
import static eu.europeana.metis.core.workflow.execution.TestExecutionProgressUtils.PROGRESS_PERCENTAGE_VALUE;
import static eu.europeana.metis.core.workflow.execution.TestExecutionProgressUtils.STATUS;
import static eu.europeana.metis.core.workflow.execution.TestExecutionProgressUtils.STATUS_VALUE;
import static eu.europeana.metis.core.workflow.execution.TestExecutionProgressUtils.TOTAL_DATABASE_RECORDS;
import static eu.europeana.metis.core.workflow.execution.TestExecutionProgressUtils.TOTAL_DATABASE_RECORDS_VALUE;
import static eu.europeana.metis.core.workflow.execution.TestExecutionProgressUtils.getExecutionProgressDTOUsingSetters;
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

class TestExecutionProgressDTO {

  @Test
  void testGetters() {
    ExecutionProgressDTO executionProgressDTO = getExecutionProgressDTOUsingSetters();
    assertExecutionProgressDTO(executionProgressDTO);
  }

  @Test
  void testSerialization() {
    ExecutionProgressDTO executionProgressDTO = getExecutionProgressDTOUsingSetters();

    ObjectMapper objectMapper = JsonMapper.builder()
                                          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                                          .build();
    String jsonOutput = objectMapper.writeValueAsString(executionProgressDTO);

    assertExecutionProgressDTO(jsonOutput);
  }

  @Test
  void testDeserialization() {
    ObjectMapper objectMapper = new ObjectMapper();
    URL resource = getClass().getClassLoader().getResource("executionProgressDTO.json");
    Objects.requireNonNull(resource);
    File jsonFile = new File(resource.getFile());
    ExecutionProgressDTO deserializedExecutionProgressDTO = objectMapper.readValue(jsonFile, ExecutionProgressDTO.class);

    assertNotNull(deserializedExecutionProgressDTO);
    assertExecutionProgressDTO(deserializedExecutionProgressDTO);
  }

  static void assertExecutionProgressDTO(ExecutionProgressDTO executionProgressDTO) {
    assertEquals(EXPECTED_RECORDS_VALUE, executionProgressDTO.getExpectedRecords());
    assertEquals(PROCESSED_RECORDS_VALUE, executionProgressDTO.getProcessedRecords());
    assertEquals(PROGRESS_PERCENTAGE_VALUE, executionProgressDTO.getProgressPercentage());
    assertEquals(IGNORED_RECORDS_VALUE, executionProgressDTO.getIgnoredRecords());
    assertEquals(DELETED_RECORDS_VALUE, executionProgressDTO.getDeletedRecords());
    assertEquals(ERRORS_VALUE, executionProgressDTO.getErrors());
    assertEquals(STATUS_VALUE, executionProgressDTO.getStatus());
    assertEquals(TOTAL_DATABASE_RECORDS_VALUE, executionProgressDTO.getTotalDatabaseRecords());
  }

  private void assertExecutionProgressDTO(String jsonOutput) {
    TestSerializationUtils.assertFieldEquals(jsonOutput, EXPECTED_RECORDS, EXPECTED_RECORDS_VALUE);
    TestSerializationUtils.assertFieldEquals(jsonOutput, PROCESSED_RECORDS, PROCESSED_RECORDS_VALUE);
    TestSerializationUtils.assertFieldEquals(jsonOutput, PROGRESS_PERCENTAGE, PROGRESS_PERCENTAGE_VALUE);
    TestSerializationUtils.assertFieldEquals(jsonOutput, IGNORED_RECORDS, IGNORED_RECORDS_VALUE);
    TestSerializationUtils.assertFieldEquals(jsonOutput, DELETED_RECORDS, DELETED_RECORDS_VALUE);
    TestSerializationUtils.assertFieldEquals(jsonOutput, ERRORS, ERRORS_VALUE);
    TestSerializationUtils.assertFieldEquals(jsonOutput, STATUS, STATUS_VALUE);
    TestSerializationUtils.assertFieldEquals(jsonOutput, TOTAL_DATABASE_RECORDS, TOTAL_DATABASE_RECORDS_VALUE);
  }
}