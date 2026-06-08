package eu.europeana.metis.core.engine.base.task.input;

import java.time.Instant;

/**
 * Represents an input data endpoint for OAI harvesting.
 *
 * @param url The URL of the OAI harvest endpoint.
 * @param set The name of the set to harvest.
 * @param metadataPrefix The metadata prefix specifying the format of the data.
 * @param from The start date of the harvesting period.
 * @param until The end date of the harvesting period.
 */
public record OaiHarvestInputDataEndpoint(
    String url,
    String set,
    String metadataPrefix,
    Instant from,
    Instant until,
    Integer stepSize) implements HarvestInputDataEndpoint {

}
