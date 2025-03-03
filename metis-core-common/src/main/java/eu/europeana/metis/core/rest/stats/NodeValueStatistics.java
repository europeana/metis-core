package eu.europeana.metis.core.rest.stats;

import java.util.List;

/**
 * Represents statistics for a specific node value.
 *
 * @param value The value of the node.
 * @param occurrences The number of times this value appears.
 * @param attributeStatistics A list of attribute statistics, stored as an immutable list.
 */
public record NodeValueStatistics(String value, long occurrences, List<AttributeStatistics> attributeStatistics) {

  /**
   * Constructs a {@code NodeValueStatistics} instance.
   *
   * <p>The provided list of attribute statistics is copied to ensure immutability.</p>
   *
   * @param value The value of the node.
   * @param occurrences The number of times this value appears.
   * @param attributeStatistics The list of attribute statistics. Must not be {@code null}.
   * @throws NullPointerException if {@code attributeStatistics} is {@code null}.
   */
  public NodeValueStatistics {
    attributeStatistics = List.copyOf(attributeStatistics); // Ensures immutability
  }
}
