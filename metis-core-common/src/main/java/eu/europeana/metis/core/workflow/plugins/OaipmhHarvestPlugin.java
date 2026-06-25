package eu.europeana.metis.core.workflow.plugins;

/**
 * OAIPMH Harvest Plugin.
 */
public class OaipmhHarvestPlugin extends AbstractExecutablePlugin<OaipmhHarvestPluginMetadata> {

  private final String topologyName = Topology.OAIPMH_HARVEST.getTopologyName();

  /**
   * Zero argument constructor that initializes the {@link #pluginType} corresponding to the plugin.
   */
  public OaipmhHarvestPlugin() {
    //Required for JSON serialization
    super(PluginType.OAIPMH_HARVEST);
  }

  /**
   * Constructor to initialize the plugin with pluginMetadata.
   * <p>Initializes the {@link #pluginType} as well.</p>
   *
   * @param pluginMetadata The plugin metadata.
   */
  OaipmhHarvestPlugin(OaipmhHarvestPluginMetadata pluginMetadata) {
    super(PluginType.OAIPMH_HARVEST, pluginMetadata);
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
