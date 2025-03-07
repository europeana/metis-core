package eu.europeana.metis.core.workflow;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import eu.europeana.metis.core.dataset.Dataset;
import eu.europeana.metis.core.workflow.plugins.AbstractMetisPlugin;
import eu.europeana.metis.core.workflow.plugins.PluginStatus;
import eu.europeana.metis.core.workflow.plugins.PluginType;
import eu.europeana.metis.mongo.utils.ObjectIdSerializer;
import eu.europeana.metis.utils.CommonStringValues;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.bson.types.ObjectId;

/**
 * Is the structure where the combined plugins of harvesting and the other plugins will be stored.
 * <p>This is the object where the execution of the workflow takes place and will host all
 * information, regarding its execution.</p>
 */
public class WorkflowExecutionDTO {

  @JsonSerialize(using = ObjectIdSerializer.class)
  private ObjectId id;
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

  private List<AbstractMetisPlugin> metisPlugins = new ArrayList<>();

  public WorkflowExecutionDTO() {
    //Required for json serialization
  }

  /**
   * Constructor with all required parameters and initializes it's internal structure.
   *
   * @param dataset the {@link Dataset} related to the execution
   * @param metisPlugins the list of {@link AbstractMetisPlugin} including harvest plugin for
   * execution
   * @param workflowPriority the positive number of the priority of the execution
   */
  public WorkflowExecutionDTO(Dataset dataset, List<? extends AbstractMetisPlugin> metisPlugins,
      int workflowPriority) {
    this.datasetId = dataset.getDatasetId();
    this.ecloudDatasetId = dataset.getEcloudDatasetId();
    this.workflowPriority = workflowPriority;
    this.metisPlugins = new ArrayList<>(metisPlugins);
  }

  /**
   * Sets all plugins inside the execution, that have status {@link PluginStatus#INQUEUE} or {@link
   * PluginStatus#RUNNING} or {@link PluginStatus#CLEANING} or {@link PluginStatus#PENDING}, to
   * {@link PluginStatus#CANCELLED}
   */
  public void setWorkflowAndAllQualifiedPluginsToCancelled() {
    this.setWorkflowStatus(WorkflowStatus.CANCELLED);
    setAllQualifiedPluginsToCancelled();
    this.setCancelling(false);
  }

  /**
   * Checks if one of the plugins has {@link PluginStatus#FAILED} and if yes sets all other plugins
   * that have status {@link PluginStatus#INQUEUE} or {@link PluginStatus#RUNNING} or {@link
   * PluginStatus#CLEANING} or {@link PluginStatus#PENDING}, to {@link PluginStatus#CANCELLED}
   */
  public void checkAndSetAllRunningAndInqueuePluginsToCancelledIfOnePluginHasFailed() {
    boolean hasAPluginFailed = false;
    for (AbstractMetisPlugin metisPlugin : this.getMetisPlugins()) {
      if (metisPlugin.getPluginStatus() == PluginStatus.FAILED) {
        hasAPluginFailed = true;
        break;
      }
    }
    if (hasAPluginFailed) {
      this.setWorkflowStatus(WorkflowStatus.FAILED);
      setAllQualifiedPluginsToCancelled();
    }
  }

  private void setAllQualifiedPluginsToCancelled() {
    for (AbstractMetisPlugin metisPlugin : this.getMetisPlugins()) {
      if (metisPlugin.getPluginStatus() == PluginStatus.INQUEUE
          || metisPlugin.getPluginStatus() == PluginStatus.RUNNING
          || metisPlugin.getPluginStatus() == PluginStatus.CLEANING
          || metisPlugin.getPluginStatus() == PluginStatus.PENDING
          || metisPlugin.getPluginStatus() == PluginStatus.IDENTIFYING_DELETED_RECORDS) {
        metisPlugin.setPluginStatusAndResetFailMessage(PluginStatus.CANCELLED);
      }
    }
  }

  /**
   * Returns an {@link Optional} for the plugin with the given plugin type.
   *
   * @param pluginType The type of the plugin we are looking for.
   * @return The plugin.
   */
  public Optional<AbstractMetisPlugin> getMetisPluginWithType(PluginType pluginType) {
    return getMetisPlugins().stream().filter(plugin -> plugin.getPluginType() == pluginType)
        .findFirst();
  }

  public ObjectId getId() {
    return id;
  }

  public void setId(ObjectId id) {
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

  public List<AbstractMetisPlugin> getMetisPlugins() {
    return metisPlugins;
  }

  public void setMetisPlugins(List<AbstractMetisPlugin> metisPlugins) {
    if(metisPlugins != null) {
      this.metisPlugins = new ArrayList<>(metisPlugins);
    } else {
      this.metisPlugins = null;
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, datasetId);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    WorkflowExecutionDTO that = (WorkflowExecutionDTO) obj;
    return Objects.equals(id, that.getId()) && Objects.equals(datasetId, that.datasetId);
  }
}
