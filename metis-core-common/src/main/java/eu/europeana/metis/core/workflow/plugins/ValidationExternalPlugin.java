package eu.europeana.metis.core.workflow.plugins;

/**
 * Validation External Plugin.
 */
public class ValidationExternalPlugin extends AbstractExecutablePlugin<ValidationExternalPluginMetadata> {

  private final String topologyName = Topology.VALIDATION.getTopologyName();

  /**
   * Zero argument constructor that initializes the {@link #pluginType} corresponding to the
   * plugin.
   */
  public ValidationExternalPlugin() {
    //Required for json serialization
    super(PluginType.VALIDATION_EXTERNAL);

  }

  /**
   * Constructor to initialize the plugin with pluginMetadata.
   * <p>Initializes the {@link #pluginType} as well.</p>
   *
   * @param pluginMetadata The plugin metadata.
   */
  ValidationExternalPlugin(ValidationExternalPluginMetadata pluginMetadata) {
    super(PluginType.VALIDATION_EXTERNAL, pluginMetadata);
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
