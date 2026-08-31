package eu.europeana.metis.core.engine.base;

import java.time.Instant;

/**
 * Represents a revision of a dataset or data representation within a processing engine.
 */
public record DataRevision(String name, String providerId, Instant creationTimeStamp, boolean deleted) {

}
