package eu.europeana.metis.core.workflow.execution;

import eu.europeana.metis.core.engine.base.task.report.EngineTaskState;
import eu.europeana.metis.core.workflow.plugins.ExecutionProgress;

public class TestExecutionProgressUtils {

  public static final String EXPECTED_RECORDS = "expectedRecords";
  public static final String PROCESSED_RECORDS = "processedRecords";
  public static final String PROGRESS_PERCENTAGE = "progressPercentage";
  public static final String IGNORED_RECORDS = "ignoredRecords";
  public static final String DELETED_RECORDS = "deletedRecords";

  public static final String  SUCCESS_RECORDS ="successRecords";
  public static final String  FAIL_RECORDS ="failRecords";
  public static final String  WARNING_RECORDS ="warningRecords";
  public static final String  DUPLICATE_RECORDS ="duplicateRecords";
  public static final String  UNCHANGED_RECORDS ="unchangedRecords";
  public static final String  EXPECTED_DEPUBLISH_RECORDS ="expectedDepublishRecords";
  public static final String  SUCCESS_DEPUBLISH_RECORDS ="successDepublishRecords";
  public static final String  FAIL_DEPUBLISH_RECORDS ="failDepublishRecords";
  public static final String  PROCESSED_DEPUBLISH_RECORDS ="processedDepublishRecords";

  public static final String ERRORS = "errors";
  public static final String STATUS = "status";
  public static final String TOTAL_DATABASE_RECORDS = "totalDatabaseRecords";
  //VALUES
  public static final int EXPECTED_RECORDS_VALUE = 100;
  public static final int PROCESSED_RECORDS_VALUE = 100;
  public static final int PROGRESS_PERCENTAGE_VALUE = 100;
  public static final String STATUS_VALUE = EngineTaskState.PROCESSED.name();
  public static final int TOTAL_DATABASE_RECORDS_VALUE = 100;

  public static final int  SUCCESS_RECORDS_VALUE = 85;
  public static final int  FAIL_RECORDS_VALUE = 5;
  public static final int  WARNING_RECORDS_VALUE = 10;
  public static final int  DUPLICATE_RECORDS_VALUE = 5;
  public static final int  UNCHANGED_RECORDS_VALUE = 5;
  public static final int  EXPECTED_DEPUBLISH_RECORDS_VALUE = 5;
  public static final int  SUCCESS_DEPUBLISH_RECORDS_VALUE = 3;
  public static final int  FAIL_DEPUBLISH_RECORDS_VALUE = 2;
  public static final int  PROCESSED_DEPUBLISH_RECORDS_VALUE = 5;

  static ExecutionProgressDTO getExecutionProgressDTOUsingSetters() {
    ExecutionProgressDTO executionProgressDTO = new ExecutionProgressDTO();
    executionProgressDTO.setExpectedRecords(EXPECTED_RECORDS_VALUE);
    executionProgressDTO.setProcessedRecords(PROCESSED_RECORDS_VALUE);
    executionProgressDTO.setProgressPercentage(PROGRESS_PERCENTAGE_VALUE);
    executionProgressDTO.setStatus(STATUS_VALUE);
    executionProgressDTO.setTotalDatabaseRecords(TOTAL_DATABASE_RECORDS_VALUE);
    executionProgressDTO.setSuccessRecords(SUCCESS_RECORDS_VALUE);
    executionProgressDTO.setFailRecords(FAIL_RECORDS_VALUE);
    executionProgressDTO.setWarningRecords(WARNING_RECORDS_VALUE);
    executionProgressDTO.setDuplicateRecords(DUPLICATE_RECORDS_VALUE);
    executionProgressDTO.setUnchangedRecords(UNCHANGED_RECORDS_VALUE);
    executionProgressDTO.setExpectedDepublishRecords(EXPECTED_DEPUBLISH_RECORDS_VALUE);
    executionProgressDTO.setSuccessDepublishRecords(SUCCESS_DEPUBLISH_RECORDS_VALUE);
    executionProgressDTO.setFailDepublishRecords(FAIL_DEPUBLISH_RECORDS_VALUE);
    executionProgressDTO.setProcessedDepublishRecords(PROCESSED_DEPUBLISH_RECORDS_VALUE);
    return executionProgressDTO;
  }

  public static ExecutionProgress getExecutionProgressUsingSetters() {
    ExecutionProgress executionProgress = new ExecutionProgress();
    executionProgress.setExpectedRecords(EXPECTED_RECORDS_VALUE);
    executionProgress.setProcessedRecords(PROCESSED_RECORDS_VALUE);
    executionProgress.setProgressPercentage(PROGRESS_PERCENTAGE_VALUE);
    executionProgress.setStatus(STATUS_VALUE);
    executionProgress.setTotalDatabaseRecords(TOTAL_DATABASE_RECORDS_VALUE);
    executionProgress.setSuccessRecords(SUCCESS_RECORDS_VALUE);
    executionProgress.setFailRecords(FAIL_RECORDS_VALUE);
    executionProgress.setWarningRecords(WARNING_RECORDS_VALUE);
    executionProgress.setDuplicateRecords(DUPLICATE_RECORDS_VALUE);
    executionProgress.setUnchangedRecords(UNCHANGED_RECORDS_VALUE);
    executionProgress.setExpectedDepublishRecords(EXPECTED_DEPUBLISH_RECORDS_VALUE);
    executionProgress.setProcessedDepublishRecords(PROCESSED_DEPUBLISH_RECORDS_VALUE);
    executionProgress.setSuccessDepublishRecords(SUCCESS_DEPUBLISH_RECORDS_VALUE);
    executionProgress.setFailDepublishRecords(FAIL_DEPUBLISH_RECORDS_VALUE);
    executionProgress.setProcessedDepublishRecords(PROCESSED_DEPUBLISH_RECORDS_VALUE);
    return executionProgress;
  }
}
