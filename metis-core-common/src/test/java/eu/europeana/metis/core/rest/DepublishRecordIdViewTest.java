package eu.europeana.metis.core.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.europeana.metis.core.dataset.DepublishRecordId;
import eu.europeana.metis.core.dataset.DepublishRecordId.DepublicationStatus;
import eu.europeana.metis.utils.DepublicationReason;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DepublishRecordIdViewTest {

  private DepublishRecordId depublishRecordId;
  private DepublishRecordIdView depublishRecordIdView;

  @BeforeEach
  void setUp() {
    depublishRecordId = new DepublishRecordId();
    depublishRecordId.setRecordId("recordId");
    depublishRecordId.setDatasetId("datasetId");
    depublishRecordId.setDepublicationReason(DepublicationReason.GENERIC);
    depublishRecordId.setDepublicationDate(Instant.MAX);
    depublishRecordId.setDepublicationStatus(DepublicationStatus.DEPUBLISHED);
    depublishRecordIdView = new DepublishRecordIdView(depublishRecordId);
  }

  @Test
  void getRecordId() {
    assertEquals(depublishRecordId.getRecordId(), depublishRecordIdView.getRecordId());
  }

  @Test
  void getDepublicationStatus() {
    assertEquals(depublishRecordId.getDepublicationStatus().name(), depublishRecordIdView.getDepublicationStatus().name());
  }

  @Test
  void getDepublicationDate() {
    assertEquals(depublishRecordId.getDepublicationDate(), depublishRecordIdView.getDepublicationDate());
  }

  @Test
  void getDepublicationReason() {
    assertEquals(depublishRecordId.getDepublicationReason().getTitle(), depublishRecordIdView.getDepublicationReason());
  }
}
