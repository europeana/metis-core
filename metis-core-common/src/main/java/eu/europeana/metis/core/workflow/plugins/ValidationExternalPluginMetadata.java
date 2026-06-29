package eu.europeana.metis.core.workflow.plugins;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Validation External Plugin Metadata.
 */
@Setter
@Getter
@NoArgsConstructor
public class ValidationExternalPluginMetadata extends AbstractExecutablePluginMetadata {

  private static final ExecutablePluginType pluginType = ExecutablePluginType.VALIDATION_EXTERNAL;
  private String urlOfSchemasZip;
  private String schemaRootPath;
  private String schematronRootPath;

  @Override
  public ExecutablePluginType getExecutablePluginType() {
    return pluginType;
  }

}
