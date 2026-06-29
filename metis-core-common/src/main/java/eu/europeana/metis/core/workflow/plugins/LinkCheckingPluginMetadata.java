package eu.europeana.metis.core.workflow.plugins;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Link Checking Plugin Metadata.
 */
@Getter
@Setter
@NoArgsConstructor
public class LinkCheckingPluginMetadata extends AbstractExecutablePluginMetadata {

  private static final ExecutablePluginType pluginType = ExecutablePluginType.LINK_CHECKING;

  private boolean performSampling;
  private Integer sampleSize;

  @Override
  public ExecutablePluginType getExecutablePluginType() {
    return pluginType;
  }
}
