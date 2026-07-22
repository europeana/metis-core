package eu.europeana.metis.core.execution.task;

import static eu.europeana.metis.core.engine.base.EngineTaskParametersConfigurator.createDefaultTaskParameters;

import eu.europeana.metis.core.engine.base.EngineTask;
import eu.europeana.metis.core.engine.base.EngineTaskClient;
import eu.europeana.metis.core.engine.base.EngineTaskKey;
import eu.europeana.metis.core.engine.base.EngineTaskSettings;
import eu.europeana.metis.core.engine.base.PluginTypeToBatchJobMapper;
import eu.europeana.metis.core.engine.base.task.input.IntermediateInputDataEndpoint;
import eu.europeana.metis.core.engine.base.task.input.SimpleIntermediateInputDataEndpoint;
import eu.europeana.metis.core.workflow.plugins.AbstractExecutablePlugin;
import eu.europeana.metis.exception.ExternalTaskException;
import java.util.EnumMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * Abstract base class for creating curate or index tasks.
 * <p>
 * Represents a task factory that has an input data revision as a base.
 *
 * @param <S> The type of {@link EngineTaskSettings} used by the task.
 * @param <T> The type of {@link EngineTask} created by the factory.
 */
public abstract class AbstractIntermediateEngineTaskFactory<S extends EngineTaskSettings, T extends EngineTask> extends
    AbstractEngineTaskFactory<S, T> {

  protected final EngineTaskClient<S, T> engineTaskClient;
  protected final AbstractExecutablePlugin<?> plugin;

  /**
   * Constructor.
   *
   * @param engineTaskClient the client interface for managing engine tasks, including task creation, monitoring, and operational
   * interactions
   * @param plugin an instance of {@code AbstractExecutablePlugin}, representing the plugin providing configuration and metadata
   * for the associated task
   */
  protected AbstractIntermediateEngineTaskFactory(EngineTaskClient<S, T> engineTaskClient, AbstractExecutablePlugin<?> plugin) {
    super(engineTaskClient.getEngineTaskSettings());
    this.engineTaskClient = engineTaskClient;
    this.plugin = plugin;
  }

  protected Map<EngineTaskKey, String> createAllParameters(
      String engineDatasetId,
      String datasetId,
      String sourceExecutionId,
      Map<EngineTaskKey, String> pluginParameters
  ) {
    Map<EngineTaskKey, String> allParameters = new EnumMap<>(EngineTaskKey.class);

    allParameters.putAll(createDefaultTaskParameters(
        engineDatasetId,
        datasetId,
        sourceExecutionId,
        engineTaskClient.getEngineTaskSettings().getProvider()
    ));

    allParameters.putAll(pluginParameters);
    return allParameters;
  }

  protected void addJobNameParameter(Map<EngineTaskKey, String> pluginParameters) {
    PluginTypeToBatchJobMapper.map(plugin.getPluginMetadata().getExecutablePluginType())
                              .ifPresent(batchJobType -> pluginParameters.put(EngineTaskKey.JOB_NAME, batchJobType.name()));
  }

  protected SimpleIntermediateInputDataEndpoint createSimpleIntermediateInputDataEndpoint(
      String sourceExecutionId, String sourceBatchId) {
    return new SimpleIntermediateInputDataEndpoint("", sourceExecutionId, sourceBatchId);
  }

  @NotNull
  protected T createIntermediateEngineTask(Map<EngineTaskKey, String> allParameters,
      IntermediateInputDataEndpoint intermediateInputDataEndpoint) throws ExternalTaskException {
    return engineTaskClient.createEngineTask(allParameters, intermediateInputDataEndpoint, plugin.getTopologyName());
  }
}
