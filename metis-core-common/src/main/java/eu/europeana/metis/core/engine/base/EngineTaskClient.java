package eu.europeana.metis.core.engine.base;

import eu.europeana.metis.core.engine.base.task.input.InputDataEndpoint;
import java.util.Map;

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

  T createEngineTask(Map<EngineTaskKey, String> parameters, InputDataEndpoint inputDataEndpoint, DataRevision outputDataRevision);

}
