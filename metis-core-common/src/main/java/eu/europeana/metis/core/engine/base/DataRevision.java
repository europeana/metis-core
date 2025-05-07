package eu.europeana.metis.core.engine.base;

import java.util.Date;

/**
 * Represents a revision of a dataset or data representation within a processing engine.
 */
public record DataRevision(String name, String providerId, Date creationTimeStamp, boolean deleted) {

}
