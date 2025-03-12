package eu.europeana.metis.core.dataset;

import eu.europeana.metis.core.user.User;
import java.util.Optional;
import org.bson.types.ObjectId;

/**
 * A utility class that provides methods for converting {@link Dataset} into {@link DatasetDTO}.
 *
 * <p>This class is designed to act as a translator between the domain model
 * and the Data Transfer Object (DTO) for ExecutionProgress, ensuring separation of concerns and easing data transfer between
 * layers.
 */
public final class DatasetConverter {

  private DatasetConverter() {
  }

  /**
   * Creates a new Dataset object from the given DatasetDTO object.
   *
   * @param datasetDTO The datasetDTO to be converted.
   * @return A Dataset object containing the same information as the given DatasetDTO object.
   */
  public static Dataset fromDTO(DatasetDTO datasetDTO) {
    Dataset dataset = new Dataset();

    dataset.setId(Optional.ofNullable(datasetDTO.getId()).map(ObjectId::new).orElse(null));
    dataset.setEcloudDatasetId(datasetDTO.getEcloudDatasetId());
    dataset.setDatasetId(datasetDTO.getDatasetId());
    dataset.setDatasetName(datasetDTO.getDatasetName());
    dataset.setOrganizationId(datasetDTO.getOrganizationId());
    dataset.setOrganizationName(datasetDTO.getOrganizationName());
    dataset.setProvider(datasetDTO.getProvider());
    dataset.setDataProvider(datasetDTO.getDataProvider());
    dataset.setIntermediateProvider(datasetDTO.getIntermediateProvider());
    dataset.setCreatedByUserId(datasetDTO.getCreatedByUserId());
    dataset.setCreatedDate(datasetDTO.getCreatedDate());
    dataset.setUpdatedDate(datasetDTO.getUpdatedDate());
    dataset.setDatasetIdsToRedirectFrom(datasetDTO.getDatasetIdsToRedirectFrom());
    dataset.setReplacedBy(datasetDTO.getReplacedBy());
    dataset.setReplaces(datasetDTO.getReplaces());
    dataset.setCountry(datasetDTO.getCountry());
    dataset.setLanguage(datasetDTO.getLanguage());
    dataset.setDescription(datasetDTO.getDescription());
    dataset.setPublicationFitness(datasetDTO.getPublicationFitness());
    dataset.setNotes(datasetDTO.getNotes());
    dataset.setXsltId(Optional.ofNullable(datasetDTO.getXsltId()).map(ObjectId::new).orElse(null));

    return dataset;
  }

  /**
   * Creates a new DatasetDTO object from the given Dataset and User objects.
   *
   * @param dataset The dataset to be converted.
   * @param user The user associated with the dataset.
   * @return A DatasetDTO object containing the same information as the given Dataset and User objects.
   */
  public static DatasetDTO toDTO(Dataset dataset, User user) {
    return new DatasetDTO(
        Optional.ofNullable(dataset.getId()).map(ObjectId::toString).orElse(null),
        dataset.getEcloudDatasetId(),
        dataset.getDatasetId(),
        dataset.getDatasetName(),
        dataset.getOrganizationId(),
        dataset.getOrganizationName(),
        dataset.getProvider(),
        dataset.getDataProvider(),
        dataset.getIntermediateProvider(),
        dataset.getCreatedByUserId(),
        Optional.ofNullable(user).map(User::getUserName).orElse(null),
        Optional.ofNullable(user).map(User::getFirstName).orElse(null),
        Optional.ofNullable(user).map(User::getLastName).orElse(null),
        dataset.getCreatedDate(),
        dataset.getUpdatedDate(),
        dataset.getDatasetIdsToRedirectFrom(),
        dataset.getReplacedBy(),
        dataset.getReplaces(),
        dataset.getCountry(),
        dataset.getLanguage(),
        dataset.getDescription(),
        dataset.getPublicationFitness(),
        dataset.getNotes(),
        Optional.ofNullable(dataset.getXsltId()).map(ObjectId::toString).orElse(null)
    );
  }
}
