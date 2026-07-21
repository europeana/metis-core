package eu.europeana.metis.core.execution.task;

import static eu.europeana.metis.core.engine.base.EngineTaskParametersConfigurator.createIndexParameters;

import eu.europeana.metis.core.engine.base.EngineTask;
import eu.europeana.metis.core.engine.base.EngineTaskClient;
import eu.europeana.metis.core.engine.base.EngineTaskKey;
import eu.europeana.metis.core.engine.base.EngineTaskSettings;
import eu.europeana.metis.core.engine.base.task.input.IntermediateInputDataEndpoint;
import eu.europeana.metis.core.execution.EngineTaskSubmitContext;
import eu.europeana.metis.core.workflow.plugins.AbstractExecutablePlugin;
import eu.europeana.metis.core.workflow.plugins.AbstractIndexPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.IndexToPreviewPlugin;
import eu.europeana.metis.core.workflow.plugins.IndexToPublishPlugin;
import eu.europeana.metis.exception.ExternalTaskException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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
  public T create(EngineTaskSubmitContext engineTaskSubmitContext) throws ExternalTaskException {
    IndexTaskContext indexTaskContext = getIndexTaskConfiguration(
        engineTaskSubmitContext.getSourceExecutionId(),
        engineTaskSubmitContext.getSourceBatchId()
    );
    addJobNameParameter(indexTaskContext.pluginParameters());
    Map<EngineTaskKey, String> allParameters = createAllParameters(
        engineTaskSubmitContext.getEngineDatasetId(),
        engineTaskSubmitContext.getDatasetId(),
        engineTaskSubmitContext.getSourceExecutionId(),
        indexTaskContext.pluginParameters()
    );

    return createIntermediateEngineTask(allParameters, indexTaskContext.inputDataEndpoint());
  }

  private @NotNull IndexTaskContext getIndexTaskConfiguration(String sourceExecutionId, String sourceBatchId) {
    if (!(plugin.getPluginMetadata() instanceof AbstractIndexPluginMetadata indexPluginMetadata)) {
      throw new IllegalStateException("Unexpected value: " + plugin.getPluginMetadata());
    }

    return new IndexTaskContext(
        createIndexPluginParameters(indexPluginMetadata),
        createSimpleIntermediateInputDataEndpoint(sourceExecutionId, sourceBatchId)
    );
  }

  private @NotNull Map<EngineTaskKey, String> createIndexPluginParameters(AbstractIndexPluginMetadata indexPluginMetadata) {

    String targetIndexingDatabase = switch (plugin) {
      case IndexToPreviewPlugin indexToPreviewPlugin ->
          indexToPreviewPlugin.getTargetIndexingDatabase().name();
      case IndexToPublishPlugin indexToPublishPlugin ->
          indexToPublishPlugin.getTargetIndexingDatabase().name();
      default -> throw new IllegalStateException("Unexpected index plugin: " + plugin);
    };

    boolean incrementalIndexing = indexPluginMetadata.isIncrementalIndexing();
    Instant harvestDate = indexPluginMetadata.getHarvestDate();
    boolean preserveTimestamps = indexPluginMetadata.isPreserveTimestamps();
    List<String> datasetIdsToRedirectFrom = indexPluginMetadata.getDatasetIdsToRedirectFrom();
    boolean performRedirects = indexPluginMetadata.isPerformRedirects();

    return createIndexParameters(
        plugin.getStartedDate(),
        incrementalIndexing,
        harvestDate,
        preserveTimestamps,
        datasetIdsToRedirectFrom,
        performRedirects,
        targetIndexingDatabase
    );
  }

  private record IndexTaskContext(
      Map<EngineTaskKey, String> pluginParameters,
      IntermediateInputDataEndpoint inputDataEndpoint
  ) {
  }
}
