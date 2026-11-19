package eu.europeana.metis.core.workflow.plugins;


/**
 * The type Dataset administration plugin.
 */
public class DatasetAdministrationPlugin extends AbstractExecutablePlugin<DatasetAdministrationPluginMetadata> {

  private final String topologyName = Topology.DATASET_ADMINISTRATION.getTopologyName();

  /**
   * Zero argument constructor that initializes the {@link #pluginType} corresponding to the plugin.
   */
  public DatasetAdministrationPlugin() {
    //Required for JSON serialization
    super(PluginType.DATASET_ADMINISTRATION);
  }

  /**
   * Constructor to initialize the plugin with pluginMetadata.
   * <p>Initializes the {@link #pluginType} as well.</p>
   *
   * @param pluginMetadata The plugin metadata.
   */
  DatasetAdministrationPlugin(DatasetAdministrationPluginMetadata pluginMetadata) {
    super(PluginType.DATASET_ADMINISTRATION, pluginMetadata);
  }

  @Override
  public String getTopologyName() {
    return topologyName;
  }
}
