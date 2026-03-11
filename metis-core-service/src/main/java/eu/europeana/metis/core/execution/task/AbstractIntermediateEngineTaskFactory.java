package eu.europeana.metis.core.execution.task;

import static eu.europeana.metis.core.engine.base.EngineTaskParametersConfigurator.createDataRevision;
import static eu.europeana.metis.core.engine.base.EngineTaskParametersConfigurator.createDefaultTaskParameters;
import static java.util.Objects.requireNonNull;

import eu.europeana.metis.core.engine.base.DataRevision;
import eu.europeana.metis.core.engine.base.EngineTask;
import eu.europeana.metis.core.engine.base.EngineTaskClient;
import eu.europeana.metis.core.engine.base.EngineTaskKey;
import eu.europeana.metis.core.engine.base.EngineTaskSettings;
import eu.europeana.metis.core.engine.base.task.input.IntermediateInputDataEndpoint;
import eu.europeana.metis.core.workflow.plugins.AbstractExecutablePlugin;
import eu.europeana.metis.core.workflow.plugins.PluginType;
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

  @NotNull
  protected T createInternalEngineTask(String datasetId, String engineDatasetId, String previousTaskId,
      Map<EngineTaskKey, String> pluginParameters) {
    final DataRevision inputDataRevision = createDataRevision(
        requireNonNull(PluginType.getPluginTypeFromEnumName(plugin.getPluginMetadata().getRevisionNamePreviousPlugin())),
        plugin.getPluginMetadata().getRevisionTimestampPreviousPlugin(),
        engineTaskClient.getEngineTaskSettings().getProvider());

    final String dataLocation = getDataLocation(engineDatasetId);
    final Map<EngineTaskKey, String> basicTaskParameters =
        createDefaultTaskParameters(engineDatasetId, datasetId, previousTaskId, inputDataRevision, dataLocation);
    final Map<EngineTaskKey, String> allParameters = new EnumMap<>(EngineTaskKey.class);
    allParameters.putAll(basicTaskParameters);
    allParameters.putAll(pluginParameters);

    final DataRevision outputDataRevision = createDataRevision(
        plugin.getPluginType(), plugin.getStartedDate(), engineTaskClient.getEngineTaskSettings().getProvider());

    final IntermediateInputDataEndpoint intermediateInputDataEndpoint =
        new IntermediateInputDataEndpoint(dataLocation, previousTaskId, inputDataRevision);
    return engineTaskClient.createEngineTask(allParameters, intermediateInputDataEndpoint, outputDataRevision);
  }
}
