package eu.europeana.metis.core.dataset;

import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Contains execution information of a dataset.
 * <p>Such as the last preview, first publish, last publish, last depublish, last harvest
 * information.</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class DatasetExecutionInformation {

  private Instant lastPreviewDate;
  private long lastPreviewRecords;
  private boolean lastPreviewRecordsReadyForViewing;
  private long totalPreviewRecords;
  private Instant firstPublishedDate;
  private Instant lastPublishedDate;
  private long lastPublishedRecords;
  private boolean lastPublishedRecordsReadyForViewing;
  private long totalPublishedRecords;
  private Instant lastDepublishedDate;
  private long lastDepublishedRecords;
  private PublicationStatus publicationStatus;
  private Instant lastHarvestedDate;
  private long lastHarvestedRecords;

  /**
   * The status of the dataset with regards to (de)publication.
   */
  public enum PublicationStatus {
    PUBLISHED, DEPUBLISHED
  }
}
