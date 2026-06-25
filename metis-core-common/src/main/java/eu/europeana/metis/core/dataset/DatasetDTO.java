package eu.europeana.metis.core.dataset;

import eu.europeana.metis.core.common.CountryDeserializer;
import eu.europeana.metis.core.common.CountrySerializer;
import eu.europeana.metis.core.common.Language;
import eu.europeana.metis.core.common.LanguageDeserializer;
import eu.europeana.metis.core.common.LanguageSerializer;
import eu.europeana.metis.core.dataset.Dataset.PublicationFitness;
import eu.europeana.metis.utils.Country;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;

/**
 * Data Transfer Object (DTO) representing a dataset.
 * <p>
 * This class encapsulates the metadata and other relevant information about a dataset.
 */
//TODO: 2025-03-10 - MET-6415 - Abstract this class in smaller components.
@Getter
@Setter
public class DatasetDTO {

  private String id;
  private String ecloudDatasetId;
  private String datasetId;
  private String datasetName;
  private String provider;
  private String dataProvider;
  private String intermediateProvider;
  private String createdByUserId;
  private String createdByUserName;
  private String createdByFirstName;
  private String createdByLastName;

  private Instant createdDate;
  private Instant updatedDate;

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
  private String xsltId;
  private String xsltIdExternal;

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
   * @param xsltIdExternal the ID of the external XSLT associated with the dataset
   */
  public DatasetDTO(
      String id,
      String ecloudDatasetId,
      String datasetId,
      String datasetName,
      String provider,
      String dataProvider,
      String intermediateProvider,
      String createdByUserId,
      String createdByUserName,
      String createdByFirstName,
      String createdByLastName,
      Instant createdDate,
      Instant updatedDate,
      List<String> datasetIdsToRedirectFrom,
      String replacedBy,
      String replaces,
      Country country,
      Language language,
      String description,
      PublicationFitness publicationFitness,
      String notes,
      String xsltId,
      String xsltIdExternal
  ) {
    this.id = id;
    this.ecloudDatasetId = ecloudDatasetId;
    this.datasetId = datasetId;
    this.datasetName = datasetName;
    this.provider = provider;
    this.dataProvider = dataProvider;
    this.intermediateProvider = intermediateProvider;
    this.createdByUserId = createdByUserId;
    this.createdByUserName = createdByUserName;
    this.createdByFirstName = createdByFirstName;
    this.createdByLastName = createdByLastName;
    this.createdDate = createdDate;
    this.updatedDate = updatedDate;
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
    this.xsltIdExternal = xsltIdExternal;
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



