package eu.europeana.metis.core.execution.task;

import static eu.europeana.metis.core.engine.base.EngineTaskParametersConfigurator.createDataRevision;
import static eu.europeana.metis.core.engine.base.EngineTaskParametersConfigurator.createDefaultTaskParametersHarvest;
import static java.util.Objects.requireNonNullElseGet;

import eu.europeana.metis.core.engine.base.DataRevision;
import eu.europeana.metis.core.engine.base.EngineTask;
import eu.europeana.metis.core.engine.base.EngineTaskClient;
import eu.europeana.metis.core.engine.base.EngineTaskKey;
import eu.europeana.metis.core.engine.base.EngineTaskSettings;
import eu.europeana.metis.core.engine.base.PluginTypeToBatchJobMapper;
import eu.europeana.metis.core.engine.base.task.input.HttpHarvestInputDataEndpoint;
import eu.europeana.metis.core.engine.base.task.input.InputDataEndpoint;
import eu.europeana.metis.core.engine.base.task.input.OaiHarvestInputDataEndpoint;
import eu.europeana.metis.core.workflow.plugins.AbstractExecutablePlugin;
import eu.europeana.metis.core.workflow.plugins.HTTPHarvestPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.OaipmhHarvestPluginMetadata;
import eu.europeana.metis.sandbox.batch.common.FullBatchJobType;
import java.util.EnumMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * Factory class for creating harvest engine tasks.
 *
 * @param <S> The type of {@link EngineTaskSettings} associated with the harvest task.
 * @param <T> The type of {@link EngineTask} created by this factory.
 */
public class HarvestTaskFactory<S extends EngineTaskSettings, T extends EngineTask> extends AbstractEngineTaskFactory<S, T> {

  private final EngineTaskClient<S, T> engineTaskClient;
  private final AbstractExecutablePlugin<?> plugin;

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
  public T create(String datasetId, String engineDatasetId, String previousTaskId) {
    PluginHarvestParameters pluginHarvestParameters = getPluginHarvestParameters();
    return createHarvestEngineTask(datasetId, engineDatasetId, pluginHarvestParameters);
  }

  private @NotNull PluginHarvestParameters getPluginHarvestParameters() {
    boolean incrementalHarvest;
    OaiHarvestInputDataEndpoint oaiHarvestInputDataParameters = null;
    String targetUrl;
    switch (plugin.getPluginMetadata()) {
      case OaipmhHarvestPluginMetadata oaipmhHarvestPluginMetadata -> {
        incrementalHarvest = oaipmhHarvestPluginMetadata.isIncrementalHarvest();
        targetUrl = oaipmhHarvestPluginMetadata.getUrl();
        oaiHarvestInputDataParameters = new OaiHarvestInputDataEndpoint(
            oaipmhHarvestPluginMetadata.getUrl(),
            oaipmhHarvestPluginMetadata.getSetSpec(),
            oaipmhHarvestPluginMetadata.getMetadataFormat(),
            oaipmhHarvestPluginMetadata.getFromDate(),
            oaipmhHarvestPluginMetadata.getUntilDate());
      }
      case HTTPHarvestPluginMetadata httpHarvestPluginMetadata -> {
        incrementalHarvest = httpHarvestPluginMetadata.isIncrementalHarvest();
        targetUrl = httpHarvestPluginMetadata.getUrl();
      }
      default -> throw new IllegalStateException("Unexpected value: " + plugin);
    }
    return new PluginHarvestParameters(targetUrl, incrementalHarvest, oaiHarvestInputDataParameters);
  }

  private @NotNull T createHarvestEngineTask(String datasetId, String engineDatasetId,
      PluginHarvestParameters pluginHarvestParameters) {
    final String dataLocation = getDataLocation(engineDatasetId);
    final Map<EngineTaskKey, String> basicTaskParameters =
        createDefaultTaskParametersHarvest(
            engineDatasetId, datasetId, pluginHarvestParameters.incrementalHarvest(), plugin.getStartedDate(), dataLocation,
            engineTaskClient.getEngineTaskSettings().getProvider());
    final Map<EngineTaskKey, String> allParameters = new EnumMap<>(EngineTaskKey.class);
    allParameters.putAll(basicTaskParameters);
    FullBatchJobType fullBatchJobType = PluginTypeToBatchJobMapper.map(plugin.getPluginType());
    if (fullBatchJobType != null) {
      allParameters.put(EngineTaskKey.JOB_NAME, fullBatchJobType.name());
    }

    final DataRevision outputDataRevision = createDataRevision(
        plugin.getPluginType(), plugin.getStartedDate(), engineTaskClient.getEngineTaskSettings().getProvider());

    final InputDataEndpoint inputDataEndpoint =
        requireNonNullElseGet(pluginHarvestParameters.oaiHarvestInputDataParameters(),
            () -> new HttpHarvestInputDataEndpoint(pluginHarvestParameters.targetUrl()));
    return engineTaskClient.createEngineTask(allParameters, inputDataEndpoint, outputDataRevision);
  }

  private record PluginHarvestParameters(String targetUrl, boolean incrementalHarvest,
                                         OaiHarvestInputDataEndpoint oaiHarvestInputDataParameters) {

  }
}
