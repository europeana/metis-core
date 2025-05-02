package eu.europeana.metis.core.workflow.execution;

import eu.europeana.metis.core.engine.base.task.report.EngineTaskState;
import eu.europeana.metis.core.workflow.plugins.ExecutionProgress;

public class TestExecutionProgressUtils {

  public static final String EXPECTED_RECORDS = "expectedRecords";
  public static final String PROCESSED_RECORDS = "processedRecords";
  public static final String PROGRESS_PERCENTAGE = "progressPercentage";
  public static final String IGNORED_RECORDS = "ignoredRecords";
  public static final String DELETED_RECORDS = "deletedRecords";
  public static final String ERRORS = "errors";
  public static final String STATUS = "status";
  public static final String TOTAL_DATABASE_RECORDS = "totalDatabaseRecords";
  //VALUES
  public static final int EXPECTED_RECORDS_VALUE = 100;
  public static final int PROCESSED_RECORDS_VALUE = 100;
  public static final int PROGRESS_PERCENTAGE_VALUE = 100;
  public static final int IGNORED_RECORDS_VALUE = 0;
  public static final int DELETED_RECORDS_VALUE = 0;
  public static final int ERRORS_VALUE = 0;
  public static final String STATUS_VALUE = EngineTaskState.PROCESSED.name();
  public static final int TOTAL_DATABASE_RECORDS_VALUE = 100;

  static ExecutionProgressDTO getExecutionProgressDTOUsingSetters() {
    ExecutionProgressDTO executionProgressDTO = new ExecutionProgressDTO();
    executionProgressDTO.setExpectedRecords(EXPECTED_RECORDS_VALUE);
    executionProgressDTO.setProcessedRecords(PROCESSED_RECORDS_VALUE);
    executionProgressDTO.setProgressPercentage(PROGRESS_PERCENTAGE_VALUE);
    executionProgressDTO.setIgnoredRecords(IGNORED_RECORDS_VALUE);
    executionProgressDTO.setDeletedRecords(DELETED_RECORDS_VALUE);
    executionProgressDTO.setErrors(ERRORS_VALUE);
    executionProgressDTO.setStatus(STATUS_VALUE);
    executionProgressDTO.setTotalDatabaseRecords(TOTAL_DATABASE_RECORDS_VALUE);
    return executionProgressDTO;
  }

  public static ExecutionProgress getExecutionProgressUsingSetters() {
    ExecutionProgress executionProgress = new ExecutionProgress();
    executionProgress.setExpectedRecords(EXPECTED_RECORDS_VALUE);
    executionProgress.setProcessedRecords(PROCESSED_RECORDS_VALUE);
    executionProgress.setProgressPercentage(PROGRESS_PERCENTAGE_VALUE);
    executionProgress.setIgnoredRecords(IGNORED_RECORDS_VALUE);
    executionProgress.setDeletedRecords(DELETED_RECORDS_VALUE);
    executionProgress.setErrors(ERRORS_VALUE);
    executionProgress.setStatus(STATUS_VALUE);
    executionProgress.setTotalDatabaseRecords(TOTAL_DATABASE_RECORDS_VALUE);
    return executionProgress;
  }
}
