package eu.europeana.metis.core.workflow.plugins;

/**
 * Transformation Plugin Metadata.
 */
public class TransformationExternalPluginMetadata extends AbstractExecutablePluginMetadata {

  private static final ExecutablePluginType pluginType = ExecutablePluginType.TRANSFORMATION_EXTERNAL;
  private String xsltId;

  public TransformationExternalPluginMetadata() {
    //Required for json serialization
  }

  @Override
  public ExecutablePluginType getExecutablePluginType() {
    return pluginType;
  }

  public String getXsltId() {
    return xsltId;
  }

  public void setXsltId(String xsltId) {
    this.xsltId = xsltId;
  }
}
