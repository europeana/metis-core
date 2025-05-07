package eu.europeana.metis.core.engine.base.task.input;

import eu.europeana.metis.core.engine.base.DataRevision;

public record InternalInputDataEndpoint(
    String url, DataRevision inputRevision) implements InputDataEndpoint {

}
