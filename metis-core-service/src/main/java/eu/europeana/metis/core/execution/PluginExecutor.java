package eu.europeana.metis.core.execution;

import static org.apache.commons.lang3.StringUtils.isBlank;

import eu.europeana.metis.core.dao.DataEvolutionUtils;
import eu.europeana.metis.core.dao.ExecutedMetisPluginId;
import eu.europeana.metis.core.dao.PluginWithExecutionId;
import eu.europeana.metis.core.dao.WorkflowExecutionDao;
import eu.europeana.metis.core.engine.base.EngineTask;
import eu.europeana.metis.core.engine.base.EngineTaskClient;
import eu.europeana.metis.core.engine.base.EngineTaskSettings;
import eu.europeana.metis.core.workflow.WorkflowExecution;
import eu.europeana.metis.core.workflow.WorkflowExecutionHelper;
import eu.europeana.metis.core.workflow.plugins.AbstractExecutablePlugin;
import eu.europeana.metis.core.workflow.plugins.AbstractExecutablePluginMetadata;
import eu.europeana.metis.core.workflow.plugins.AbstractHarvestPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.AbstractIndexPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.ExecutablePlugin;
import eu.europeana.metis.core.workflow.plugins.PluginStatus;
import eu.europeana.metis.exception.ExternalTaskException;
import java.util.Date;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * A service class responsible for executing plugins in the context of a workflow execution. This service manages the interaction
 * between plugins, task clients, and workflow execution state, ensuring a proper plugin execution flow and handling any
 * exceptions that occur.
 *
 * @param <S> The type of {@link EngineTaskSettings} used for the engine task settings of the plugin.
 * @param <T> The type of {@link EngineTask} used for representing the tasks to be executed.
 */
@Slf4j
public class PluginExecutor<S extends EngineTaskSettings, T extends EngineTask> {

  private static final String TRIGGER_ERROR_PREFIX = "An error occurred while triggering the external task. ";
  private static final String DETAILED_EXCEPTION_FORMAT = "%s%nDetailed exception:%s";

  private final EngineTaskClient<S, T> engineTaskClient;
  private final WorkflowExecutionDao workflowExecutionDao;
  private final WorkflowExecutionHelper workflowExecutionHelper = new WorkflowExecutionHelper();

  /**
   * Constructor.
   *
   * @param engineTaskClient the {@link EngineTaskClient} instance used to manage and interact with engine tasks
   * @param workflowExecutionDao the {@link WorkflowExecutionDao} instance used to perform database operations related to workflow
   * executions
   */
  public PluginExecutor(EngineTaskClient<S, T> engineTaskClient, WorkflowExecutionDao workflowExecutionDao) {
    this.engineTaskClient = engineTaskClient;
    this.workflowExecutionDao = workflowExecutionDao;
  }

  /**
   * Prepares the plugin metadata and submits it.
   *
   * @param plugin the {@link AbstractExecutablePlugin} instance to be executed
   * @param workflowExecution the {@link WorkflowExecution} object representing the current workflow execution context
   * @return true if the plugin execution was successful, false otherwise
   */
  public boolean execute(AbstractExecutablePlugin<?> plugin, WorkflowExecution workflowExecution) {
    EngineTaskSubmitter<S, T> engineTaskSubmitter = new EngineTaskSubmitter<>(plugin, engineTaskClient);
    try {
      preparePredecessorMetadata(plugin, workflowExecution);
      prepareHarvestInfoForIndexPlugin(plugin, workflowExecution);
      submitIfNotStarted(plugin, workflowExecution, engineTaskSubmitter);
    } catch (ExternalTaskException | RuntimeException e) {
      log.warn(String.format("workflowExecutionId: %s, pluginType: %s - Execution of plugin failed", workflowExecution.getId(),
          plugin.getPluginType()), e);

      plugin.setFinishedDate(null);
      plugin.setPluginStatusAndResetFailMessage(PluginStatus.FAILED);
      plugin.setFailMessage(String.format(DETAILED_EXCEPTION_FORMAT, TRIGGER_ERROR_PREFIX, ExceptionUtils.getStackTrace(e)));
      return false;
    } finally {
      workflowExecutionDao.updateWorkflowPlugins(workflowExecution);
    }
    return true;
  }

