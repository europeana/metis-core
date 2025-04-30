package eu.europeana.metis.core.workflow.plugins;

import eu.europeana.cloud.service.dps.PluginParameterKeys;
import eu.europeana.metis.core.engine.base.ProcessingEngineTask;
import eu.europeana.metis.core.engine.base.ProcessingEngineTaskClient;
import eu.europeana.metis.core.engine.base.ProcessingEngineTaskSettings;
import eu.europeana.metis.core.engine.base.OaiHarvestParameters;
import java.util.HashMap;
import java.util.Map;

/**
 * OAIPMH Harvest Plugin.
 */
public class OaipmhHarvestPlugin extends AbstractExecutablePlugin<OaipmhHarvestPluginMetadata> {

  private final String topologyName = Topology.OAIPMH_HARVEST.getTopologyName();

  /**
   * Zero argument constructor that initializes the {@link #pluginType} corresponding to the plugin.
   */
  public OaipmhHarvestPlugin() {
    //Required for json serialization
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
   * Required for json serialization.
   *
   * @return the String representation of the topology
   */
  @Override
  public String getTopologyName() {
    return topologyName;
  }

  @Override
  <S extends ProcessingEngineTaskSettings, T extends ProcessingEngineTask>
  T prepareExternalTask(String datasetId, String previousTaskId, ProcessingEngineTaskClient<S, T> processingEngineTaskClient) {
    String targetUrl = getPluginMetadata().getUrl();
    Map<String, String> parameters = new HashMap<>();
    parameters.put(PluginParameterKeys.METIS_DATASET_ID, datasetId);
    T externalTaskForHarvestPlugin =
        createExternalTaskForHarvestPlugin(datasetId, processingEngineTaskClient, parameters, targetUrl,
            getPluginMetadata().isIncrementalHarvest());
    OaiHarvestParameters oaiHarvestParameters = new OaiHarvestParameters(
        getPluginMetadata().getSetSpec(),
        getPluginMetadata().getMetadataFormat(),
        getPluginMetadata().getFromDate(),
        getPluginMetadata().getUntilDate());

    externalTaskForHarvestPlugin.setOaiHarvestParameters(oaiHarvestParameters);
    return externalTaskForHarvestPlugin;
  }
}
