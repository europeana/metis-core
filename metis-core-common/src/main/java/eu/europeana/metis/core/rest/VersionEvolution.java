package eu.europeana.metis.core.rest;

import eu.europeana.metis.core.workflow.plugins.ExecutablePluginType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * This class represents a history of operations that are applied to a dataset.
 */
public class VersionEvolution {

  private List<VersionEvolutionStep> evolutionSteps;

  public List<VersionEvolutionStep> getEvolutionSteps() {
    return Collections.unmodifiableList(evolutionSteps);
  }

  public void setEvolutionSteps(Collection<VersionEvolutionStep> versions) {
    this.evolutionSteps = new ArrayList<>(versions);
  }

  /**
   * This class represents one operation applied to a dataset.
   */
  @Getter
  @Setter
  public static class VersionEvolutionStep {

    private String workflowExecutionId;
    private ExecutablePluginType pluginType;
    private Instant finishedTime;
  }
}
