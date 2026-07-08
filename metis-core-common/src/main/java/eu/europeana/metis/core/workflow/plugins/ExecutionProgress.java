package eu.europeana.metis.core.workflow.plugins;

import dev.morphia.annotations.Entity;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Contains execution progress information of a task.
 */
@Entity
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
public class ExecutionProgress {

  private static final double PERCENTAGE_SCALE = 100.0;
  // The total number of expected records excluding deleted records.
  private long expectedRecords;

  // The total number of records processed so far excluding deleted records and including ignored records if applicable.
  private long processedRecords;

  // The percentage: the division of the actual and expected number of processed records.
  private long progressPercentage;

  // The current state of the task.
  private String status;

  // TODO: 01/11/2021 The correct values should be updated with a script for the latest preview and publish executions, during release
  // The total records in the database, not used to capture progress but the final result(post process check)
  private int totalDatabaseRecords = -1;

  private long successRecords;
  private long failRecords;
  private long warningRecords;
  private long duplicateRecords;
  private long unchangedRecords;
  private long expectedDepublishRecords;
  private long successDepublishRecords;
  private long failDepublishRecords;
  private long processedDepublishRecords;

  /**
   * Creates a new instance of ExecutionProgress by copying the data from the provided instance.
   *
   * @param other the ExecutionProgress object to copy data from
   */
  public ExecutionProgress(ExecutionProgress other) {
    this.expectedRecords = other.expectedRecords;
    this.processedRecords = other.processedRecords;
    this.progressPercentage = other.progressPercentage;
    this.status = other.status;
    this.totalDatabaseRecords = other.totalDatabaseRecords;
    this.successRecords = other.successRecords;
    this.failRecords = other.failRecords;
    this.warningRecords = other.warningRecords;
    this.duplicateRecords = other.duplicateRecords;
    this.unchangedRecords = other.unchangedRecords;
    this.expectedDepublishRecords = other.expectedDepublishRecords;
    this.successDepublishRecords = other.successDepublishRecords;
    this.failDepublishRecords = other.failDepublishRecords;
    this.processedDepublishRecords = other.processedDepublishRecords;
  }

  /**
   * Recalculates the progress percentage based on the expected and processed records. The progress percentage is computed as the
   * ratio of processed and depublished records to the sum of expected and depublished records, scaled to a percentage. If the expected
   * records count is zero, the progress percentage is set to zero.
   */
  public void recalculateProgressPercentage() {
    long totalExpected = expectedRecords + expectedDepublishRecords;
    long totalProcessed = processedRecords + processedDepublishRecords;

    this.progressPercentage = totalExpected == 0 ? 0
        : (int) Math.round(PERCENTAGE_SCALE * ((double) totalProcessed / totalExpected));
  }

  /**
   * Determines whether the task has yielded any successful results.
   *
   * @return true if there are successful records; false otherwise.
   */
  public boolean hasSuccessfulResults() {
    return successRecords > 0 || successDepublishRecords > 0;
  }
}
