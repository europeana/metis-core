package eu.europeana.metis.core.dataset;

import lombok.Getter;
import lombok.Setter;

/**
 * Used to send over HTTP the dataset with it's corresponding xslt.
 */
@Getter
@Setter
public class DatasetXsltStringWrapper {

  private DatasetDTO dataset;
  private String xslt;
  private String xsltExternal;

  public DatasetXsltStringWrapper() {
    //Required for json serialization
  }

  /**
   * Constructor with all the required paramets
   *
   * @param dataset {@link DatasetDTO}
   * @param xslt the String representation of the xslt text
   * @param xsltExternal the String representation of the external xslt text
   */
  public DatasetXsltStringWrapper(DatasetDTO dataset, String xslt, String xsltExternal) {
    this.dataset = dataset;
    this.xslt = xslt;
    this.xsltExternal = xsltExternal;
  }
}
