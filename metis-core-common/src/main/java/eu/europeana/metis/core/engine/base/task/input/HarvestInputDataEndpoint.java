package eu.europeana.metis.core.engine.base.task.input;

/**
 * Represents a harvest input data endpoint used within the processing engine.
 */
public sealed interface HarvestInputDataEndpoint extends InputDataEndpoint
    permits OaiHarvestInputDataEndpoint, HttpHarvestInputDataEndpoint {

  /**
   * Retrieves the step size of the harvest operation.
   *
   * @return The step size as an Integer, or null if not specified.
   */
  Integer stepSize();
}
