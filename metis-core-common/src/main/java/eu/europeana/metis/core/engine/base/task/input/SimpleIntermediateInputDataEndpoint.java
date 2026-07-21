package eu.europeana.metis.core.engine.base.task.input;

/**
 * Represents a basic implementation of the {@code IntermediateInputDataEndpoint} interface. This endpoint is intended for
 * intermediate data processing where a source execution is associated with the input data and no other specific paramters are
 * required.
 *
 * @param url The URL of the intermediate input data endpoint.
 * @param sourceExecutionId The identifier of the source execution that produced the input data.
 * @param sourceBatchId The identifier of the source batch that produced the input data.
 */
//todo: see if we can remove url from this level. It used to be for the dataLocation required from ecloud.
public record SimpleIntermediateInputDataEndpoint(String url, String sourceExecutionId, String sourceBatchId) implements
    IntermediateInputDataEndpoint {

}
