package eu.europeana.metis.core.workflow.execution;

import eu.europeana.metis.core.workflow.WorkflowExecution;
import eu.europeana.metis.core.workflow.WorkflowStatus;
import eu.europeana.metis.core.workflow.plugins.AbstractExecutablePlugin;
import eu.europeana.metis.core.workflow.plugins.DataStatus;
import eu.europeana.metis.core.workflow.plugins.ExecutablePluginFactory;
import eu.europeana.metis.core.workflow.plugins.OaipmhHarvestPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.PluginType;
import eu.europeana.metis.core.workflow.plugins.ValidationExternalPluginMetadata;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import org.bson.types.ObjectId;

public class TestWorkflowExecutionUtils {

  private static final ZonedDateTime zonedDateTime = ZonedDateTime.parse("2025-03-10T10:10:10.000Z[UTC]");
  //FIELDS
  public static final String ID = "id";
  public static final String DATASET_ID = "datasetId";
  public static final String WORKFLOW_STATUS = "workflowStatus";
  public static final String ECLOUD_DATASET_ID = "ecloudDatasetId";
  public static final String CANCELLED_BY = "cancelledBy";
  public static final String CANCELLED_BY_USER_NAME = "cancelledByUserName";
  public static final String CANCELLED_BY_FIRST_NAME = "cancelledByFirstName";
  public static final String CANCELLED_BY_LAST_NAME = "cancelledByLastName";
  public static final String STARTED_BY = "startedBy";
  public static final String STARTED_BY_USER_NAME = "startedByUserName";
  public static final String STARTED_BY_FIRST_NAME = "startedByFirstName";
  public static final String STARTED_BY_LAST_NAME = "startedByLastName";
  public static final String WORKFLOW_PRIORIOTY = "workflowPriority";
  public static final String CANCELLING = "cancelling";
  public static final String CREATED_DATE = "createdDate";
  public static final String STARTED_DATE = "startedDate";
  public static final String UPDATED_DATE = "updatedDate";
  public static final String FINISHED_DATE = "finishedDate";
  public static final String IS_INCREMENTAL = "isIncremental";
  public static final String METIS_PLUGINS = "metisPlugins";
  public static final String PLUGIN_TYPE = "pluginType";
  //VALUES
  public static final ObjectId OBJECT_ID_VALUE = new ObjectId("64bffcdde13e6c25d4efb2ac");
  public static final Date CREATED_DATE_VALUE = Date.from(zonedDateTime.toInstant());
  public static final Date UPDATED_DATE_VALUE = Date.from(zonedDateTime.toInstant());
  public static final Date STARTED_DATE_VALUE = Date.from(zonedDateTime.toInstant());
  public static final Date FINISHED_DATE_VALUE = Date.from(zonedDateTime.toInstant());
  public static final PluginType PLUGIN_TYPE_1_VALUE = PluginType.OAIPMH_HARVEST;
  public static final PluginType PLUGIN_TYPE_2_VALUE = PluginType.VALIDATION_EXTERNAL;

