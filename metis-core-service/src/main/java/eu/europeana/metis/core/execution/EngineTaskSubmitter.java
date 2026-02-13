package eu.europeana.metis.core.execution;

import static eu.europeana.metis.core.workflow.plugins.ExecutablePluginType.ExecutablePluginTypeGroup.DEPUBLISH;
import static eu.europeana.metis.core.workflow.plugins.ExecutablePluginType.ExecutablePluginTypeGroup.HARVEST;
import static eu.europeana.metis.core.workflow.plugins.ExecutablePluginType.ExecutablePluginTypeGroup.INDEX;
import static eu.europeana.metis.core.workflow.plugins.ExecutablePluginType.ExecutablePluginTypeGroup.CURATE;
import static java.lang.String.format;

import eu.europeana.metis.core.engine.base.EngineTask;
import eu.europeana.metis.core.engine.base.EngineTaskClient;
import eu.europeana.metis.core.engine.base.EngineTaskSettings;
import eu.europeana.metis.core.execution.task.DepublishTaskFactory;
import eu.europeana.metis.core.execution.task.EngineTaskFactory;
import eu.europeana.metis.core.execution.task.HarvestTaskFactory;
import eu.europeana.metis.core.execution.task.IndexTaskFactory;
import eu.europeana.metis.core.execution.task.CurateTaskFactory;
import eu.europeana.metis.core.workflow.plugins.AbstractExecutablePlugin;
import eu.europeana.metis.core.workflow.plugins.DataStatus;
import eu.europeana.metis.core.workflow.plugins.ExecutablePluginType.ExecutablePluginTypeGroup;
import eu.europeana.metis.exception.ExternalTaskException;
import java.lang.invoke.MethodHandles;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class responsible for submitting engine tasks based on the provided plugin.
 *
 * @param <S> Generic type parameter extending AbstractEngineTaskSettings.
 * @param <T> Generic type parameter extending AbstractEngineTask.
 */
public class EngineTaskSubmitter<S extends EngineTaskSettings, T extends EngineTask> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final AbstractExecutablePlugin<?> plugin;
  private final EngineTaskClient<S, T> engineTaskClient;
  private final Map<ExecutablePluginTypeGroup, EngineTaskFactory<T>> engineTaskFactory;

  /**
   * Constructor.
   *
   * @param plugin AbstractExecutablePlugin instance used to execute the plugin logic.
   * @param engineTaskClient EngineTaskClient instance used to manage and interact with engine tasks.
   */
  public EngineTaskSubmitter(AbstractExecutablePlugin<?> plugin, EngineTaskClient<S, T> engineTaskClient) {
    this.plugin = plugin;
    this.engineTaskClient = engineTaskClient;

    this.engineTaskFactory = Map.of(
        HARVEST, new HarvestTaskFactory<>(engineTaskClient, plugin),
        CURATE, new CurateTaskFactory<>(engineTaskClient, plugin),
        INDEX, new IndexTaskFactory<>(engineTaskClient, plugin),
        DEPUBLISH, new DepublishTaskFactory<>(engineTaskClient, plugin)
    );
  }

  /**
   * Submits a new task to the engine for execution based on the provided dataset identifiers and the previous task information.
   * This method creates an engine task using the given parameters, submits it to the engine, and updates the plugin with the
   * submitted task ID and data status. If an error occurs during task creation or submission, an exception is thrown.
   *
   * @param datasetId The unique identifier of the dataset to be processed by the plugin.
   * @param engineDatasetId The unique identifier of the dataset within the engine context.
   * @param previousTaskId The unique identifier of the previous task, used for task chaining or dependencies.
   * @throws ExternalTaskException If an error occurs during task submission or execution.
   */
  public void submit(String datasetId, String engineDatasetId, String previousTaskId) throws ExternalTaskException {
    ExecutablePluginTypeGroup executablePluginTypeGroup = plugin.getPluginMetadata().getExecutablePluginType()
                                                                .getExecutablePluginTypeGroup();
    T engineTask = engineTaskFactory.get(executablePluginTypeGroup).create(datasetId, engineDatasetId, previousTaskId);

    LOGGER.info("Starting execution of {} plugin for externalDatasetId {}", plugin.getPluginType(), datasetId);
    try {
      String taskId = engineTaskClient.submitEngineTask(engineTask, plugin.getTopologyName());
      plugin.setExternalTaskId(taskId);
      plugin.setDataStatus(DataStatus.VALID);
    } catch (ExternalTaskException | RuntimeException e) {
      throw new ExternalTaskException(
          format("Submitting task for plugin type %s and dataset %s failed", plugin.getPluginType(), datasetId), e);
    }
    LOGGER.info("Submitted task with externalTaskId: {}", plugin.getExternalTaskId());
  }
}
