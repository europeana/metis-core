package eu.europeana.metis.core.dataset;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;

/**
 * Contains execution information of a dataset.
 * <p>Such as the last preview, first publish, last publish, last depublish, last harvest
 * information.</p>
 */
@Getter
@Setter
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

  public Date getLastDepublishedDate() {
    return lastDepublishedDate == null ? null : new Date(lastDepublishedDate.getTime());
  }

  public void setLastDepublishedDate(Date lastDepublishedDate) {
    this.lastDepublishedDate =
        lastDepublishedDate == null ? null : new Date(lastDepublishedDate.getTime());
  }

  public Date getLastHarvestedDate() {
    return lastHarvestedDate == null ? null : new Date(lastHarvestedDate.getTime());
  }

  public void setLastHarvestedDate(Date lastHarvestedDate) {
    this.lastHarvestedDate =
        lastHarvestedDate == null ? null : new Date(lastHarvestedDate.getTime());
  }

  /**
   * The status of the dataset with regards to (de)publication.
   */
  public enum PublicationStatus {
    PUBLISHED, DEPUBLISHED
  }
}
