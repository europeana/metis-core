package eu.europeana.metis.core.engine.base;

/**
 * A processing engine task with a task identifier and an optional output batch identifier.
 */
public interface EngineTask {

  String getEngineTaskId();
  /** Returns the output batch identifier, or null when the task produces no record output. */
  String getEngineBatchId();

}
