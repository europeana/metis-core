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
  private long ignoredRecords;
  private long deletedRecords;
  private long errors;
  private String status;
  private long totalDatabaseRecords;
}
