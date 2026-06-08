package eu.europeana.metis.core.dataset;

import com.fasterxml.jackson.annotation.JsonFormat;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.IndexOptions;
import dev.morphia.annotations.Indexes;
import eu.europeana.metis.core.common.CountryDeserializer;
import eu.europeana.metis.core.common.CountrySerializer;
import eu.europeana.metis.core.common.Language;
import eu.europeana.metis.core.common.LanguageDeserializer;
import eu.europeana.metis.core.common.LanguageSerializer;
import eu.europeana.metis.mongo.model.HasMongoObjectId;
import eu.europeana.metis.mongo.utils.ObjectIdSerializer;
import eu.europeana.metis.utils.Country;
import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Dataset model that contains all the required fields for Dataset functionality.
 */
@Entity
@Indexes({
    @Index(fields = {@Field("datasetName")}, options = @IndexOptions(unique = true)),
    @Index(fields = {@Field("ecloudDatasetId")}, options = @IndexOptions(unique = true)),
    @Index(fields = {@Field("datasetId")}),
    @Index(fields = {@Field("provider")}),
    @Index(fields = {@Field("intermediateProvider")}),
    @Index(fields = {@Field("dataProvider")}),
    @Index(fields = {@Field("createdByUserId")})})
@Getter
@Setter
public class Dataset implements HasMongoObjectId {

  /**
   * Whether a dataset is fit for publication. {@link #PARTIALLY_FIT} means that some records may be
   * unfit for publication.
   */
  public enum PublicationFitness {
    FIT, PARTIALLY_FIT, UNFIT
  }

  @Id
  @JsonSerialize(using = ObjectIdSerializer.class)
  private ObjectId id;
  private String ecloudDatasetId;
  private String datasetId;
  private String datasetName;
  private String provider;
  private String intermediateProvider;
  private String dataProvider;
  private String createdByUserId;

  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
  private Date createdDate;
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
  private Date updatedDate;

  private List<String> datasetIdsToRedirectFrom = new ArrayList<>();
  private String replacedBy;
  private String replaces;

  @JsonSerialize(using = CountrySerializer.class)
  @JsonDeserialize(using = CountryDeserializer.class)
  private Country country;
  @JsonSerialize(using = LanguageSerializer.class)
  @JsonDeserialize(using = LanguageDeserializer.class)
  private Language language;

  private String description;
  private PublicationFitness publicationFitness;
  private String notes;

  @JsonSerialize(using = ObjectIdSerializer.class)
  private ObjectId xsltId;
  @JsonSerialize(using = ObjectIdSerializer.class)
  private ObjectId xsltIdExternal;

  @Override
  public ObjectId getId() {
    return id;
  }

  @Override
  public void setId(ObjectId id) {
    this.id = id;
  }

  public Date getCreatedDate() {
    return createdDate == null ? null : new Date(createdDate.getTime());
  }

  public void setCreatedDate(Date createdDate) {
    this.createdDate = createdDate == null ? null : new Date(createdDate.getTime());
  }

  public Date getUpdatedDate() {
    return updatedDate == null ? null : new Date(updatedDate.getTime());
  }

  public void setUpdatedDate(Date updatedDate) {
    this.updatedDate = updatedDate == null ? null : new Date(updatedDate.getTime());
  }

  public List<String> getDatasetIdsToRedirectFrom() {
    return new ArrayList<>(datasetIdsToRedirectFrom);
  }

  public void setDatasetIdsToRedirectFrom(List<String> datasetIdsToRedirectFrom) {
    this.datasetIdsToRedirectFrom =
        datasetIdsToRedirectFrom == null ? new ArrayList<>() : new ArrayList<>(
            datasetIdsToRedirectFrom);
  }
}
