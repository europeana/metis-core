package eu.europeana.metis.core.workflow.plugins;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Transformation Plugin Metadata.
 */
@Setter
@Getter
@NoArgsConstructor
public class TransformationExternalPluginMetadata extends AbstractExecutablePluginMetadata {

  private static final ExecutablePluginType pluginType = ExecutablePluginType.TRANSFORMATION_EXTERNAL;
  private String xsltId;

  @Override
  public ExecutablePluginType getExecutablePluginType() {
    return pluginType;
  }

}
