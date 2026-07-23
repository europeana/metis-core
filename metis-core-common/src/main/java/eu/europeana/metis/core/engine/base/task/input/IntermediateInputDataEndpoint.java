package eu.europeana.metis.core.engine.base.task.input;

/**
 * Represents an internal input data endpoint used within the processing engine.
 */
public sealed interface IntermediateInputDataEndpoint extends InputDataEndpoint
    permits SimpleIntermediateInputDataEndpoint, TransformExternalInputDataEndpoint, TransformInternalInputDataEndpoint {

  /**
   * Retrieves the identifier of the source execution that produced the input data.
   *
   * @return The source execution identifier.
   */
  String sourceExecutionId();

  /**
   * Retrieves the identifier of the source batch that produced the input data.
   *
   * @return The source batch identifier.
   */
  String sourceBatchId();
}
