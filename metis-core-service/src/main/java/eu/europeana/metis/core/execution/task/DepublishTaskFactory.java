package eu.europeana.metis.core.execution.task;

import static eu.europeana.metis.core.engine.base.EngineTaskParametersConfigurator.createDepublishParameters;

import eu.europeana.metis.core.engine.base.EngineTask;
import eu.europeana.metis.core.engine.base.EngineTaskClient;
import eu.europeana.metis.core.engine.base.EngineTaskKey;
import eu.europeana.metis.core.engine.base.EngineTaskSettings;
import eu.europeana.metis.core.engine.base.PluginTypeToBatchJobMapper;
import eu.europeana.metis.core.engine.base.task.input.DepublishInputDataEndpoint;
import eu.europeana.metis.core.execution.EngineTaskSubmitContext;
import eu.europeana.metis.core.workflow.plugins.AbstractExecutablePlugin;
import eu.europeana.metis.core.workflow.plugins.DepublishPluginMetadata;
import eu.europeana.metis.exception.ExternalTaskException;
import eu.europeana.metis.sandbox.common.batch.FullBatchJobType;
import eu.europeana.metis.utils.DepublicationReason;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.util.CollectionUtils;

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
  public T create(EngineTaskSubmitContext engineTaskSubmitContext)
      throws ExternalTaskException {
    DepublishContext depublishContext = getDepublishPluginParameters(
        engineTaskSubmitContext.getDatasetId(), engineTaskSubmitContext.getEngineDatasetId());
    Optional<FullBatchJobType> fullBatchJobType = PluginTypeToBatchJobMapper.map(
        plugin.getPluginMetadata().getExecutablePluginType());
    fullBatchJobType.ifPresent(
        batchJobType -> depublishContext.pluginParameters.put(EngineTaskKey.JOB_NAME, batchJobType.name()));
    depublishContext.pluginParameters.put(EngineTaskKey.ENGINE_DATASET_ID, engineTaskSubmitContext.getEngineDatasetId());
    return engineTaskClient.createEngineTask(depublishContext.pluginParameters, depublishContext.depublishInputDataEndpoint,
        plugin.getTopologyName());
  }

  private DepublishContext getDepublishPluginParameters(String datasetId, String engineDatasetId) {
    if (plugin.getPluginMetadata() instanceof DepublishPluginMetadata depublishPluginMetadata) {
      boolean datasetDepublish = depublishPluginMetadata.isDatasetDepublish();
      Set<String> recordIdsToDepublish = depublishPluginMetadata.getRecordIdsToDepublish();
      if (!datasetDepublish && CollectionUtils.isEmpty(recordIdsToDepublish)) {
        throw new IllegalStateException(
            "Requested record depublication but there are no record ids for depublication");
      }

      String depublicationReason = depublishPluginMetadata.getDepublicationReason() == null ? DepublicationReason.GENERIC.name()
          : depublishPluginMetadata.getDepublicationReason().name();
      Map<EngineTaskKey, String> depublishParameters = createDepublishParameters(datasetId, depublicationReason);
      return new DepublishContext(depublishParameters,
          new DepublishInputDataEndpoint("", datasetDepublish, recordIdsToDepublish));
    } else {
      throw new IllegalStateException("Unexpected value: " + plugin);
    }
  }

  record DepublishContext(Map<EngineTaskKey, String> pluginParameters, DepublishInputDataEndpoint depublishInputDataEndpoint) {

  }
}
