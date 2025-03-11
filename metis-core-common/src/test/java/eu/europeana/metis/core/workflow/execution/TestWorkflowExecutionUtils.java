package eu.europeana.metis.core.workflow.execution;

import eu.europeana.metis.core.workflow.WorkflowStatus;
import eu.europeana.metis.core.workflow.plugins.ExecutablePluginFactory;
import eu.europeana.metis.core.workflow.plugins.OaipmhHarvestPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.PluginType;
import eu.europeana.metis.core.workflow.plugins.ValidationExternalPluginMetadata;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import org.bson.types.ObjectId;

public class TestWorkflowExecutionUtils {

  static final ObjectId id = new ObjectId("64bffcdde13e6c25d4efb2ac");
  private static final ZonedDateTime zonedDateTime = ZonedDateTime.parse("2025-03-10T10:10:10.000Z[UTC]");
  static final Date createdDate = Date.from(zonedDateTime.toInstant());
  static final Date updatedDate = Date.from(zonedDateTime.toInstant());
  static final Date startedDate = Date.from(zonedDateTime.toInstant());
  static final Date finishedDate = Date.from(zonedDateTime.toInstant());

  static final String ID = "id";
  static final String DATASET_ID = "datasetId";
  static final String WORKFLOW_STATUS = "workflowStatus";
  static final String ECLOUD_DATASET_ID = "ecloudDatasetId";
  static final String CANCELLED_BY = "cancelledBy";
  static final String CANCELLED_BY_USER_NAME = "cancelledByUserName";
  static final String CANCELLED_BY_FIRST_NAME = "cancelledByFirstName";
  static final String CANCELLED_BY_LAST_NAME = "cancelledByLastName";
  static final String STARTED_BY = "startedBy";
  static final String STARTED_BY_USER_NAME = "startedByUserName";
  static final String STARTED_BY_FIRST_NAME = "startedByFirstName";
  static final String STARTED_BY_LAST_NAME = "startedByLastName";
  static final String WORKFLOW_PRIORIOTY = "workflowPriority";
  static final String CANCELLING = "cancelling";
  static final String CREATED_DATE = "createdDate";
  static final String STARTED_DATE = "startedDate";
  static final String UPDATED_DATE = "updatedDate";
  static final String FINISHED_DATE = "finishedDate";
  static final String IS_INCREMENTAL = "isIncremental";
  static final String METIS_PLUGINS = "metisPlugins";
  static final String PLUGIN_TYPE = "pluginType";
  static final PluginType PLUGIN_TYPE_1_VALUE = PluginType.OAIPMH_HARVEST;
  static final PluginType PLUGIN_TYPE_2_VALUE = PluginType.VALIDATION_EXTERNAL;

  static WorkflowExecutionDTO getDatasetDTOUsingSetters() {
    WorkflowExecutionDTO workflowExecutionDTO = new WorkflowExecutionDTO();
    workflowExecutionDTO.setId(id.toString());
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
    workflowExecutionDTO.setCreatedDate(createdDate);
    workflowExecutionDTO.setStartedDate(startedDate);
    workflowExecutionDTO.setUpdatedDate(updatedDate);
    workflowExecutionDTO.setFinishedDate(finishedDate);
    workflowExecutionDTO.setIncremental(false);

    PluginDTO pluginDTO1 = new PluginDTO(ExecutablePluginFactory.createPlugin(new OaipmhHarvestPluginMetadata()), true);
    PluginDTO pluginDTO2 = new PluginDTO(ExecutablePluginFactory.createPlugin(new ValidationExternalPluginMetadata()), true);
    workflowExecutionDTO.setMetisPlugins(List.of(pluginDTO1, pluginDTO2));
    return workflowExecutionDTO;
  }

  static WorkflowExecutionDTO getDatasetDTOUsingSettersWithNullValues() {
    WorkflowExecutionDTO workflowExecutionDTO = getDatasetDTOUsingSetters();
    workflowExecutionDTO.setCreatedDate(null);
    workflowExecutionDTO.setStartedDate(null);
    workflowExecutionDTO.setUpdatedDate(null);
    workflowExecutionDTO.setFinishedDate(null);
    workflowExecutionDTO.setMetisPlugins(null);
    return workflowExecutionDTO;
  }



}
