package eu.europeana.metis.core.workflow.plugins;

import lombok.NoArgsConstructor;

/**
 * Normalization Plugin Metadata.
 */
@NoArgsConstructor
public class NormalizationPluginMetadata extends AbstractExecutablePluginMetadata {

  private static final ExecutablePluginType pluginType = ExecutablePluginType.NORMALIZATION;

  @Override
  public ExecutablePluginType getExecutablePluginType() {
    return pluginType;
  }
}
