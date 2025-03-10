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
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestDatasetDTO {

  private DatasetDTO datasetDTO;
  private Date createdDate;
  private Date updatedDate;
  private ObjectId xsltId;

  @BeforeEach
  void setUp() {
    ZonedDateTime zonedDateTime = ZonedDateTime.now(ZoneOffset.UTC);
    createdDate = Date.from(zonedDateTime.toInstant());
    updatedDate = Date.from(zonedDateTime.toInstant());
    xsltId = new ObjectId("507f191e810c19729de860ea");

    datasetDTO = new DatasetDTO(
        "123",
        "ecloudDatasetId",
        "datasetId",
        "datasetName",
        "organizationId",
        "organizationName",
        "provider",
        "dataProvider",
        "intermediateProvider",
        "createdByUserId",
        "createdByUserName",
        "createdByFirstName",
        "createdByLastName",
        createdDate,
        updatedDate,
        List.of("redirectId1", "redirectId2"),
        "replacedBy",
        "replaces",
        Country.GREECE,
        Language.EL,
        "description",
        PublicationFitness.FIT,
        "notes",
        xsltId
    );
  }

  @Test
  void testGetters() {
    assertEquals("123", datasetDTO.getId());
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
    assertEquals(createdDate, datasetDTO.getCreatedDate());
    assertEquals(updatedDate, datasetDTO.getUpdatedDate());
    assertEquals(2, datasetDTO.getDatasetIdsToRedirectFrom().size());
    assertTrue(datasetDTO.getDatasetIdsToRedirectFrom().contains("redirectId1"));
    assertEquals("replacedBy", datasetDTO.getReplacedBy());
    assertEquals("replaces", datasetDTO.getReplaces());
    assertEquals(Country.GREECE, datasetDTO.getCountry());
    assertEquals(Language.EL, datasetDTO.getLanguage());
    assertEquals("description", datasetDTO.getDescription());
    assertEquals(PublicationFitness.FIT, datasetDTO.getPublicationFitness());
    assertEquals("notes", datasetDTO.getNotes());
    assertEquals(xsltId, datasetDTO.getXsltId());
  }

  @Test
  void testSetters() {
    DatasetDTO datasetDTO1 = copyDatasetDTO(datasetDTO);

    assertEquals(datasetDTO1.getId(), datasetDTO.getId());
    assertEquals(datasetDTO1.getEcloudDatasetId(), datasetDTO.getEcloudDatasetId());
    assertEquals(datasetDTO1.getDatasetId(), datasetDTO.getDatasetId());
    assertEquals(datasetDTO1.getDatasetName(), datasetDTO.getDatasetName());
    assertEquals(datasetDTO1.getOrganizationId(), datasetDTO.getOrganizationId());
    assertEquals(datasetDTO1.getOrganizationName(), datasetDTO.getOrganizationName());
    assertEquals(datasetDTO1.getProvider(), datasetDTO.getProvider());
    assertEquals(datasetDTO1.getDataProvider(), datasetDTO.getDataProvider());
    assertEquals(datasetDTO1.getIntermediateProvider(), datasetDTO.getIntermediateProvider());
    assertEquals(datasetDTO1.getCreatedByUserId(), datasetDTO.getCreatedByUserId());
    assertEquals(datasetDTO1.getCreatedByUserName(), datasetDTO.getCreatedByUserName());
    assertEquals(datasetDTO1.getCreatedByFirstName(), datasetDTO.getCreatedByFirstName());
    assertEquals(datasetDTO1.getCreatedByLastName(), datasetDTO.getCreatedByLastName());
    assertEquals(datasetDTO1.getCreatedDate(), datasetDTO.getCreatedDate());
    assertEquals(datasetDTO1.getUpdatedDate(), datasetDTO.getUpdatedDate());
    assertEquals(datasetDTO1.getDatasetIdsToRedirectFrom(), datasetDTO.getDatasetIdsToRedirectFrom());
    assertEquals(datasetDTO1.getReplacedBy(), datasetDTO.getReplacedBy());
    assertEquals(datasetDTO1.getReplaces(), datasetDTO.getReplaces());
    assertEquals(datasetDTO1.getCountry(), datasetDTO.getCountry());
    assertEquals(datasetDTO1.getLanguage(), datasetDTO.getLanguage());
    assertEquals(datasetDTO1.getDescription(), datasetDTO.getDescription());
    assertEquals(datasetDTO1.getPublicationFitness(), datasetDTO.getPublicationFitness());
    assertEquals(datasetDTO1.getNotes(), datasetDTO.getNotes());
    assertEquals(datasetDTO1.getXsltId(), datasetDTO.getXsltId());
  }

  private DatasetDTO copyDatasetDTO(DatasetDTO datasetDTO) {
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

  private DatasetDTO copyDatasetDTO(DatasetDTO datasetDTO, Date createdDate, Date updatedDate,
      List<String> datasetIdsToRedirectFrom) {
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
        createdDate,
        updatedDate,
        datasetIdsToRedirectFrom,
        datasetDTO.getReplacedBy(),
        datasetDTO.getReplaces(),
        datasetDTO.getCountry(),
        datasetDTO.getLanguage(),
        datasetDTO.getDescription(),
        datasetDTO.getPublicationFitness(),
        datasetDTO.getNotes(),
        datasetDTO.getXsltId());
  }

  @Test
  void testEmptyConstructor() {
    DatasetDTO emptyDatasetDTO = new DatasetDTO();
    assertNotNull(emptyDatasetDTO);
  }

  @Test
  void testNullProvidedValues() {
    DatasetDTO datasetDTO1 = copyDatasetDTO(datasetDTO);
    datasetDTO1.setCreatedDate(null);
    datasetDTO1.setUpdatedDate(null);
    datasetDTO1.setDatasetIdsToRedirectFrom(null);

    assertNull(datasetDTO1.getCreatedDate());
    assertNull(datasetDTO1.getUpdatedDate());
    assertNotNull(datasetDTO1.getDatasetIdsToRedirectFrom());

    DatasetDTO datasetDTO2 = copyDatasetDTO(datasetDTO, null, null, null);

    assertNull(datasetDTO2.getCreatedDate());
    assertNull(datasetDTO2.getUpdatedDate());
    assertNotNull(datasetDTO2.getDatasetIdsToRedirectFrom());
  }

  @Test
  void testDeserialization() throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    URL resource = getClass().getClassLoader().getResource("datasetDTO.json");
    Objects.requireNonNull(resource);
    File jsonFile = new File(resource.getFile());
    DatasetDTO deserializedDatasetDTO = objectMapper.readValue(jsonFile, DatasetDTO.class);

    assertNotNull(deserializedDatasetDTO);
    assertEquals("123", deserializedDatasetDTO.getId());
    assertEquals("ecloudDatasetId", deserializedDatasetDTO.getEcloudDatasetId());
    assertEquals("datasetId", deserializedDatasetDTO.getDatasetId());
    assertEquals("datasetName", deserializedDatasetDTO.getDatasetName());
    assertEquals("organizationId", deserializedDatasetDTO.getOrganizationId());
    assertEquals("organizationName", deserializedDatasetDTO.getOrganizationName());
    assertEquals("provider", deserializedDatasetDTO.getProvider());
    assertEquals("dataProvider", deserializedDatasetDTO.getDataProvider());
    assertEquals("intermediateProvider", deserializedDatasetDTO.getIntermediateProvider());
    assertEquals("createdByUserId", deserializedDatasetDTO.getCreatedByUserId());
    assertEquals("createdByUserName", deserializedDatasetDTO.getCreatedByUserName());
    assertEquals("createdByFirstName", deserializedDatasetDTO.getCreatedByFirstName());
    assertEquals("createdByLastName", deserializedDatasetDTO.getCreatedByLastName());
    assertEquals(createdDate, datasetDTO.getCreatedDate());
    assertEquals(updatedDate, datasetDTO.getUpdatedDate());
    assertEquals(2, deserializedDatasetDTO.getDatasetIdsToRedirectFrom().size());
    assertTrue(deserializedDatasetDTO.getDatasetIdsToRedirectFrom().contains("redirectId1"));
    assertEquals("replacedBy", deserializedDatasetDTO.getReplacedBy());
    assertEquals("replaces", deserializedDatasetDTO.getReplaces());
    assertEquals(Country.GREECE, deserializedDatasetDTO.getCountry());
    assertEquals(Language.EL, deserializedDatasetDTO.getLanguage());
    assertEquals("description", deserializedDatasetDTO.getDescription());
    assertEquals(PublicationFitness.FIT, deserializedDatasetDTO.getPublicationFitness());
    assertEquals("notes", deserializedDatasetDTO.getNotes());
    assertEquals(xsltId, deserializedDatasetDTO.getXsltId());
  }

  @Test
  void testSerialization() throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();

    String jsonOutput = objectMapper.writeValueAsString(datasetDTO);
    JsonNode jsonNode = objectMapper.readTree(jsonOutput);

    // Verify each field and its value
    assertEquals("123", jsonNode.get("id").asText());
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
    String expectedCreatedDate = simpleDateFormat.format(createdDate);
    String expectedUpdatedDate = simpleDateFormat.format(updatedDate);
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
    assertEquals(xsltId.toString(), jsonNode.get("xsltId").asText());
  }
}
