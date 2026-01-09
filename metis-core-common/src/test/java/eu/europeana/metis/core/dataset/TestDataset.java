package eu.europeana.metis.core.dataset;

import eu.europeana.metis.core.common.Language;
import eu.europeana.metis.core.common.TestSerializationUtils;
import eu.europeana.metis.core.dataset.Dataset.PublicationFitness;
import eu.europeana.metis.utils.Country;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Objects;

import static eu.europeana.metis.core.dataset.TestDatasetUtils.*;
import static io.smallrye.common.constraint.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.*;

class TestDataset {

  @Test
  void testGetters() {
    Dataset dataset = getDataset();
    assertDataset(dataset);
  }

  @Test
  void testNullProvidedValues() {
    Dataset dataset = getDatasetUsingSettersWithNullValues();
    assertNull(dataset.getCreatedDate());
    assertNull(dataset.getUpdatedDate());
    assertTrue(dataset.getDatasetIdsToRedirectFrom().isEmpty());
  }

  @Test
  void testDeserialization() {
    ObjectMapper objectMapper = new ObjectMapper();
    URL resource = getClass().getClassLoader().getResource("dataset.json");
    Objects.requireNonNull(resource);
    File jsonFile = new File(resource.getFile());
    Dataset deserializedDataset = objectMapper.readValue(jsonFile, Dataset.class);

    assertNotNull(deserializedDataset);
    assertDataset(deserializedDataset);
  }

  @Test
  void testSerialization() {
    Dataset dataset = getDataset();

    ObjectMapper objectMapper = new ObjectMapper();
    String jsonOutput = objectMapper.writeValueAsString(dataset);

    assertDataset(jsonOutput);
  }

  private void assertDataset(String jsonOutput) {
    TestSerializationUtils.assertFieldEquals(jsonOutput, ID, OBJECT_ID_VALUE.toString());
    TestSerializationUtils.assertFieldEquals(jsonOutput, ECLOUD_DATASET_ID, ECLOUD_DATASET_ID);
    TestSerializationUtils.assertFieldEquals(jsonOutput, DATASET_ID, DATASET_ID);
    TestSerializationUtils.assertFieldEquals(jsonOutput, DATASET_NAME, DATASET_NAME);
    TestSerializationUtils.assertFieldEquals(jsonOutput, PROVIDER, PROVIDER);
    TestSerializationUtils.assertFieldEquals(jsonOutput, DATA_PROVIDER, DATA_PROVIDER);
    TestSerializationUtils.assertFieldEquals(jsonOutput, INTERMEDIATE_PROVIDER, INTERMEDIATE_PROVIDER);
    TestSerializationUtils.assertFieldEquals(jsonOutput, CREATED_BY_USER_ID, CREATED_BY_USER_ID);
    TestSerializationUtils.assertFieldEquals(jsonOutput, CREATED_DATE, CREATED_DATE_VALUE);
    TestSerializationUtils.assertFieldEquals(jsonOutput, UPDATED_DATE, UPDATED_DATE_VALUE);
    TestSerializationUtils.assertListContains(jsonOutput, DATASET_IDS_TO_REDIRECT_FROM, List.of(REDIRECT_ID_1_VALUE, REDIRECT_ID_2_VALUE));
    TestSerializationUtils.assertFieldEquals(jsonOutput, REPLACED_BY, REPLACED_BY);
    TestSerializationUtils.assertFieldEquals(jsonOutput, REPLACES, REPLACES);
    TestSerializationUtils.assertNestedFieldEquals(jsonOutput, COUNTRY, COUNTRY_ENUM, Country.GREECE.name());
    TestSerializationUtils.assertNestedFieldEquals(jsonOutput, LANGUAGE, LANGUAGE_ENUM, Language.EL.name());
    TestSerializationUtils.assertFieldEquals(jsonOutput, DESCRIPTION, DESCRIPTION);
    TestSerializationUtils.assertFieldEquals(jsonOutput, PUBLICATION_FITNESS, PublicationFitness.FIT.name());
    TestSerializationUtils.assertFieldEquals(jsonOutput, NOTES, NOTES);
    TestSerializationUtils.assertFieldEquals(jsonOutput, XSLT_ID, XSLT_OBJECT_ID_VALUE.toString());
  }


  private void assertDataset(Dataset dataset) {
    assertEquals(OBJECT_ID_VALUE, dataset.getId());
    assertEquals(ECLOUD_DATASET_ID, dataset.getEcloudDatasetId());
    assertEquals(DATASET_ID, dataset.getDatasetId());
    assertEquals(DATASET_NAME, dataset.getDatasetName());
    assertEquals(PROVIDER, dataset.getProvider());
    assertEquals(DATA_PROVIDER, dataset.getDataProvider());
    assertEquals(INTERMEDIATE_PROVIDER, dataset.getIntermediateProvider());
    assertEquals(CREATED_BY_USER_ID, dataset.getCreatedByUserId());
    assertEquals(CREATED_DATE_VALUE, dataset.getCreatedDate());
    assertEquals(UPDATED_DATE_VALUE, dataset.getUpdatedDate());
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
    assertEquals(XSLT_OBJECT_ID_VALUE, dataset.getXsltId());
  }
}