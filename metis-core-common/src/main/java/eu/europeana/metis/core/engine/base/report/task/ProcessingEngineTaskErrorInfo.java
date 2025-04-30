package eu.europeana.metis.core.engine.base.report.task;

import java.util.List;

public record ProcessingEngineTaskErrorInfo(String errorType, String message, int occurrences,
                                            List<ProcessingEngineTaskErrorDetails> errorDetails) {
}
