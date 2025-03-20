package eu.europeana.metis.core.workflow.execution;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.metis.core.workflow.WorkflowStatus;
import eu.europeana.metis.utils.CommonStringValues;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * This class represents the full information on a workflow execution needed for the execution history.
 */
public class WorkflowExecutionDTO {

  private String id;
  private String datasetId;
  private WorkflowStatus workflowStatus;
  private String ecloudDatasetId;
  private String cancelledBy;
  private String cancelledByUserName;
  private String cancelledByFirstName;
  private String cancelledByLastName;
  private String startedBy;
  private String startedByUserName;
  private String startedByFirstName;
  private String startedByLastName;
  private int workflowPriority;
  private boolean cancelling;
  @JsonFormat(pattern = CommonStringValues.DATE_FORMAT)
  private Date createdDate;
  @JsonFormat(pattern = CommonStringValues.DATE_FORMAT)
  private Date startedDate;
  @JsonFormat(pattern = CommonStringValues.DATE_FORMAT)
  private Date updatedDate;
  @JsonFormat(pattern = CommonStringValues.DATE_FORMAT)
  private Date finishedDate;
  private boolean isIncremental;
  private List<MetisPluginDTO> metisPlugins = new ArrayList<>();

  public WorkflowExecutionDTO() {
    //Required for json serialization
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public boolean isCancelling() {
    return cancelling;
  }

  public void setCancelling(boolean cancelling) {
    this.cancelling = cancelling;
  }

  public WorkflowStatus getWorkflowStatus() {
    return workflowStatus;
  }

  public void setWorkflowStatus(WorkflowStatus workflowStatus) {
    this.workflowStatus = workflowStatus;
  }

  public String getDatasetId() {
    return datasetId;
  }

  public void setDatasetId(String datasetId) {
    this.datasetId = datasetId;
  }

  public String getCancelledBy() {
    return cancelledBy;
  }

  public void setCancelledBy(String cancelledBy) {
    this.cancelledBy = cancelledBy;
  }

  public String getCancelledByUserName() {
    return cancelledByUserName;
  }

  public void setCancelledByUserName(String cancelledByUserName) {
    this.cancelledByUserName = cancelledByUserName;
  }

  public String getCancelledByFirstName() {
    return cancelledByFirstName;
  }

  public void setCancelledByFirstName(String cancelledByFirstName) {
    this.cancelledByFirstName = cancelledByFirstName;
  }

  public String getCancelledByLastName() {
    return cancelledByLastName;
  }

  public void setCancelledByLastName(String cancelledByLastName) {
    this.cancelledByLastName = cancelledByLastName;
  }

  public String getStartedBy() {
    return startedBy;
  }

  public void setStartedBy(String startedBy) {
    this.startedBy = startedBy;
  }

  public String getStartedByUserName() {
    return startedByUserName;
  }

  public void setStartedByUserName(String startedByUserName) {
    this.startedByUserName = startedByUserName;
  }

  public String getStartedByFirstName() {
    return startedByFirstName;
  }

  public void setStartedByFirstName(String startedByFirstName) {
    this.startedByFirstName = startedByFirstName;
  }

  public String getStartedByLastName() {
    return startedByLastName;
  }

  public void setStartedByLastName(String startedByLastName) {
    this.startedByLastName = startedByLastName;
  }

  public String getEcloudDatasetId() {
    return ecloudDatasetId;
  }

  public void setEcloudDatasetId(String ecloudDatasetId) {
    this.ecloudDatasetId = ecloudDatasetId;
  }

  public int getWorkflowPriority() {
    return workflowPriority;
  }

  public void setWorkflowPriority(int workflowPriority) {
    this.workflowPriority = workflowPriority;
  }

  public Date getCreatedDate() {
    return createdDate == null ? null : new Date(createdDate.getTime());
  }

  public void setCreatedDate(Date createdDate) {
    this.createdDate = createdDate == null ? null : new Date(createdDate.getTime());
  }

  public Date getStartedDate() {
    return startedDate == null ? null : new Date(startedDate.getTime());
  }

  public void setStartedDate(Date startedDate) {
    this.startedDate = startedDate == null ? null : new Date(startedDate.getTime());
  }

  public Date getFinishedDate() {
    return finishedDate == null ? null : new Date(finishedDate.getTime());
  }

  public void setFinishedDate(Date finishedDate) {
    this.finishedDate = finishedDate == null ? null : new Date(finishedDate.getTime());
  }

  public Date getUpdatedDate() {
    return updatedDate == null ? null : new Date(updatedDate.getTime());
  }

  public void setUpdatedDate(Date updatedDate) {
    this.updatedDate = updatedDate == null ? null : new Date(updatedDate.getTime());
  }

  @JsonProperty("isIncremental")
  public boolean isIncremental() {
    return isIncremental;
  }

  public void setIncremental(boolean incremental) {
    isIncremental = incremental;
  }

  public List<MetisPluginDTO> getMetisPlugins() {
    return new ArrayList<>(metisPlugins);
  }

  public void setMetisPlugins(List<MetisPluginDTO> metisPlugins) {
    this.metisPlugins = metisPlugins == null ? new ArrayList<>() : new ArrayList<>(metisPlugins);
  }
}
