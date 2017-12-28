package eu.europeana.metis.core.workflow;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import eu.europeana.metis.core.dataset.Dataset;
import eu.europeana.metis.json.ObjectIdSerializer;
import eu.europeana.metis.core.workflow.plugins.AbstractMetisPlugin;
import eu.europeana.metis.core.workflow.plugins.AbstractMetisPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.DereferencePlugin;
import eu.europeana.metis.core.workflow.plugins.HTTPHarvestPlugin;
import eu.europeana.metis.core.workflow.plugins.OaipmhHarvestPlugin;
import eu.europeana.metis.core.workflow.plugins.PluginStatus;
import eu.europeana.metis.core.workflow.plugins.PluginType;
import eu.europeana.metis.core.workflow.plugins.VoidMetisPlugin;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;

/**
 * @author Simon Tzanakis (Simon.Tzanakis@europeana.eu)
 * @since 2017-05-26
 */
@Entity
@Indexes({@Index(fields = {@Field("workflowOwner"), @Field("workflowName")})})
@JsonPropertyOrder({"id", "workflowOnwer", "workflowName", "workflowStatus", "datasetId",
    "workflowPriority", "createdDate", "startedDate", "updatedDate", "finishedDate",
    "httpHarvestPlugin", "oaipmhHarvestPlugin", "dereferencePlugin", "voidMetisPlugin"})
public class WorkflowExecution implements HasMongoObjectId {

  @Id
  @JsonSerialize(using = ObjectIdSerializer.class)
  private ObjectId id;
  private int datasetId;
  @Indexed
  private String workflowOwner;
  @Indexed
  private String workflowName;
  @Indexed
  private WorkflowStatus workflowStatus;
  @Indexed
  private String ecloudDatasetId;
  private int workflowPriority;
  private boolean cancelling;

  @Indexed
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
  private Date createdDate;
  @Indexed
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
  private Date startedDate;
  @Indexed
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
  private Date updatedDate;
  @Indexed
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
  private Date finishedDate;

  private List<AbstractMetisPlugin> metisPlugins = new ArrayList<>();

  //Keep this constructor
  public WorkflowExecution() {
  }

  public WorkflowExecution(Dataset dataset, Workflow workflow, int workflowPriority) {
    this.workflowOwner = workflow.getWorkflowOwner();
    this.workflowName = workflow.getWorkflowName();
    this.datasetId = dataset.getDatasetId();
    this.ecloudDatasetId = dataset.getEcloudDatasetId();
    this.workflowPriority = workflowPriority;

    addHarvestingPlugin(dataset, workflow);

    // TODO: 31-5-17 Add transformation plugin retrieved probably from the dataset, and generated from the mapping tool.

    addProcessPlugins(workflow);
  }

  private void addHarvestingPlugin(Dataset dataset, Workflow workflow) {
    AbstractMetisPluginMetadata harvestingMetadata = dataset.getHarvestingMetadata();
    if (workflow.isHarvestPlugin()) {
      switch (harvestingMetadata.getPluginType()) {
        case HTTP_HARVEST:
          HTTPHarvestPlugin httpHarvestPlugin = new HTTPHarvestPlugin(harvestingMetadata);
          httpHarvestPlugin
              .setId(
                  new ObjectId().toString() + "-" + httpHarvestPlugin.getPluginType().name());
          metisPlugins.add(httpHarvestPlugin);
          break;
        case OAIPMH_HARVEST:
          OaipmhHarvestPlugin oaipmhHarvestPlugin = new OaipmhHarvestPlugin(harvestingMetadata);
          oaipmhHarvestPlugin
              .setId(
                  new ObjectId().toString() + "-" + oaipmhHarvestPlugin.getPluginType().name());
          metisPlugins.add(oaipmhHarvestPlugin);
          break;
        case NULL:
          break;
        default:
          break;
      }
    }
  }

