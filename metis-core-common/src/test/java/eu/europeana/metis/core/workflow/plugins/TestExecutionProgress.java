package eu.europeana.metis.core.workflow.plugins;

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
import static eu.europeana.metis.core.workflow.execution.TestExecutionProgressUtils.getExecutionProgressUsingSetters;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europeana.metis.core.common.TestSerializationUtils;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import org.junit.jupiter.api.Test;

class TestExecutionProgress {

  @Test
  void testGetters() {
    ExecutionProgress executionProgress = getExecutionProgressUsingSetters();
    assertExecutionProgress(executionProgress);
  }

  @Test
  void testSerialization() throws IOException {
    ExecutionProgress executionProgress = getExecutionProgressUsingSetters();

    ObjectMapper objectMapper = new ObjectMapper();
    String jsonOutput = objectMapper.writeValueAsString(executionProgress);

    assertExecutionProgress(jsonOutput);
  }

  @Test
  void testDeserialization() throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    URL resource = getClass().getClassLoader().getResource("executionProgress.json");
    Objects.requireNonNull(resource);
    File jsonFile = new File(resource.getFile());
    ExecutionProgress deserializedExecutionProgress = objectMapper.readValue(jsonFile, ExecutionProgress.class);

    assertNotNull(deserializedExecutionProgress);
    assertExecutionProgress(deserializedExecutionProgress);
  }

  private void assertExecutionProgress(ExecutionProgress executionProgress) {
    assertEquals(EXPECTED_RECORDS_VALUE, executionProgress.getExpectedRecords());
    assertEquals(PROCESSED_RECORDS_VALUE, executionProgress.getProcessedRecords());
    assertEquals(PROGRESS_PERCENTAGE_VALUE, executionProgress.getProgressPercentage());
    assertEquals(IGNORED_RECORDS_VALUE, executionProgress.getIgnoredRecords());
    assertEquals(DELETED_RECORDS_VALUE, executionProgress.getDeletedRecords());
    assertEquals(ERRORS_VALUE, executionProgress.getErrors());
    assertEquals(STATUS_VALUE, executionProgress.getStatus());
    assertEquals(TOTAL_DATABASE_RECORDS_VALUE, executionProgress.getTotalDatabaseRecords());
  }

  private void assertExecutionProgress(String jsonOutput) {
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