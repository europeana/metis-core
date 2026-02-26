package eu.europeana.metis.core.rest.execution.overview;

import eu.europeana.metis.core.workflow.plugins.ExecutionProgress;
import lombok.Getter;

/**
 * This class contains executionProgress information on a plugin's execution.
 */
@Getter
public class PluginProgressView {

  private long expectedRecords;
  private long processedRecords;
  private long ignoredRecords;
  private long deletedRecords;
  private long errors;
  private long progressPercentage;

  PluginProgressView() {
  }

  PluginProgressView(ExecutionProgress progress) {
    if (progress != null) {
      this.expectedRecords = progress.getExpectedRecords();
      this.processedRecords = progress.getProcessedRecords();
      this.ignoredRecords = progress.getIgnoredRecords();
      this.deletedRecords = progress.getDeletedRecords();
      this.errors = progress.getErrors();
      this.progressPercentage = progress.getProgressPercentage();
    }
  }
}
