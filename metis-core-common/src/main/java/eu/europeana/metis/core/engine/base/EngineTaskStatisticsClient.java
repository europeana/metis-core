package eu.europeana.metis.core.engine.base;

import eu.europeana.metis.core.rest.stats.NodePathStatisticsDTO;
import eu.europeana.metis.core.rest.stats.RecordStatisticsDTO;
import eu.europeana.metis.exception.ExternalTaskException;

public interface EngineTaskStatisticsClient {

  RecordStatisticsDTO getEngineTaskContentRecordStatistics(String topologyName, long taskId) throws ExternalTaskException;

  NodePathStatisticsDTO getEngineTaskContentNodePathStatistics(String topologyName, long taskId, String nodePath)
      throws ExternalTaskException;

}
