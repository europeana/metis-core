package eu.europeana.metis.core.workflow.plugins;

import lombok.NoArgsConstructor;

/**
 * Index to Preview Plugin Metadata.
 */
@NoArgsConstructor
public class IndexToPreviewPluginMetadata extends AbstractIndexPluginMetadata {

  private static final ExecutablePluginType pluginType = ExecutablePluginType.PREVIEW;

  @Override
  public ExecutablePluginType getExecutablePluginType() {
    return pluginType;
  }
}
