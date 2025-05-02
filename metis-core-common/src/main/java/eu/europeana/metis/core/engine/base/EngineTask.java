package eu.europeana.metis.core.engine.base;

import eu.europeana.metis.core.engine.base.task.input.InputDataEndpoint;
import java.util.Map;

/**
 * Represents a task that can be submitted to an external system.
 */
public interface EngineTask {

  void setParameters(Map<EngineTaskKey, String> parameters);
  <T extends InputDataEndpoint> void setInputDataLocation(T inputDataEndpoint);
  void setOutputRevision(DataRevision dataRevision);
}
