package eu.europeana.metis.core.workflow.execution;

import static eu.europeana.metis.core.workflow.execution.TestExecutionProgressUtils.getExecutionProgressUsingSetters;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import eu.europeana.metis.core.workflow.plugins.ExecutionProgress;
import org.junit.jupiter.api.Test;

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
    assertEquals(executionProgress.getStatus(), executionProgressDTO.getStatus());
    assertEquals(executionProgress.getTotalDatabaseRecords(), executionProgressDTO.getTotalDatabaseRecords());
    assertEquals(executionProgress.getSuccessRecords(), executionProgressDTO.getSuccessRecords());
    assertEquals(executionProgress.getFailRecords(), executionProgressDTO.getFailRecords());
    assertEquals(executionProgress.getWarningRecords(), executionProgressDTO.getWarningRecords());
    assertEquals(executionProgress.getDuplicateRecords(), executionProgressDTO.getDuplicateRecords());
    assertEquals(executionProgress.getUnchangedRecords(), executionProgressDTO.getUnchangedRecords());
    assertEquals(executionProgress.getExpectedDepublishRecords(), executionProgressDTO.getExpectedDepublishRecords());
    assertEquals(executionProgress.getSuccessDepublishRecords(), executionProgressDTO.getSuccessDepublishRecords());
    assertEquals(executionProgress.getFailDepublishRecords(), executionProgressDTO.getFailDepublishRecords());
    assertEquals(executionProgress.getProcessedDepublishRecords(), executionProgressDTO.getProcessedDepublishRecords());
  }
}
