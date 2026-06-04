package eu.europeana.metis.core.workflow.plugins;

/**
 * Transformation Plugin Metadata.
 */
public class TransformationExternalPluginMetadata extends AbstractExecutablePluginMetadata {

  private static final ExecutablePluginType pluginType = ExecutablePluginType.TRANSFORMATION_EXTERNAL;
  private String xslt;

  public TransformationExternalPluginMetadata() {
    //Required for json serialization
  }

  @Override
  public ExecutablePluginType getExecutablePluginType() {
    return pluginType;
  }

  public String getXslt() {
    return xslt;
  }

  public void setXslt(String xslt) {
    this.xslt = xslt;
  }
}
