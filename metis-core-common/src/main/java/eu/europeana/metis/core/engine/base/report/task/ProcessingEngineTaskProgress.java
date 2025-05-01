package eu.europeana.metis.core.engine.base.report.task;

/**
 * Contains execution progress information of a task.
 */
public class ProcessingEngineTaskProgress {

  // The total number of expected records excluding deleted records.
  private int expectedRecords;

  // The total number of records processed so far excluding deleted records and including ignored records if applicable.
  private int processedRecords;

  // The number of processed records so far that are to be ignored for follow-up tasks.
  private int ignoredRecords = 0;

  // The number of deleted records processed so far.
  private int deletedRecords = 0;

  // The number of errors encountered so far.
  private int processedErrors;

  private int deletedErrors;

  // The current state of the task.
  private ProcessingEngineTaskState processingEngineTaskState;

  private String processingEngineTaskStateInfo;

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

  public int getProcessedErrors() {
    return processedErrors;
  }

  public void setProcessedErrors(int processedErrors) {
    this.processedErrors = processedErrors;
  }

  public int getDeletedErrors() {
    return deletedErrors;
  }

  public void setDeletedErrors(int deletedErrors) {
    this.deletedErrors = deletedErrors;
  }

  public ProcessingEngineTaskState getProcessingEngineTaskState() {
    return processingEngineTaskState;
  }

  public void setProcessingEngineTaskState(ProcessingEngineTaskState processingEngineTaskState) {
    this.processingEngineTaskState = processingEngineTaskState;
  }

  public String getProcessingEngineTaskStateInfo() {
    return processingEngineTaskStateInfo;
  }

  public void setProcessingEngineTaskStateInfo(String processingEngineTaskStateInfo) {
    this.processingEngineTaskStateInfo = processingEngineTaskStateInfo;
  }
}
