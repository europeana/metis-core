package eu.europeana.metis.core.dataset;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europeana.metis.core.common.Language;
import eu.europeana.metis.core.dataset.Dataset.PublicationFitness;
import eu.europeana.metis.utils.Country;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Objects;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestDatasetDTO {

  @Test
  void testGetters() {
    DatasetDTO datasetDTO = TestDatasetObjectFactory.getDatasetDTO();
    assertDatasetDTO(datasetDTO);
  }

  @Test
  void testSetters() {
    DatasetDTO datasetDTO = getDatasetDTOUsingSetters();
    assertDatasetDTO(datasetDTO);
  }

  @Test
  void testEmptyConstructor() {
    DatasetDTO emptyDatasetDTO = new DatasetDTO();
    assertNotNull(emptyDatasetDTO);
  }

  @Test
  void testNullProvidedValues() {
    DatasetDTO datasetDTO2 = getDatasetDTOWithNullValues();
    assertNull(datasetDTO2.getCreatedDate());
    assertNull(datasetDTO2.getUpdatedDate());
    assertNotNull(datasetDTO2.getDatasetIdsToRedirectFrom());

    DatasetDTO datasetDTO = getDatasetDTOUsingSettersWithNullValues();
    assertNull(datasetDTO.getCreatedDate());
    assertNull(datasetDTO.getUpdatedDate());
    assertNotNull(datasetDTO.getDatasetIdsToRedirectFrom());
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
    DatasetDTO datasetDTO = TestDatasetObjectFactory.getDatasetDTO();

    ObjectMapper objectMapper = new ObjectMapper();
    String jsonOutput = objectMapper.writeValueAsString(datasetDTO);
    JsonNode jsonNode = objectMapper.readTree(jsonOutput);

    assertEquals(TestDatasetObjectFactory.id.toString(), jsonNode.get("id").asText());
    assertEquals("ecloudDatasetId", jsonNode.get("ecloudDatasetId").asText());
    assertEquals("datasetId", jsonNode.get("datasetId").asText());
    assertEquals("datasetName", jsonNode.get("datasetName").asText());
    assertEquals("organizationId", jsonNode.get("organizationId").asText());
    assertEquals("organizationName", jsonNode.get("organizationName").asText());
    assertEquals("provider", jsonNode.get("provider").asText());
    assertEquals("dataProvider", jsonNode.get("dataProvider").asText());
    assertEquals("intermediateProvider", jsonNode.get("intermediateProvider").asText());
    assertEquals("createdByUserId", jsonNode.get("createdByUserId").asText());
    assertEquals("createdByUserName", jsonNode.get("createdByUserName").asText());
    assertEquals("createdByFirstName", jsonNode.get("createdByFirstName").asText());
    assertEquals("createdByLastName", jsonNode.get("createdByLastName").asText());
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    String expectedCreatedDate = simpleDateFormat.format(TestDatasetObjectFactory.createdDate);
    String expectedUpdatedDate = simpleDateFormat.format(TestDatasetObjectFactory.updatedDate);
    assertEquals(expectedCreatedDate, jsonNode.get("createdDate").asText());
    assertEquals(expectedUpdatedDate, jsonNode.get("updatedDate").asText());
    assertEquals(2, jsonNode.get("datasetIdsToRedirectFrom").size());
    assertTrue(jsonNode.get("datasetIdsToRedirectFrom").toString().contains("redirectId1"));
    assertTrue(jsonNode.get("datasetIdsToRedirectFrom").toString().contains("redirectId2"));
    assertEquals("replacedBy", jsonNode.get("replacedBy").asText());
    assertEquals("replaces", jsonNode.get("replaces").asText());
    assertEquals("GREECE", jsonNode.get("country").get("enum").asText());
    assertEquals("EL", jsonNode.get("language").get("enum").asText());
    assertEquals("description", jsonNode.get("description").asText());
    assertEquals("FIT", jsonNode.get("publicationFitness").asText());
    assertEquals("notes", jsonNode.get("notes").asText());
    assertEquals(TestDatasetObjectFactory.xsltId.toString(), jsonNode.get("xsltId").asText());
  }

  private void assertDatasetDTO(DatasetDTO datasetDTO) {
    assertEquals(TestDatasetObjectFactory.id.toString(), datasetDTO.getId());
    assertEquals("ecloudDatasetId", datasetDTO.getEcloudDatasetId());
    assertEquals("datasetId", datasetDTO.getDatasetId());
    assertEquals("datasetName", datasetDTO.getDatasetName());
    assertEquals("organizationId", datasetDTO.getOrganizationId());
    assertEquals("organizationName", datasetDTO.getOrganizationName());
    assertEquals("provider", datasetDTO.getProvider());
    assertEquals("dataProvider", datasetDTO.getDataProvider());
    assertEquals("intermediateProvider", datasetDTO.getIntermediateProvider());
    assertEquals("createdByUserId", datasetDTO.getCreatedByUserId());
    assertEquals("createdByUserName", datasetDTO.getCreatedByUserName());
    assertEquals("createdByFirstName", datasetDTO.getCreatedByFirstName());
    assertEquals("createdByLastName", datasetDTO.getCreatedByLastName());
    assertEquals(TestDatasetObjectFactory.createdDate, datasetDTO.getCreatedDate());
    assertEquals(TestDatasetObjectFactory.updatedDate, datasetDTO.getUpdatedDate());
    assertEquals(2, datasetDTO.getDatasetIdsToRedirectFrom().size());
    assertTrue(datasetDTO.getDatasetIdsToRedirectFrom().contains("redirectId1"));
    assertEquals("replacedBy", datasetDTO.getReplacedBy());
    assertEquals("replaces", datasetDTO.getReplaces());
    assertEquals(Country.GREECE, datasetDTO.getCountry());
    assertEquals(Language.EL, datasetDTO.getLanguage());
    assertEquals("description", datasetDTO.getDescription());
    assertEquals(PublicationFitness.FIT, datasetDTO.getPublicationFitness());
    assertEquals("notes", datasetDTO.getNotes());
    assertEquals(TestDatasetObjectFactory.xsltId, datasetDTO.getXsltId());
  }

  private DatasetDTO getDatasetDTOUsingSetters() {
    DatasetDTO datasetDTO = TestDatasetObjectFactory.getDatasetDTO();
    DatasetDTO datasetDTO1 = new DatasetDTO();
    datasetDTO1.setId(datasetDTO.getId());
    datasetDTO1.setEcloudDatasetId(datasetDTO.getEcloudDatasetId());
    datasetDTO1.setDatasetId(datasetDTO.getDatasetId());
    datasetDTO1.setDatasetName(datasetDTO.getDatasetName());
    datasetDTO1.setOrganizationId(datasetDTO.getOrganizationId());
    datasetDTO1.setOrganizationName(datasetDTO.getOrganizationName());
    datasetDTO1.setProvider(datasetDTO.getProvider());
    datasetDTO1.setDataProvider(datasetDTO.getDataProvider());
    datasetDTO1.setIntermediateProvider(datasetDTO.getIntermediateProvider());
    datasetDTO1.setCreatedByUserId(datasetDTO.getCreatedByUserId());
    datasetDTO1.setCreatedByUserName(datasetDTO.getCreatedByUserName());
    datasetDTO1.setCreatedByFirstName(datasetDTO.getCreatedByFirstName());
    datasetDTO1.setCreatedByLastName(datasetDTO.getCreatedByLastName());
    datasetDTO1.setCreatedDate(datasetDTO.getCreatedDate());
    datasetDTO1.setUpdatedDate(datasetDTO.getUpdatedDate());
    datasetDTO1.setDatasetIdsToRedirectFrom(datasetDTO.getDatasetIdsToRedirectFrom());
    datasetDTO1.setReplacedBy(datasetDTO.getReplacedBy());
    datasetDTO1.setReplaces(datasetDTO.getReplaces());
    datasetDTO1.setCountry(datasetDTO.getCountry());
    datasetDTO1.setLanguage(datasetDTO.getLanguage());
    datasetDTO1.setDescription(datasetDTO.getDescription());
    datasetDTO1.setPublicationFitness(datasetDTO.getPublicationFitness());
    datasetDTO1.setNotes(datasetDTO.getNotes());
    datasetDTO1.setXsltId(datasetDTO.getXsltId());
    return datasetDTO1;
  }

  private DatasetDTO getDatasetDTOUsingSettersWithNullValues() {
    DatasetDTO datasetDTO = TestDatasetObjectFactory.getDatasetDTO();
    DatasetDTO datasetDTO1 = new DatasetDTO();
    datasetDTO1.setId(datasetDTO.getId());
    datasetDTO1.setEcloudDatasetId(datasetDTO.getEcloudDatasetId());
    datasetDTO1.setDatasetId(datasetDTO.getDatasetId());
    datasetDTO1.setDatasetName(datasetDTO.getDatasetName());
    datasetDTO1.setOrganizationId(datasetDTO.getOrganizationId());
    datasetDTO1.setOrganizationName(datasetDTO.getOrganizationName());
    datasetDTO1.setProvider(datasetDTO.getProvider());
    datasetDTO1.setDataProvider(datasetDTO.getDataProvider());
    datasetDTO1.setIntermediateProvider(datasetDTO.getIntermediateProvider());
    datasetDTO1.setCreatedByUserId(datasetDTO.getCreatedByUserId());
    datasetDTO1.setCreatedByUserName(datasetDTO.getCreatedByUserName());
    datasetDTO1.setCreatedByFirstName(datasetDTO.getCreatedByFirstName());
    datasetDTO1.setCreatedByLastName(datasetDTO.getCreatedByLastName());
    datasetDTO1.setCreatedDate(null);
    datasetDTO1.setUpdatedDate(null);
    datasetDTO1.setDatasetIdsToRedirectFrom(null);
    datasetDTO1.setReplacedBy(datasetDTO.getReplacedBy());
    datasetDTO1.setReplaces(datasetDTO.getReplaces());
    datasetDTO1.setCountry(datasetDTO.getCountry());
    datasetDTO1.setLanguage(datasetDTO.getLanguage());
    datasetDTO1.setDescription(datasetDTO.getDescription());
    datasetDTO1.setPublicationFitness(datasetDTO.getPublicationFitness());
    datasetDTO1.setNotes(datasetDTO.getNotes());
    datasetDTO1.setXsltId(datasetDTO.getXsltId());
    return datasetDTO1;
  }

  private DatasetDTO getDatasetDTOWithNullValues() {
    DatasetDTO datasetDTO = TestDatasetObjectFactory.getDatasetDTO();
    return new DatasetDTO(
        datasetDTO.getId(),
        datasetDTO.getEcloudDatasetId(),
        datasetDTO.getDatasetId(),
        datasetDTO.getDatasetName(),
        datasetDTO.getOrganizationId(),
        datasetDTO.getOrganizationName(),
        datasetDTO.getProvider(),
        datasetDTO.getDataProvider(),
        datasetDTO.getIntermediateProvider(),
        datasetDTO.getCreatedByUserId(),
        datasetDTO.getCreatedByUserName(),
        datasetDTO.getCreatedByFirstName(),
        datasetDTO.getCreatedByLastName(),
        null,
        null,
        null,
        datasetDTO.getReplacedBy(),
        datasetDTO.getReplaces(),
        datasetDTO.getCountry(),
        datasetDTO.getLanguage(),
        datasetDTO.getDescription(),
        datasetDTO.getPublicationFitness(),
        datasetDTO.getNotes(),
        datasetDTO.getXsltId());
  }
}
