package eu.europeana.metis.core.workflow;

import eu.europeana.metis.core.user.User;

public class WorkflowExecutionConverter {

  public static WorkflowExecution fromDTO(WorkflowExecutionDTO workflowExecutionDTO) {
    if (workflowExecutionDTO == null) {
      return null;
    }

    WorkflowExecution workflowExecution = new WorkflowExecution();
    workflowExecution.setId(workflowExecutionDTO.getId());
    workflowExecution.setDatasetId(workflowExecutionDTO.getDatasetId());
    workflowExecution.setWorkflowStatus(workflowExecutionDTO.getWorkflowStatus());
    workflowExecution.setEcloudDatasetId(workflowExecutionDTO.getEcloudDatasetId());
    workflowExecution.setCancelledBy(workflowExecutionDTO.getCancelledBy());
    workflowExecution.setStartedBy(workflowExecutionDTO.getStartedBy());
    workflowExecution.setWorkflowPriority(workflowExecutionDTO.getWorkflowPriority());
    workflowExecution.setCancelling(workflowExecutionDTO.isCancelling());
    workflowExecution.setCreatedDate(workflowExecutionDTO.getCreatedDate());
    workflowExecution.setStartedDate(workflowExecutionDTO.getStartedDate());
    workflowExecution.setUpdatedDate(workflowExecutionDTO.getUpdatedDate());
    workflowExecution.setFinishedDate(workflowExecutionDTO.getFinishedDate());
    workflowExecution.setMetisPlugins(workflowExecutionDTO.getMetisPlugins());

    return workflowExecution;
  }

  public static WorkflowExecutionDTO toDTO(WorkflowExecution workflowExecution, User userStarted, User userCancelled) {
    if (workflowExecution == null) {
      return null;
    }

    WorkflowExecutionDTO workflowExecutionDTO = new WorkflowExecutionDTO();
    workflowExecutionDTO.setId(workflowExecution.getId());
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
    workflowExecutionDTO.setMetisPlugins(workflowExecution.getMetisPlugins());

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
