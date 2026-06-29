package eu.europeana.metis.core.workflow.plugins;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Validation Internal Plugin Metadata.
 */
@Setter
@Getter
@NoArgsConstructor
public class ValidationInternalPluginMetadata extends AbstractExecutablePluginMetadata {

  private static final ExecutablePluginType pluginType = ExecutablePluginType.VALIDATION_INTERNAL;
  private String urlOfSchemasZip;
  private String schemaRootPath;
  private String schematronRootPath;

  @Override
  public ExecutablePluginType getExecutablePluginType() {
    return pluginType;
  }

}
