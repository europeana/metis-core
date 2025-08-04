package eu.europeana.metis.core.engine.base.task.report;

import java.util.List;

/**
 * Represents error information related to an engine task.
 *
 * @param errorType Type of the error.
 * @param message Descriptive message for the error.
 * @param occurrences Number of times this error occurred.
 * @param errorDetails List of detailed information about each error occurrence.
 */
public record EngineTaskErrorInfo(
    String errorType,
    String message,
    int occurrences,
    List<EngineTaskErrorDetails> errorDetails) {

}
