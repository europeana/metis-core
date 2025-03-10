package eu.europeana.metis.core.workflow.execution;

import eu.europeana.metis.core.user.User;
import eu.europeana.metis.core.workflow.WorkflowExecution;
import eu.europeana.metis.core.workflow.plugins.AbstractMetisPlugin;
import java.util.Optional;
import java.util.function.Predicate;
import org.bson.types.ObjectId;

/**
 * This class is responsible for converting WorkflowExecution objects to WorkflowExecutionDTO objects.
 * It provides a method to perform the conversion, taking into account the workflow's possibility to be incremental,
 * the display of raw XML for each plugin, and the users who started and cancelled the workflow.
 */
public final class WorkflowExecutionConverter {

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
   * @param canDisplayRawXml a predicate to determine if raw XML can be displayed for each plugin
   * @param userStarted the user who started the workflow
   * @param userCancelled the user who cancelled the workflow
   * @return the converted WorkflowExecutionDTO object, or null if the input WorkflowExecution is null
   */
  public static WorkflowExecutionDTO toDTO(WorkflowExecution workflowExecution, boolean isIncremental,
      Predicate<AbstractMetisPlugin<?>> canDisplayRawXml, User userStarted, User userCancelled) {
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
    workflowExecutionDTO.setWorkflowPriority(workflowExecution.getWorkflowPriority());
    workflowExecutionDTO.setCancelling(workflowExecution.isCancelling());
    workflowExecutionDTO.setCreatedDate(workflowExecution.getCreatedDate());
    workflowExecutionDTO.setStartedDate(workflowExecution.getStartedDate());
    workflowExecutionDTO.setUpdatedDate(workflowExecution.getUpdatedDate());
    workflowExecutionDTO.setFinishedDate(workflowExecution.getFinishedDate());
    workflowExecutionDTO.setIncremental(isIncremental);
    workflowExecutionDTO.setMetisPlugins(workflowExecution.getMetisPlugins().stream()
                                                          .map(plugin -> new PluginDTO(plugin, canDisplayRawXml.test(plugin)))
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
}
