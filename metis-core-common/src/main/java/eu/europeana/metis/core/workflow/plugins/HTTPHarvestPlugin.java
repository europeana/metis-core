package eu.europeana.metis.core.workflow.plugins;

/**
 * HTTP Harvest Plugin.
 */
public class HTTPHarvestPlugin extends AbstractExecutablePlugin<HTTPHarvestPluginMetadata> {

  private final String topologyName = Topology.HTTP_HARVEST.getTopologyName();

  /**
   * Zero argument constructor that initializes the {@link #pluginType} corresponding to the
   * plugin.
   */
  public HTTPHarvestPlugin() {
    // Required for JSON serialization
    super(PluginType.HTTP_HARVEST);
  }

  /**
   * Constructor to initialize the plugin with pluginMetadata.
   * <p>
   * Initializes the {@link #pluginType} as well.
   * </p>
   *
   * @param pluginMetadata The plugin metadata.
   */
  HTTPHarvestPlugin(HTTPHarvestPluginMetadata pluginMetadata) {
    super(PluginType.HTTP_HARVEST, pluginMetadata);
  }

  @Override
  public String getTopologyName() {
    return topologyName;
  }
}
