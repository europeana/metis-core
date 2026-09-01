package eu.europeana.metis.core.engine.base.task.input;

import java.util.Set;

/**
 * Represents a depublish input data endpoint used within the processing engine.
 */
public record DepublishInputDataEndpoint(boolean datasetDepublish, Set<String> idsToDepublish) implements InputDataEndpoint {

}
