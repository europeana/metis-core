package eu.europeana.metis.core.engine.base;

/**
 * Enum representing the target database for indexing operations.
 * Used to distinguish between preview and publish environments.
 */
public enum IndexDatabase {
  PREVIEW,
  PUBLISH;
}
