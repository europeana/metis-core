package eu.europeana.metis.core.engine.base;

import java.util.function.Supplier;

/**
 * Client for submitting, monitoring and cancelling external tasks.
 */
public interface EngineTaskClient<S extends EngineTaskSettings, T extends EngineTask> extends
    EngineTaskSubmissionClient<T>,
    EngineTaskMonitoringClient,
    EngineTaskStatisticsClient,
    EngineRecordClient,
    AutoCloseable {

  S getEngineTaskSettings();

  Supplier<T> getEngineTaskCreator();

}
