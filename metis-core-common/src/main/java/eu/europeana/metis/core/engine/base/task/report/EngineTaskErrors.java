package eu.europeana.metis.core.engine.base.task.report;

import java.util.List;

public record EngineTaskErrors(
    long id,
    List<EngineTaskErrorInfo> errors) {

}
