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
  private long progressPercentage;
  private long successRecords;
  private long failRecords;
  private long warningRecords;
  private long duplicateRecords;
  private long unchangedRecords;
  private long expectedDepublishRecords;
  private long successDepublishRecords;
  private long failDepublishRecords;
  private long processedDepublishRecords;

  PluginProgressView() {
  }

  PluginProgressView(ExecutionProgress progress) {
    if (progress != null) {
      this.expectedRecords = progress.getExpectedRecords();
      this.processedRecords = progress.getProcessedRecords();
      this.progressPercentage = progress.getProgressPercentage();
      this.successRecords = progress.getSuccessRecords();
      this.failRecords = progress.getFailRecords();
      this.warningRecords = progress.getWarningRecords();
      this.duplicateRecords = progress.getDuplicateRecords();
      this.unchangedRecords = progress.getUnchangedRecords();
      this.expectedDepublishRecords = progress.getExpectedDepublishRecords();
      this.successDepublishRecords = progress.getSuccessDepublishRecords();
      this.failDepublishRecords = progress.getFailDepublishRecords();
      this.processedDepublishRecords = progress.getProcessedDepublishRecords();
    }
  }
}
