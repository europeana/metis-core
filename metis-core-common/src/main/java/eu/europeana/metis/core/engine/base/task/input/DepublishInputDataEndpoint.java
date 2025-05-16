package eu.europeana.metis.core.engine.base.task.input;

/**
 * Represents an depublish input data endpoint used within the processing engine.
 *
 * @param url The URL of the input data endpoint.
 */
public record DepublishInputDataEndpoint(String url) implements InputDataEndpoint {

}
