package eu.europeana.metis.core.workflow.plugins;

import eu.europeana.metis.core.engine.base.ProcessingEngineTask;
import eu.europeana.metis.core.engine.base.ProcessingEngineTaskClient;
import eu.europeana.metis.core.engine.base.ProcessingEngineTaskSettings;
import eu.europeana.metis.core.engine.base.IndexDatabase;

/**
 * Index to Preview Plugin.
 * <b>Note: Adding another layer of hierarchy e.g. AbstractIndexPlugin seems to not work with morphia at this point in time
 * 18/11/2021</b>
 */
public class IndexToPreviewPlugin extends AbstractExecutablePlugin<IndexToPreviewPluginMetadata> {

  protected final String topologyName = Topology.INDEX.getTopologyName();

  /**
   * Zero argument constructor that initializes the {@link #pluginType} corresponding to the plugin.
   */
  IndexToPreviewPlugin() {
    //Required for json serialization
    this(null);
  }

  /**
   * Constructor to initialize the plugin with pluginMetadata.
   * <p>Initializes the {@link #pluginType} as well.</p>
   *
   * @param pluginMetadata The plugin metadata.
   */
  IndexToPreviewPlugin(IndexToPreviewPluginMetadata pluginMetadata) {
    super(PluginType.PREVIEW, pluginMetadata);
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
    return IndexDatabase.PREVIEW;
  }
}
