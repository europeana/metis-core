package eu.europeana.metis.core.workflow.execution;

import eu.europeana.metis.core.user.User;
import eu.europeana.metis.core.workflow.WorkflowExecution;
import eu.europeana.metis.core.workflow.plugins.AbstractExecutablePlugin;
import eu.europeana.metis.core.workflow.plugins.AbstractMetisPlugin;
import eu.europeana.metis.core.workflow.plugins.DataStatus;
import eu.europeana.metis.core.workflow.plugins.ExecutablePluginFactory;
import eu.europeana.metis.core.workflow.plugins.LinkCheckingPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.MetisPlugin;
import eu.europeana.metis.core.workflow.plugins.OaipmhHarvestPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.ReindexToPreviewPlugin;
import eu.europeana.metis.core.workflow.plugins.ReindexToPreviewPluginMetadata;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.CANCELLED_BY;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.STARTED_BY;
import static eu.europeana.metis.core.workflow.execution.TestWorkflowExecutionUtils.OBJECT_ID_VALUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestWorkflowExecutionConverter {

  @Test
  void testToDTO() {
    WorkflowExecution workflowExecution = TestWorkflowExecutionUtils.getWorkflowExecutionUsingSetters();
    User userStarted = new User.UserBuilder()
        .userId(STARTED_BY).userName("startedByUserName").firstName("startedByFirstName")
        .lastName("startedByLastName").issuedAt(Instant.now()).build();

    User cancelledUser = new User.UserBuilder()
        .userId(CANCELLED_BY).userName("cancelledByUserName").firstName("cancelledByFirstName")
        .lastName("cancelledByLastName").issuedAt(Instant.now()).build();

    final boolean isIncremental = false;
    WorkflowExecutionDTO workflowExecutionDTO = WorkflowExecutionConverter.toDTO(workflowExecution, isIncremental, userStarted,
        cancelledUser);

    assertEquals(OBJECT_ID_VALUE.toString(), workflowExecutionDTO.getId());
    assertEquals(workflowExecution.getDatasetId(), workflowExecutionDTO.getDatasetId());
    assertEquals(workflowExecution.getWorkflowStatus(), workflowExecutionDTO.getWorkflowStatus());
    assertEquals(workflowExecution.getEcloudDatasetId(), workflowExecutionDTO.getEcloudDatasetId());
    assertEquals(workflowExecution.getCancelledBy(), workflowExecutionDTO.getCancelledBy());
    assertEquals(cancelledUser.getUserName(), workflowExecutionDTO.getCancelledByUserName());
    assertEquals(cancelledUser.getFirstName(), workflowExecutionDTO.getCancelledByFirstName());
    assertEquals(cancelledUser.getLastName(), workflowExecutionDTO.getCancelledByLastName());
    assertEquals(workflowExecution.getStartedBy(), workflowExecutionDTO.getStartedBy());
    assertEquals(userStarted.getUserName(), workflowExecutionDTO.getStartedByUserName());
    assertEquals(userStarted.getFirstName(), workflowExecutionDTO.getStartedByFirstName());
    assertEquals(userStarted.getLastName(), workflowExecutionDTO.getStartedByLastName());
    assertEquals(workflowExecution.getWorkflowPriority(), workflowExecutionDTO.getWorkflowPriority());
    assertEquals(workflowExecution.isCancelling(), workflowExecutionDTO.isCancelling());
    assertEquals(workflowExecution.getCreatedDate(), workflowExecutionDTO.getCreatedDate());
    assertEquals(workflowExecution.getStartedDate(), workflowExecutionDTO.getStartedDate());
    assertEquals(workflowExecution.getUpdatedDate(), workflowExecutionDTO.getUpdatedDate());
    assertEquals(workflowExecution.getFinishedDate(), workflowExecutionDTO.getFinishedDate());
    assertEquals(isIncremental, workflowExecutionDTO.isIncremental());
    assertMetisPluginsEqual(workflowExecution, workflowExecutionDTO);
  }

  @Test
  void testToDTO_NullWorkflowExecution() {
    WorkflowExecutionDTO workflowExecutionDTO = WorkflowExecutionConverter.toDTO(null, false, null, null);
    assertNull(workflowExecutionDTO);
  }

  @Test
  void testToDTO_WorkflowExecutionWithoutUsers() {
    WorkflowExecution workflowExecution = TestWorkflowExecutionUtils.getWorkflowExecutionUsingSetters();
    WorkflowExecutionDTO workflowExecutionDTO = WorkflowExecutionConverter.toDTO(workflowExecution, false, null, null);

    assertNotNull(workflowExecutionDTO.getCancelledBy());
    assertNull(workflowExecutionDTO.getCancelledByUserName());
    assertNull(workflowExecutionDTO.getCancelledByFirstName());
    assertNull(workflowExecutionDTO.getCancelledByLastName());
    assertNotNull(workflowExecutionDTO.getStartedBy());
    assertNull(workflowExecutionDTO.getStartedByUserName());
    assertNull(workflowExecutionDTO.getStartedByFirstName());
    assertNull(workflowExecutionDTO.getStartedByLastName());
  }

  @Test
  void testCanDisplayXml_WithNonExecutablePlugin() {
    ReindexToPreviewPlugin plugin = new ReindexToPreviewPlugin(new ReindexToPreviewPluginMetadata());
    assertFalse(WorkflowExecutionConverter.canDisplayRawXml(plugin));
  }

  @Test
  void testCanDisplayXml_WithDeletedDataStatus() {
    AbstractExecutablePlugin<?> plugin = ExecutablePluginFactory.createPlugin(new OaipmhHarvestPluginMetadata());
    plugin.setDataStatus(DataStatus.DELETED);
    assertFalse(WorkflowExecutionConverter.canDisplayRawXml(plugin));
  }

  @Test
  void testCanDisplayXml_WithDataStatus_WithBlackListedExecutablePlugin() {
    AbstractExecutablePlugin<?> plugin = ExecutablePluginFactory.createPlugin(new LinkCheckingPluginMetadata());
    plugin.setDataStatus(DataStatus.VALID);
    assertFalse(WorkflowExecutionConverter.canDisplayRawXml(plugin));
  }

  @Test
  void testCanDisplayXml_WithDataStatus_WithNonBlackListedExecutablePlugin_WithNullProgress() {
    AbstractExecutablePlugin<?> plugin = ExecutablePluginFactory.createPlugin(new OaipmhHarvestPluginMetadata());
    plugin.setDataStatus(DataStatus.VALID);
    plugin.setExecutionProgress(null);
    assertFalse(WorkflowExecutionConverter.canDisplayRawXml(plugin));
  }

  @Test
  void testCanDisplayXml_WithDataStatus_WithNonBlackListedExecutablePlugin_WithErrors() {
    AbstractExecutablePlugin<?> plugin = ExecutablePluginFactory.createPlugin(new OaipmhHarvestPluginMetadata());
    plugin.setDataStatus(DataStatus.VALID);
    plugin.getExecutionProgress().setProcessedRecords(0);
    plugin.getExecutionProgress().setErrors(1);
    assertFalse(WorkflowExecutionConverter.canDisplayRawXml(plugin));
  }

  private void assertMetisPluginsEqual(WorkflowExecution workflowExecution, WorkflowExecutionDTO workflowExecutionDTO) {
    List<AbstractMetisPlugin> originalPlugins = workflowExecution.getMetisPlugins();
    List<MetisPluginDTO> convertedPlugins = workflowExecutionDTO.getMetisPlugins();

    assertEquals(originalPlugins.size(), convertedPlugins.size());

    for (int i = 0; i < originalPlugins.size(); i++) {
      MetisPlugin metisPlugin = originalPlugins.get(i);
      MetisPluginDTO metisPluginDTO = convertedPlugins.get(i);
      assertEquals(metisPlugin.getPluginType(), metisPluginDTO.getPluginType());
      assertTrue(metisPluginDTO.isCanDisplayRawXml());
    }
  }
}