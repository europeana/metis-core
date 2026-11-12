package eu.europeana.metis.core.engine.base;

public interface EngineTask {

  String getEngineTaskId();
  /** Returns the output batch identifier, or null when the task produces no record output. */
  String getEngineBatchId();

}
