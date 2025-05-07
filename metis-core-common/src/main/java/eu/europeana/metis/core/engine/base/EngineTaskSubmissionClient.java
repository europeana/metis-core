package eu.europeana.metis.core.engine.base;

import eu.europeana.metis.exception.ExternalTaskException;

/**
 * Interface for submitting and managing tasks to be executed within a processing engine.
 *
 * @param <T> The type of task extending {@link AbstractEngineTask} to be submitted and managed.
 */
public interface EngineTaskSubmissionClient<T extends AbstractEngineTask> {

  long submitEngineTask(T engineTask, String topologyName) throws ExternalTaskException;

  void cancelEngineTask(String topologyName, long taskId, String message) throws ExternalTaskException;
}
