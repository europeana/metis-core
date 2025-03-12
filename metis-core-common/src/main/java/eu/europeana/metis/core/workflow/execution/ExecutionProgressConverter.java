package eu.europeana.metis.core.workflow.execution;

import eu.europeana.metis.core.workflow.plugins.ExecutionProgress;

public final class ExecutionProgressConverter {

  private ExecutionProgressConverter() {
  }

  public static ExecutionProgressDTO toDTO(ExecutionProgress executionProgress){
    ExecutionProgressDTO executionProgressDTO = new ExecutionProgressDTO();
    executionProgressDTO.setExpectedRecords(executionProgress.getExpectedRecords());
    executionProgressDTO.setProcessedRecords(executionProgress.getProcessedRecords());
    executionProgressDTO.setProgressPercentage(executionProgress.getProgressPercentage());
    executionProgressDTO.setIgnoredRecords(executionProgress.getIgnoredRecords());
    executionProgressDTO.setDeletedRecords(executionProgress.getDeletedRecords());
    executionProgressDTO.setErrors(executionProgress.getErrors());
    executionProgressDTO.setStatus(executionProgress.getStatus());
    executionProgressDTO.setTotalDatabaseRecords(executionProgress.getTotalDatabaseRecords());
    return executionProgressDTO;
  }
}
