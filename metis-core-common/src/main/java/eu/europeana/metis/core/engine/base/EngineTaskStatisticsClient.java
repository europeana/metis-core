package eu.europeana.metis.core.engine.base;

import eu.europeana.metis.core.rest.stats.NodePathStatisticsDTO;
import eu.europeana.metis.core.rest.stats.RecordStatisticsDTO;
import eu.europeana.metis.exception.ExternalTaskException;

/**
 * Interface for retrieving statistical data related to xml records from engine tasks.
 */
public interface EngineTaskStatisticsClient {

  /**
   * Retrieves statistical data for records in a specific task.
   *
   * @param topologyName The name of the topology to which the task belongs.
   * @param taskId The unique identifier of the task for which statistics are requested.
   * @return An instance of {@code RecordStatisticsDTO} containing statistical data about the task.
   * @throws ExternalTaskException If an error occurs while accessing the task statistics.
   */
  RecordStatisticsDTO getEngineTaskContentRecordStatistics(String topologyName, long taskId) throws ExternalTaskException;

  /**
   * Retrieves the statistics for records of a specific node path in a specific task.
   *
   * @param topologyName Name of the topology where the engine task is located.
   * @param taskId ID of the engine task for which statistics are being retrieved.
   * @param nodePath Path to the specific node in the task's content.
   * @return NodePathStatisticsDTO containing the statistics of the specified node path.
   * @throws ExternalTaskException If there is an error retrieving the statistics.
   */
  NodePathStatisticsDTO getEngineTaskContentNodePathStatistics(String topologyName, long taskId, String nodePath)
      throws ExternalTaskException;

}
