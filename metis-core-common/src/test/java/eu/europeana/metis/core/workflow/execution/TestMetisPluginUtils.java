package eu.europeana.metis.core.workflow.execution;

import eu.europeana.metis.core.workflow.plugins.AbstractExecutablePlugin;
import eu.europeana.metis.core.workflow.plugins.DataStatus;
import eu.europeana.metis.core.workflow.plugins.OaipmhHarvestPlugin;
import eu.europeana.metis.core.workflow.plugins.OaipmhHarvestPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.PluginStatus;
import eu.europeana.metis.core.workflow.plugins.PluginType;
import java.time.ZonedDateTime;
import java.util.Date;
import org.bson.types.ObjectId;

import static eu.europeana.metis.core.workflow.execution.TestExecutionProgressUtils.getExecutionProgressUsingSetters;

public class TestMetisPluginUtils {

  private static final ZonedDateTime zonedDateTime = ZonedDateTime.parse("2025-03-10T10:10:10.000Z[UTC]");
  //FIELDS
  public static final String ID = "id";
  public static final String PLUGIN_TYPE = "pluginType";
  public static final String PLUGIN_STATUS = "pluginStatus";
  public static final String DATA_TATUS = "dataStatus";
  public static final String FAIL_MESSAGE = "failMessage";
  public static final String STARTED_DATE = "startedDate";
  public static final String UPDATED_DATE = "updatedDate";
  public static final String FINISHED_DATE = "finishedDate";
  public static final String EXTERNAL_TASK_ID = "externalTaskId";
  public static final String EXECUTION_PROGRESS_DTO = "executionProgress";
  public static final String TOPOLOGY_NAME = "topologyName";
  public static final String CAN_DISPLAY_RAW_XML = "canDisplayRawXml";
  public static final String METIS_PLUGIN_METADATA = "pluginMetadata";
  //VALUES
  public static final ObjectId OBJECT_ID_VALUE = new ObjectId("7e4d3bcfe12a9f34c9ad8e5b");
  public static final PluginType PLUGIN_TYPE_VALUE = PluginType.OAIPMH_HARVEST;
  public static final PluginStatus PLUGIN_STATUS_VALUE = PluginStatus.RUNNING;
  public static final DataStatus DATA_TATUS_VALUE = DataStatus.VALID;
  public static final String FAIL_MESSAGE_VALUE = "failMessage";
  public static final Date STARTED_DATE_VALUE = Date.from(zonedDateTime.toInstant());
  public static final Date UPDATED_DATE_VALUE = Date.from(zonedDateTime.toInstant());
  public static final Date FINISHED_DATE_VALUE = Date.from(zonedDateTime.toInstant());
  public static final boolean CAN_DISPLAY_RAW_XML_VALUE = true;
  public static final String EXTERNAL_TASK_ID_VALUE = "externalTaskId";
  public static final ExecutionProgressDTO EXECUTION_PROGRESS_DTO_VALUE = TestExecutionProgressUtils.getExecutionProgressDTOUsingSetters();
  public static final String TOPOLOGY_NAME_VALUE = "topologyName";
  public static final OaipmhHarvestPluginMetadata METIS_PLUGIN_METADATA_VALUE = new OaipmhHarvestPluginMetadata();

  static MetisPluginDTO getMetisPluginDTOUsingSetters() {
    MetisPluginDTO metisPluginDTO = new MetisPluginDTO();
    metisPluginDTO.setId(OBJECT_ID_VALUE.toString());
    metisPluginDTO.setPluginType(PLUGIN_TYPE_VALUE);
    metisPluginDTO.setPluginStatus(PLUGIN_STATUS_VALUE);
    metisPluginDTO.setDataStatus(DATA_TATUS_VALUE);
    metisPluginDTO.setFailMessage(FAIL_MESSAGE_VALUE);
    metisPluginDTO.setStartedDate(STARTED_DATE_VALUE);
    metisPluginDTO.setUpdatedDate(UPDATED_DATE_VALUE);
    metisPluginDTO.setFinishedDate(FINISHED_DATE_VALUE);
    metisPluginDTO.setExternalTaskId(EXTERNAL_TASK_ID_VALUE);
    metisPluginDTO.setExecutionProgress(EXECUTION_PROGRESS_DTO_VALUE);
    metisPluginDTO.setTopologyName(TOPOLOGY_NAME_VALUE);
    metisPluginDTO.setCanDisplayRawXml(CAN_DISPLAY_RAW_XML_VALUE);
    metisPluginDTO.setPluginMetadata(METIS_PLUGIN_METADATA_VALUE);

    return metisPluginDTO;
  }

  static MetisPluginDTO getMetisPluginDTOUsingSettersWithNullValues(){
    MetisPluginDTO metisPluginDTO = getMetisPluginDTOUsingSetters();
    metisPluginDTO.setStartedDate(null);
    metisPluginDTO.setUpdatedDate(null);
    metisPluginDTO.setFinishedDate(null);
    return metisPluginDTO;
  }

  public static AbstractExecutablePlugin<?> getAbstractMetisPluginUsingSetters() {
    OaipmhHarvestPlugin oaipmhHarvestPlugin = new OaipmhHarvestPlugin();
    oaipmhHarvestPlugin.setId(OBJECT_ID_VALUE.toString());
    oaipmhHarvestPlugin.setPluginStatus(PLUGIN_STATUS_VALUE);
    oaipmhHarvestPlugin.setDataStatus(DATA_TATUS_VALUE);
    oaipmhHarvestPlugin.setFailMessage(FAIL_MESSAGE_VALUE);
    oaipmhHarvestPlugin.setStartedDate(STARTED_DATE_VALUE);
    oaipmhHarvestPlugin.setUpdatedDate(UPDATED_DATE_VALUE);
    oaipmhHarvestPlugin.setFinishedDate(FINISHED_DATE_VALUE);
    oaipmhHarvestPlugin.setExternalTaskId(EXTERNAL_TASK_ID_VALUE);
    oaipmhHarvestPlugin.setExecutionProgress(getExecutionProgressUsingSetters());
    oaipmhHarvestPlugin.setPluginMetadata(METIS_PLUGIN_METADATA_VALUE);
    return oaipmhHarvestPlugin;
  }
}
