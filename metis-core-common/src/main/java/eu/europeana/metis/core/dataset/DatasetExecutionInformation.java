package eu.europeana.metis.core.dataset;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;

/**
 * Contains execution information of a dataset.
 * <p>Such as the last preview, first publish, last publish, last depublish, last harvest
 * information.</p>
 *
 * @author Simon Tzanakis (Simon.Tzanakis@europeana.eu)
 * @since 2018-04-03
 */
public class DatasetExecutionInformation {

  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
  private Date lastPreviewDate;
  private long lastPreviewRecords;
  private boolean lastPreviewRecordsReadyForViewing;
  private long totalPreviewRecords;
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
  private Date firstPublishedDate;
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
  private Date lastPublishedDate;
  private long lastPublishedRecords;
  private boolean lastPublishedRecordsReadyForViewing;
  private long totalPublishedRecords;
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
  private Date lastDepublishedDate;
  private long lastDepublishedRecords;
  private PublicationStatus publicationStatus;
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
  private Date lastHarvestedDate;
  private long lastHarvestedRecords;

  public DatasetExecutionInformation() {
    //Required for json serialization
  }

  public Date getLastPreviewDate() {
    return lastPreviewDate == null ? null : new Date(lastPreviewDate.getTime());
  }

  public void setLastPreviewDate(Date lastPreviewDate) {
    this.lastPreviewDate = lastPreviewDate == null ? null : new Date(lastPreviewDate.getTime());
  }

  public long getLastPreviewRecords() {
    return lastPreviewRecords;
  }

  public void setLastPreviewRecords(long lastPreviewRecords) {
    this.lastPreviewRecords = lastPreviewRecords;
  }

  public boolean isLastPreviewRecordsReadyForViewing() {
    return lastPreviewRecordsReadyForViewing;
  }

  public void setLastPreviewRecordsReadyForViewing(boolean lastPreviewRecordsReadyForViewing) {
    this.lastPreviewRecordsReadyForViewing = lastPreviewRecordsReadyForViewing;
  }

  public long getTotalPreviewRecords() {
    return totalPreviewRecords;
  }

  public void setTotalPreviewRecords(long totalPreviewRecords) {
    this.totalPreviewRecords = totalPreviewRecords;
  }

  public Date getFirstPublishedDate() {
    return firstPublishedDate == null ? null : new Date(firstPublishedDate.getTime());
  }

  public void setFirstPublishedDate(Date firstPublishedDate) {
    this.firstPublishedDate =
        firstPublishedDate == null ? null : new Date(firstPublishedDate.getTime());
  }

  public Date getLastPublishedDate() {
    return lastPublishedDate == null ? null : new Date(lastPublishedDate.getTime());
  }

  public void setLastPublishedDate(Date lastPublishedDate) {
    this.lastPublishedDate =
        lastPublishedDate == null ? null : new Date(lastPublishedDate.getTime());
  }

  public long getLastPublishedRecords() {
    return lastPublishedRecords;
  }

  public void setLastPublishedRecords(long lastPublishedRecords) {
    this.lastPublishedRecords = lastPublishedRecords;
  }

  public boolean isLastPublishedRecordsReadyForViewing() {
    return lastPublishedRecordsReadyForViewing;
  }

  public void setLastPublishedRecordsReadyForViewing(boolean lastPublishedRecordsReadyForViewing) {
    this.lastPublishedRecordsReadyForViewing = lastPublishedRecordsReadyForViewing;
  }

  public long getTotalPublishedRecords() {
    return totalPublishedRecords;
  }

  public void setTotalPublishedRecords(long totalPublishedRecords) {
    this.totalPublishedRecords = totalPublishedRecords;
  }

  public Date getLastDepublishedDate() {
    return lastDepublishedDate == null ? null : new Date(lastDepublishedDate.getTime());
  }

  public void setLastDepublishedDate(Date lastDepublishedDate) {
    this.lastDepublishedDate =
        lastDepublishedDate == null ? null : new Date(lastDepublishedDate.getTime());
  }

  public long getLastDepublishedRecords() {
    return lastDepublishedRecords;
  }

  public void setLastDepublishedRecords(long lastDepublishedRecords) {
    this.lastDepublishedRecords = lastDepublishedRecords;
  }

  public PublicationStatus getPublicationStatus() {
    return publicationStatus;
  }

  public void setPublicationStatus(PublicationStatus publicationStatus) {
    this.publicationStatus = publicationStatus;
  }

  public Date getLastHarvestedDate() {
    return lastHarvestedDate == null ? null : new Date(lastHarvestedDate.getTime());
  }

  public void setLastHarvestedDate(Date lastHarvestedDate) {
    this.lastHarvestedDate =
        lastHarvestedDate == null ? null : new Date(lastHarvestedDate.getTime());
  }

  public long getLastHarvestedRecords() {
    return lastHarvestedRecords;
  }

  public void setLastHarvestedRecords(long lastHarvestedRecords) {
    this.lastHarvestedRecords = lastHarvestedRecords;
  }

  /**
   * The status of the dataset with regards to (de)publication.
   */
  public enum PublicationStatus {
    PUBLISHED, DEPUBLISHED
  }
}
