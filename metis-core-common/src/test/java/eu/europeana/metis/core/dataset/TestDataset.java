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

import static eu.europeana.metis.core.dataset.TestDatasetUtils.*;
import static io.smallrye.common.constraint.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class TestDataset {

  @Test
  void testGetters() {
    Dataset dataset = getDataset();
    assertDataset(dataset);
  }

  @Test
  void testEmptyConstructor() {
    Dataset dataset = new Dataset();
    assertNotNull(dataset);
  }

  @Test
  void testNullProvidedValues() {
    Dataset dataset = getDatasetUsingSettersWithNullValues();
    assertNull(dataset.getCreatedDate());
    assertNull(dataset.getUpdatedDate());
    assertTrue(dataset.getDatasetIdsToRedirectFrom().isEmpty());
  }

  @Test
  void testDeserialization() throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    URL resource = getClass().getClassLoader().getResource("dataset.json");
    Objects.requireNonNull(resource);
    File jsonFile = new File(resource.getFile());
    Dataset deserializedDataset = objectMapper.readValue(jsonFile, Dataset.class);

    assertNotNull(deserializedDataset);
    assertDataset(deserializedDataset);
  }

  @Test
  void testSerialization() throws IOException {
    Dataset dataset = getDataset();

    ObjectMapper objectMapper = new ObjectMapper();
    String jsonOutput = objectMapper.writeValueAsString(dataset);
    JsonNode jsonNode = objectMapper.readTree(jsonOutput);

    assertDataset(jsonNode);
  }

  private void assertDataset(JsonNode jsonNode) {
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


  private void assertDataset(Dataset dataset) {
    assertEquals(id, dataset.getId());
    assertEquals(ECLOUD_DATASET_ID, dataset.getEcloudDatasetId());
    assertEquals(DATASET_ID, dataset.getDatasetId());
    assertEquals(DATASET_NAME, dataset.getDatasetName());
    assertEquals(ORGANIZATION_ID, dataset.getOrganizationId());
    assertEquals(ORGANIZATION_NAME, dataset.getOrganizationName());
    assertEquals(PROVIDER, dataset.getProvider());
    assertEquals(DATA_PROVIDER, dataset.getDataProvider());
    assertEquals(INTERMEDIATE_PROVIDER, dataset.getIntermediateProvider());
    assertEquals(CREATED_BY_USER_ID, dataset.getCreatedByUserId());
    assertEquals(createdDate, dataset.getCreatedDate());
    assertEquals(updatedDate, dataset.getUpdatedDate());
    assertEquals(2, dataset.getDatasetIdsToRedirectFrom().size());
    assertTrue(dataset.getDatasetIdsToRedirectFrom().contains(REDIRECT_ID_1_VALUE));
    assertTrue(dataset.getDatasetIdsToRedirectFrom().contains(REDIRECT_ID_2_VALUE));
    assertEquals(REPLACED_BY, dataset.getReplacedBy());
    assertEquals(REPLACES, dataset.getReplaces());
    assertEquals(Country.GREECE, dataset.getCountry());
    assertEquals(Language.EL, dataset.getLanguage());
    assertEquals(DESCRIPTION, dataset.getDescription());
    assertEquals(PublicationFitness.FIT, dataset.getPublicationFitness());
    assertEquals(NOTES, dataset.getNotes());
    assertEquals(xsltId, dataset.getXsltId());
  }
}