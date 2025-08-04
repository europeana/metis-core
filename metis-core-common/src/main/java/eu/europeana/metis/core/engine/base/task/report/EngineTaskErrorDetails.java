package eu.europeana.metis.core.engine.base.task.report;

/**
 * Represents detailed information about an error encountered during an engine task execution.
 *
 * @param identifier Unique identifier of the error.
 * @param additionalInfo Additional information or context about the error.
 */
public record EngineTaskErrorDetails(
    String identifier,
    String additionalInfo) {

}
