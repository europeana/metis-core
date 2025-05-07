package eu.europeana.metis.core.engine.base;

import eu.europeana.metis.core.rest.stats.NodePathStatistics;
import eu.europeana.metis.core.rest.stats.RecordStatistics;
import eu.europeana.metis.exception.ExternalTaskException;

public interface EngineTaskStatisticsClient {

  RecordStatistics getEngineTaskContentRecordStatistics(String topologyName, long taskId) throws ExternalTaskException;

  NodePathStatistics getEngineTaskContentNodePathStatistics(String topologyName, long taskId, String nodePath)
      throws ExternalTaskException;

}
