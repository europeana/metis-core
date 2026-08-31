package eu.europeana.metis.core.workflow.plugins;

import lombok.NoArgsConstructor;

/**
 * Index to Publish Plugin Metadata.
 */
@NoArgsConstructor
public class IndexToPublishPluginMetadata extends AbstractIndexPluginMetadata {

  private static final ExecutablePluginType pluginType = ExecutablePluginType.PUBLISH;

  @Override
  public ExecutablePluginType getExecutablePluginType() {
    return pluginType;
  }
}
