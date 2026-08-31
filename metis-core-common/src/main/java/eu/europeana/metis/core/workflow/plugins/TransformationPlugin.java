package eu.europeana.metis.core.workflow.plugins;

/**
 * Transformation Plugin.
 */
public class TransformationPlugin extends AbstractExecutablePlugin<TransformationPluginMetadata> {

  private final String topologyName = Topology.TRANSFORMATION.getTopologyName();

  /**
   * Zero argument constructor that initializes the {@link #pluginType} corresponding to the
   * plugin.
   */
  public TransformationPlugin() {
    //Required for JSON serialization
    super(PluginType.TRANSFORMATION);
  }

  /**
   * Constructor to initialize the plugin with pluginMetadata.
   * <p>Initializes the {@link #pluginType} as well.</p>
   *
   * @param pluginMetadata The plugin metadata.
   */
  TransformationPlugin(TransformationPluginMetadata pluginMetadata) {
    super(PluginType.TRANSFORMATION, pluginMetadata);
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
