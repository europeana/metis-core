package eu.europeana.metis.core.engine.base;

import eu.europeana.metis.core.engine.base.task.input.InputDataEndpoint;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a task that can be submitted to a processing engine.
 */
public abstract class AbstractEngineTask {
  protected final Map<EngineTaskKey, String> parameters;
  protected final InputDataEndpoint inputDataEndpoint;
  protected final DataRevision outputDataRevision;

  protected AbstractEngineTask(
      Map<EngineTaskKey, String> parameters,
      InputDataEndpoint inputDataEndpoint,
      DataRevision outputDataRevision) {
    this.parameters = Objects.requireNonNull(parameters);
    this.inputDataEndpoint = Objects.requireNonNull(inputDataEndpoint);
    this.outputDataRevision = Objects.requireNonNull(outputDataRevision);
  }
}
