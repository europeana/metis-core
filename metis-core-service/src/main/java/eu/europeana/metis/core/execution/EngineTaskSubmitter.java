package eu.europeana.metis.core.execution;

import eu.europeana.metis.core.dao.DatasetXsltDao;
import eu.europeana.metis.core.engine.base.EngineTask;
import eu.europeana.metis.core.engine.base.EngineTaskClient;
import eu.europeana.metis.core.engine.base.EngineTaskSettings;
import eu.europeana.metis.core.execution.task.CurateTaskFactory;
import eu.europeana.metis.core.execution.task.DepublishTaskFactory;
import eu.europeana.metis.core.execution.task.EngineTaskFactory;
import eu.europeana.metis.core.execution.task.HarvestTaskFactory;
import eu.europeana.metis.core.execution.task.IndexTaskFactory;
import eu.europeana.metis.core.workflow.plugins.AbstractExecutablePlugin;
import eu.europeana.metis.core.workflow.plugins.DataStatus;
import eu.europeana.metis.core.workflow.plugins.ExecutablePluginType.ExecutablePluginTypeGroup;
import eu.europeana.metis.exception.ExternalTaskException;
import lombok.extern.slf4j.Slf4j;

/**
 * A class responsible for submitting engine tasks based on the provided plugin.
 *
 * @param <S> Generic type parameter extending AbstractEngineTaskSettings.
 * @param <T> Generic type parameter extending AbstractEngineTask.
 */
@Slf4j
public class EngineTaskSubmitter<S extends EngineTaskSettings, T extends EngineTask> {

  private final AbstractExecutablePlugin<?> plugin;
  private final EngineTaskClient<S, T> engineTaskClient;
  private final EngineTaskFactory<T> engineTaskFactory;

  /**
   * Constructor.
   *
   * @param plugin AbstractExecutablePlugin instance used to execute the plugin logic.
   * @param engineTaskClient EngineTaskClient instance used to manage and interact with engine tasks.
   * @param datasetXsltDao DatasetXsltDao instance used to retrieve dataset-specific XSLT data.
   */
  public EngineTaskSubmitter(AbstractExecutablePlugin<?> plugin, EngineTaskClient<S, T> engineTaskClient,
      DatasetXsltDao datasetXsltDao) {
    this.plugin = plugin;
    this.engineTaskClient = engineTaskClient;

    ExecutablePluginTypeGroup executablePluginTypeGroup = plugin.getPluginMetadata()
                                                                .getExecutablePluginType()
                                                                .getExecutablePluginTypeGroup();

    this.engineTaskFactory = switch (executablePluginTypeGroup) {
      case HARVEST -> new HarvestTaskFactory<>(engineTaskClient, plugin);
      case CURATE -> new CurateTaskFactory<>(engineTaskClient, plugin, datasetXsltDao);
      case INDEX -> new IndexTaskFactory<>(engineTaskClient, plugin);
      case DEPUBLISH -> new DepublishTaskFactory<>(engineTaskClient, plugin);
    };
  }

  /**
   * Creates a new task to the engine for execution based on the provided dataset identifiers and the previous task information.
   * This method creates an engine task using the given parameters, submits it to the engine, and updates the plugin with the
   * submitted task ID and data status. If an error occurs during task creation or submission, an exception is thrown.
   *
   * @throws ExternalTaskException If an error occurs during task submission or execution.
   */
  public T createTask(EngineTaskSubmitContext engineTaskSubmitContext) throws ExternalTaskException {
    log.info("Create task of {} plugin for engineDatasetId {}", plugin.getPluginType(), engineTaskSubmitContext.getEngineDatasetId());
    try {
      T engineTask = engineTaskFactory.create(engineTaskSubmitContext);
      plugin.setExternalTaskId(engineTask.getExternalTaskId());
      plugin.setBatchId(engineTask.getBatchId());
      plugin.setDataStatus(DataStatus.VALID);
      log.info("Created task with externalTaskId: {}", plugin.getExternalTaskId());
      return engineTask;
    } catch (IllegalStateException | ExternalTaskException e) {
      throw e;
    } catch (RuntimeException e) {
      throw new ExternalTaskException(
          "Create task for plugin type %s and dataset %s failed"
              .formatted(plugin.getPluginMetadata().getExecutablePluginType(), engineTaskSubmitContext.getDatasetId()),
          e
      );
    }
  }

  /**
   * Submits a task identified by the specified external task ID to the processing engine for execution.
   *
   * @param engineTask The unique identifier of the external task to be submitted.
   * @throws ExternalTaskException If an error occurs during the submission process, such as a failure in external resource
   * interaction.
   */
  public void submitTask(T engineTask) throws ExternalTaskException {
    log.info("Submit task with externalTaskId: {}", engineTask.getExternalTaskId());
    String engineTaskId = engineTaskClient.submitEngineTask(engineTask, plugin.getTopologyName());
    //todo: this can be removed when sandbox creates the id on task creation instead of submission.
    plugin.setExternalTaskId(engineTaskId);
    log.info("Submitted task with externalTaskId: {}", engineTask.getExternalTaskId());
  }
}
