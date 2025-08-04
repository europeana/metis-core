package eu.europeana.metis.core.workflow.execution;

import com.fasterxml.jackson.annotation.JsonFormat;
import eu.europeana.metis.core.workflow.plugins.DataStatus;
import eu.europeana.metis.core.workflow.plugins.MetisPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.PluginStatus;
import eu.europeana.metis.core.workflow.plugins.PluginType;
import eu.europeana.metis.utils.CommonStringValues;
import java.util.Date;

/**
 * This class represents the complete information on a plugin execution needed for the execution history.
 */
public class MetisPluginDTO {

  private PluginType pluginType;
  private String id;
  private PluginStatus pluginStatus;
  private DataStatus dataStatus;
  private String failMessage;
  @JsonFormat(pattern = CommonStringValues.DATE_FORMAT)
  private Date startedDate;
  @JsonFormat(pattern = CommonStringValues.DATE_FORMAT)
  private Date updatedDate;
  @JsonFormat(pattern = CommonStringValues.DATE_FORMAT)
  private Date finishedDate;
  private String externalTaskId;
  private ExecutionProgressDTO executionProgress;
  private String topologyName;
  private boolean canDisplayRawXml;
  private MetisPluginMetadata pluginMetadata;

  public MetisPluginDTO() {
    //Required for json serialization
  }

  public PluginType getPluginType() {
    return pluginType;
  }

  public void setPluginType(PluginType pluginType) {
    this.pluginType = pluginType;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public PluginStatus getPluginStatus() {
    return pluginStatus;
  }

  public void setPluginStatus(PluginStatus pluginStatus) {
    this.pluginStatus = pluginStatus;
  }

  public DataStatus getDataStatus() {
    return dataStatus;
  }

  public void setDataStatus(DataStatus dataStatus) {
    this.dataStatus = dataStatus;
  }

  public String getFailMessage() {
    return failMessage;
  }

  public void setFailMessage(String failMessage) {
    this.failMessage = failMessage;
  }

  public Date getStartedDate() {
    return startedDate != null ? new Date(startedDate.getTime()) : null;
  }

  public void setStartedDate(Date startedDate) {
    this.startedDate = startedDate == null ? null : new Date(startedDate.getTime());
  }

  public Date getUpdatedDate() {
    return updatedDate != null ? new Date(updatedDate.getTime()) : null;
  }

  public void setUpdatedDate(Date updatedDate) {
    this.updatedDate = updatedDate == null ? null : new Date(updatedDate.getTime());
  }

  public Date getFinishedDate() {
    return finishedDate != null ? new Date(finishedDate.getTime()) : null;
  }

  public void setFinishedDate(Date finishedDate) {
    this.finishedDate = finishedDate == null ? null : new Date(finishedDate.getTime());
  }

  public String getExternalTaskId() {
    return externalTaskId;
  }

  public void setExternalTaskId(String externalTaskId) {
    this.externalTaskId = externalTaskId;
  }

  public ExecutionProgressDTO getExecutionProgress() {
    return executionProgress;
  }

  public void setExecutionProgress(ExecutionProgressDTO executionProgress) {
    this.executionProgress = executionProgress;
  }

  public String getTopologyName() {
    return topologyName;
  }

  public void setTopologyName(String topologyName) {
    this.topologyName = topologyName;
  }

  public boolean isCanDisplayRawXml() {
    return canDisplayRawXml;
  }

  public void setCanDisplayRawXml(boolean canDisplayRawXml) {
    this.canDisplayRawXml = canDisplayRawXml;
  }

  public MetisPluginMetadata getPluginMetadata() {
    return pluginMetadata;
  }

  public void setPluginMetadata(MetisPluginMetadata pluginMetadata) {
    this.pluginMetadata = pluginMetadata;
  }
}
