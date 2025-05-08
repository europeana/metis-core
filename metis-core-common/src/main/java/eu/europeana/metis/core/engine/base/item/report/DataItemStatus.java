package eu.europeana.metis.core.engine.base.item.report;

/**
 * Immutable record representing the status of a data item during processing.
 * Contains details about the resource, its state, and processing information.
 *
 * @param resourceNum Resource number.
 * @param resource String representing the resource.
 * @param dataItemState DataItemState enum indicating the processing state of the data item.
 * @param info String containing additional information or comments for the data item.
 * @param europeanaId The europeana identifier.
 * @param processingTime Long value representing the time taken to process the item in milliseconds.
 * @param resultResource String describing the result or output resource linked to the data item.
 */
public record DataItemStatus(int resourceNum,
                             String resource,
                             DataItemState dataItemState,
                             String info,
                             String europeanaId,
                             long processingTime,
                             String resultResource) {

}
