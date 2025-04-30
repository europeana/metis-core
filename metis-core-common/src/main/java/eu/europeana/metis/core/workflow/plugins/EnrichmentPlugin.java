package eu.europeana.metis.core.workflow.plugins;

import eu.europeana.metis.core.engine.base.ProcessingEngineTask;
import eu.europeana.metis.core.engine.base.ProcessingEngineTaskClient;
import eu.europeana.metis.core.engine.base.ProcessingEngineTaskSettings;

/**
 * Enrichment Plugin.
 *
 * @author Simon Tzanakis (Simon.Tzanakis@europeana.eu)
 * @since 2017-05-26
 */
public class EnrichmentPlugin extends AbstractExecutablePlugin<EnrichmentPluginMetadata> {

  private final String topologyName = Topology.ENRICHMENT.getTopologyName();

  /**
   * Zero argument constructor that initializes the {@link #pluginType} corresponding to the
   * plugin.
   */
  EnrichmentPlugin() {
    //Required for json serialization
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

  @Override
  <S extends ProcessingEngineTaskSettings, T extends ProcessingEngineTask>
  T prepareExternalTask(String datasetId, String previousTaskId, ProcessingEngineTaskClient<S, T> processingEngineTaskClient) {
    return createExternalTaskForProcessPlugin(datasetId, previousTaskId, processingEngineTaskClient,  null);
  }
}