  private void addProcessPlugins(Workflow workflow) {
    AbstractMetisPluginMetadata dereferencePluginMetadata = workflow
        .getPluginMetadata(PluginType.DEREFERENCE);
    if (dereferencePluginMetadata != null) {
      DereferencePlugin dereferencePlugin = new DereferencePlugin(dereferencePluginMetadata);
      dereferencePlugin
          .setId(new ObjectId().toString() + "-" + dereferencePlugin.getPluginType().name());
      metisPlugins.add(dereferencePlugin);
    }
    AbstractMetisPluginMetadata voidMetisPluginMetadata = workflow
        .getPluginMetadata(PluginType.VOID);
    if (voidMetisPluginMetadata != null) {
      VoidMetisPlugin voidMetisPlugin = new VoidMetisPlugin(voidMetisPluginMetadata);
      voidMetisPlugin
          .setId(new ObjectId().toString() + "-" + voidMetisPlugin.getPluginType().name());
      metisPlugins.add(voidMetisPlugin);
    }
  }

  public void setAllRunningAndInqueuePluginsToCancelled()
  {
    this.setWorkflowStatus(WorkflowStatus.CANCELLED);
    for (AbstractMetisPlugin metisPlugin :
        this.getMetisPlugins()) {
      if (metisPlugin.getPluginStatus() == PluginStatus.INQUEUE
          || metisPlugin.getPluginStatus() == PluginStatus.RUNNING) {
        metisPlugin.setPluginStatus(PluginStatus.CANCELLED);
      }
    }
    this.setCancelling(false);
  }

  @Override
  public ObjectId getId() {
    return id;
  }

  @Override
  public void setId(ObjectId id) {
    this.id = id;
  }

  public boolean isCancelling() {
    return cancelling;
  }

  public void setCancelling(boolean cancelling) {
    this.cancelling = cancelling;
  }

  public String getWorkflowOwner() {
    return workflowOwner;
  }

  public void setWorkflowOwner(String workflowOwner) {
    this.workflowOwner = workflowOwner;
  }

  public String getWorkflowName() {
    return workflowName;
  }

  public void setWorkflowName(String workflowName) {
    this.workflowName = workflowName;
  }

  public WorkflowStatus getWorkflowStatus() {
    return workflowStatus;
  }

  public void setWorkflowStatus(WorkflowStatus workflowStatus) {
    this.workflowStatus = workflowStatus;
  }

  public long getDatasetId() {
    return datasetId;
  }

  public void setDatasetId(int datasetId) {
    this.datasetId = datasetId;
  }

  public String getEcloudDatasetId() {
    return ecloudDatasetId;
  }

  public void setEcloudDatasetId(String ecloudDatasetId) {
    this.ecloudDatasetId = ecloudDatasetId;
  }

  public int getWorkflowPriority() {
    return workflowPriority;
  }

  public void setWorkflowPriority(int workflowPriority) {
    this.workflowPriority = workflowPriority;
  }

  public Date getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(Date createdDate) {
    this.createdDate = createdDate;
  }

  public Date getStartedDate() {
    return startedDate;
  }

  public void setStartedDate(Date startedDate) {
    this.startedDate = startedDate;
  }

  public Date getFinishedDate() {
    return finishedDate;
  }

  public void setFinishedDate(Date finishedDate) {
    this.finishedDate = finishedDate;
  }

  public Date getUpdatedDate() {
    return updatedDate;
  }

  public void setUpdatedDate(Date updatedDate) {
    this.updatedDate = updatedDate;
  }

  public List<AbstractMetisPlugin> getMetisPlugins() {
    return metisPlugins;
  }

  public void setMetisPlugins(
      List<AbstractMetisPlugin> metisPlugins) {
    this.metisPlugins = metisPlugins;
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 1;
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    result = prime * result + Integer.hashCode(datasetId);
    result = prime * result + ((workflowOwner == null) ? 0 : workflowOwner.hashCode());
    result = prime * result + ((workflowName == null) ? 0 : workflowName.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    WorkflowExecution that = (WorkflowExecution) obj;
    return (id == that.getId() && datasetId == that.datasetId && workflowOwner
        .equals(that.workflowOwner)
        && workflowName.equals(that.workflowName));
  }
}


