package eu.europeana.metis.core.workflow.plugins;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Media Process Plugin Metadata.
 */
@Setter
@Getter
@NoArgsConstructor
public class MediaProcessPluginMetadata extends AbstractExecutablePluginMetadata {

  private static final ExecutablePluginType pluginType = ExecutablePluginType.MEDIA_PROCESS;
  private ThrottlingLevel throttlingLevel;


  @Override
  public ExecutablePluginType getExecutablePluginType() {
    return pluginType;
  }

}
