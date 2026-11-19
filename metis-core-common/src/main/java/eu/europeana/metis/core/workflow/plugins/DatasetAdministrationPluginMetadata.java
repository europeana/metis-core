package eu.europeana.metis.core.workflow.plugins;

import lombok.NoArgsConstructor;

/**
 * The type Dataset administration plugin metadata.
 */
@NoArgsConstructor
public class DatasetAdministrationPluginMetadata extends AbstractExecutablePluginMetadata {

  private static final ExecutablePluginType pluginType = ExecutablePluginType.DATASET_ADMINISTRATION;

  @Override
  public ExecutablePluginType getExecutablePluginType() {
    return pluginType;
  }
}
