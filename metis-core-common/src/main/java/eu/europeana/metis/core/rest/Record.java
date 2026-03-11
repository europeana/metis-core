package eu.europeana.metis.core.rest;

/**
 * Model class that encapsulates the ecloud identifier and the xml contents of a particular state of that record, which can be
 * different on each use.
 */
public record Record(String ecloudId, String xmlRecord) {

}
