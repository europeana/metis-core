package eu.europeana.metis.core.execution;

import eu.europeana.metis.core.dao.WorkflowExecutionDao;
import eu.europeana.metis.core.workflow.plugins.AbstractMetisPlugin;
import eu.europeana.metis.core.workflow.plugins.PluginType;
import java.util.EnumSet;
import java.util.Set;

/**
 * @author Simon Tzanakis (Simon.Tzanakis@europeana.eu)
 * @since 2018-01-29
 */
public final class ExecutionRules {

  private static final Set<PluginType> HARVEST_PLUGIN_GROUP = EnumSet
      .of(PluginType.OAIPMH_HARVEST, PluginType.HTTP_HARVEST);
  private static final Set<PluginType> PROCESS_PLUGIN_GROUP = EnumSet
      .of(PluginType.VALIDATION_EXTERNAL, PluginType.TRANSFORMATION,
          PluginType.VALIDATION_INTERNAL, PluginType.NORMALIZATION, PluginType.ENRICHMENT,
          PluginType.MEDIA_PROCESS);
  private static final Set<PluginType> INDEX_PLUGIN_GROUP =
      EnumSet.of(PluginType.PREVIEW, PluginType.PUBLISH);

  private ExecutionRules() {
    //Private constructor
  }

  /**
   * Get the latest plugin that is allowed to be run for a plugin that is requested for execution.
   * <p>A pluginType execution must have a source pluginType, except if it's a harvesting plugin.
   * The ordering of the pluginTypes are predefined in code, but an enforcedPluginType can overwrite that, and
   * will try to use the enforcedPluginType as a source, if an execution that has properly finished exists.
   * Executions that are reported as FINISHED but have all records have errors, is not a valid execution as a source.</p>
   *
   * @param pluginType the {@link PluginType} that is to be executed
   * @param enforcedPluginType the {@link PluginType} used to enforce the source pluginType of the execution
   * @param datasetId the dataset identifier to check for
   * @param workflowExecutionDao {@link WorkflowExecutionDao} to access the corresponding database
   * @return the {@link AbstractMetisPlugin} that the pluginType execution will use as a source or null
   */
  public static AbstractMetisPlugin getLatestFinishedPluginIfRequestedPluginAllowedForExecution(
      PluginType pluginType, PluginType enforcedPluginType,
      String datasetId,
      WorkflowExecutionDao workflowExecutionDao) {
    AbstractMetisPlugin abstractMetisPlugin = null;
    if (enforcedPluginType != null) {
      abstractMetisPlugin = workflowExecutionDao
          .getLastFinishedWorkflowExecutionPluginByDatasetIdAndPluginType(datasetId,
              EnumSet.of(enforcedPluginType));
    } else if (PROCESS_PLUGIN_GROUP.contains(pluginType) || INDEX_PLUGIN_GROUP.contains(pluginType)) { 
      // Get latest FINISHED plugin for datasetId
      abstractMetisPlugin = getLatestFinishedPluginAllowedForExecution(pluginType, datasetId,
          workflowExecutionDao);
    }
    return abstractMetisPlugin;
  }

  private static AbstractMetisPlugin getLatestFinishedPluginAllowedForExecution(
      PluginType pluginType, String datasetId, WorkflowExecutionDao workflowExecutionDao) {

    AbstractMetisPlugin latestFinishedWorkflowExecutionByDatasetIdAndPluginType = null;

    Set<PluginType> latestPreviousPluginTypesSet = null;
    switch (pluginType) {
      case VALIDATION_EXTERNAL:
        latestPreviousPluginTypesSet = HARVEST_PLUGIN_GROUP;
        break;
      case TRANSFORMATION:
        latestPreviousPluginTypesSet = EnumSet.of(PluginType.VALIDATION_EXTERNAL);
        break;
      case VALIDATION_INTERNAL:
        latestPreviousPluginTypesSet = EnumSet.of(PluginType.TRANSFORMATION);
        break;
      case NORMALIZATION:
        latestPreviousPluginTypesSet = EnumSet.of(PluginType.VALIDATION_INTERNAL);
        break;
      case ENRICHMENT:
        latestPreviousPluginTypesSet = EnumSet.of(PluginType.NORMALIZATION);
        break;
      case MEDIA_PROCESS:
        latestPreviousPluginTypesSet = EnumSet.of(PluginType.ENRICHMENT);
        break;
      case PREVIEW:
        latestPreviousPluginTypesSet = EnumSet.of(PluginType.MEDIA_PROCESS);
        break;
      case PUBLISH:
        latestPreviousPluginTypesSet = EnumSet.of(PluginType.PREVIEW);
        break;
      default:
        break;
    }
    if (latestPreviousPluginTypesSet != null) {
      latestFinishedWorkflowExecutionByDatasetIdAndPluginType = workflowExecutionDao
          .getLastFinishedWorkflowExecutionPluginByDatasetIdAndPluginType(datasetId,
              latestPreviousPluginTypesSet);
    }
    return latestFinishedWorkflowExecutionByDatasetIdAndPluginType;
  }

  public static Set<PluginType> getHarvestPluginGroup() {
    return EnumSet.copyOf(HARVEST_PLUGIN_GROUP);
  }

  public static Set<PluginType> getProcessPluginGroup() {
    return EnumSet.copyOf(PROCESS_PLUGIN_GROUP);
  }

  public static Set<PluginType> getIndexPluginGroup() {
    return EnumSet.copyOf(INDEX_PLUGIN_GROUP);
  }
}
