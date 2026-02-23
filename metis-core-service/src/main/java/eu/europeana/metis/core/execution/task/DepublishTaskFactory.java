package eu.europeana.metis.core.execution.task;

import static eu.europeana.metis.core.engine.base.EngineTaskParametersConfigurator.createDataRevision;
import static eu.europeana.metis.core.engine.base.EngineTaskParametersConfigurator.createDepublishParameters;

import eu.europeana.metis.core.engine.base.DataRevision;
import eu.europeana.metis.core.engine.base.EngineTask;
import eu.europeana.metis.core.engine.base.EngineTaskClient;
import eu.europeana.metis.core.engine.base.EngineTaskKey;
import eu.europeana.metis.core.engine.base.EngineTaskSettings;
import eu.europeana.metis.core.engine.base.PluginTypeToBatchJobMapper;
import eu.europeana.metis.core.engine.base.task.input.DepublishInputDataEndpoint;
import eu.europeana.metis.core.workflow.plugins.AbstractExecutablePlugin;
import eu.europeana.metis.core.workflow.plugins.DepublishPluginMetadata;
import eu.europeana.metis.sandbox.batch.common.FullBatchJobType;
import eu.europeana.metis.utils.DepublicationReason;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

/**
 * Factory class for creating depublish engine tasks.
 *
 * @param <S> The type of {@link EngineTaskSettings} associated with the harvest task.
 * @param <T> The type of {@link EngineTask} created by this factory.
 */
public class DepublishTaskFactory<S extends EngineTaskSettings, T extends EngineTask> extends AbstractEngineTaskFactory<S, T> {

  private final EngineTaskClient<S, T> engineTaskClient;
  private final AbstractExecutablePlugin<?> plugin;

  /**
   * Constructor.
   *
   * @param engineTaskClient the client responsible for managing and interacting with engine tasks.
   * @param plugin the executable plugin associated with the depublish process.
   */
  public DepublishTaskFactory(EngineTaskClient<S, T> engineTaskClient, AbstractExecutablePlugin<?> plugin) {
    super(engineTaskClient.getEngineTaskSettings());
    this.engineTaskClient = engineTaskClient;
    this.plugin = plugin;
  }

  @Override
  public T create(String datasetId, String engineDatasetId, String previousTaskId) {
    Map<EngineTaskKey, String> pluginParameters = getDepublishPluginParameters(datasetId);
    FullBatchJobType fullBatchJobType = PluginTypeToBatchJobMapper.map(plugin.getPluginType());
    if (fullBatchJobType != null) {
      pluginParameters.put(EngineTaskKey.JOB_NAME, fullBatchJobType.name());
    }
    pluginParameters.put(EngineTaskKey.ENGINE_DATASET_ID, engineDatasetId);
    return createDepublishEngineTask(engineDatasetId, pluginParameters);
  }

  private @NotNull Map<EngineTaskKey, String> getDepublishPluginParameters(String datasetId) {
    if (plugin.getPluginMetadata() instanceof DepublishPluginMetadata depublishPluginMetadata) {
      boolean datasetDepublish = depublishPluginMetadata.isDatasetDepublish();
      Set<String> recordIdsToDepublish = depublishPluginMetadata.getRecordIdsToDepublish();
      String depublicationReason = depublishPluginMetadata.getDepublicationReason() == null ? DepublicationReason.GENERIC.name()
          : depublishPluginMetadata.getDepublicationReason().name();
      return createDepublishParameters(datasetId, datasetDepublish, recordIdsToDepublish,
          depublicationReason);
    } else {
      throw new IllegalStateException("Unexpected value: " + plugin);
    }
  }

  @NotNull
  private T createDepublishEngineTask(String engineDatasetId, Map<EngineTaskKey, String> pluginParameters) {
    final String dataLocation = getDataLocation(engineDatasetId);
    final DepublishInputDataEndpoint internalInputDataEndpoint = new DepublishInputDataEndpoint(dataLocation);
    final DataRevision outputDataRevision = createDataRevision(
        plugin.getPluginType(), plugin.getStartedDate(), engineTaskClient.getEngineTaskSettings().getProvider());
    return engineTaskClient.createEngineTask(pluginParameters, internalInputDataEndpoint, outputDataRevision);
  }
}
