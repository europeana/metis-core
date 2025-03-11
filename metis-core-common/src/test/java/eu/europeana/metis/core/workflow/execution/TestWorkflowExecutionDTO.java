package eu.europeana.metis.core.workflow.execution;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europeana.metis.core.workflow.WorkflowStatus;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;

import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.CANCELLED_BY;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.CANCELLED_BY_FIRST_NAME;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.CANCELLED_BY_LAST_NAME;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.CANCELLED_BY_USER_NAME;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.CANCELLING;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.CREATED_DATE;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.DATASET_ID;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.ECLOUD_DATASET_ID;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.FINISHED_DATE;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.ID;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.IS_INCREMENTAL;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.METIS_PLUGINS;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.PLUGIN_TYPE;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.PLUGIN_TYPE_1_VALUE;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.PLUGIN_TYPE_2_VALUE;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.STARTED_BY;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.STARTED_BY_FIRST_NAME;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.STARTED_BY_LAST_NAME;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.STARTED_BY_USER_NAME;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.STARTED_DATE;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.UPDATED_DATE;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.WORKFLOW_PRIORIOTY;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.WORKFLOW_STATUS;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.createdDate;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.finishedDate;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.getDatasetDTOUsingSetters;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.getDatasetDTOUsingSettersWithNullValues;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.id;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.startedDate;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.updatedDate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestWorkflowExecutionDTO {

  @Test
  void testGetters() {
    WorkflowExecutionDTO workflowExecutionDTO = getDatasetDTOUsingSetters();
    assertWorkflowExecutionDTO(workflowExecutionDTO);
  }

  @Test
  void testNullProvidedValues() {
    WorkflowExecutionDTO workflowExecutionDTO = getDatasetDTOUsingSettersWithNullValues();
    assertNull(workflowExecutionDTO.getCreatedDate());
    assertNull(workflowExecutionDTO.getStartedDate());
    assertNull(workflowExecutionDTO.getUpdatedDate());
    assertNull(workflowExecutionDTO.getFinishedDate());
    assertTrue(workflowExecutionDTO.getMetisPlugins().isEmpty());
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
    URL resource = getClass().getClassLoader().getResource("workflowExecutionDTO.json");
    Objects.requireNonNull(resource);
    File jsonFile = new File(resource.getFile());
    WorkflowExecutionDTO deserializedWorkflowExecutionDTO = objectMapper.readValue(jsonFile, WorkflowExecutionDTO.class);

    assertNotNull(deserializedWorkflowExecutionDTO);
    assertWorkflowExecutionDTO(deserializedWorkflowExecutionDTO);
  }

  @Test
  void testSerialization() throws IOException {
    WorkflowExecutionDTO workflowExecutionDTO = getDatasetDTOUsingSetters();

    ObjectMapper objectMapper = new ObjectMapper();
    String jsonOutput = objectMapper.writeValueAsString(workflowExecutionDTO);
    JsonNode jsonNode = objectMapper.readTree(jsonOutput);

    assertWorkflowExecutionDTO(jsonNode);
  }

  private void assertWorkflowExecutionDTO(JsonNode jsonNode) {
    assertFieldEquals(jsonNode, ID, id.toString());
    assertFieldEquals(jsonNode, DATASET_ID, DATASET_ID);
    assertFieldEquals(jsonNode, WORKFLOW_STATUS, WorkflowStatus.RUNNING.name());
    assertFieldEquals(jsonNode, ECLOUD_DATASET_ID, ECLOUD_DATASET_ID);
    assertFieldEquals(jsonNode, CANCELLED_BY, CANCELLED_BY);
    assertFieldEquals(jsonNode, CANCELLED_BY_USER_NAME, CANCELLED_BY_USER_NAME);
    assertFieldEquals(jsonNode, CANCELLED_BY_FIRST_NAME, CANCELLED_BY_FIRST_NAME);
    assertFieldEquals(jsonNode, CANCELLED_BY_LAST_NAME, CANCELLED_BY_LAST_NAME);
    assertFieldEquals(jsonNode, STARTED_BY, STARTED_BY);
    assertFieldEquals(jsonNode, STARTED_BY_USER_NAME, STARTED_BY_USER_NAME);
    assertFieldEquals(jsonNode, STARTED_BY_FIRST_NAME, STARTED_BY_FIRST_NAME);
    assertFieldEquals(jsonNode, STARTED_BY_LAST_NAME, STARTED_BY_LAST_NAME);
    assertFieldEquals(jsonNode, WORKFLOW_PRIORIOTY, String.valueOf(0));
    assertFieldEquals(jsonNode, CANCELLING, String.valueOf(false));
    String expectedCreatedDate = formatAsUTC(createdDate);
    String expectedStartedDate = formatAsUTC(startedDate);
    String expectedUpdatedDate = formatAsUTC(updatedDate);
    String expectedFinishedDate = formatAsUTC(finishedDate);
    assertFieldEquals(jsonNode, CREATED_DATE, expectedCreatedDate);
    assertFieldEquals(jsonNode, STARTED_DATE, expectedStartedDate);
    assertFieldEquals(jsonNode, UPDATED_DATE, expectedUpdatedDate);
    assertFieldEquals(jsonNode, FINISHED_DATE, expectedFinishedDate);
    assertFieldEquals(jsonNode, IS_INCREMENTAL, String.valueOf(false));
    assertNestedFieldInArrayEquals(jsonNode, METIS_PLUGINS, PLUGIN_TYPE, PLUGIN_TYPE_1_VALUE.name());
    assertNestedFieldInArrayEquals(jsonNode, METIS_PLUGINS, PLUGIN_TYPE, PLUGIN_TYPE_2_VALUE.name());
  }

  private void assertWorkflowExecutionDTO(WorkflowExecutionDTO workflowExecutionDTO) {
    assertEquals(id.toString(), workflowExecutionDTO.getId());
    assertEquals(DATASET_ID, workflowExecutionDTO.getDatasetId());
    assertEquals(WorkflowStatus.RUNNING, workflowExecutionDTO.getWorkflowStatus());
    assertEquals(ECLOUD_DATASET_ID, workflowExecutionDTO.getEcloudDatasetId());
    assertEquals(CANCELLED_BY, workflowExecutionDTO.getCancelledBy());
    assertEquals(CANCELLED_BY_USER_NAME, workflowExecutionDTO.getCancelledByUserName());
    assertEquals(CANCELLED_BY_FIRST_NAME, workflowExecutionDTO.getCancelledByFirstName());
    assertEquals(CANCELLED_BY_LAST_NAME, workflowExecutionDTO.getCancelledByLastName());
    assertEquals(STARTED_BY, workflowExecutionDTO.getStartedBy());
    assertEquals(STARTED_BY_USER_NAME, workflowExecutionDTO.getStartedByUserName());
    assertEquals(STARTED_BY_FIRST_NAME, workflowExecutionDTO.getStartedByFirstName());
    assertEquals(STARTED_BY_LAST_NAME, workflowExecutionDTO.getStartedByLastName());
    assertEquals(0, workflowExecutionDTO.getWorkflowPriority());
    assertFalse(workflowExecutionDTO.isCancelling());
    assertEquals(createdDate, workflowExecutionDTO.getCreatedDate());
    assertEquals(startedDate, workflowExecutionDTO.getStartedDate());
    assertEquals(updatedDate, workflowExecutionDTO.getUpdatedDate());
    assertEquals(finishedDate, workflowExecutionDTO.getFinishedDate());
    assertFalse(workflowExecutionDTO.isIncremental());
    assertEquals(2, workflowExecutionDTO.getMetisPlugins().size());
    assertTrue(workflowExecutionDTO.getMetisPlugins().stream().anyMatch(plugin -> plugin.getPluginType() == PLUGIN_TYPE_1_VALUE));
    assertTrue(workflowExecutionDTO.getMetisPlugins().stream().anyMatch(plugin -> plugin.getPluginType() == PLUGIN_TYPE_2_VALUE));
  }

  static void assertFieldEquals(JsonNode jsonNode, String fieldName, String expectedValue) {
    assertEquals(expectedValue, jsonNode.get(fieldName).asText());
  }

  static void assertNestedFieldInArrayEquals(JsonNode jsonNode, String parentField, String nestedField, String expectedValue) {
    JsonNode parentArray = jsonNode.get(parentField);
    List<String> nestedValues = new ArrayList<>();
    for (JsonNode arrayElement : parentArray) {
      if (arrayElement.has(nestedField)) {
        nestedValues.add(arrayElement.get(nestedField).asText());
      }
    }
    assertTrue(nestedValues.contains(expectedValue));
  }

  static String formatAsUTC(Date date) {
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    return simpleDateFormat.format(date);
  }
}