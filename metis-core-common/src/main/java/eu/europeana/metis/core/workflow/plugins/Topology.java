package eu.europeana.metis.core.workflow.plugins;

import lombok.Getter;

/**
 * Contains all topology names.
 */
@Getter
public enum Topology {

  HTTP_HARVEST("http_harvest"),
  OAIPMH_HARVEST("oai_harvest"),
  VALIDATION("validation"),
  TRANSFORMATION("xslt_transform"),
  NORMALIZATION("normalization"),
  ENRICHMENT("enrichment"),
  MEDIA_PROCESS("media_process"),
  LINK_CHECKING("link_checker"),
  INDEX("indexer"),
  DEPUBLISH("depublication");

  private final String topologyName;

  Topology(String topologyName) {
    this.topologyName = topologyName;
  }

}
