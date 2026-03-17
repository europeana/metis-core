package eu.europeana.metis.core.workflow.plugins;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * This abstract class is the base implementation of {@link ExecutablePluginMetadata} and all executable plugins should inherit
 * from it.
 */
@Getter
@Setter
@NoArgsConstructor
public abstract class AbstractExecutablePluginMetadata extends AbstractMetisPluginMetadata implements ExecutablePluginMetadata {

  private boolean enabled;

  @Override
  public final PluginType getPluginType() {
    return getExecutablePluginType().toPluginType();
  }
}
