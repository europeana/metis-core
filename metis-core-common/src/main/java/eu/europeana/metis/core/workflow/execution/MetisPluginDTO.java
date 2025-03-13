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

  public void setPluginType(PluginType pluginType) {
    this.pluginType = pluginType;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setPluginStatus(PluginStatus pluginStatus) {
    this.pluginStatus = pluginStatus;
  }

  public void setDataStatus(DataStatus dataStatus) {
    this.dataStatus = dataStatus;
  }

  public void setFailMessage(String failMessage) {
    this.failMessage = failMessage;
  }

  public void setStartedDate(Date startedDate) {
    this.startedDate = startedDate;
  }

  public void setUpdatedDate(Date updatedDate) {
    this.updatedDate = updatedDate;
  }

  public void setFinishedDate(Date finishedDate) {
    this.finishedDate = finishedDate;
  }

  public void setExternalTaskId(String externalTaskId) {
    this.externalTaskId = externalTaskId;
  }

  public void setExecutionProgress(ExecutionProgressDTO executionProgress) {
    this.executionProgress = executionProgress;
  }

  public void setTopologyName(String topologyName) {
    this.topologyName = topologyName;
  }

  public void setCanDisplayRawXml(boolean canDisplayRawXml) {
    this.canDisplayRawXml = canDisplayRawXml;
  }

  public void setPluginMetadata(MetisPluginMetadata pluginMetadata) {
    this.pluginMetadata = pluginMetadata;
  }

  public PluginType getPluginType() {
    return pluginType;
  }

  public String getId() {
    return id;
  }

  public PluginStatus getPluginStatus() {
    return pluginStatus;
  }

  public DataStatus getDataStatus() {
    return dataStatus;
  }

  public String getFailMessage() {
    return failMessage;
  }

  public Date getStartedDate() {
    return startedDate != null ? new Date(startedDate.getTime()) : null;
  }

  public Date getUpdatedDate() {
    return updatedDate != null ? new Date(updatedDate.getTime()) : null;
  }

  public Date getFinishedDate() {
    return finishedDate != null ? new Date(finishedDate.getTime()) : null;
  }

  public String getExternalTaskId() {
    return externalTaskId;
  }

  public ExecutionProgressDTO getExecutionProgress() {
    return executionProgress;
  }

  public String getTopologyName() {
    return topologyName;
  }

  public boolean isCanDisplayRawXml() {
    return canDisplayRawXml;
  }

  public MetisPluginMetadata getPluginMetadata() {
    return pluginMetadata;
  }
}
