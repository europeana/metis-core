package eu.europeana.metis.core.workflow.execution;

import eu.europeana.metis.core.workflow.plugins.ExecutionProgress;

/**
 * A utility class that provides methods for converting {@link ExecutionProgress} into {@link ExecutionProgressDTO}.
 *
 * <p>This class is designed to act as a translator between the domain model
 * and the Data Transfer Object (DTO) for ExecutionProgress, ensuring separation of concerns and easing data transfer between
 * layers.
 */
public final class ExecutionProgressConverter {

  /**
   * Private constructor to prevent instantiation of this utility class.
   */
  private ExecutionProgressConverter() {
  }

  /**
   * Converts an {@link ExecutionProgress} object into an {@link ExecutionProgressDTO}.
   *
   * @param executionProgress the {@link ExecutionProgress} object to be converted
   * @return a new {@link ExecutionProgressDTO} containing the mapped data
   */
  public static ExecutionProgressDTO toDTO(ExecutionProgress executionProgress) {
    if (executionProgress == null) {
      return null;
    }

    ExecutionProgressDTO executionProgressDTO = new ExecutionProgressDTO();
    executionProgressDTO.setExpectedRecords(executionProgress.getExpectedRecords());
    executionProgressDTO.setProcessedRecords(executionProgress.getProcessedRecords());
    executionProgressDTO.setProgressPercentage(executionProgress.getProgressPercentage());
    executionProgressDTO.setStatus(executionProgress.getStatus());
    executionProgressDTO.setTotalDatabaseRecords(executionProgress.getTotalDatabaseRecords());
    executionProgressDTO.setSuccessRecords(executionProgress.getSuccessRecords());
    executionProgressDTO.setFailRecords(executionProgress.getFailRecords());
    executionProgressDTO.setWarningRecords(executionProgress.getWarningRecords());
    executionProgressDTO.setDuplicateRecords(executionProgress.getDuplicateRecords());
    executionProgressDTO.setUnchangedRecords(executionProgress.getUnchangedRecords());
    executionProgressDTO.setExpectedDepublishRecords(executionProgress.getExpectedDepublishRecords());
    executionProgressDTO.setSuccessDepublishRecords(executionProgress.getSuccessDepublishRecords());
    executionProgressDTO.setFailDepublishRecords(executionProgress.getFailDepublishRecords());
    executionProgressDTO.setProcessedDepublishRecords(executionProgress.getProcessedDepublishRecords());
    return executionProgressDTO;
  }
}