  static WorkflowExecutionDTO getWorkflowExecutionDTOUsingSetters() {
    WorkflowExecutionDTO workflowExecutionDTO = new WorkflowExecutionDTO();
    workflowExecutionDTO.setId(OBJECT_ID_VALUE.toString());
    workflowExecutionDTO.setDatasetId(DATASET_ID);
    workflowExecutionDTO.setWorkflowStatus(WorkflowStatus.RUNNING);
    workflowExecutionDTO.setEcloudDatasetId(ECLOUD_DATASET_ID);
    workflowExecutionDTO.setCancelledBy(CANCELLED_BY);
    workflowExecutionDTO.setCancelledByUserName(CANCELLED_BY_USER_NAME);
    workflowExecutionDTO.setCancelledByFirstName(CANCELLED_BY_FIRST_NAME);
    workflowExecutionDTO.setCancelledByLastName(CANCELLED_BY_LAST_NAME);
    workflowExecutionDTO.setStartedBy(STARTED_BY);
    workflowExecutionDTO.setStartedByUserName(STARTED_BY_USER_NAME);
    workflowExecutionDTO.setStartedByFirstName(STARTED_BY_FIRST_NAME);
    workflowExecutionDTO.setStartedByLastName(STARTED_BY_LAST_NAME);
    workflowExecutionDTO.setWorkflowPriority(0);
    workflowExecutionDTO.setCancelling(false);
    workflowExecutionDTO.setCreatedDate(CREATED_DATE_VALUE);
    workflowExecutionDTO.setStartedDate(STARTED_DATE_VALUE);
    workflowExecutionDTO.setUpdatedDate(UPDATED_DATE_VALUE);
    workflowExecutionDTO.setFinishedDate(FINISHED_DATE_VALUE);
    workflowExecutionDTO.setIncremental(false);

    MetisPluginDTO metisPluginDTO1 = MetisPluginConverter.toDTO(ExecutablePluginFactory.createPlugin(new OaipmhHarvestPluginMetadata()), true);
    MetisPluginDTO metisPluginDTO2 = MetisPluginConverter.toDTO(ExecutablePluginFactory.createPlugin(new ValidationExternalPluginMetadata()), true);
    workflowExecutionDTO.setMetisPlugins(List.of(metisPluginDTO1, metisPluginDTO2));
    return workflowExecutionDTO;
  }

  static WorkflowExecutionDTO getWorkflowExecutionDTOUsingSettersWithNullValues() {
    WorkflowExecutionDTO workflowExecutionDTO = getWorkflowExecutionDTOUsingSetters();
    workflowExecutionDTO.setCreatedDate(null);
    workflowExecutionDTO.setStartedDate(null);
    workflowExecutionDTO.setUpdatedDate(null);
    workflowExecutionDTO.setFinishedDate(null);
    workflowExecutionDTO.setMetisPlugins(null);
    return workflowExecutionDTO;
  }

  public static WorkflowExecution getWorkflowExecutionUsingSetters() {
    WorkflowExecution workflowExecution = new WorkflowExecution();
    workflowExecution.setId(OBJECT_ID_VALUE);
    workflowExecution.setDatasetId(DATASET_ID);
    workflowExecution.setWorkflowStatus(WorkflowStatus.RUNNING);
    workflowExecution.setEcloudDatasetId(ECLOUD_DATASET_ID);
    workflowExecution.setCancelledBy(CANCELLED_BY);
    workflowExecution.setStartedBy(STARTED_BY);
    workflowExecution.setWorkflowPriority(0);
    workflowExecution.setCancelling(false);
    workflowExecution.setCreatedDate(CREATED_DATE_VALUE);
    workflowExecution.setStartedDate(STARTED_DATE_VALUE);
    workflowExecution.setUpdatedDate(UPDATED_DATE_VALUE);
    workflowExecution.setFinishedDate(FINISHED_DATE_VALUE);
    AbstractExecutablePlugin<?> plugin1 = ExecutablePluginFactory.createPlugin(new OaipmhHarvestPluginMetadata());
    plugin1.setDataStatus(DataStatus.VALID);
    plugin1.getExecutionProgress().setProcessedRecords(1);
    AbstractExecutablePlugin<?> plugin2 = ExecutablePluginFactory.createPlugin(new ValidationExternalPluginMetadata());
    plugin2.setDataStatus(DataStatus.VALID);
    plugin2.getExecutionProgress().setProcessedRecords(1);
    workflowExecution.setMetisPlugins(List.of(plugin1, plugin2));
    return workflowExecution;
  }

  public static WorkflowExecution getWorkflowExecutionUsingSettersWithNullValues() {
    WorkflowExecution workflowExecution = getWorkflowExecutionUsingSetters();
    workflowExecution.setCreatedDate(null);
    workflowExecution.setStartedDate(null);
    workflowExecution.setUpdatedDate(null);
    workflowExecution.setFinishedDate(null);
    workflowExecution.setMetisPlugins(null);
    return workflowExecution;
  }
}
