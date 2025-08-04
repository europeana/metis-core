package eu.europeana.metis.core.engine.base.task.report;

/**
 * Represents the various states of an engine task during its lifecycle.
 * <p>
 * Each state provides a description of its purpose or action being performed.
 * <p>
 * This follows ECloud states, and the keys here should be updated in a future migration.
 */
public enum EngineTaskState {
  PENDING("Task is being prepared by the REST application"),
  PROCESSING_BY_REST_APPLICATION("Task is being processed by the REST application"),
  QUEUED("All task's records pushed to Kafka queue"),
  SENT("Sent"),
  CURRENTLY_PROCESSING("Currently processed by the topology"),
  DROPPED("Task was dropped"),
  PROCESSED("Completely processed"),
  REMOVING_FROM_SOLR_AND_MONGO("Records are being removed from Solr and Mongo"),
  /**
   * @deprecated
   */
  @Deprecated
  DEPUBLISHING("Depublishing"),
  READY_FOR_POST_PROCESSING("Ready for post-processing after topology stage is finished"),
  IN_POST_PROCESSING("Task in post-processing");

  private final String defaultMessage;

  EngineTaskState(String s) {
    this.defaultMessage = s;
  }

  public String getDefaultMessage() {
    return this.defaultMessage;
  }
}
