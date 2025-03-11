package eu.europeana.metis.core.workflow.execution;

import com.fasterxml.jackson.annotation.JsonFormat;
import eu.europeana.metis.core.workflow.plugins.AbstractExecutablePlugin;
import eu.europeana.metis.core.workflow.plugins.AbstractMetisPlugin;
import eu.europeana.metis.core.workflow.plugins.DataStatus;
import eu.europeana.metis.core.workflow.plugins.MetisPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.PluginStatus;
import eu.europeana.metis.core.workflow.plugins.PluginType;
import eu.europeana.metis.utils.CommonStringValues;
import java.util.Date;

/**
 * This class represents the complete information on a plugin execution needed for the execution history.
 */
public class PluginDTO {

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
  private PluginProgressDTO executionProgress;
  private String topologyName;
  private boolean canDisplayRawXml;
  private MetisPluginMetadata pluginMetadata;

  public PluginDTO() {
  }

  /**
   * Creates a new PluginDTO instance based on the provided AbstractMetisPlugin and canDisplayRawXml flag.
   *
   * @param plugin the AbstractMetisPlugin instance to extract data from
   * @param canDisplayRawXml a flag indicating whether raw XML can be displayed
   */
  public PluginDTO(AbstractMetisPlugin plugin, boolean canDisplayRawXml) {
    this.pluginType = plugin.getPluginType();
    this.id = plugin.getId();
    this.pluginStatus = plugin.getPluginStatus();
    this.dataStatus = plugin.getDataStatus();
    this.failMessage = plugin.getFailMessage();
    this.startedDate = plugin.getStartedDate();
    this.finishedDate = plugin.getFinishedDate();
    this.canDisplayRawXml = canDisplayRawXml;
    if (plugin instanceof AbstractExecutablePlugin) {
      this.updatedDate = ((AbstractExecutablePlugin<?>) plugin).getUpdatedDate();
      this.externalTaskId = ((AbstractExecutablePlugin<?>) plugin).getExternalTaskId();
      this.executionProgress = new PluginProgressDTO(((AbstractExecutablePlugin<?>) plugin).getExecutionProgress());
      this.topologyName = ((AbstractExecutablePlugin<?>) plugin).getTopologyName();
      this.pluginMetadata = ((AbstractExecutablePlugin<?>) plugin).getPluginMetadata();
    } else {
      this.updatedDate = null;
      this.externalTaskId = null;
      this.executionProgress = null;
      this.topologyName = null;
      this.pluginMetadata = null;
    }
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

  public PluginProgressDTO getExecutionProgress() {
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
