package eu.europeana.metis.core.workflow.execution;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europeana.metis.core.common.TestSerializationUtils;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import org.junit.jupiter.api.Test;

import static eu.europeana.metis.core.workflow.execution.TestExecutionProgressDTO.assertExecutionProgressDTO;
import static eu.europeana.metis.core.workflow.execution.TestExecutionProgressUtils.EXPECTED_RECORDS;
import static eu.europeana.metis.core.workflow.execution.TestExecutionProgressUtils.EXPECTED_RECORDS_VALUE;
import static eu.europeana.metis.core.workflow.execution.TestMetisPluginUtils.CAN_DISPLAY_RAW_XML;
import static eu.europeana.metis.core.workflow.execution.TestMetisPluginUtils.CAN_DISPLAY_RAW_XML_VALUE;
import static eu.europeana.metis.core.workflow.execution.TestMetisPluginUtils.DATA_TATUS;
import static eu.europeana.metis.core.workflow.execution.TestMetisPluginUtils.DATA_TATUS_VALUE;
import static eu.europeana.metis.core.workflow.execution.TestMetisPluginUtils.EXECUTION_PROGRESS_DTO;
import static eu.europeana.metis.core.workflow.execution.TestMetisPluginUtils.EXTERNAL_TASK_ID;
import static eu.europeana.metis.core.workflow.execution.TestMetisPluginUtils.EXTERNAL_TASK_ID_VALUE;
import static eu.europeana.metis.core.workflow.execution.TestMetisPluginUtils.FAIL_MESSAGE;
import static eu.europeana.metis.core.workflow.execution.TestMetisPluginUtils.FAIL_MESSAGE_VALUE;
import static eu.europeana.metis.core.workflow.execution.TestMetisPluginUtils.FINISHED_DATE;
import static eu.europeana.metis.core.workflow.execution.TestMetisPluginUtils.FINISHED_DATE_VALUE;
import static eu.europeana.metis.core.workflow.execution.TestMetisPluginUtils.ID;
import static eu.europeana.metis.core.workflow.execution.TestMetisPluginUtils.METIS_PLUGIN_METADATA;
import static eu.europeana.metis.core.workflow.execution.TestMetisPluginUtils.METIS_PLUGIN_METADATA_VALUE;
import static eu.europeana.metis.core.workflow.execution.TestMetisPluginUtils.OBJECT_ID_VALUE;
import static eu.europeana.metis.core.workflow.execution.TestMetisPluginUtils.PLUGIN_STATUS;
import static eu.europeana.metis.core.workflow.execution.TestMetisPluginUtils.PLUGIN_STATUS_VALUE;
import static eu.europeana.metis.core.workflow.execution.TestMetisPluginUtils.PLUGIN_TYPE;
import static eu.europeana.metis.core.workflow.execution.TestMetisPluginUtils.PLUGIN_TYPE_VALUE;
import static eu.europeana.metis.core.workflow.execution.TestMetisPluginUtils.STARTED_DATE;
import static eu.europeana.metis.core.workflow.execution.TestMetisPluginUtils.STARTED_DATE_VALUE;
import static eu.europeana.metis.core.workflow.execution.TestMetisPluginUtils.TOPOLOGY_NAME;
import static eu.europeana.metis.core.workflow.execution.TestMetisPluginUtils.TOPOLOGY_NAME_VALUE;
import static eu.europeana.metis.core.workflow.execution.TestMetisPluginUtils.UPDATED_DATE;
import static eu.europeana.metis.core.workflow.execution.TestMetisPluginUtils.UPDATED_DATE_VALUE;
import static eu.europeana.metis.core.workflow.execution.TestMetisPluginUtils.getMetisPluginDTOUsingSetters;
import static eu.europeana.metis.core.workflow.execution.TestMetisPluginUtils.getMetisPluginDTOUsingSettersWithNullValues;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class TestMetisPluginDTO {

  @Test
  void testGetters() {
    MetisPluginDTO metisPluginDTO = getMetisPluginDTOUsingSetters();
    assertMetisPluginDTO(metisPluginDTO);
  }

  @Test
  void testNullProvidedValues() {
    MetisPluginDTO metisPluginDTO = getMetisPluginDTOUsingSettersWithNullValues();
    assertNull(metisPluginDTO.getStartedDate());
    assertNull(metisPluginDTO.getUpdatedDate());
    assertNull(metisPluginDTO.getFinishedDate());
  }

  @Test
  void testSerialization() throws IOException {
    MetisPluginDTO metisPluginDTO = getMetisPluginDTOUsingSetters();

    ObjectMapper objectMapper = new ObjectMapper();
    String jsonOutput = objectMapper.writeValueAsString(metisPluginDTO);

    assertMetisPluginDTO(jsonOutput);
  }

  @Test
  void testDeserialization() throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    //TODO: 2025-03-11 - This is configured because some fields that can be serialized cannot be deserialized(see field
    // "executablePluginType" in the json file example) with the current implementation. This is not a functionality needed at
    // the moment since we do not deserialize a WorkflowExecutionDTO received from the controller.
    // To fix this, some refactoring is required and it needs to be carefully performed so that the morphia/mongo communication
    // won't start failing. There needs to be abstraction of the two entity model and DTO.
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    URL resource = getClass().getClassLoader().getResource("metisPluginDTO.json");
    Objects.requireNonNull(resource);
    File jsonFile = new File(resource.getFile());
    MetisPluginDTO deserializedExecutionProgressDTO = objectMapper.readValue(jsonFile, MetisPluginDTO.class);

    assertNotNull(deserializedExecutionProgressDTO);
    assertMetisPluginDTO(deserializedExecutionProgressDTO);
  }

  private void assertMetisPluginDTO(MetisPluginDTO metisPluginDTO) {
    assertEquals(OBJECT_ID_VALUE.toString(), metisPluginDTO.getId());
    assertEquals(PLUGIN_TYPE_VALUE, metisPluginDTO.getPluginType());
    assertEquals(PLUGIN_STATUS_VALUE, metisPluginDTO.getPluginStatus());
    assertEquals(DATA_TATUS_VALUE, metisPluginDTO.getDataStatus());
    assertEquals(FAIL_MESSAGE_VALUE, metisPluginDTO.getFailMessage());
    assertEquals(STARTED_DATE_VALUE, metisPluginDTO.getStartedDate());
    assertEquals(UPDATED_DATE_VALUE, metisPluginDTO.getUpdatedDate());
    assertEquals(FINISHED_DATE_VALUE, metisPluginDTO.getFinishedDate());
    assertEquals(EXTERNAL_TASK_ID_VALUE, metisPluginDTO.getExternalTaskId());
    assertExecutionProgressDTO(metisPluginDTO.getExecutionProgress());
    assertEquals(TOPOLOGY_NAME_VALUE, metisPluginDTO.getTopologyName());
    assertEquals(CAN_DISPLAY_RAW_XML_VALUE, metisPluginDTO.isCanDisplayRawXml());
    assertEquals(METIS_PLUGIN_METADATA_VALUE.getPluginType(), metisPluginDTO.getPluginMetadata().getPluginType());
  }

  private void assertMetisPluginDTO(String jsonOutput) {
    TestSerializationUtils.assertFieldEquals(jsonOutput, ID, OBJECT_ID_VALUE.toString());
    TestSerializationUtils.assertFieldEquals(jsonOutput, PLUGIN_TYPE, PLUGIN_TYPE_VALUE.name());
    TestSerializationUtils.assertFieldEquals(jsonOutput, PLUGIN_STATUS, PLUGIN_STATUS_VALUE.name());
    TestSerializationUtils.assertFieldEquals(jsonOutput, DATA_TATUS, DATA_TATUS_VALUE.name());
    TestSerializationUtils.assertFieldEquals(jsonOutput, FAIL_MESSAGE, FAIL_MESSAGE_VALUE);
    TestSerializationUtils.assertFieldEquals(jsonOutput, STARTED_DATE, STARTED_DATE_VALUE);
    TestSerializationUtils.assertFieldEquals(jsonOutput, UPDATED_DATE, UPDATED_DATE_VALUE);
    TestSerializationUtils.assertFieldEquals(jsonOutput, FINISHED_DATE, FINISHED_DATE_VALUE);
    TestSerializationUtils.assertFieldEquals(jsonOutput, EXTERNAL_TASK_ID, EXTERNAL_TASK_ID_VALUE);
    TestSerializationUtils.assertNestedFieldEquals(jsonOutput, EXECUTION_PROGRESS_DTO, EXPECTED_RECORDS, EXPECTED_RECORDS_VALUE);
    TestSerializationUtils.assertFieldEquals(jsonOutput, TOPOLOGY_NAME, TOPOLOGY_NAME_VALUE);
    TestSerializationUtils.assertFieldEquals(jsonOutput, CAN_DISPLAY_RAW_XML, CAN_DISPLAY_RAW_XML_VALUE);
    TestSerializationUtils.assertNestedFieldEquals(jsonOutput, METIS_PLUGIN_METADATA, PLUGIN_TYPE,
        METIS_PLUGIN_METADATA_VALUE.getPluginType().name());
  }

}