package eu.europeana.metis.core.workflow.execution;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * This class contains executionProgress information on a plugin's execution.
 */
@Getter
@Setter
@NoArgsConstructor
public class ExecutionProgressDTO {

  private long expectedRecords;
  private long processedRecords;
  private long progressPercentage;
  private String status;
  private long totalDatabaseRecords;
  private long successRecords;
  private long failRecords;
  private long warningRecords;
  private long duplicateRecords;
  private long unchangedRecords;
  private long expectedDepublishRecords;
  private long successDepublishRecords;
  private long failDepublishRecords;
  private long processedDepublishRecords;
}
