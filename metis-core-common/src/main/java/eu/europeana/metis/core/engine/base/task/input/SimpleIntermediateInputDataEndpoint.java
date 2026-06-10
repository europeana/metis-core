package eu.europeana.metis.core.engine.base.task.input;

import eu.europeana.metis.core.engine.base.DataRevision;

/**
 * Represents a basic implementation of the {@code IntermediateInputDataEndpoint} interface. This endpoint is intended for
 * intermediate data processing where a source execution is associated with the input data and no other specific paramters are
 * required.
 *
 * @param url The URL of the intermediate input data endpoint.
 * @param sourceExecutionId The identifier of the source execution that produced the input data.
 * @param inputRevision The revision of the input data represented by this endpoint.
 */
public record SimpleIntermediateInputDataEndpoint(String url, String sourceExecutionId, DataRevision inputRevision) implements
    IntermediateInputDataEndpoint {

}
