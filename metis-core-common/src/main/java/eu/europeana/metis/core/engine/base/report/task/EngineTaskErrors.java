package eu.europeana.metis.core.engine.base.report.task;

import java.util.List;

public record EngineTaskErrors(
    long id,
    List<EngineTaskErrorInfo> errors) {

}
