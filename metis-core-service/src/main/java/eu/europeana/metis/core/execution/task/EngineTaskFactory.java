package eu.europeana.metis.core.execution.task;

import eu.europeana.metis.core.engine.base.EngineTask;
import eu.europeana.metis.core.execution.EngineTaskCreationContext;
import eu.europeana.metis.exception.ExternalTaskException;

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
   * @param engineTaskCreationContext The context containing the identifiers and configurations for creating the task.
   * @return An instance of the task represented by the generic type parameter {@code T}.
   * @throws ExternalTaskException If there is an error, creating the task.
   */
  T create(EngineTaskCreationContext engineTaskCreationContext) throws ExternalTaskException;
}
