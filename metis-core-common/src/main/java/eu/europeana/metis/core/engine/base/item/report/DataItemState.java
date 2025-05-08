package eu.europeana.metis.core.engine.base.item.report;

/**
 * Represents the state of a data item during the processing workflow.
 * Provides various states to track the progress and outcome of the item.
 * <p>
 * This follows ECloud states, and the keys here should be updated in a future migration.
 */
public enum DataItemState {
  QUEUED,
  PROCESSED_BY_SPOUT,
  STATS_GENERATED,
  SUCCESS,
  ERROR
}
