package eu.europeana.metis.core.workflow.plugins;

import com.fasterxml.jackson.annotation.JsonFormat;
import eu.europeana.metis.utils.CommonStringValues;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * OAIPMH Harvest Plugin Metadata.
 */
@Getter
@Setter
@NoArgsConstructor
public class OaipmhHarvestPluginMetadata extends AbstractHarvestPluginMetadata {

  private static final ExecutablePluginType PLUGIN_TYPE = ExecutablePluginType.OAIPMH_HARVEST;
  private String url;
  private String metadataFormat;
  private String setSpec;
  private boolean incrementalHarvest; // Default: false (i.e., full harvest)
  @JsonFormat(pattern = CommonStringValues.DATE_FORMAT)
  private Instant fromDate;
  @JsonFormat(pattern = CommonStringValues.DATE_FORMAT)
  private Instant untilDate;
  //If useDefaultIdentifiers == true then this is the prefix to be trimmed from the OAI Header Identifier
  private String identifierPrefixRemoval;

  @Override
  public ExecutablePluginType getExecutablePluginType() {
    return PLUGIN_TYPE;
  }
}
