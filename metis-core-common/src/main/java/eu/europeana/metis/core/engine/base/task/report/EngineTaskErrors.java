package eu.europeana.metis.core.engine.base.task.report;

import java.util.List;

/**
 * Represents a collection of errors encountered during the execution of an engine task.
 *
 * @param id Unique identifier of the engine task.
 * @param errors List of error information associated with the task.
 */
public record EngineTaskErrors(
    String id,
    List<EngineTaskErrorInfo> errors) {

}
