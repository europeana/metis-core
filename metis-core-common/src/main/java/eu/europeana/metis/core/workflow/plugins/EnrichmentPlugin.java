package eu.europeana.metis.core.workflow.plugins;

/**
 * Enrichment Plugin.
 */
public class EnrichmentPlugin extends AbstractExecutablePlugin<EnrichmentPluginMetadata> {

  private final String topologyName = Topology.ENRICHMENT.getTopologyName();

  /**
   * Zero argument constructor that initializes the {@link #pluginType} corresponding to the
   * plugin.
   */
  public EnrichmentPlugin() {
    //Required for JSON serialization
    super(PluginType.ENRICHMENT);
  }

  /**
   * Constructor to initialize the plugin with pluginMetadata.
   * <p>Initializes the {@link #pluginType} as well.</p>
   *
   * @param pluginMetadata The plugin metadata.
   */
  EnrichmentPlugin(EnrichmentPluginMetadata pluginMetadata) {
    super(PluginType.ENRICHMENT, pluginMetadata);
  }

  @Override
  public String getTopologyName() {
    return topologyName;
  }
}
