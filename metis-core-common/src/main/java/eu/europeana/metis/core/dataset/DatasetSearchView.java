package eu.europeana.metis.core.dataset;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;

/**
 * Dataset search model that contains all the required fields for Dataset Search functionality.
 */
@Getter
@Setter
public class DatasetSearchView {

  private String datasetId;
  private String datasetName;
  private String provider;
  private String dataProvider;
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
  private Date lastExecutionDate;

  public DatasetSearchView() {
    //Required for json (de)serialization
  }

  public Date getLastExecutionDate() {
    return lastExecutionDate == null ? null : new Date(lastExecutionDate.getTime());
  }

  public void setLastExecutionDate(Date lastExecutionDate) {
    this.lastExecutionDate =
        lastExecutionDate == null ? null : new Date(lastExecutionDate.getTime());
  }
}
