package eu.europeana.metis.core.rest.execution.overview;

import eu.europeana.metis.core.workflow.WorkflowExecution;
import eu.europeana.metis.core.workflow.WorkflowStatus;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * This class represents the vital information on a workflow execution needed for the execution
 * overview.
 */
@Getter
@Setter
public class ExecutionSummaryView {

  private String id;
  private WorkflowStatus workflowStatus;
  private boolean cancelling;

  private Instant createdDate;
  private Instant startedDate;
  private Instant updatedDate;
  private Instant finishedDate;

  private List<PluginSummaryView> plugins;

  ExecutionSummaryView() {
  }

  ExecutionSummaryView(WorkflowExecution execution) {
    this.id = execution.getId().toString();
    this.workflowStatus = execution.getWorkflowStatus();
    this.cancelling = execution.isCancelling();
    this.createdDate = execution.getCreatedDate();
    this.startedDate = execution.getStartedDate();
    this.updatedDate = execution.getUpdatedDate();
    this.finishedDate = execution.getFinishedDate();
    this.plugins = execution.getMetisPlugins().stream().map(PluginSummaryView::new).toList();
  }

  public List<PluginSummaryView> getPlugins() {
    return Collections.unmodifiableList(plugins);
  }
}
