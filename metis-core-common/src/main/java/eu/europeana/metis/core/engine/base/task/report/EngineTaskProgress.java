package eu.europeana.metis.core.engine.base.task.report;

import lombok.Getter;
import lombok.Setter;

/**
 * Contains execution progress information of a task.
 */
@Setter
@Getter
public class EngineTaskProgress {

  // The total number of expected records excluding deleted records.
  private long expectedRecords;

  // The total number of records processed so far excluding deleted records and including ignored records if applicable.
  private long processedRecords;

  // The number of processed records so far that are to be ignored for follow-up tasks.
  private long ignoredRecords = 0;

  // The number of deleted records processed so far.
  private long deletedRecords = 0;

  // The number of post processed records.
  private long postProcessedRecordsCount = 0;

  // The number of errors encountered so far.
  private long processedErrors;

  private long deletedErrors;

  // The current state of the task.
  private EngineTaskState engineTaskState;

  private String engineTaskStateInfo;

}
