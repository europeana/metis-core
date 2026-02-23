package eu.europeana.metis.core.engine.base;

import eu.europeana.metis.exception.ExternalTaskException;
import eu.europeana.metis.sandbox.batch.common.FullBatchJobType;

/**
 * Interface for submitting and managing tasks to be executed within a processing engine.
 *
 * @param <T> The type of task extending {@link EngineTask} to be submitted and managed.
 */
public interface EngineTaskSubmissionClient<T extends EngineTask> {

  /**
   * Submits a task to the processing engine for execution.
   *
   * @param engineTask The task to be executed.
   * @param topologyName The name of the topology where the task will be submitted.
   * @return A unique identifier for the submitted task.
   * @throws ExternalTaskException If the submission fails due to an error with the external resource.
   */
  String submitEngineTask(T engineTask, String topologyName) throws ExternalTaskException;

  /**
   * Cancels the specified engine task with the provided details.
   *
   * @param topologyName Name of the topology associated with the task.
   * @param taskId Unique identifier of the task to be canceled.
   * @param message Reason or message indicating why the task is being canceled.
   * @param step
   * @throws ExternalTaskException If an error occurs during task cancellation.
   */
  void cancelEngineTask(String topologyName, String taskId, String message, FullBatchJobType step) throws ExternalTaskException;
}
