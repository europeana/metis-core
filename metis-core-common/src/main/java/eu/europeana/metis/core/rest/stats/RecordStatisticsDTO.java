package eu.europeana.metis.core.rest.stats;

import java.util.List;

/**
 * Represents statistical data associated with a task.
 *
 * @param taskId The ID of the task.
 * @param nodePathStatistics A list of node path statistics, stored as an immutable list.
 */
public record RecordStatisticsDTO(long taskId, List<NodePathStatisticsDTO> nodePathStatistics) {

  /**
   * Constructs a {@code RecordStatistics} instance.
   *
   * <p>The provided list of node path statistics is copied to ensure immutability.</p>
   *
   * @param taskId The ID of the task.
   * @param nodePathStatistics The list of node path statistics. Must not be {@code null}.
   * @throws NullPointerException if {@code nodePathStatistics} is {@code null}.
   */
  public RecordStatisticsDTO {
    nodePathStatistics = List.copyOf(nodePathStatistics); // Ensures immutability
  }
}
