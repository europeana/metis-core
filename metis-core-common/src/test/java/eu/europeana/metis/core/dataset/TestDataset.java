package eu.europeana.metis.core.dataset;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europeana.metis.core.common.Language;
import eu.europeana.metis.core.common.TestSerializationUtils;
import eu.europeana.metis.core.dataset.Dataset.PublicationFitness;
import eu.europeana.metis.utils.Country;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;

import static eu.europeana.metis.core.dataset.TestDatasetUtils.COUNTRY;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.COUNTRY_ENUM;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.CREATED_BY_USER_ID;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.CREATED_DATE;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.CREATED_DATE_VALUE;
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
import static eu.europeana.metis.core.dataset.TestDatasetUtils.OBJECT_ID_VALUE;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.ORGANIZATION_ID;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.ORGANIZATION_NAME;
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
import static eu.europeana.metis.core.dataset.TestDatasetUtils.getDataset;
import static eu.europeana.metis.core.dataset.TestDatasetUtils.getDatasetUsingSettersWithNullValues;
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

    assertDataset(jsonOutput);
  }

  private void assertDataset(String jsonOutput) {
    TestSerializationUtils.assertFieldEquals(jsonOutput, ID, OBJECT_ID_VALUE.toString());
    TestSerializationUtils.assertFieldEquals(jsonOutput, ECLOUD_DATASET_ID, ECLOUD_DATASET_ID);
    TestSerializationUtils.assertFieldEquals(jsonOutput, DATASET_ID, DATASET_ID);
    TestSerializationUtils.assertFieldEquals(jsonOutput, DATASET_NAME, DATASET_NAME);
    TestSerializationUtils.assertFieldEquals(jsonOutput, ORGANIZATION_ID, ORGANIZATION_ID);
    TestSerializationUtils.assertFieldEquals(jsonOutput, ORGANIZATION_NAME, ORGANIZATION_NAME);
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
    assertEquals(ORGANIZATION_ID, dataset.getOrganizationId());
    assertEquals(ORGANIZATION_NAME, dataset.getOrganizationName());
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