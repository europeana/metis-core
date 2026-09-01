package eu.europeana.metis.core.execution.task;

import eu.europeana.metis.core.engine.base.EngineTask;
import eu.europeana.metis.core.engine.base.EngineTaskSettings;

/**
 * Abstract factory class for creating engine tasks. This class provides the shared logic and structure for building tasks to be
 * executed by a processing engine.
 *
 * @param <S> The type of {@link EngineTaskSettings} used by the task.
 * @param <T> The type of {@link EngineTask} created by the factory.
 */
public abstract class AbstractEngineTaskFactory<S extends EngineTaskSettings, T extends EngineTask> implements
    EngineTaskFactory<T> {

  protected final S engineTaskSettings;

  protected AbstractEngineTaskFactory(S engineTaskSettings) {
    this.engineTaskSettings = engineTaskSettings;
  }
}

