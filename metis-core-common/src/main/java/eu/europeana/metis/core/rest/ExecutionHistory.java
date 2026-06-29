package eu.europeana.metis.core.rest;

import com.fasterxml.jackson.annotation.JsonFormat;
import eu.europeana.metis.utils.CommonStringValues;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * This class represents the entire execution history for a dataset.
 */
public class ExecutionHistory {

  private List<Execution> executions;

  public List<Execution> getExecutions() {
    return Collections.unmodifiableList(executions);
  }

  public void setExecutions(Collection<Execution> executions) {
    this.executions = new ArrayList<>(executions);
  }

  /**
   * This class represents one workflow execution.
   */
  @Getter
  @Setter
  public static class Execution {

    private String workflowExecutionId;

    @JsonFormat(pattern = CommonStringValues.DATE_FORMAT, timezone = "UTC")
    private Instant startedDate;
  }
}
