package eu.europeana.metis.core.dataset;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import eu.europeana.metis.core.common.CountryDeserializer;
import eu.europeana.metis.core.common.CountrySerializer;
import eu.europeana.metis.core.common.Language;
import eu.europeana.metis.core.common.LanguageDeserializer;
import eu.europeana.metis.core.common.LanguageSerializer;
import eu.europeana.metis.core.dataset.Dataset.PublicationFitness;
import eu.europeana.metis.mongo.utils.ObjectIdSerializer;
import eu.europeana.metis.utils.Country;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.bson.types.ObjectId;

/**
 * Data Transfer Object (DTO) representing a dataset.
 * <p>
 * This class encapsulates the metadata and other relevant information about a dataset.
 */
//TODO: 2025-03-10 - MET-6415 - Abstract this class in smaller components.
public class DatasetDTO {

  private String id;
  private String ecloudDatasetId;
  private String datasetId;
  private String datasetName;
  private String organizationId;
  private String organizationName;
  private String provider;
  private String dataProvider;
  private String intermediateProvider;
  private String createdByUserId;
  private String createdByUserName;
  private String createdByFirstName;
  private String createdByLastName;

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

  public DatasetDTO() {
    //Required for json serialization
  }

  /**
   * Constructs a new DatasetDTO object with the specified parameters.
   *
   * @param id the ID of the dataset
   * @param ecloudDatasetId the ECloud dataset ID
   * @param datasetId the dataset ID
   * @param datasetName the name of the dataset
   * @param organizationId the ID of the organization
   * @param organizationName the name of the organization
   * @param provider the provider of the dataset
   * @param dataProvider the data provider of the dataset
   * @param intermediateProvider the intermediate provider of the dataset
   * @param createdByUserId the ID of the user who created the dataset
   * @param createdByUserName the username of the user who created the dataset
   * @param createdByFirstName the first name of the user who created the dataset
   * @param createdByLastName the last name of the user who created the dataset
   * @param createdDate the date the dataset was created
   * @param updatedDate the date the dataset was last updated
   * @param datasetIdsToRedirectFrom the IDs of datasets to redirect from
   * @param replacedBy the ID of the dataset that replaced this one
   * @param replaces the ID of the dataset that this one replaces
   * @param country the country associated with the dataset
   * @param language the language associated with the dataset
   * @param description the description of the dataset
   * @param publicationFitness the publication fitness of the dataset
   * @param notes the notes associated with the dataset
   * @param xsltId the ID of the XSLT associated with the dataset
   */
  public DatasetDTO(
      String id,
      String ecloudDatasetId,
      String datasetId,
      String datasetName,
      String organizationId,
      String organizationName,
      String provider,
      String dataProvider,
      String intermediateProvider,
      String createdByUserId,
      String createdByUserName,
      String createdByFirstName,
      String createdByLastName,
      Date createdDate,
      Date updatedDate,
      List<String> datasetIdsToRedirectFrom,
      String replacedBy,
      String replaces,
      Country country,
      Language language,
      String description,
      PublicationFitness publicationFitness,
      String notes,
      ObjectId xsltId
  ) {
    this.id = id;
    this.ecloudDatasetId = ecloudDatasetId;
    this.datasetId = datasetId;
    this.datasetName = datasetName;
    this.organizationId = organizationId;
    this.organizationName = organizationName;
    this.provider = provider;
    this.dataProvider = dataProvider;
    this.intermediateProvider = intermediateProvider;
    this.createdByUserId = createdByUserId;
    this.createdByUserName = createdByUserName;
    this.createdByFirstName = createdByFirstName;
    this.createdByLastName = createdByLastName;
    this.createdDate = createdDate == null ? null : new Date(createdDate.getTime());
    this.updatedDate = updatedDate == null ? null : new Date(updatedDate.getTime());
    this.datasetIdsToRedirectFrom =
        datasetIdsToRedirectFrom == null ? new ArrayList<>() : new ArrayList<>(datasetIdsToRedirectFrom);
    this.replacedBy = replacedBy;
    this.replaces = replaces;
    this.country = country;
    this.language = language;
    this.description = description;
    this.publicationFitness = publicationFitness;
    this.notes = notes;
    this.xsltId = xsltId;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  // Getters and setters
  public String getEcloudDatasetId() {
    return ecloudDatasetId;
  }

  public void setEcloudDatasetId(String ecloudDatasetId) {
    this.ecloudDatasetId = ecloudDatasetId;
  }

  public String getDatasetId() {
    return datasetId;
  }

  public void setDatasetId(String datasetId) {
    this.datasetId = datasetId;
  }

  public String getDatasetName() {
    return datasetName;
  }

  public void setDatasetName(String datasetName) {
    this.datasetName = datasetName;
  }

  public String getOrganizationId() {
    return organizationId;
  }

  public void setOrganizationId(String organizationId) {
    this.organizationId = organizationId;
  }

  public String getOrganizationName() {
    return organizationName;
  }

  public void setOrganizationName(String organizationName) {
    this.organizationName = organizationName;
  }

  public String getProvider() {
    return provider;
  }

  public void setProvider(String provider) {
    this.provider = provider;
  }

  public String getDataProvider() {
    return dataProvider;
  }

  public void setDataProvider(String dataProvider) {
    this.dataProvider = dataProvider;
  }

  public String getIntermediateProvider() {
    return intermediateProvider;
  }

  public void setIntermediateProvider(String intermediateProvider) {
    this.intermediateProvider = intermediateProvider;
  }

  public String getCreatedByUserId() {
    return createdByUserId;
  }

  public void setCreatedByUserId(String createdByUserId) {
    this.createdByUserId = createdByUserId;
  }

  public String getCreatedByUserName() {
    return createdByUserName;
  }

  public void setCreatedByUserName(String createdByUserName) {
    this.createdByUserName = createdByUserName;
  }

  public String getCreatedByFirstName() {
    return createdByFirstName;
  }

  public void setCreatedByFirstName(String createdByFirstName) {
    this.createdByFirstName = createdByFirstName;
  }

  public String getCreatedByLastName() {
    return createdByLastName;
  }

  public void setCreatedByLastName(String createdByLastName) {
    this.createdByLastName = createdByLastName;
  }

  public Date getCreatedDate() {
    return createdDate == null ? null : new Date(createdDate.getTime());
  }

  public void setCreatedDate(Date createdDate) {
    this.createdDate = new Date(createdDate.getTime());
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

  public String getReplacedBy() {
    return replacedBy;
  }

  public void setReplacedBy(String replacedBy) {
    this.replacedBy = replacedBy;
  }

  public String getReplaces() {
    return replaces;
  }

  public void setReplaces(String replaces) {
    this.replaces = replaces;
  }

  public Country getCountry() {
    return country;
  }

  public void setCountry(Country country) {
    this.country = country;
  }

  public Language getLanguage() {
    return language;
  }

  public void setLanguage(Language language) {
    this.language = language;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public PublicationFitness getPublicationFitness() {
    return publicationFitness;
  }

  public void setPublicationFitness(PublicationFitness publicationFitness) {
    this.publicationFitness = publicationFitness;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public ObjectId getXsltId() {
    return xsltId;
  }

  public void setXsltId(ObjectId xsltId) {
    this.xsltId = xsltId;
  }
}



