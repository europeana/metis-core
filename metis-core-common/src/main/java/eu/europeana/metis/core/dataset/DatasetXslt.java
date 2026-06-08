package eu.europeana.metis.core.dataset;

import com.fasterxml.jackson.annotation.JsonFormat;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.Indexes;
import eu.europeana.metis.mongo.utils.ObjectIdSerializer;
import java.util.Date;
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
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
  private Date createdDate;

  public DatasetXslt() {
    //Required for json serialization
  }

  /**
   * Constructor with required parameters for a dataset-specific XSLT. When created it assigns the
   * current date to it.
   *
   * @param datasetId the datasetId that this class is related to
   * @param xsltType the type of the xslt
   * @param xslt the raw xslt
   */
  public DatasetXslt(String datasetId, XsltType xsltType, String xslt) {
    this.datasetId = datasetId;
    this.xsltType = xsltType;
    this.xslt = xslt;
    this.createdDate = new Date();
  }

  /**
   * Constructor with required parameters for a default XSLT. When created it assigns the current
   * date to it.
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

  public Date getCreatedDate() {
    return createdDate == null ? null : new Date(createdDate.getTime());
  }

  public void setCreatedDate(Date createdDate) {
    this.createdDate = createdDate == null ? null : new Date(createdDate.getTime());
  }

  public enum XsltType {
    DEFAULT,
    EXTERNAL,
    INTERNAL
  }
}
