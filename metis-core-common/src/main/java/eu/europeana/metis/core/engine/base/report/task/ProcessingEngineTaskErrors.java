package eu.europeana.metis.core.engine.base.report.task;

import java.util.List;

public record ProcessingEngineTaskErrors(long id, List<ProcessingEngineTaskErrorInfo> errors) {
}
