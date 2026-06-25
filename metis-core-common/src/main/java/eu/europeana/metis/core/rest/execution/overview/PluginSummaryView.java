package eu.europeana.metis.core.rest.execution.overview;

import eu.europeana.metis.core.workflow.plugins.AbstractExecutablePlugin;
import eu.europeana.metis.core.workflow.plugins.AbstractMetisPlugin;
import eu.europeana.metis.core.workflow.plugins.PluginStatus;
import eu.europeana.metis.core.workflow.plugins.PluginType;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/**
 * This class represents the vital information on a plugin execution needed for the execution
 * overview.
 */
@Getter
@Setter
public class PluginSummaryView {

  private PluginType pluginType;
  private PluginStatus pluginStatus;
  private String failMessage;
  private Instant startedDate;
  private Instant updatedDate;
  private Instant finishedDate;
  private PluginProgressView progress;

  PluginSummaryView() {
  }

  PluginSummaryView(AbstractMetisPlugin plugin) {
    this.pluginType = plugin.getPluginType();
    this.pluginStatus = plugin.getPluginStatus();
    this.failMessage = plugin.getFailMessage();
    this.startedDate = plugin.getStartedDate();
    this.finishedDate = plugin.getFinishedDate();
    if (plugin instanceof AbstractExecutablePlugin abstractExecutablePlugin) {
      this.updatedDate = abstractExecutablePlugin.getUpdatedDate();
      this.progress = new PluginProgressView(abstractExecutablePlugin.getExecutionProgress());
    }
  }
}
