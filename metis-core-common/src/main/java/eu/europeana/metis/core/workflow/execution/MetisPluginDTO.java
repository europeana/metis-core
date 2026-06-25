package eu.europeana.metis.core.workflow.execution;

import eu.europeana.metis.core.workflow.plugins.DataStatus;
import eu.europeana.metis.core.workflow.plugins.MetisPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.PluginStatus;
import eu.europeana.metis.core.workflow.plugins.PluginType;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/**
 * This class represents the complete information on a plugin execution needed for the execution history.
 */
@Getter
@Setter
public class MetisPluginDTO {

  private PluginType pluginType;
  private String id;
  private PluginStatus pluginStatus;
  private DataStatus dataStatus;
  private String failMessage;
  private Instant startedDate;
  private Instant updatedDate;
  private Instant finishedDate;
  private String externalTaskId;
  private ExecutionProgressDTO executionProgress;
  private String topologyName;
  private boolean canDisplayRawXml;
  private MetisPluginMetadata pluginMetadata;

  public MetisPluginDTO() {
    //Required for json serialization
  }
}
