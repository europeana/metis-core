package eu.europeana.metis.core.engine.base;

import eu.europeana.metis.core.engine.base.task.input.InputDataEndpoint;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a task that can be submitted to a processing engine.
 */
public class EngineTaskRequest {
  protected final Map<EngineTaskKey, String> parameters;
  protected final InputDataEndpoint inputDataEndpoint;

  /**
   * Constructs an EngineTask with specified parameters and input data endpoint.
   *
   * @param parameters Map of EngineTaskKey and String values used to configure the task. Must not be null.
   * @param inputDataEndpoint InputDataEndpoint representing the input data source for the task. Must not be null.
   */
  protected EngineTaskRequest(
      Map<EngineTaskKey, String> parameters,
      InputDataEndpoint inputDataEndpoint) {
    this.parameters = Objects.requireNonNull(parameters);
    this.inputDataEndpoint = Objects.requireNonNull(inputDataEndpoint);
  }
}
