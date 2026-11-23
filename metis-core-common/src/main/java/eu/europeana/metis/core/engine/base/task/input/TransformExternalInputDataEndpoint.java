package eu.europeana.metis.core.engine.base.task.input;

/**
 * Represents an intermediate input data endpoint that applies an XSLT transformation to external data before it is processed
 * further.
 *
 * @param xslt The XSLT stylesheet used to transform the external data.
 * @param sourceExecutionId The identifier of the source execution that generated the external input data.
 * @param sourceBatchId The identifier of the source batch that generated the external input data.
 */
public record TransformExternalInputDataEndpoint(String xslt, String sourceExecutionId, String sourceBatchId)
    implements IntermediateInputDataEndpoint {

}
