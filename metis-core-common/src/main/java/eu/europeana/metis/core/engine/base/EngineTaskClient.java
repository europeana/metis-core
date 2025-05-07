package eu.europeana.metis.core.engine.base;

import eu.europeana.metis.core.engine.base.task.input.InputDataEndpoint;
import java.util.Map;

/**
 * Interface defining the client API for managing and interacting with engine tasks.
 * <p>
 * This client provides mechanisms for task submission, monitoring, statistics reporting, and record operations associated with
 * tasks executed in a processing engine.
 * <p>
 * The generic parameters allow the client to be tailored to specific implementations of engine task settings and engine tasks.
 *
 * @param <S> The type representing the settings required for the engine tasks.
 * @param <T> The type representing the tasks to be managed by the engine.
 */
public interface EngineTaskClient<S extends AbstractEngineTaskSettings, T extends AbstractEngineTask> extends
    EngineTaskSubmissionClient<T>,
    EngineTaskMonitoringClient,
    EngineTaskStatisticsClient,
    EngineRecordClient,
    AutoCloseable {

  S getEngineTaskSettings();

  T createEngineTask(Map<EngineTaskKey, String> parameters, InputDataEndpoint inputDataEndpoint, DataRevision outputDataRevision);

}
