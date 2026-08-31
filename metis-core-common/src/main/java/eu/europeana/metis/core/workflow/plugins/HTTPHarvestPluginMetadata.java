package eu.europeana.metis.core.workflow.plugins;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * HTTP Harvest Plugin Metadata.
 */
@Getter
@Setter
@NoArgsConstructor
public class HTTPHarvestPluginMetadata extends AbstractHarvestPluginMetadata {

  private static final ExecutablePluginType pluginType = ExecutablePluginType.HTTP_HARVEST;
  private String url;
  private boolean incrementalHarvest; // Default: false (i.e., full harvest)

  @Override
  public ExecutablePluginType getExecutablePluginType() {
    return pluginType;
  }

}