  private void submitIfNotStarted(AbstractExecutablePlugin<?> plugin,
      WorkflowExecution workflowExecution, EngineTaskSubmitter<S, T> engineTaskSubmitter)
      throws ExternalTaskException {
    if (isBlank(plugin.getExternalTaskId())) {
      if (plugin.getStartedDate() == null) {
        plugin.setStartedDate(new Date());
      }

      engineTaskSubmitter.submit(
          workflowExecution.getDatasetId(),
          workflowExecution.getEcloudDatasetId(),
          getExternalTaskIdOfPreviousPlugin(plugin.getPluginMetadata(), workflowExecution)
      );
    }
  }

  private void preparePredecessorMetadata(AbstractExecutablePlugin<?> plugin, WorkflowExecution workflowExecution) {
    AbstractExecutablePluginMetadata metadata = plugin.getPluginMetadata();
    ExecutedMetisPluginId executedMetisPluginId = ExecutedMetisPluginId.forPredecessor(plugin);

    if (executedMetisPluginId == null) {
      ExecutablePlugin predecessor =
          DataEvolutionUtils.computePredecessorPlugin(metadata.getExecutablePluginType(), workflowExecution);
      if (predecessor != null) {
        metadata.setPreviousRevisionInformation(predecessor);
        workflowExecutionDao.updateWorkflowPlugins(workflowExecution);
      }
    }
  }

  private void prepareHarvestInfoForIndexPlugin(AbstractExecutablePlugin<?> plugin, WorkflowExecution workflowExecution) {
    // Compute base harvesting plugin information. We can't do this when creating the workflow
    // execution: the harvest might be part of this very workflow.
    if (DataEvolutionUtils.getIndexPluginGroup().contains(plugin.getPluginMetadata().getExecutablePluginType())) {
      PluginWithExecutionId<ExecutablePlugin> rootAncestor =
          new DataEvolutionUtils(workflowExecutionDao)
              .getRootAncestor(new PluginWithExecutionId<>(workflowExecution, plugin));

      setHarvestParametersToIndexingPlugin(plugin, rootAncestor.getPlugin());
    }
  }

  private void setHarvestParametersToIndexingPlugin(ExecutablePlugin indexingPlugin, ExecutablePlugin harvestPlugin) {
    // Check the harvesting types
    if (!DataEvolutionUtils.getHarvestPluginGroup().contains(harvestPlugin.getPluginMetadata().getExecutablePluginType())) {
      throw new IllegalStateException("Root ancestor is not a harvesting plugin");
    }

    // get the information from the harvesting plugin.
    final boolean incrementalHarvest =
        harvestPlugin.getPluginMetadata() instanceof AbstractHarvestPluginMetadata abstractHarvestPluginMetadata
            && abstractHarvestPluginMetadata.isIncrementalHarvest();
    final Date harvestDate = harvestPlugin.getStartedDate();

    // Set the information to the indexing plugin.
    if (indexingPlugin.getPluginMetadata() instanceof AbstractIndexPluginMetadata abstractIndexPluginMetadata) {
      abstractIndexPluginMetadata.setIncrementalIndexing(incrementalHarvest);
      abstractIndexPluginMetadata.setHarvestDate(harvestDate);
    }
  }

  private String getExternalTaskIdOfPreviousPlugin(AbstractExecutablePluginMetadata metadata,
      WorkflowExecution workflowExecution) {

    ExecutedMetisPluginId predecessorPlugin =
        ExecutedMetisPluginId.forPredecessor(metadata);

    if (predecessorPlugin == null) {
      return null;
    }

    WorkflowExecution previousExecution = workflowExecutionDao.getByTaskExecution(predecessorPlugin,
        workflowExecution.getDatasetId());

    return Optional.ofNullable(previousExecution)
                   .flatMap(exec ->
                       workflowExecutionHelper.getMetisPluginWithType(exec, predecessorPlugin.getPluginType()))
                   .map(AbstractExecutablePlugin.class::cast)
                   .map(AbstractExecutablePlugin::getExternalTaskId)
                   .orElse(null);
  }
}
