package eu.europeana.metis.core.dataset;

import static eu.europeana.metis.core.common.TestSerializationUtils.assertFieldEquals;
import static eu.europeana.metis.core.common.TestSerializationUtils.assertListContains;
import static eu.europeana.metis.core.common.TestSerializationUtils.assertNestedFieldEquals;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.COUNTRY;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.COUNTRY_ENUM;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.CREATED_BY_FIRST_NAME;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.CREATED_BY_LAST_NAME;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.CREATED_BY_USER_ID;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.CREATED_BY_USER_NAME;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.CREATED_DATE;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.CREATED_DATE_VALUE;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.DATASET_ID;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.DATASET_IDS_TO_REDIRECT_FROM;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.DATASET_NAME;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.DATA_PROVIDER;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.DESCRIPTION;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.ENGINE_DATASET_ID;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.ID;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.INTERMEDIATE_PROVIDER;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.LANGUAGE;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.LANGUAGE_ENUM;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.NOTES;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.OBJECT_ID_VALUE;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.PROVIDER;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.PUBLICATION_FITNESS;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.REDIRECT_ID_1_VALUE;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.REDIRECT_ID_2_VALUE;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.REPLACED_BY;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.REPLACES;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.UPDATED_DATE;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.UPDATED_DATE_VALUE;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.XSLT_ID;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.XSLT_OBJECT_ID_VALUE;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.getDatasetDTO;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.getDatasetDTOUsingSetters;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.getDatasetDTOUsingSettersWithNullValues;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.getDatasetDTOWithNullValues;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.europeana.metis.core.common.Language;
import eu.europeana.metis.core.dataset.Dataset.PublicationFitness;
import eu.europeana.metis.utils.Country;
import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class TestDatasetDTO {

  @Test
  void testGetters() {
    DatasetDTO datasetDTO = getDatasetDTO();
    assertDatasetDTO(datasetDTO);
  }

  @Test
  void testSetters() {
    DatasetDTO datasetDTO = getDatasetDTOUsingSetters();
    assertDatasetDTO(datasetDTO);
  }

  @Test
  void testNullProvidedValues() {
    DatasetDTO datasetDTO = getDatasetDTOWithNullValues();
    assertNull(datasetDTO.getCreatedDate());
    assertNull(datasetDTO.getUpdatedDate());
    assertTrue(datasetDTO.getDatasetIdsToRedirectFrom().isEmpty());

    DatasetDTO datasetDTO1 = getDatasetDTOUsingSettersWithNullValues();
    assertNull(datasetDTO1.getCreatedDate());
    assertNull(datasetDTO1.getUpdatedDate());
    assertTrue(datasetDTO1.getDatasetIdsToRedirectFrom().isEmpty());
  }

  @Test
  void testSerialization() {
    DatasetDTO datasetDTO = getDatasetDTO();

    ObjectMapper objectMapper = new ObjectMapper();
    String jsonOutput = objectMapper.writeValueAsString(datasetDTO);

    assertDatasetDTO(jsonOutput);
  }

  @Test
  void testDeserialization() {
    ObjectMapper objectMapper = new ObjectMapper();
    URL resource = getClass().getClassLoader().getResource("datasetDTO.json");
    Objects.requireNonNull(resource);
    File jsonFile = new File(resource.getFile());
    DatasetDTO deserializedDatasetDTO = objectMapper.readValue(jsonFile, DatasetDTO.class);

    assertNotNull(deserializedDatasetDTO);
    assertDatasetDTO(deserializedDatasetDTO);
  }

  private void assertDatasetDTO(String jsonOutput) {
    assertFieldEquals(jsonOutput, ID, OBJECT_ID_VALUE.toString());
    assertFieldEquals(jsonOutput, ENGINE_DATASET_ID, ENGINE_DATASET_ID);
    assertFieldEquals(jsonOutput, DATASET_ID, DATASET_ID);
    assertFieldEquals(jsonOutput, DATASET_NAME, DATASET_NAME);
    assertFieldEquals(jsonOutput, PROVIDER, PROVIDER);
    assertFieldEquals(jsonOutput, DATA_PROVIDER, DATA_PROVIDER);
    assertFieldEquals(jsonOutput, INTERMEDIATE_PROVIDER, INTERMEDIATE_PROVIDER);
    assertFieldEquals(jsonOutput, CREATED_BY_USER_ID, CREATED_BY_USER_ID);
    assertFieldEquals(jsonOutput, CREATED_BY_USER_NAME, CREATED_BY_USER_NAME);
    assertFieldEquals(jsonOutput, CREATED_BY_FIRST_NAME, CREATED_BY_FIRST_NAME);
    assertFieldEquals(jsonOutput, CREATED_BY_LAST_NAME, CREATED_BY_LAST_NAME);
    assertFieldEquals(jsonOutput, CREATED_DATE, CREATED_DATE_VALUE);
    assertFieldEquals(jsonOutput, UPDATED_DATE, UPDATED_DATE_VALUE);
    assertListContains(jsonOutput, DATASET_IDS_TO_REDIRECT_FROM, List.of(REDIRECT_ID_1_VALUE, REDIRECT_ID_2_VALUE));
    assertFieldEquals(jsonOutput, REPLACED_BY, REPLACED_BY);
    assertFieldEquals(jsonOutput, REPLACES, REPLACES);
    assertNestedFieldEquals(jsonOutput, COUNTRY, COUNTRY_ENUM, Country.GREECE.name());
    assertNestedFieldEquals(jsonOutput, LANGUAGE, LANGUAGE_ENUM, Language.EL.name());
    assertFieldEquals(jsonOutput, DESCRIPTION, DESCRIPTION);
    assertFieldEquals(jsonOutput, PUBLICATION_FITNESS, PublicationFitness.FIT.name());
    assertFieldEquals(jsonOutput, NOTES, NOTES);
    assertFieldEquals(jsonOutput, XSLT_ID, XSLT_OBJECT_ID_VALUE.toString());
  }

  private void assertDatasetDTO(DatasetDTO datasetDTO) {
    assertEquals(OBJECT_ID_VALUE.toString(), datasetDTO.getId());
    assertEquals(ENGINE_DATASET_ID, datasetDTO.getEngineDatasetId());
    assertEquals(DATASET_ID, datasetDTO.getDatasetId());
    assertEquals(DATASET_NAME, datasetDTO.getDatasetName());
    assertEquals(PROVIDER, datasetDTO.getProvider());
    assertEquals(DATA_PROVIDER, datasetDTO.getDataProvider());
    assertEquals(INTERMEDIATE_PROVIDER, datasetDTO.getIntermediateProvider());
    assertEquals(CREATED_BY_USER_ID, datasetDTO.getCreatedByUserId());
    assertEquals(CREATED_BY_USER_NAME, datasetDTO.getCreatedByUserName());
    assertEquals(CREATED_BY_FIRST_NAME, datasetDTO.getCreatedByFirstName());
    assertEquals(CREATED_BY_LAST_NAME, datasetDTO.getCreatedByLastName());
    assertEquals(CREATED_DATE_VALUE, datasetDTO.getCreatedDate());
    assertEquals(UPDATED_DATE_VALUE, datasetDTO.getUpdatedDate());
    assertEquals(2, datasetDTO.getDatasetIdsToRedirectFrom().size());
    assertTrue(datasetDTO.getDatasetIdsToRedirectFrom().contains(REDIRECT_ID_1_VALUE));
    assertTrue(datasetDTO.getDatasetIdsToRedirectFrom().contains(REDIRECT_ID_2_VALUE));
    assertEquals(REPLACED_BY, datasetDTO.getReplacedBy());
    assertEquals(REPLACES, datasetDTO.getReplaces());
    assertEquals(Country.GREECE, datasetDTO.getCountry());
    assertEquals(Language.EL, datasetDTO.getLanguage());
    assertEquals(DESCRIPTION, datasetDTO.getDescription());
    assertEquals(PublicationFitness.FIT, datasetDTO.getPublicationFitness());
    assertEquals(NOTES, datasetDTO.getNotes());
    assertEquals(XSLT_OBJECT_ID_VALUE.toString(), datasetDTO.getXsltId());
  }
}
