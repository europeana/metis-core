package eu.europeana.metis.core.engine.base.task.input;

import java.util.Date;

public record OaiHarvestInputDataEndpoint(
    String url,
    String set,
    String metadataPrefix,
    Date from,
    Date until) implements InputDataEndpoint {

}
