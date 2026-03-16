package eu.europeana.metis.core.workflow;

import eu.europeana.metis.core.workflow.plugins.AbstractExecutablePlugin;
import eu.europeana.metis.core.workflow.plugins.AbstractMetisPlugin;
import eu.europeana.metis.core.workflow.plugins.ExecutablePluginType;
import eu.europeana.metis.core.workflow.plugins.PluginStatus;
import eu.europeana.metis.core.workflow.plugins.PluginType;
import java.util.List;
import java.util.Optional;

/**
 * This class provides helper methods for managing workflow executions.
 */
public class WorkflowExecutionHelper {

  /**
   * Returns an {@link Optional} for the plugin with the given plugin type.
   *
   * @param workflowExecution The workflow execution to search for the plugin in.
   * @param pluginType The type of the plugin we are looking for.
   * @return The plugin.
   */
  public Optional<AbstractMetisPlugin> getMetisPluginWithType(WorkflowExecution workflowExecution, PluginType pluginType) {
    return workflowExecution.getMetisPlugins().stream().filter(plugin -> plugin.getPluginType() == pluginType)
                            .findFirst();
  }

  /**
   * Sets all plugins inside the execution, that have status {@link PluginStatus#INQUEUE} or {@link PluginStatus#RUNNING} or
   * {@link PluginStatus#CLEANING} or {@link PluginStatus#PENDING}, to {@link PluginStatus#CANCELLED}
   *
   * @param workflowExecution the workflow execution to check and update
   */
  public void setWorkflowAndAllQualifiedPluginsToCancelled(WorkflowExecution workflowExecution) {
    workflowExecution.setWorkflowStatus(WorkflowStatus.CANCELLED);
    setAllQualifiedPluginsToCancelled(workflowExecution);
    workflowExecution.setCancelling(false);
  }

  /**
   * Checks if one of the plugins has {@link PluginStatus#FAILED} and if yes sets all other plugins that have status
   * {@link PluginStatus#INQUEUE} or {@link PluginStatus#RUNNING} or {@link PluginStatus#CLEANING} or
   * {@link PluginStatus#PENDING}, to {@link PluginStatus#CANCELLED}
   *
   * @param workflowExecution the workflow execution to check and update
   */
  public void checkAndSetAllRunningAndInqueuePluginsToCancelledIfOnePluginHasFailed(WorkflowExecution workflowExecution) {
    boolean hasAPluginFailed = false;
    for (AbstractMetisPlugin<?> metisPlugin : workflowExecution.getMetisPlugins()) {
      if (metisPlugin.getPluginStatus() == PluginStatus.FAILED) {
        hasAPluginFailed = true;
        break;
      }
    }
    if (hasAPluginFailed) {
      workflowExecution.setWorkflowStatus(WorkflowStatus.FAILED);
      setAllQualifiedPluginsToCancelled(workflowExecution);
    }
  }

  public void moveToNextPlugin(WorkflowExecution workflowExecution) {
    List<AbstractMetisPlugin> plugins = workflowExecution.getMetisPlugins();
    AbstractExecutablePlugin<?> nextPlugin = null;
    for (AbstractMetisPlugin<?> plugin : plugins) {
      if (plugin instanceof AbstractExecutablePlugin<?> executablePlugin
          && executablePlugin.getPluginStatus() == PluginStatus.INQUEUE) {

        nextPlugin = executablePlugin;
        break;
      }
    }

    if (nextPlugin == null) {
      workflowExecution.setNextExecutablePluginType(null);
    } else {
      ExecutablePluginType nextExecutablePluginType =
          ExecutablePluginType.getExecutablePluginFromPluginType(nextPlugin.getPluginType());
      workflowExecution.setNextExecutablePluginType(nextExecutablePluginType);
    }
  }

  private void setAllQualifiedPluginsToCancelled(WorkflowExecution workflowExecution) {
    for (AbstractMetisPlugin metisPlugin : workflowExecution.getMetisPlugins()) {
      if (metisPlugin.getPluginStatus().isRunnable()) {
        metisPlugin.setPluginStatusAndResetFailMessage(PluginStatus.CANCELLED);
      }
    }
  }
}
