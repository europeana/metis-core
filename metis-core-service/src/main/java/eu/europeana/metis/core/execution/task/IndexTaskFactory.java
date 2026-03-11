package eu.europeana.metis.core.execution.task;

import static eu.europeana.metis.core.engine.base.EngineTaskParametersConfigurator.createIndexParameters;

import eu.europeana.metis.core.engine.base.EngineTask;
import eu.europeana.metis.core.engine.base.EngineTaskClient;
import eu.europeana.metis.core.engine.base.EngineTaskKey;
import eu.europeana.metis.core.engine.base.EngineTaskSettings;
import eu.europeana.metis.core.engine.base.PluginTypeToBatchJobMapper;
import eu.europeana.metis.core.workflow.plugins.AbstractExecutablePlugin;
import eu.europeana.metis.core.workflow.plugins.AbstractIndexPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.IndexToPreviewPlugin;
import eu.europeana.metis.core.workflow.plugins.IndexToPublishPlugin;
import eu.europeana.metis.sandbox.common.batch.FullBatchJobType;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

/**
 * Factory class for creating index engine tasks.
 *
 * @param <S> The type of {@link EngineTaskSettings} associated with the harvest task.
 * @param <T> The type of {@link EngineTask} created by this factory.
 */
public class IndexTaskFactory<S extends EngineTaskSettings, T extends EngineTask> extends
    AbstractIntermediateEngineTaskFactory<S, T> {

  /**
   * Constructor.
   *
   * @param engineTaskClient the client responsible for managing and interacting with engine tasks of types {@code S} and
   * {@code T}. This client provides mechanisms for submitting tasks, monitoring their progress, reporting statistics, and
   * handling task-related operations.
   * @param plugin the plugin used for task execution. The plugin provides the necessary metadata and behavior required for
   * executing tasks created by this factory.
   */
  public IndexTaskFactory(EngineTaskClient<S, T> engineTaskClient, AbstractExecutablePlugin<?> plugin) {
    super(engineTaskClient, plugin);
  }

  @Override
  public T create(String datasetId, String engineDatasetId, String previousTaskId) {
    Map<EngineTaskKey, String> pluginParameters = getIndexPluginParameters();
    Optional<FullBatchJobType> fullBatchJobType = PluginTypeToBatchJobMapper.map(
        plugin.getPluginMetadata().getExecutablePluginType());
    fullBatchJobType.ifPresent(batchJobType -> pluginParameters.put(EngineTaskKey.JOB_NAME, batchJobType.name()));
    return createInternalEngineTask(datasetId, engineDatasetId, previousTaskId, pluginParameters);
  }

  private @NotNull Map<EngineTaskKey, String> getIndexPluginParameters() {
    if (plugin.getPluginMetadata() instanceof AbstractIndexPluginMetadata indexPluginMetadata) {
      boolean incrementalIndexing = indexPluginMetadata.isIncrementalIndexing();
      Date harvestDate = indexPluginMetadata.getHarvestDate();
      boolean preserveTimestamps = indexPluginMetadata.isPreserveTimestamps();
      List<String> datasetIdsToRedirectFrom = indexPluginMetadata.getDatasetIdsToRedirectFrom();
      boolean performRedirects = indexPluginMetadata.isPerformRedirects();
      final String targetIndexingDatabase;
      if (plugin instanceof IndexToPreviewPlugin indexToPreviewPlugin) {
        targetIndexingDatabase = indexToPreviewPlugin.getTargetIndexingDatabase().name();
      } else {
        targetIndexingDatabase = ((IndexToPublishPlugin) plugin).getTargetIndexingDatabase().name();
      }
      return createIndexParameters(plugin.getStartedDate(), incrementalIndexing,
          harvestDate, preserveTimestamps, datasetIdsToRedirectFrom, performRedirects, targetIndexingDatabase);
    } else {
      throw new IllegalStateException("Unexpected value: " + plugin);
    }
  }
}
