package eu.europeana.metis.core.engine.base.task.input;

/**
 * Represents an intermediate input data endpoint that applies an XSLT transformation to external data before it is processed
 * further.
 *
 * @param xslt The XSLT stylesheet used to transform the external data.
 * @param url The URL of the external input data.
 * @param sourceExecutionId The identifier of the source execution that generated the external input data.
 */
//todo: see if we can remove url from this level. It used to be for the dataLocation required from ecloud.
public record TransformExternalInputDataEndpoint(String xslt, String url, String sourceExecutionId, String sourceBatchId)
    implements IntermediateInputDataEndpoint {

}
