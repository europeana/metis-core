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
public class TransformationPluginMetadata extends AbstractExecutablePluginMetadata {

  private static final ExecutablePluginType pluginType = ExecutablePluginType.TRANSFORMATION;
  private String xsltId;
  private boolean customXslt;
  private String datasetName;
  private String country;
  private String language;

  @Override
  public ExecutablePluginType getExecutablePluginType() {
    return pluginType;
  }

}
