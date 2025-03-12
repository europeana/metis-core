package eu.europeana.metis.core.workflow.plugins;

import dev.morphia.annotations.Entity;
import eu.europeana.cloud.common.model.dps.TaskState;

/**
 * Contains execution progress information of a task.
 */
@Entity
public class ExecutionProgress {

  private static final double PERCENTAGE_SCALE = 100.0;
  // The total number of expected records excluding deleted records.
  private int expectedRecords;

  // The total number of records processed so far excluding deleted records and including ignored records if applicable.
  private int processedRecords;

  // The percentage: the division of the actual and expected number of processed records.
  private int progressPercentage;

  // The number of processed records so far that are to be ignored for follow-up tasks.
  private int ignoredRecords = 0;

  // The number of deleted records processed so far.
  private int deletedRecords = 0;

  // The number of errors encountered so far.
  private int errors;

  // The current state of the task.
  private TaskState status;

  // TODO: 01/11/2021 The correct values should be updated with a script for the latest preview and publish executions, during release
  // The total records in the database, not used to capture progress but the final result(post process check)
  private int totalDatabaseRecords = -1;

  public int getExpectedRecords() {
    return expectedRecords;
  }

  public void setExpectedRecords(int expectedRecords) {
    this.expectedRecords = expectedRecords;
  }

  public int getProcessedRecords() {
    return processedRecords;
  }

  public void setProcessedRecords(int processedRecords) {
    this.processedRecords = processedRecords;
  }

  public int getProgressPercentage() {
    return progressPercentage;
  }

  public void setProgressPercentage(int progressPercentage) {
    this.progressPercentage = progressPercentage;
  }

  public int getIgnoredRecords() {
    return ignoredRecords;
  }

  public void setIgnoredRecords(int ignoredRecords) {
    this.ignoredRecords = ignoredRecords;
  }

  public int getDeletedRecords() {
    return deletedRecords;
  }

  public void setDeletedRecords(int deletedRecords) {
    this.deletedRecords = deletedRecords;
  }

  public int getErrors() {
    return errors;
  }

  public void setErrors(int errors) {
    this.errors = errors;
  }

  public TaskState getStatus() {
    return status;
  }

  public void setStatus(TaskState status) {
    this.status = status;
  }

  public int getTotalDatabaseRecords() {
    return totalDatabaseRecords;
  }

  public void setTotalDatabaseRecords(int totalDatabaseRecords) {
    this.totalDatabaseRecords = totalDatabaseRecords;
  }

  /**
   * Recalculates the progress percentage based on the expected and processed records. The progress percentage is computed as the
   * ratio of processed and deleted records to the sum of expected and deleted records, scaled to a percentage. If the expected
   * records count is zero, the progress percentage is set to zero.
   */
  public void recalculateProgressPercentage() {
    this.progressPercentage = this.expectedRecords == 0 ? 0
        : (int) Math.round(PERCENTAGE_SCALE *
            (this.processedRecords + this.deletedRecords) / (this.expectedRecords + this.deletedRecords));
  }
}
