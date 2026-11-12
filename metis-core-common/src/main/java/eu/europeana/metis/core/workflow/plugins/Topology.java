package eu.europeana.metis.core.workflow.plugins;

import lombok.Getter;

/**
 * Contains all topology names.
 */
@Getter
public enum Topology {

  HTTP_HARVEST("http_topology"),
  OAIPMH_HARVEST("oai_topology"),
  VALIDATION("validation_topology"),
  TRANSFORMATION("xslt_topology"),
  NORMALIZATION("normalization_topology"),
  ENRICHMENT("enrichment_topology"),
  MEDIA_PROCESS("media_topology"),
  LINK_CHECKING("linkcheck_topology"),
  INDEX("indexing_topology"),
  DEPUBLISH("depublication_topology");

  private final String topologyName;

  Topology(String topologyName) {
    this.topologyName = topologyName;
  }

}
