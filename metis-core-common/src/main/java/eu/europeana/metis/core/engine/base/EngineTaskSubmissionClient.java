package eu.europeana.metis.core.engine.base;

import eu.europeana.metis.exception.ExternalTaskException;

public interface EngineTaskSubmissionClient<T extends EngineTask> {

  long submitEngineTask(T engineTask, String topologyName) throws ExternalTaskException;

  void cancelEngineTask(String topologyName, long taskId, String message) throws ExternalTaskException;
}
