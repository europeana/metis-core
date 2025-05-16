package eu.europeana.metis.core.engine.base.task.input;

/**
 * Represents an input data endpoint used in the processing engine.
 */
//False positive: Check https://community.sonarsource.com/t/s7027-prevents-from-using-sealed-classes/133423/5
@SuppressWarnings("javaarchitecture:S7027")
public sealed interface InputDataEndpoint
    permits HttpHarvestInputDataEndpoint, OaiHarvestInputDataEndpoint, InternalInputDataEndpoint {

  /**
   * Retrieves the URL associated with the input data endpoint.
   *
   * @return The URL of the input data endpoint.
   */
  String url();
}
