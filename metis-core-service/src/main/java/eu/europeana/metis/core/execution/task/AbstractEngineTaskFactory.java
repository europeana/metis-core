package eu.europeana.metis.core.execution.task;

import static java.lang.String.format;

import eu.europeana.metis.core.engine.base.EngineTask;
import eu.europeana.metis.core.engine.base.EngineTaskSettings;
import eu.europeana.metis.utils.CommonStringValues;

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

  protected String getDataLocation(String engineDatasetId) {
    return format(
        CommonStringValues.S_DATA_PROVIDERS_S_DATA_SETS_S_TEMPLATE,
        engineTaskSettings.getBaseUrl(),
        engineTaskSettings.getProvider(),
        engineDatasetId);
  }
}

