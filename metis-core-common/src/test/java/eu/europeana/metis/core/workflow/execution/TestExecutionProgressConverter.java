package eu.europeana.metis.core.workflow.execution;

import eu.europeana.metis.core.workflow.plugins.ExecutionProgress;
import org.junit.jupiter.api.Test;

import static eu.europeana.metis.core.workflow.execution.TestExecutionProgressUtils.getExecutionProgressUsingSetters;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TestExecutionProgressConverter {

  @Test
  void testToDTO() {
    ExecutionProgress executionProgress = getExecutionProgressUsingSetters();
    ExecutionProgressDTO executionProgressDTO = ExecutionProgressConverter.toDTO(executionProgress);
    assertExecutionProgressEquals(executionProgress, executionProgressDTO);
  }

  @Test
  void testToDTO_NullWorkflowExecution() {
    assertNull(ExecutionProgressConverter.toDTO(null));
  }

  static void assertExecutionProgressEquals(ExecutionProgress executionProgress, ExecutionProgressDTO executionProgressDTO) {
    assertEquals(executionProgress.getExpectedRecords(), executionProgressDTO.getExpectedRecords());
    assertEquals(executionProgress.getProcessedRecords(), executionProgressDTO.getProcessedRecords());
    assertEquals(executionProgress.getProgressPercentage(), executionProgressDTO.getProgressPercentage());
    assertEquals(executionProgress.getIgnoredRecords(), executionProgressDTO.getIgnoredRecords());
    assertEquals(executionProgress.getDeletedRecords(), executionProgressDTO.getDeletedRecords());
    assertEquals(executionProgress.getErrors(), executionProgressDTO.getErrors());
    assertEquals(executionProgress.getStatus(), executionProgressDTO.getStatus());
    assertEquals(executionProgress.getTotalDatabaseRecords(), executionProgressDTO.getTotalDatabaseRecords());
  }
}