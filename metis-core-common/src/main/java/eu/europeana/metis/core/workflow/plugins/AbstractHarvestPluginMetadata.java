package eu.europeana.metis.core.workflow.plugins;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * This abstract class is the base implementation of {@link ExecutablePluginMetadata} for harvest tasks. All executable harvest
 * plugins should inherit from it.
 */
@Setter
@Getter
@NoArgsConstructor
public abstract class AbstractHarvestPluginMetadata extends AbstractExecutablePluginMetadata {

  /**
   * Default false. If false, it indicates that the ProvidedCHO rdf:about should be used to set the identifier for ECloud
   */
  private boolean useDefaultIdentifiers;

  public abstract boolean isIncrementalHarvest();
}
