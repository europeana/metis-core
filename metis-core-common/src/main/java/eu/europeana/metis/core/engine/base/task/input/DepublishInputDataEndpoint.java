package eu.europeana.metis.core.engine.base.task.input;

import java.util.Set;

/**
 * Represents an depublish input data endpoint used within the processing engine.
 *
 * @param url The URL of the input data endpoint.
 */
//todo: see if we can remove url from this level. It used to be for the dataLocation required from ecloud.
public record DepublishInputDataEndpoint(String url, boolean datasetDepublish, Set<String> idsToDepublish) implements InputDataEndpoint {

}
