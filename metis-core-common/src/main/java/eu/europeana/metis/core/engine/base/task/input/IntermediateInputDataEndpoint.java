package eu.europeana.metis.core.engine.base.task.input;

import eu.europeana.metis.core.engine.base.DataRevision;

/**
 * Represents an internal input data endpoint used within the processing engine.
 *
 * @param url The URL of the input data endpoint.
 * @param inputRevision The data revision associated with the input data endpoint.
 */
public record IntermediateInputDataEndpoint(
    String url, String sourceExecutionId, DataRevision inputRevision) implements InputDataEndpoint {

}
