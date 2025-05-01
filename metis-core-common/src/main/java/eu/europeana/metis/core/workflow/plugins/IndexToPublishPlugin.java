package eu.europeana.metis.core.workflow.plugins;

import eu.europeana.metis.core.engine.base.IndexDatabase;

/**
 * Index to Publish Plugin.
 * <b>Note: Adding another layer of hierarchy e.g. AbstractIndexPlugin seems to not work with morphia at this point in time
 * 18/11/2021</b>
 */
public class IndexToPublishPlugin extends AbstractExecutablePlugin<IndexToPublishPluginMetadata> {

  protected final String topologyName = Topology.INDEX.getTopologyName();

  /**
   * Zero argument constructor that initializes the {@link #pluginType} corresponding to the plugin.
   */
  IndexToPublishPlugin() {
    //Required for json serialization
    this(null);
  }

  /**
   * Constructor to initialize the plugin with pluginMetadata.
   * <p>Initializes the {@link #pluginType} as well.</p>
   *
   * @param pluginMetadata The plugin metadata.
   */
  IndexToPublishPlugin(IndexToPublishPluginMetadata pluginMetadata) {
    super(PluginType.PUBLISH, pluginMetadata);
  }

  @Override
  public String getTopologyName() {
    return topologyName;
  }

  /**
   * Get the target indexing database.
   *
   * @return the target indexing database
   */
  public IndexDatabase getTargetIndexingDatabase() {
    return IndexDatabase.PUBLISH;
  }
}
