package eu.europeana.metis.core.execution.task;

import static eu.europeana.metis.core.engine.base.EngineTaskParametersConfigurator.createDefaultTaskParametersHarvest;

import eu.europeana.metis.core.engine.base.EngineTask;
import eu.europeana.metis.core.engine.base.EngineTaskClient;
import eu.europeana.metis.core.engine.base.EngineTaskKey;
import eu.europeana.metis.core.engine.base.EngineTaskSettings;
import eu.europeana.metis.core.engine.base.PluginTypeToBatchJobMapper;
import eu.europeana.metis.core.engine.base.task.input.HarvestInputDataEndpoint;
import eu.europeana.metis.core.engine.base.task.input.HttpHarvestInputDataEndpoint;
import eu.europeana.metis.core.engine.base.task.input.OaiHarvestInputDataEndpoint;
import eu.europeana.metis.core.workflow.plugins.AbstractExecutablePlugin;
import eu.europeana.metis.core.workflow.plugins.HTTPHarvestPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.OaipmhHarvestPluginMetadata;
import eu.europeana.metis.exception.ExternalTaskException;
import eu.europeana.metis.sandbox.common.batch.FullBatchJobType;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

/**
 * Factory class for creating harvest engine tasks.
 *
 * @param <S> The type of {@link EngineTaskSettings} associated with the harvest task.
 * @param <T> The type of {@link EngineTask} created by this factory.
 */
public class HarvestTaskFactory<S extends EngineTaskSettings, T extends EngineTask> extends AbstractEngineTaskFactory<S, T> {

  private final AbstractExecutablePlugin<?> plugin;
  private final EngineTaskClient<S, T> engineTaskClient;

  /**
   * Constructor.
   *
   * @param engineTaskClient The engine task client responsible for handling task submission, monitoring, and management. This
   * client acts as the interface to interact with the processing engine, providing mechanisms for task creation and execution.
   * @param plugin The plugin instance representing the executable logic used in the harvesting tasks. This plugin defines the
   * specific configuration and behavior needed for the execution of harvesting processes within the workflow.
   */
  public HarvestTaskFactory(EngineTaskClient<S, T> engineTaskClient, AbstractExecutablePlugin<?> plugin) {
    super(engineTaskClient.getEngineTaskSettings());
    this.engineTaskClient = engineTaskClient;
    this.plugin = plugin;
  }

  @Override
  public T create(String datasetId, String engineDatasetId, String previousExecutionId, String sourceBatchId)
      throws ExternalTaskException {
    PluginHarvestParameters pluginHarvestParameters = getPluginHarvestParameters();
    return createHarvestEngineTask(datasetId, engineDatasetId, pluginHarvestParameters);
  }

  private @NotNull PluginHarvestParameters getPluginHarvestParameters() {
    return switch (plugin.getPluginMetadata()) {
      case OaipmhHarvestPluginMetadata oaipmhHarvestPluginMetadata ->
          new PluginHarvestParameters(oaipmhHarvestPluginMetadata.isIncrementalHarvest(),
              new OaiHarvestInputDataEndpoint(
                  oaipmhHarvestPluginMetadata.getUrl(),
                  oaipmhHarvestPluginMetadata.getSetSpec(),
                  oaipmhHarvestPluginMetadata.getMetadataFormat(),
                  oaipmhHarvestPluginMetadata.getFromDate(),
                  oaipmhHarvestPluginMetadata.getUntilDate(),
                  oaipmhHarvestPluginMetadata.getStepSize())
          );
      case HTTPHarvestPluginMetadata httpHarvestPluginMetadata ->
          new PluginHarvestParameters(httpHarvestPluginMetadata.isIncrementalHarvest(),
              new HttpHarvestInputDataEndpoint(
                  httpHarvestPluginMetadata.getUrl(),
                  httpHarvestPluginMetadata.getStepSize())
          );
      default -> throw new IllegalStateException("Unexpected value: " + plugin.getPluginMetadata());
    };
  }

  private @NotNull T createHarvestEngineTask(String datasetId, String engineDatasetId,
      PluginHarvestParameters pluginHarvestParameters) throws ExternalTaskException {
    final Map<EngineTaskKey, String> basicTaskParameters =
        createDefaultTaskParametersHarvest(
            engineDatasetId, datasetId, pluginHarvestParameters.incrementalHarvest(), plugin.getStartedDate(),
            engineTaskClient.getEngineTaskSettings().getProvider());
    final Map<EngineTaskKey, String> allParameters = new EnumMap<>(EngineTaskKey.class);
    allParameters.putAll(basicTaskParameters);
    Optional<FullBatchJobType> fullBatchJobType = PluginTypeToBatchJobMapper.map(
        plugin.getPluginMetadata().getExecutablePluginType());
    fullBatchJobType.ifPresent(batchJobType -> allParameters.put(EngineTaskKey.JOB_NAME, batchJobType.name()));

    final HarvestInputDataEndpoint harvestInputDataEndpoint = pluginHarvestParameters.harvestInputDataEndpoint();
    return engineTaskClient.createEngineTask(allParameters, harvestInputDataEndpoint, plugin.getTopologyName());
  }

  private record PluginHarvestParameters(boolean incrementalHarvest,
                                         HarvestInputDataEndpoint harvestInputDataEndpoint) {

  }
}
