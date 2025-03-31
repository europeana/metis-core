package eu.europeana.metis.core.workflow;

import static eu.europeana.metis.core.common.TestSerializationUtils.assertFieldEquals;
import static eu.europeana.metis.core.common.TestSerializationUtils.assertNestedFieldInArrayEquals;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.CANCELLED_BY;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.CANCELLING;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.CANCELLING_VALUE;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.CREATED_DATE;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.CREATED_DATE_VALUE;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.DATASET_ID;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.ECLOUD_DATASET_ID;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.FINISHED_DATE;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.FINISHED_DATE_VALUE;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.ID;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.METIS_PLUGINS;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.OBJECT_ID_VALUE;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.PLUGIN_TYPE;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.PLUGIN_TYPE_1_VALUE;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.PLUGIN_TYPE_2_VALUE;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.STARTED_BY;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.STARTED_DATE;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.STARTED_DATE_VALUE;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.UPDATED_DATE;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.UPDATED_DATE_VALUE;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.WORKFLOW_STATUS;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.WORKFLOW_STATUS_VALUE;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.getWorkflowExecutionUsingSetters;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.getWorkflowExecutionUsingSettersWithNullValues;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import org.junit.jupiter.api.Test;

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
    //TODO: 2025-03-11 - MET-6427 - This is configured because some fields that can be serialized cannot be deserialized(see field
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

    assertWorkflowExecution(jsonOutput);
  }

  private void assertWorkflowExecution(String jsonOutput) {
    assertFieldEquals(jsonOutput, ID, OBJECT_ID_VALUE.toString());
    assertFieldEquals(jsonOutput, DATASET_ID, DATASET_ID);
    assertFieldEquals(jsonOutput, WORKFLOW_STATUS, WORKFLOW_STATUS_VALUE.name());
    assertFieldEquals(jsonOutput, ECLOUD_DATASET_ID, ECLOUD_DATASET_ID);
    assertFieldEquals(jsonOutput, CANCELLED_BY, CANCELLED_BY);
    assertFieldEquals(jsonOutput, STARTED_BY, STARTED_BY);
    assertFieldEquals(jsonOutput, CANCELLING, CANCELLING_VALUE);
    assertFieldEquals(jsonOutput, CREATED_DATE, CREATED_DATE_VALUE);
    assertFieldEquals(jsonOutput, STARTED_DATE, STARTED_DATE_VALUE);
    assertFieldEquals(jsonOutput, UPDATED_DATE, UPDATED_DATE_VALUE);
    assertFieldEquals(jsonOutput, FINISHED_DATE, FINISHED_DATE_VALUE);
    assertNestedFieldInArrayEquals(jsonOutput, METIS_PLUGINS, PLUGIN_TYPE, PLUGIN_TYPE_1_VALUE.name());
    assertNestedFieldInArrayEquals(jsonOutput, METIS_PLUGINS, PLUGIN_TYPE, PLUGIN_TYPE_2_VALUE.name());
  }

  private void assertWorkflowExecution(WorkflowExecution workflowExecution) {
    assertEquals(OBJECT_ID_VALUE, workflowExecution.getId());
    assertEquals(DATASET_ID, workflowExecution.getDatasetId());
    assertEquals(WorkflowStatus.RUNNING, workflowExecution.getWorkflowStatus());
    assertEquals(ECLOUD_DATASET_ID, workflowExecution.getEcloudDatasetId());
    assertEquals(CANCELLED_BY, workflowExecution.getCancelledBy());
    assertEquals(STARTED_BY, workflowExecution.getStartedBy());
    assertFalse(workflowExecution.isCancelling());
    assertEquals(CREATED_DATE_VALUE, workflowExecution.getCreatedDate());
    assertEquals(STARTED_DATE_VALUE, workflowExecution.getStartedDate());
    assertEquals(UPDATED_DATE_VALUE, workflowExecution.getUpdatedDate());
    assertEquals(FINISHED_DATE_VALUE, workflowExecution.getFinishedDate());
    assertEquals(2, workflowExecution.getMetisPlugins().size());
    assertTrue(workflowExecution.getMetisPlugins().stream().anyMatch(plugin -> plugin.getPluginType() == PLUGIN_TYPE_1_VALUE));
    assertTrue(workflowExecution.getMetisPlugins().stream().anyMatch(plugin -> plugin.getPluginType() == PLUGIN_TYPE_2_VALUE));
  }
}