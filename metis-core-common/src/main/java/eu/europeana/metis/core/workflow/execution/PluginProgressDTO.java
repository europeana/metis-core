package eu.europeana.metis.core.workflow.execution;

import eu.europeana.cloud.common.model.dps.TaskState;
import eu.europeana.metis.core.workflow.plugins.ExecutionProgress;

/**
 * This class contains executionProgress information on a plugin's execution.
 */
public class PluginProgressDTO {

  private int expectedRecords;
  private int processedRecords;
  private int ignoredRecords;
  private int deletedRecords;
  private int progressPercentage;
  private int errors;
  private TaskState status;
  private int totalDatabaseRecords;

  public PluginProgressDTO() {
  }

  PluginProgressDTO(ExecutionProgress progress) {
    this.expectedRecords = progress.getExpectedRecords();
    this.processedRecords = progress.getProcessedRecords();
    this.ignoredRecords = progress.getIgnoredRecords();
    this.deletedRecords = progress.getDeletedRecords();
    this.errors = progress.getErrors();
    this.progressPercentage = progress.getProgressPercentage();
    this.status = progress.getStatus();
    this.totalDatabaseRecords = progress.getTotalDatabaseRecords();
  }

  public int getExpectedRecords() {
    return expectedRecords;
  }

  public int getProcessedRecords() {
    return processedRecords;
  }

  public int getIgnoredRecords() {
    return ignoredRecords;
  }

  public int getDeletedRecords() {
    return deletedRecords;
  }

  public int getProgressPercentage() {
    return progressPercentage;
  }

  public int getErrors() {
    return errors;
  }

  public TaskState getStatus() {
    return status;
  }

  public int getTotalDatabaseRecords() {
    return totalDatabaseRecords;
  }
}
