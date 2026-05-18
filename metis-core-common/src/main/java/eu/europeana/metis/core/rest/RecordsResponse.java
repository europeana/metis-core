package eu.europeana.metis.core.rest;

import java.util.ArrayList;
import java.util.List;

/**
 * Class that encapsulates a list of {@link Record} objects.
 */
public class RecordsResponse {

  private List<Record> records;

  /**
   * Constructor with the required parameters.
   *
   * @param records the list of {@link Record}
   */
  public RecordsResponse(List<Record> records) {
    this.records = new ArrayList<>(records);
  }

  public List<Record> getRecords() {
    return new ArrayList<>(records);
  }

  public void setRecords(List<Record> records) {
    this.records = new ArrayList<>(records);
  }
}
