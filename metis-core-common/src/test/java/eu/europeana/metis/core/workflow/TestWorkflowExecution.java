package eu.europeana.metis.core.workflow;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.CANCELLING;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.CREATED_DATE;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.DATASET_ID;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.ECLOUD_DATASET_ID;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.FINISHED_DATE;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.ID;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.METIS_PLUGINS;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.PLUGIN_TYPE;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.PLUGIN_TYPE_1_VALUE;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.PLUGIN_TYPE_2_VALUE;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.STARTED_BY;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.STARTED_DATE;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.UPDATED_DATE;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.WORKFLOW_PRIORIOTY;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.WORKFLOW_STATUS;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.createdDate;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.finishedDate;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.getWorkflowExecutionUsingSetters;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.getWorkflowExecutionUsingSettersWithNullValues;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.id;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.startedDate;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.updatedDate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestWorkflowExecution {

  @Test
  void testGetters() {
    WorkflowExecution workflowExecution = getWorkflowExecutionUsingSetters();
    assertWorkflowExecution(workflowExecution);
  }

  @Test
  void testNullProvidedValues() {
    WorkflowExecution workflowExecution = getWorkflowExecutionUsingSettersWithNullValues();
    assertNull(workflowExecution.getCreatedDate());
    assertNull(workflowExecution.getStartedDate());
    assertNull(workflowExecution.getUpdatedDate());
    assertNull(workflowExecution.getFinishedDate());
    assertTrue(workflowExecution.getMetisPlugins().isEmpty());
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
    URL resource = getClass().getClassLoader().getResource("workflowExecution.json");
    Objects.requireNonNull(resource);
    File jsonFile = new File(resource.getFile());
    WorkflowExecution deserializedWorkflowExecution = objectMapper.readValue(jsonFile, WorkflowExecution.class);

    assertNotNull(deserializedWorkflowExecution);
    assertWorkflowExecution(deserializedWorkflowExecution);
  }

  @Test
  void testSerialization() throws IOException {
    WorkflowExecution workflowExecution = getWorkflowExecutionUsingSetters();

    ObjectMapper objectMapper = new ObjectMapper();
    String jsonOutput = objectMapper.writeValueAsString(workflowExecution);
    JsonNode jsonNode = objectMapper.readTree(jsonOutput);

    assertWorkflowExecutionDTO(jsonNode);
  }

  private void assertWorkflowExecutionDTO(JsonNode jsonNode) {
    assertFieldEquals(jsonNode, ID, id.toString());
    assertFieldEquals(jsonNode, DATASET_ID, DATASET_ID);
    assertFieldEquals(jsonNode, WORKFLOW_STATUS, WorkflowStatus.RUNNING.name());
    assertFieldEquals(jsonNode, ECLOUD_DATASET_ID, ECLOUD_DATASET_ID);
    assertFieldEquals(jsonNode, CANCELLED_BY, CANCELLED_BY);
    assertFieldEquals(jsonNode, STARTED_BY, STARTED_BY);
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
    assertNestedFieldInArrayEquals(jsonNode, METIS_PLUGINS, PLUGIN_TYPE, PLUGIN_TYPE_1_VALUE.name());
    assertNestedFieldInArrayEquals(jsonNode, METIS_PLUGINS, PLUGIN_TYPE, PLUGIN_TYPE_2_VALUE.name());
  }

  private void assertWorkflowExecution(WorkflowExecution workflowExecution) {
    assertEquals(id, workflowExecution.getId());
    assertEquals(DATASET_ID, workflowExecution.getDatasetId());
    assertEquals(WorkflowStatus.RUNNING, workflowExecution.getWorkflowStatus());
    assertEquals(ECLOUD_DATASET_ID, workflowExecution.getEcloudDatasetId());
    assertEquals(CANCELLED_BY, workflowExecution.getCancelledBy());
    assertEquals(STARTED_BY, workflowExecution.getStartedBy());
    assertEquals(0, workflowExecution.getWorkflowPriority());
    assertFalse(workflowExecution.isCancelling());
    assertEquals(createdDate, workflowExecution.getCreatedDate());
    assertEquals(startedDate, workflowExecution.getStartedDate());
    assertEquals(updatedDate, workflowExecution.getUpdatedDate());
    assertEquals(finishedDate, workflowExecution.getFinishedDate());
    assertEquals(2, workflowExecution.getMetisPlugins().size());
    assertTrue(workflowExecution.getMetisPlugins().stream().anyMatch(plugin -> plugin.getPluginType() == PLUGIN_TYPE_1_VALUE));
    assertTrue(workflowExecution.getMetisPlugins().stream().anyMatch(plugin -> plugin.getPluginType() == PLUGIN_TYPE_2_VALUE));
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