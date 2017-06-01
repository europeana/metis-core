package eu.europeana.metis.core.workflow.plugins;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import eu.europeana.metis.core.workflow.CloudStatistics;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * This interface specifies the minimum o plugin should support so that it can be plugged in the
 * Metis workflow registry and can be accessible via the REST API of Metis Created by ymamakis on
 * 11/9/16.
 */
//@JsonDeserialize(using = PluginDeserializer.class)
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME,
    include=JsonTypeInfo.As.PROPERTY,
    property="pluginType")
@JsonSubTypes({
    @JsonSubTypes.Type(value=VoidOaipmhHarvestPlugin.class, name="OAIPMH_HARVEST"),
    @JsonSubTypes.Type(value=VoidHTTPHarvestPlugin.class, name="HTTP_HARVEST"),
    @JsonSubTypes.Type(value=VoidDereferencePlugin.class, name="DEREFERENCE"),
    @JsonSubTypes.Type(value=VoidMetisPlugin.class, name="VOID")
})
public interface AbstractMetisPlugin {

  PluginStatus getPluginStatus();

  PluginType getPluginType();

  Date getStartedDate();

  void setStartedDate(Date startedDate);

  Date getFinishedDate();

  void setFinishedDate(Date finishedDate);

  Date getUpdatedDate();

  void setUpdatedDate(Date updatedDate);

//  long getRecordsProcessed();
//
//  void setRecordsProcessed(long recordsProcessed);
//
//  long getRecordsFailed();
//
//  void setRecordsFailed(long recordsFailed);
//
//  long getRecordsUpdated();
//
//  void setRecordsUpdated(long recordsUpdated);
//
//  long getRecordsCreated();
//
//  void setRecordsCreated(long recordsCreated);
//
//  long getRecordsDeleted();
//
//  void setRecordsDeleted(long recordsDeleted);

  void setPluginStatus(PluginStatus pluginStatus);

  ExecutionRecordsStatistics getExecutionRecordsStatistics();

  void setExecutionRecordsStatistics(
      ExecutionRecordsStatistics executionRecordsStatistics);

  /**
   * The parameters of the workflow
   *
   * @param parameters The parameters of the workflow
   */
  void setParameters(Map<String, List<String>> parameters);

  /**
   * Set the parameters of the workflow
   *
   * @return The parameters of the workflow
   */
  Map<String, List<String>> getParameters();

  /**
   * The business logic that the UserWorkflow implements. This is where the connection to the
   * Europeana Cloud DPS REST API is implemented.
   */
  void execute();

  CloudStatistics monitor(String dataseId);

}
