package eu.europeana.metis.core.dataset;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.Indexes;
import eu.europeana.metis.mongo.utils.ObjectIdSerializer;
import java.time.Instant;
import org.bson.types.ObjectId;
import tools.jackson.databind.annotation.JsonSerialize;

/**
 * A wrapper class with metadata about an xslt and the xslt as a string field.
 */
//@formatter:off
@Entity
@Indexes({
    @Index(fields = {@Field("datasetId")}),
    @Index(fields = {@Field("createdDate")}),
    @Index(fields = {@Field("datasetId"), @Field("xsltType"), @Field("createdDate")})
})
//@formatter:on
public class DatasetXslt {

  public static final String DEFAULT_DATASET_ID = "-1";

  @Id
  @JsonSerialize(using = ObjectIdSerializer.class)
  private ObjectId id;

  private String datasetId;
  private XsltType xsltType;
  private String xslt;
  private Instant createdDate;

  public DatasetXslt() {
    //Required for json serialization
  }

  /**
   * Constructor with required parameters for a dataset-specific XSLT. When created it assigns the current date to it.
   *
   * @param datasetId the datasetId that this class is related to
   * @param xsltType the type of the xslt
   * @param xslt the raw xslt
   */
  public DatasetXslt(String datasetId, XsltType xsltType, String xslt) {
    this.datasetId = datasetId;
    this.xsltType = xsltType;
    this.xslt = xslt;
    this.createdDate = Instant.now();
  }

  /**
   * Constructor with required parameters for a default XSLT. When created it assigns the current date to it.
   *
   * @param xslt the raw xslt
   */
  public DatasetXslt(String xslt) {
    this(DEFAULT_DATASET_ID, XsltType.DEFAULT, xslt);
  }

  public ObjectId getId() {
    return id;
  }

  public void setId(ObjectId id) {
    this.id = id;
  }

  public String getDatasetId() {
    return datasetId;
  }

  public void setDatasetId(String datasetId) {
    this.datasetId = datasetId;
  }

  public XsltType getXsltType() {
    return xsltType;
  }

  public void setXsltType(XsltType xsltType) {
    this.xsltType = xsltType;
  }

  public String getXslt() {
    return xslt;
  }

  public void setXslt(String xslt) {
    this.xslt = xslt;
  }

  public Instant getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(Instant createdDate) {
    this.createdDate = createdDate;
  }

  /**
   * Represents the type of XSLT.
   * <p>
   * Types:
   * <ul>
   *   <li>DEFAULT: Represents the default configuration or fallback XSLT.</li>
   *   <li>EXTERNAL: Represents an XSLT for transformation to EDM EXTERNAL.</li>
   *   <li>INTERNAL: Represents an XSLT for transformation to EDM INTERNAL.</li>
   * </ul>
   */
  public enum XsltType {
    DEFAULT,
    EXTERNAL,
    INTERNAL
  }
}
