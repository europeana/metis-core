package eu.europeana.metis.core.dataset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import eu.europeana.metis.core.user.User;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class TestDatasetConverter {

  @Test
  void testFromDTO() {
    DatasetDTO datasetDTO = TestDatasetUtils.getDatasetDTO();
    Dataset dataset = DatasetConverter.fromDTO(datasetDTO);

    assertEquals(datasetDTO.getId(), dataset.getId().toString());
    assertEquals(datasetDTO.getEcloudDatasetId(), dataset.getEcloudDatasetId());
    assertEquals(datasetDTO.getDatasetId(), dataset.getDatasetId());
    assertEquals(datasetDTO.getDatasetName(), dataset.getDatasetName());
    assertEquals(datasetDTO.getProvider(), dataset.getProvider());
    assertEquals(datasetDTO.getDataProvider(), dataset.getDataProvider());
    assertEquals(datasetDTO.getIntermediateProvider(), dataset.getIntermediateProvider());
    assertEquals(datasetDTO.getCreatedByUserId(), dataset.getCreatedByUserId());
    assertEquals(datasetDTO.getCreatedDate(), dataset.getCreatedDate());
    assertEquals(datasetDTO.getUpdatedDate(), dataset.getUpdatedDate());
    assertEquals(datasetDTO.getDatasetIdsToRedirectFrom(), dataset.getDatasetIdsToRedirectFrom());
    assertEquals(datasetDTO.getReplacedBy(), dataset.getReplacedBy());
    assertEquals(datasetDTO.getReplaces(), dataset.getReplaces());
    assertEquals(datasetDTO.getCountry(), dataset.getCountry());
    assertEquals(datasetDTO.getLanguage(), dataset.getLanguage());
    assertEquals(datasetDTO.getDescription(), dataset.getDescription());
    assertEquals(datasetDTO.getPublicationFitness(), dataset.getPublicationFitness());
    assertEquals(datasetDTO.getNotes(), dataset.getNotes());
    assertEquals(datasetDTO.getXsltId(), dataset.getXsltId().toString());

    datasetDTO.setId(null);
    Dataset dataset1 = DatasetConverter.fromDTO(datasetDTO);
    assertNull(dataset1.getId());
  }

  @Test
  void testToDTO() {
    Dataset dataset = TestDatasetUtils.getDataset();

    User user = new User.UserBuilder()
        .userId("createdByUserId").userName("createdByUserName").firstName("createdByFirstName")
        .lastName("createdByLastName").issuedAt(Instant.now()).build();

    DatasetDTO datasetDTO = DatasetConverter.toDTO(dataset, user);

    assertEquals(dataset.getId().toString(), datasetDTO.getId());
    assertEquals(dataset.getEcloudDatasetId(), datasetDTO.getEcloudDatasetId());
    assertEquals(dataset.getDatasetId(), datasetDTO.getDatasetId());
    assertEquals(dataset.getDatasetName(), datasetDTO.getDatasetName());
    assertEquals(dataset.getProvider(), datasetDTO.getProvider());
    assertEquals(dataset.getDataProvider(), datasetDTO.getDataProvider());
    assertEquals(dataset.getIntermediateProvider(), datasetDTO.getIntermediateProvider());
    assertEquals(dataset.getCreatedByUserId(), datasetDTO.getCreatedByUserId());
    assertEquals(user.getUserName(), datasetDTO.getCreatedByUserName());
    assertEquals(user.getFirstName(), datasetDTO.getCreatedByFirstName());
    assertEquals(user.getLastName(), datasetDTO.getCreatedByLastName());
    assertEquals(dataset.getCreatedDate(), datasetDTO.getCreatedDate());
    assertEquals(dataset.getUpdatedDate(), datasetDTO.getUpdatedDate());
    assertEquals(dataset.getDatasetIdsToRedirectFrom(), datasetDTO.getDatasetIdsToRedirectFrom());
    assertEquals(dataset.getReplacedBy(), datasetDTO.getReplacedBy());
    assertEquals(dataset.getReplaces(), datasetDTO.getReplaces());
    assertEquals(dataset.getCountry(), datasetDTO.getCountry());
    assertEquals(dataset.getLanguage(), datasetDTO.getLanguage());
    assertEquals(dataset.getDescription(), datasetDTO.getDescription());
    assertEquals(dataset.getPublicationFitness(), datasetDTO.getPublicationFitness());
    assertEquals(dataset.getNotes(), datasetDTO.getNotes());
    assertEquals(dataset.getXsltId().toString(), datasetDTO.getXsltId());

    DatasetDTO datasetDTO1 = DatasetConverter.toDTO(dataset, null);

    assertNull(datasetDTO1.getCreatedByUserName());
    assertNull(datasetDTO1.getCreatedByFirstName());
    assertNull(datasetDTO1.getCreatedByLastName());
  }
}