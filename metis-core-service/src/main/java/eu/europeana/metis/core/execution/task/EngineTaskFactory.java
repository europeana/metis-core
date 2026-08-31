package eu.europeana.metis.core.execution.task;

import eu.europeana.metis.core.engine.base.EngineTask;

/**
 * Interface for a factory that creates instances of {@link EngineTask}, which represent tasks to be processed
 * by an engine. Implementations of this factory are responsible for constructing tasks based on the provided
 * identifiers and any additional configurations required.
 *
 * @param <T> The type of {@link EngineTask} created by the factory.
 */
public interface EngineTaskFactory<T extends EngineTask> {

  /**
   * Creates an instance of the engine task.
   *
   * @param datasetId The identifier of the dataset for which the task is created.
   * @param engineDatasetId The identifier of the engine's representation of the dataset.
   * @param previousTaskId The identifier of the previous task in the workflow, if applicable.
   * @return An instance of the task represented by the generic type parameter {@code T}.
   */
  T create(String datasetId, String engineDatasetId, String previousTaskId);
}
