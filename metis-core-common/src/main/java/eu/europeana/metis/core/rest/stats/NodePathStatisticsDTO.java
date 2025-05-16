package eu.europeana.metis.core.rest.stats;

import java.util.List;

/**
 * Represents statistics for a specific node path.
 *
 * @param xPath The XPath expression representing the node.
 * @param nodeValueStatistics A list of value statistics for the node, stored as an immutable list.
 */
public record NodePathStatisticsDTO(String xPath, List<NodeValueStatisticsDTO> nodeValueStatistics) {

  /**
   * Constructs a {@code NodePathStatistics} instance.
   *
   * <p>The provided list of node value statistics is copied to ensure immutability.</p>
   *
   * @param xPath The XPath expression representing the node.
   * @param nodeValueStatistics The list of node value statistics. Must not be {@code null}.
   * @throws NullPointerException if {@code nodeValueStatistics} is {@code null}.
   */
  public NodePathStatisticsDTO {
    nodeValueStatistics = List.copyOf(nodeValueStatistics); // Ensures immutability
  }
}
