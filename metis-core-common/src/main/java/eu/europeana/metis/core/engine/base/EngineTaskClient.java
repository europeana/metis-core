package eu.europeana.metis.core.engine.base;

import eu.europeana.metis.core.engine.base.task.input.InputDataEndpoint;
import eu.europeana.metis.exception.ExternalTaskException;
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
public interface EngineTaskClient<S extends EngineTaskSettings, T extends EngineTask> extends
    EngineTaskSubmissionClient<T>,
    EngineTaskMonitoringClient,
    EngineTaskStatisticsClient,
    EngineRecordClient,
    AutoCloseable {

  /**
   * Retrieves the current engine task settings.
   *
   * @return an instance of the engine task settings.
   */
  S getEngineTaskSettings();

  /**
   * Creates a new engine task with the specified parameters and input data endpoint.
   *
   * @param parameters a map of {@link EngineTaskKey} keys and their corresponding values used to configure the engine task
   * @param inputDataEndpoint the input data source for the engine task
   * @param topologyName the name of the topology to be used for the engine task
   * @return a new instance of the engine task
   * @throws ExternalTaskException if an error occurs while creating the engine task
   */
  T createEngineTask(Map<EngineTaskKey, String> parameters, InputDataEndpoint inputDataEndpoint, String topologyName)
      throws ExternalTaskException;

}
