package eu.europeana.metis.core.engine.base;

import java.util.Date;

public record OaiHarvestParameters(
    String set,
    String metadataPrefix,
    Date from,
    Date until) {

}
