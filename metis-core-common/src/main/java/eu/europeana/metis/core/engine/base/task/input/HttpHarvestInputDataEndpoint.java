package eu.europeana.metis.core.engine.base.task.input;

/**
 * Represents an input data endpoint for http harvesting.
 *
 * @param url The URL of the harvest input data endpoint.
 */
public record HttpHarvestInputDataEndpoint(
    String url,
    Integer stepSize
) implements HarvestInputDataEndpoint {

}
