package eu.europeana.metis.core.workflow.execution;

import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.CANCELLED_BY;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.CANCELLED_BY_FIRST_NAME;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.CANCELLED_BY_LAST_NAME;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.CANCELLED_BY_USER_NAME;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.CANCELLING;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.CANCELLING_VALUE;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.CREATED_DATE;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.CREATED_DATE_VALUE;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.DATASET_ID;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.ECLOUD_DATASET_ID;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.FINISHED_DATE;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.FINISHED_DATE_VALUE;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.ID;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.IS_INCREMENTAL;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.IS_INCREMENTAL_VALUE;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.METIS_PLUGINS;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.OBJECT_ID_VALUE;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.PLUGIN_TYPE;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.PLUGIN_TYPE_1_VALUE;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.PLUGIN_TYPE_2_VALUE;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.STARTED_BY;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.STARTED_BY_FIRST_NAME;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.STARTED_BY_LAST_NAME;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.STARTED_BY_USER_NAME;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.STARTED_DATE;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.STARTED_DATE_VALUE;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.UPDATED_DATE;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.UPDATED_DATE_VALUE;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.WORKFLOW_STATUS;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.WORKFLOW_STATUS_VALUE;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.getWorkflowExecutionDTOUsingSetters;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.getWorkflowExecutionDTOUsingSettersWithNullValues;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.europeana.metis.core.common.TestSerializationUtils;
import eu.europeana.metis.core.workflow.WorkflowStatus;
import java.io.File;
import java.net.URL;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class TestWorkflowExecutionDTO {

  @Test
  void testGetters() {
    WorkflowExecutionDTO workflowExecutionDTO = getWorkflowExecutionDTOUsingSetters();
    assertWorkflowExecutionDTO(workflowExecutionDTO);
  }

  @Test
  void testNullProvidedValues() {
    WorkflowExecutionDTO workflowExecutionDTO = getWorkflowExecutionDTOUsingSettersWithNullValues();
    assertNull(workflowExecutionDTO.getCreatedDate());
    assertNull(workflowExecutionDTO.getStartedDate());
    assertNull(workflowExecutionDTO.getUpdatedDate());
    assertNull(workflowExecutionDTO.getFinishedDate());
    assertTrue(workflowExecutionDTO.getMetisPlugins().isEmpty());
  }

  @Test
  void testSerialization() {
    WorkflowExecutionDTO workflowExecutionDTO = getWorkflowExecutionDTOUsingSetters();

    ObjectMapper objectMapper = JsonMapper.builder()
                                          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                                          .build();
    String jsonOutput = objectMapper.writeValueAsString(workflowExecutionDTO);

    assertWorkflowExecutionDTO(jsonOutput);
  }

  @Test
  void testDeserialization() {
    ObjectMapper objectMapper = JsonMapper.builder()
                                          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                                          .build();
    //TODO: 2025-03-11 - MET-6427 - This is configured because some fields that can be serialized cannot be deserialized(see field
    // "executablePluginType" in the json file example) with the current implementation. This is not a functionality needed at
    // the moment since we do not deserialize a WorkflowExecutionDTO received from the controller.
    // To fix this, some refactoring is required and it needs to be carefully performed so that the morphia/mongo communication
    // won't start failing. There needs to be abstraction of the two entity model and DTO.
    URL resource = getClass().getClassLoader().getResource("workflowExecutionDTO.json");
    Objects.requireNonNull(resource);
    File jsonFile = new File(resource.getFile());
    WorkflowExecutionDTO deserializedWorkflowExecutionDTO = objectMapper.readValue(jsonFile, WorkflowExecutionDTO.class);

    assertNotNull(deserializedWorkflowExecutionDTO);
    assertWorkflowExecutionDTO(deserializedWorkflowExecutionDTO);
  }

  private void assertWorkflowExecutionDTO(String jsonOutput) {
    TestSerializationUtils.assertFieldEquals(jsonOutput, ID, OBJECT_ID_VALUE.toString());
    TestSerializationUtils.assertFieldEquals(jsonOutput, DATASET_ID, DATASET_ID);
    TestSerializationUtils.assertFieldEquals(jsonOutput, WORKFLOW_STATUS, WORKFLOW_STATUS_VALUE.name());
    TestSerializationUtils.assertFieldEquals(jsonOutput, ECLOUD_DATASET_ID, ECLOUD_DATASET_ID);
    TestSerializationUtils.assertFieldEquals(jsonOutput, CANCELLED_BY, CANCELLED_BY);
    TestSerializationUtils.assertFieldEquals(jsonOutput, CANCELLED_BY_USER_NAME, CANCELLED_BY_USER_NAME);
    TestSerializationUtils.assertFieldEquals(jsonOutput, CANCELLED_BY_FIRST_NAME, CANCELLED_BY_FIRST_NAME);
    TestSerializationUtils.assertFieldEquals(jsonOutput, CANCELLED_BY_LAST_NAME, CANCELLED_BY_LAST_NAME);
    TestSerializationUtils.assertFieldEquals(jsonOutput, STARTED_BY, STARTED_BY);
    TestSerializationUtils.assertFieldEquals(jsonOutput, STARTED_BY_USER_NAME, STARTED_BY_USER_NAME);
    TestSerializationUtils.assertFieldEquals(jsonOutput, STARTED_BY_FIRST_NAME, STARTED_BY_FIRST_NAME);
    TestSerializationUtils.assertFieldEquals(jsonOutput, STARTED_BY_LAST_NAME, STARTED_BY_LAST_NAME);
    TestSerializationUtils.assertFieldEquals(jsonOutput, CANCELLING, CANCELLING_VALUE);
    TestSerializationUtils.assertFieldEquals(jsonOutput, CREATED_DATE, CREATED_DATE_VALUE);
    TestSerializationUtils.assertFieldEquals(jsonOutput, STARTED_DATE, STARTED_DATE_VALUE);
    TestSerializationUtils.assertFieldEquals(jsonOutput, UPDATED_DATE, UPDATED_DATE_VALUE);
    TestSerializationUtils.assertFieldEquals(jsonOutput, FINISHED_DATE, FINISHED_DATE_VALUE);
    TestSerializationUtils.assertFieldEquals(jsonOutput, IS_INCREMENTAL, IS_INCREMENTAL_VALUE);
    TestSerializationUtils.assertNestedFieldInArrayEquals(jsonOutput, METIS_PLUGINS, PLUGIN_TYPE, PLUGIN_TYPE_1_VALUE.name());
    TestSerializationUtils.assertNestedFieldInArrayEquals(jsonOutput, METIS_PLUGINS, PLUGIN_TYPE, PLUGIN_TYPE_2_VALUE.name());
  }

  private void assertWorkflowExecutionDTO(WorkflowExecutionDTO workflowExecutionDTO) {
    assertEquals(OBJECT_ID_VALUE.toString(), workflowExecutionDTO.getId());
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
    assertFalse(workflowExecutionDTO.isCancelling());
    assertEquals(CREATED_DATE_VALUE, workflowExecutionDTO.getCreatedDate());
    assertEquals(STARTED_DATE_VALUE, workflowExecutionDTO.getStartedDate());
    assertEquals(UPDATED_DATE_VALUE, workflowExecutionDTO.getUpdatedDate());
    assertEquals(FINISHED_DATE_VALUE, workflowExecutionDTO.getFinishedDate());
    assertFalse(workflowExecutionDTO.isIncremental());
    assertEquals(2, workflowExecutionDTO.getMetisPlugins().size());
    assertTrue(workflowExecutionDTO.getMetisPlugins().stream().anyMatch(plugin -> plugin.getPluginType() == PLUGIN_TYPE_1_VALUE));
    assertTrue(workflowExecutionDTO.getMetisPlugins().stream().anyMatch(plugin -> plugin.getPluginType() == PLUGIN_TYPE_2_VALUE));
  }
}