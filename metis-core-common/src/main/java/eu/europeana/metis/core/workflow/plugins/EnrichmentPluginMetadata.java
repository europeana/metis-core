package eu.europeana.metis.core.workflow.plugins;

import lombok.NoArgsConstructor;

/**
 * Enrichment Plugin Metadata.
 */
@NoArgsConstructor
public class EnrichmentPluginMetadata extends AbstractExecutablePluginMetadata {

  private static final ExecutablePluginType pluginType = ExecutablePluginType.ENRICHMENT;

  @Override
  public ExecutablePluginType getExecutablePluginType() {
    return pluginType;
  }
}
