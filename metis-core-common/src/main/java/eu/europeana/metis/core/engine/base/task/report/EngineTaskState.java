package eu.europeana.metis.core.engine.base.task.report;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Represents the various states of an engine task during its lifecycle.
 * <p>
 * Each state provides a description of its purpose or action being performed.
 * <p>
 * This follows ECloud states, and the keys here should be updated in a future migration.
 */
@Getter
@AllArgsConstructor
public enum EngineTaskState {
  RECEIVED("Task request was received, and task is being validated"),
  INVALID("Task failed parameters validation"),
  CREATED("Task created, but not started yet"),
  PROCESSING_BY_REST_APPLICATION("Task is being processed by the REST application"),
  QUEUED("All task's records pushed to Kafka queue"),
  READY_FOR_POST_PROCESSING("Ready for post-processing after topology stage is finished"),
  IN_POST_PROCESSING("Task in post-processing"),
  DROPPED("Task was dropped"),
  PROCESSED("Completely processed"),
  /**
   * @deprecated
   */
  @Deprecated
  CURRENTLY_PROCESSING("Currently processed by the topology"),
  /**
   * @deprecated
   */
  @Deprecated
  REMOVING_FROM_SOLR_AND_MONGO("Records are being removed from Solr and Mongo"),
  /**
   * @deprecated
   */
  @Deprecated
  SENT("Sent");

  private final String defaultMessage;
}
