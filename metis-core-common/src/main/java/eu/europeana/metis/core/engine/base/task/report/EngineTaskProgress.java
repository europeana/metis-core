package eu.europeana.metis.core.engine.base.task.report;

import lombok.Getter;
import lombok.Setter;

/**
 * Contains execution progress information of a task.
 */
@Setter
@Getter
public class EngineTaskProgress {

  private long expectedRecords;
  private long processedRecords;
  private long successRecords;
  private long failRecords;
  private long warningRecords;
  private long duplicateRecords;
  private long unchangedRecords;
  private long expectedDepublishRecords;
  private long successDepublishRecords;
  private long failDepublishRecords;
  private long processedDepublishRecords;

  private EngineTaskState engineTaskState;
  private String engineTaskStateInfo;

  /**
   * Determines whether the task has yielded any successful results.
   * <p>
   * A task is considered to have successful results if there are processed records (or processed depublished records), and the
   * number of successful records (or successful depublished records) is greater than zero.
   *
   * @return true if there are both processed records and successful records; false otherwise.
   */
  public boolean hasSuccessfulResults() {
    return (processedRecords > 0 || processedDepublishRecords > 0)
        && (successRecords > 0 || successDepublishRecords > 0);
  }
}
