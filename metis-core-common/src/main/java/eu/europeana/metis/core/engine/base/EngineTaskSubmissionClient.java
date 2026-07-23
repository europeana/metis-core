package eu.europeana.metis.core.engine.base;

import eu.europeana.metis.core.workflow.plugins.ExecutablePluginType;
import eu.europeana.metis.exception.ExternalTaskException;

/**
 * Interface for submitting and managing tasks to be executed within a processing engine.
 *
 * @param <T> The type of {@link EngineTask} to be submitted and managed.
 */
public interface EngineTaskSubmissionClient<T extends EngineTask> {

  /**
   * Submits a task to the processing engine for execution.
   *
   * @param engineTask The task to be executed.
   * @param topologyName The name of the topology where the task will be submitted.
   * @throws ExternalTaskException If the submission fails due to an error with the external resource.
   */
  void submitEngineTask(T engineTask, String topologyName) throws ExternalTaskException;

  /**
   * Cancels the specified engine task with the provided details.
   *
   * @param topologyName name of the topology associated with the task.
   * @param taskId unique identifier of the task to be canceled.
   * @param message reason or message indicating why the task is being canceled.
   * @param pluginType the plugin type associated with the task.
   * @throws ExternalTaskException if an error occurs during task cancellation.
   */
  void cancelEngineTask(String topologyName, String taskId, String message, ExecutablePluginType pluginType) throws ExternalTaskException;
}
