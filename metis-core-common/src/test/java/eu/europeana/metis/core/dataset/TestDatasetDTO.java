package eu.europeana.metis.core.dataset;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europeana.metis.core.common.Language;
import eu.europeana.metis.core.dataset.Dataset.PublicationFitness;
import eu.europeana.metis.utils.Country;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import org.junit.jupiter.api.Test;

import static eu.europeana.metis.core.dataset.TestDatasetUtils.COUNTRY;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.COUNTRY_ENUM;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.CREATED_BY_FIRST_NAME;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.CREATED_BY_LAST_NAME;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.CREATED_BY_USER_ID;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.CREATED_BY_USER_NAME;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.CREATED_DATE;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.DATASET_ID;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.DATASET_IDS_TO_REDIRECT_FROM;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.DATASET_NAME;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.DATA_PROVIDER;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.DESCRIPTION;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.ECLOUD_DATASET_ID;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.ID;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.INTERMEDIATE_PROVIDER;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.LANGUAGE;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.LANGUAGE_ENUM;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.NOTES;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.ORGANIZATION_ID;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.ORGANIZATION_NAME;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.PROVIDER;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.PUBLICATION_FITNESS;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.REDIRECT_ID_1_VALUE;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.REDIRECT_ID_2_VALUE;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.REPLACED_BY;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.REPLACES;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.UPDATED_DATE;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.XSLT_ID;
import static eu.europeana.metis.core.common.TestSerializationUtils.assertFieldEquals;
import static eu.europeana.metis.core.common.TestSerializationUtils.assertListContains;
import static eu.europeana.metis.core.common.TestSerializationUtils.assertNestedFieldEquals;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.createdDate;
import static eu.europeana.metis.core.common.TestSerializationUtils.formatAsUTC;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.getDatasetDTO;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.getDatasetDTOUsingSetters;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.getDatasetDTOUsingSettersWithNullValues;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.getDatasetDTOWithNullValues;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.id;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.updatedDate;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.xsltId;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
  void testDeserialization() throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    URL resource = getClass().getClassLoader().getResource("datasetDTO.json");
    Objects.requireNonNull(resource);
    File jsonFile = new File(resource.getFile());
    DatasetDTO deserializedDatasetDTO = objectMapper.readValue(jsonFile, DatasetDTO.class);

    assertNotNull(deserializedDatasetDTO);
    assertDatasetDTO(deserializedDatasetDTO);
  }

  @Test
  void testSerialization() throws IOException {
    DatasetDTO datasetDTO = getDatasetDTO();

    ObjectMapper objectMapper = new ObjectMapper();
    String jsonOutput = objectMapper.writeValueAsString(datasetDTO);
    JsonNode jsonNode = objectMapper.readTree(jsonOutput);

    assertDatasetDTO(jsonNode);
  }

  private void assertDatasetDTO(JsonNode jsonNode) {
    assertFieldEquals(jsonNode, ID, id.toString());
    assertFieldEquals(jsonNode, ECLOUD_DATASET_ID, ECLOUD_DATASET_ID);
    assertFieldEquals(jsonNode, DATASET_ID, DATASET_ID);
    assertFieldEquals(jsonNode, DATASET_NAME, DATASET_NAME);
    assertFieldEquals(jsonNode, ORGANIZATION_ID, ORGANIZATION_ID);
    assertFieldEquals(jsonNode, ORGANIZATION_NAME, ORGANIZATION_NAME);
    assertFieldEquals(jsonNode, PROVIDER, PROVIDER);
    assertFieldEquals(jsonNode, DATA_PROVIDER, DATA_PROVIDER);
    assertFieldEquals(jsonNode, INTERMEDIATE_PROVIDER, INTERMEDIATE_PROVIDER);
    assertFieldEquals(jsonNode, CREATED_BY_USER_ID, CREATED_BY_USER_ID);
    assertFieldEquals(jsonNode, CREATED_BY_USER_NAME, CREATED_BY_USER_NAME);
    assertFieldEquals(jsonNode, CREATED_BY_FIRST_NAME, CREATED_BY_FIRST_NAME);
    assertFieldEquals(jsonNode, CREATED_BY_LAST_NAME, CREATED_BY_LAST_NAME);
    String expectedCreatedDate = formatAsUTC(createdDate);
    String expectedUpdatedDate = formatAsUTC(updatedDate);
    assertFieldEquals(jsonNode, CREATED_DATE, expectedCreatedDate);
    assertFieldEquals(jsonNode, UPDATED_DATE, expectedUpdatedDate);
    assertListContains(jsonNode.get(DATASET_IDS_TO_REDIRECT_FROM), REDIRECT_ID_1_VALUE, REDIRECT_ID_2_VALUE);
    assertFieldEquals(jsonNode, REPLACED_BY, REPLACED_BY);
    assertFieldEquals(jsonNode, REPLACES, REPLACES);
    assertNestedFieldEquals(jsonNode, COUNTRY, COUNTRY_ENUM, Country.GREECE.name());
    assertNestedFieldEquals(jsonNode, LANGUAGE, LANGUAGE_ENUM, Language.EL.name());
    assertFieldEquals(jsonNode, DESCRIPTION, DESCRIPTION);
    assertFieldEquals(jsonNode, PUBLICATION_FITNESS, PublicationFitness.FIT.name());
    assertFieldEquals(jsonNode, NOTES, NOTES);
    assertFieldEquals(jsonNode, XSLT_ID, xsltId.toString());
  }

  private void assertDatasetDTO(DatasetDTO datasetDTO) {
    assertEquals(id.toString(), datasetDTO.getId());
    assertEquals(ECLOUD_DATASET_ID, datasetDTO.getEcloudDatasetId());
    assertEquals(DATASET_ID, datasetDTO.getDatasetId());
    assertEquals(DATASET_NAME, datasetDTO.getDatasetName());
    assertEquals(ORGANIZATION_ID, datasetDTO.getOrganizationId());
    assertEquals(ORGANIZATION_NAME, datasetDTO.getOrganizationName());
    assertEquals(PROVIDER, datasetDTO.getProvider());
    assertEquals(DATA_PROVIDER, datasetDTO.getDataProvider());
    assertEquals(INTERMEDIATE_PROVIDER, datasetDTO.getIntermediateProvider());
    assertEquals(CREATED_BY_USER_ID, datasetDTO.getCreatedByUserId());
    assertEquals(CREATED_BY_USER_NAME, datasetDTO.getCreatedByUserName());
    assertEquals(CREATED_BY_FIRST_NAME, datasetDTO.getCreatedByFirstName());
    assertEquals(CREATED_BY_LAST_NAME, datasetDTO.getCreatedByLastName());
    assertEquals(createdDate, datasetDTO.getCreatedDate());
    assertEquals(updatedDate, datasetDTO.getUpdatedDate());
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
    assertEquals(xsltId.toString(), datasetDTO.getXsltId());
  }
}
