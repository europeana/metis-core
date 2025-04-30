package eu.europeana.metis.core.engine.base;

import java.util.Date;

public record DataRevision(String name, String providerId, Date creationTimeStamp, boolean deleted) {
}
