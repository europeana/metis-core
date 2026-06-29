package eu.europeana.metis.core.rest;


/**
 * Immutable view class of depublication information.
 */
public record DepublicationInfoView(
    ResponseListWrapper<DepublishRecordIdView> depublicationRecordIds,
    boolean depublicationTriggerable) {

}
