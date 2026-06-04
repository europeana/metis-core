package eu.europeana.metis.core.engine.base.task.input;

import eu.europeana.metis.core.engine.base.DataRevision;

public record TransformExternalInputDataEndpoint(String xslt, String url, String sourceExecutionId,
                                                 DataRevision inputRevision)
    implements IntermediateInputDataEndpoint {

}
