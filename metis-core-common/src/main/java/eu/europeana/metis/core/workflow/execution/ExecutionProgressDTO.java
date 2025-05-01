package eu.europeana.metis.core.workflow.execution;

/**
 * This class contains executionProgress information on a plugin's execution.
 */
public class ExecutionProgressDTO {

  private int expectedRecords;
  private int processedRecords;
  private int progressPercentage;
  private int ignoredRecords;
  private int deletedRecords;
  private int errors;
  private String status;
  private int totalDatabaseRecords;

  public ExecutionProgressDTO() {
    //Required for json serialization
  }

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

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public int getTotalDatabaseRecords() {
    return totalDatabaseRecords;
  }

  public void setTotalDatabaseRecords(int totalDatabaseRecords) {
    this.totalDatabaseRecords = totalDatabaseRecords;
  }
}
