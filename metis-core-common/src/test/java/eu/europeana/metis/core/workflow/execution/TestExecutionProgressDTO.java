package eu.europeana.metis.core.workflow.execution;

import static eu.europeana.metis.core.workflow.execution.TestExecutionProgressUtils.DUPLICATE_RECORDS;
import static eu.europeana.metis.core.workflow.execution.TestExecutionProgressUtils.DUPLICATE_RECORDS_VALUE;
import static eu.europeana.metis.core.workflow.execution.TestExecutionProgressUtils.EXPECTED_DEPUBLISH_RECORDS;
import static eu.europeana.metis.core.workflow.execution.TestExecutionProgressUtils.EXPECTED_DEPUBLISH_RECORDS_VALUE;
import static eu.europeana.metis.core.workflow.execution.TestExecutionProgressUtils.EXPECTED_RECORDS;
import static eu.europeana.metis.core.workflow.execution.TestExecutionProgressUtils.EXPECTED_RECORDS_VALUE;
import static eu.europeana.metis.core.workflow.execution.TestExecutionProgressUtils.FAIL_DEPUBLISH_RECORDS;
import static eu.europeana.metis.core.workflow.execution.TestExecutionProgressUtils.FAIL_DEPUBLISH_RECORDS_VALUE;
import static eu.europeana.metis.core.workflow.execution.TestExecutionProgressUtils.FAIL_RECORDS;
import static eu.europeana.metis.core.workflow.execution.TestExecutionProgressUtils.FAIL_RECORDS_VALUE;
import static eu.europeana.metis.core.workflow.execution.TestExecutionProgressUtils.PROCESSED_DEPUBLISH_RECORDS;
import static eu.europeana.metis.core.workflow.execution.TestExecutionProgressUtils.PROCESSED_DEPUBLISH_RECORDS_VALUE;
import static eu.europeana.metis.core.workflow.execution.TestExecutionProgressUtils.PROCESSED_RECORDS;
import static eu.europeana.metis.core.workflow.execution.TestExecutionProgressUtils.PROCESSED_RECORDS_VALUE;
import static eu.europeana.metis.core.workflow.execution.TestExecutionProgressUtils.PROGRESS_PERCENTAGE;
import static eu.europeana.metis.core.workflow.execution.TestExecutionProgressUtils.PROGRESS_PERCENTAGE_VALUE;
import static eu.europeana.metis.core.workflow.execution.TestExecutionProgressUtils.STATUS;
import static eu.europeana.metis.core.workflow.execution.TestExecutionProgressUtils.STATUS_VALUE;
import static eu.europeana.metis.core.workflow.execution.TestExecutionProgressUtils.SUCCESS_DEPUBLISH_RECORDS;
import static eu.europeana.metis.core.workflow.execution.TestExecutionProgressUtils.SUCCESS_DEPUBLISH_RECORDS_VALUE;
import static eu.europeana.metis.core.workflow.execution.TestExecutionProgressUtils.SUCCESS_RECORDS;
import static eu.europeana.metis.core.workflow.execution.TestExecutionProgressUtils.SUCCESS_RECORDS_VALUE;
import static eu.europeana.metis.core.workflow.execution.TestExecutionProgressUtils.TOTAL_DATABASE_RECORDS;
import static eu.europeana.metis.core.workflow.execution.TestExecutionProgressUtils.TOTAL_DATABASE_RECORDS_VALUE;
import static eu.europeana.metis.core.workflow.execution.TestExecutionProgressUtils.UNCHANGED_RECORDS;
import static eu.europeana.metis.core.workflow.execution.TestExecutionProgressUtils.UNCHANGED_RECORDS_VALUE;
import static eu.europeana.metis.core.workflow.execution.TestExecutionProgressUtils.WARNING_RECORDS;
import static eu.europeana.metis.core.workflow.execution.TestExecutionProgressUtils.WARNING_RECORDS_VALUE;
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
    assertEquals(STATUS_VALUE, executionProgressDTO.getStatus());
    assertEquals(TOTAL_DATABASE_RECORDS_VALUE, executionProgressDTO.getTotalDatabaseRecords());
    assertEquals(SUCCESS_RECORDS_VALUE, executionProgressDTO.getSuccessRecords());
    assertEquals(FAIL_RECORDS_VALUE, executionProgressDTO.getFailRecords());
    assertEquals(WARNING_RECORDS_VALUE, executionProgressDTO.getWarningRecords());
    assertEquals(DUPLICATE_RECORDS_VALUE, executionProgressDTO.getDuplicateRecords());
    assertEquals(UNCHANGED_RECORDS_VALUE, executionProgressDTO.getUnchangedRecords());
    assertEquals(EXPECTED_DEPUBLISH_RECORDS_VALUE, executionProgressDTO.getExpectedDepublishRecords());
    assertEquals(SUCCESS_DEPUBLISH_RECORDS_VALUE, executionProgressDTO.getSuccessDepublishRecords());
    assertEquals(FAIL_DEPUBLISH_RECORDS_VALUE, executionProgressDTO.getFailDepublishRecords());
    assertEquals(PROCESSED_DEPUBLISH_RECORDS_VALUE, executionProgressDTO.getProcessedDepublishRecords());
  }

  private void assertExecutionProgressDTO(String jsonOutput) {
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
}
