package eu.europeana.metis.core.dataset;

import eu.europeana.metis.core.user.User;

public class DatasetConverter {

  public static Dataset fromDTO(DatasetDTO datasetDTO) {
    Dataset dataset = new Dataset();

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
    dataset.setXsltId(datasetDTO.getXsltId());

    return dataset;
  }

  public static DatasetDTO toDTO(Dataset dataset, User user) {
    return new DatasetDTO(
        dataset.getId(),
        dataset.getEcloudDatasetId(),
        dataset.getDatasetId(),
        dataset.getDatasetName(),
        dataset.getOrganizationId(),
        dataset.getOrganizationName(),
        dataset.getProvider(),
        dataset.getDataProvider(),
        dataset.getIntermediateProvider(),
        dataset.getCreatedByUserId(),
        user.getUserName(),
        user.getFirstName(),
        user.getLastName(),
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
        dataset.getXsltId()
    );
  }
}
