package eu.europeana.metis.core.dataset;

import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Dataset search model that contains all the required fields for Dataset Search functionality.
 */
@Getter
@Setter
@NoArgsConstructor
public class DatasetSearchView {

  private String datasetId;
  private String datasetName;
  private String provider;
  private String dataProvider;
  private Instant lastExecutionDate;
}
