package eu.europeana.metis.core.rest;


/**
 * Immutable view class of depublication information.
 */
public class DepublicationInfoView {

  private final ResponseListWrapper<DepublishRecordIdView> depublicationRecordIds;
  private final boolean depublicationTriggerable;

  /**
   * Constructor.
   *
   * @param depublicationRecordIds the depublication record ids
   * @param depublicationTriggerable the depublication triggerable flag
   */
  public DepublicationInfoView(
          ResponseListWrapper<DepublishRecordIdView> depublicationRecordIds,
          boolean depublicationTriggerable) {
    this.depublicationRecordIds = depublicationRecordIds;
    this.depublicationTriggerable = depublicationTriggerable;
  }

  public ResponseListWrapper<DepublishRecordIdView> getDepublicationRecordIds() {
    return depublicationRecordIds;
  }

  public boolean isDepublicationTriggerable() {
    return depublicationTriggerable;
  }
}
