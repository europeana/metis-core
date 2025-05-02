package eu.europeana.metis.core.engine.base.report.task;

import java.util.List;

public record EngineTaskErrorInfo(
    String errorType,
    String message,
    int occurrences,
    List<EngineTaskErrorDetails> errorDetails) {

}
