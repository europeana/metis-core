package eu.europeana.metis.core.dataset;

/**
 * Used to send over HTTP the dataset with it's corresponding xslt.
 *
 * @author Simon Tzanakis (Simon.Tzanakis@europeana.eu)
 * @since 2018-02-28
 */
public class DatasetXsltStringWrapper {

  private DatasetDTO dataset;
  private String xslt;

  public DatasetXsltStringWrapper() {
    //Required for json serialization
  }

  /**
   * Constructor with all the required paramets
   *
   * @param dataset {@link DatasetDTO}
   * @param xslt the String representation of the xslt text
   */
  public DatasetXsltStringWrapper(DatasetDTO dataset, String xslt) {
    this.dataset = dataset;
    this.xslt = xslt;
  }

  public DatasetDTO getDataset() {
    return dataset;
  }

  public void setDataset(DatasetDTO dataset) {
    this.dataset = dataset;
  }

  public String getXslt() {
    return xslt;
  }

  public void setXslt(String xslt) {
    this.xslt = xslt;
  }
}
