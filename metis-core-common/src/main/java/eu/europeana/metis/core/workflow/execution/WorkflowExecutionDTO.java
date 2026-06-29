package eu.europeana.metis.core.workflow.execution;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.metis.core.workflow.WorkflowStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * This class represents the full information on a workflow execution needed for the execution history.
 */
@Getter
@Setter
@NoArgsConstructor
public class WorkflowExecutionDTO {

  private String id;
  private String datasetId;
  private WorkflowStatus workflowStatus;
  private String ecloudDatasetId;
  private String cancelledBy;
  private String cancelledByUserName;
  private String cancelledByFirstName;
  private String cancelledByLastName;
  private String startedBy;
  private String startedByUserName;
  private String startedByFirstName;
  private String startedByLastName;
  private boolean cancelling;
  private Instant createdDate;
  private Instant startedDate;
  private Instant updatedDate;
  private Instant finishedDate;
  private boolean incremental;
  private List<MetisPluginDTO> metisPlugins = new ArrayList<>();

  @JsonProperty("isIncremental")
  public boolean isIncremental() {
    return incremental;
  }

  public List<MetisPluginDTO> getMetisPlugins() {
    return new ArrayList<>(metisPlugins);
  }

  public void setMetisPlugins(List<MetisPluginDTO> metisPlugins) {
    this.metisPlugins = metisPlugins == null ? new ArrayList<>() : new ArrayList<>(metisPlugins);
  }
}
