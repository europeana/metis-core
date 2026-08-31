package eu.europeana.metis.core.workflow.plugins;

/**
 * Transformation Plugin.
 */
public class TransformationExternalPlugin extends AbstractExecutablePlugin<TransformationExternalPluginMetadata> {

  private final String topologyName = Topology.TRANSFORMATION.getTopologyName();

  /**
   * Zero argument constructor that initializes the {@link #pluginType} corresponding to the
   * plugin.
   */
  public TransformationExternalPlugin() {
    //Required for JSON serialization
    super(PluginType.TRANSFORMATION_EXTERNAL);
  }

  /**
   * Constructor to initialize the plugin with pluginMetadata.
   * <p>Initializes the {@link #pluginType} as well.</p>
   *
   * @param pluginMetadata The plugin metadata.
   */
  TransformationExternalPlugin(TransformationExternalPluginMetadata pluginMetadata) {
    super(PluginType.TRANSFORMATION_EXTERNAL, pluginMetadata);
  }

  /**
   * Required for JSON serialization.
   *
   * @return the String representation of the topology
   */
  @Override
  public String getTopologyName() {
    return topologyName;
  }
}
