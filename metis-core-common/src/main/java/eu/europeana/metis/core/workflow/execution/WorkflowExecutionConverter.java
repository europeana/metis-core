package eu.europeana.metis.core.workflow.execution;

import com.google.common.collect.Sets;
import eu.europeana.metis.core.user.User;
import eu.europeana.metis.core.workflow.WorkflowExecution;
import eu.europeana.metis.core.workflow.plugins.DataStatus;
import eu.europeana.metis.core.workflow.plugins.ExecutablePlugin;
import eu.europeana.metis.core.workflow.plugins.ExecutablePluginMetadata;
import eu.europeana.metis.core.workflow.plugins.ExecutablePluginType;
import eu.europeana.metis.core.workflow.plugins.ExecutionProgress;
import eu.europeana.metis.core.workflow.plugins.MetisPlugin;
import java.util.Optional;
import java.util.Set;
import org.bson.types.ObjectId;

/**
 * A utility class that provides methods for converting {@link WorkflowExecution} into {@link WorkflowExecutionDTO}.
 *
 * <p>This class is designed to act as a translator between the domain model
 * and the Data Transfer Object (DTO) for ExecutionProgress, ensuring separation of concerns and easing data transfer between
 * layers.
 */
public final class WorkflowExecutionConverter {

  public static final Set<ExecutablePluginType> NO_XML_PREVIEW_TYPES = Sets
      .immutableEnumSet(ExecutablePluginType.LINK_CHECKING, ExecutablePluginType.DEPUBLISH);

  private WorkflowExecutionConverter() {
  }

  /**
   * Converts a WorkflowExecution object to a WorkflowExecutionDTO object.
   * <p>
   * This method takes a WorkflowExecution object, a boolean indicating whether the workflow is incremental, a predicate to
   * determine if raw XML can be displayed for each plugin, and the users who started and cancelled the workflow. It returns a
   * WorkflowExecutionDTO object containing the converted data.
   *
   * @param workflowExecution the WorkflowExecution object to convert
   * @param isIncremental whether the workflow is incremental
   * @param userStarted the user who started the workflow
   * @param userCancelled the user who cancelled the workflow
   * @return the converted WorkflowExecutionDTO object, or null if the input WorkflowExecution is null
   */
  public static WorkflowExecutionDTO toDTO(WorkflowExecution workflowExecution, boolean isIncremental,
      User userStarted, User userCancelled) {
    if (workflowExecution == null) {
      return null;
    }

    WorkflowExecutionDTO workflowExecutionDTO = new WorkflowExecutionDTO();
    workflowExecutionDTO.setId(Optional.ofNullable(workflowExecution.getId()).map(ObjectId::toString).orElse(null));
    workflowExecutionDTO.setDatasetId(workflowExecution.getDatasetId());
    workflowExecutionDTO.setWorkflowStatus(workflowExecution.getWorkflowStatus());
    workflowExecutionDTO.setEcloudDatasetId(workflowExecution.getEcloudDatasetId());
    workflowExecutionDTO.setCancelledBy(workflowExecution.getCancelledBy());
    workflowExecutionDTO.setStartedBy(workflowExecution.getStartedBy());
    workflowExecutionDTO.setCancelling(workflowExecution.isCancelling());
    workflowExecutionDTO.setCreatedDate(workflowExecution.getCreatedDate());
    workflowExecutionDTO.setStartedDate(workflowExecution.getStartedDate());
    workflowExecutionDTO.setUpdatedDate(workflowExecution.getUpdatedDate());
    workflowExecutionDTO.setFinishedDate(workflowExecution.getFinishedDate());
    workflowExecutionDTO.setIncremental(isIncremental);
    workflowExecutionDTO.setMetisPlugins(
        workflowExecution.getMetisPlugins().stream()
                         .map(plugin -> MetisPluginConverter.toDTO(plugin, canDisplayRawXml(plugin)))
                         .toList());

    if (userStarted != null) {
      workflowExecutionDTO.setStartedByUserName(userStarted.getUserName());
      workflowExecutionDTO.setStartedByFirstName(userStarted.getFirstName());
      workflowExecutionDTO.setStartedByLastName(userStarted.getLastName());
    }

    if (userCancelled != null) {
      workflowExecutionDTO.setCancelledByUserName(userCancelled.getUserName());
      workflowExecutionDTO.setCancelledByFirstName(userCancelled.getFirstName());
      workflowExecutionDTO.setCancelledByLastName(userCancelled.getLastName());
    }

    return workflowExecutionDTO;

  }

  /**
   * Checks if a plugin can display raw XML data.
   * <p>
   * This method checks if the plugin's data is valid, if it has a blacklisted type, and if its execution progress is valid.
   *
   * @param plugin the plugin to check
   * @return true if the plugin can display raw XML data, false otherwise
   */
  public static boolean canDisplayRawXml(MetisPlugin plugin) {
    final boolean result;
    if (plugin instanceof ExecutablePlugin executablePlugin) {
      final boolean dataIsValid =
          MetisPlugin.getDataStatus(executablePlugin) == DataStatus.VALID;
      final ExecutionProgress progress = executablePlugin.getExecutionProgress();
      final boolean pluginHasBlacklistedType = Optional.of(executablePlugin)
                                                       .map(ExecutablePlugin::getPluginMetadata)
                                                       .map(ExecutablePluginMetadata::getExecutablePluginType)
                                                       .map(NO_XML_PREVIEW_TYPES::contains).orElse(Boolean.TRUE);
      result = dataIsValid && !pluginHasBlacklistedType && progress != null
          && progress.getProcessedRecords() > progress.getErrors();
    } else {
      result = false;
    }
    return result;
  }
}
