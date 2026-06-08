package eu.europeana.metis.core.workflow.plugins;

import com.fasterxml.jackson.annotation.JsonFormat;
import eu.europeana.metis.utils.CommonStringValues;
import java.time.Instant;

/**
 * OAIPMH Harvest Plugin Metadata.
 */
public class OaipmhHarvestPluginMetadata extends AbstractHarvestPluginMetadata {

  private static final ExecutablePluginType PLUGIN_TYPE = ExecutablePluginType.OAIPMH_HARVEST;
  private String url;
  private String metadataFormat;
  private String setSpec;
  private boolean incrementalHarvest; // Default: false (i.e. full harvest)
  @JsonFormat(pattern = CommonStringValues.DATE_FORMAT)
  private Instant fromDate;
  @JsonFormat(pattern = CommonStringValues.DATE_FORMAT)
  private Instant untilDate;
  //If useDefaultIdentifiers == true then this is the prefix to be trimmed from the OAI Header Identifier
  private String identifierPrefixRemoval;

  public OaipmhHarvestPluginMetadata() {
    //Required for json serialization
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getMetadataFormat() {
    return metadataFormat;
  }

  public void setMetadataFormat(String metadataFormat) {
    this.metadataFormat = metadataFormat;
  }

  public String getSetSpec() {
    return setSpec;
  }

  public void setSetSpec(String setSpec) {
    this.setSpec = setSpec;
  }

  public void setIncrementalHarvest(boolean incrementalHarvest) {
    this.incrementalHarvest = incrementalHarvest;
  }

  @Override
  public boolean isIncrementalHarvest() {
    return incrementalHarvest;
  }

  public Instant getFromDate() {
    return fromDate;
  }

  public void setFromDate(Instant fromDate) {
    this.fromDate = fromDate;
  }

  public String getIdentifierPrefixRemoval() {
    return identifierPrefixRemoval;
  }

  public void setIdentifierPrefixRemoval(String identifierPrefixRemoval) {
    this.identifierPrefixRemoval = identifierPrefixRemoval;
  }

  public Instant getUntilDate() {
    return untilDate;
  }

  public void setUntilDate(Instant untilDate) {
    this.untilDate = untilDate;
  }

  @Override
  public ExecutablePluginType getExecutablePluginType() {
    return PLUGIN_TYPE;
  }

}
