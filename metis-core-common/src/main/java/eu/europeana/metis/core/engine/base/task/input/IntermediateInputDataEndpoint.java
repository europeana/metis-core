package eu.europeana.metis.core.engine.base.task.input;

/**
 * Represents an internal input data endpoint used within the processing engine.
 */
public sealed interface IntermediateInputDataEndpoint extends InputDataEndpoint
    permits SimpleIntermediateInputDataEndpoint, TransformExternalInputDataEndpoint {

}
