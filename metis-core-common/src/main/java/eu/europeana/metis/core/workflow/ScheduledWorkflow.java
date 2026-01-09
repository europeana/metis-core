package eu.europeana.metis.core.workflow;

import com.fasterxml.jackson.annotation.JsonFormat;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.Indexes;
import eu.europeana.metis.mongo.model.HasMongoObjectId;
import eu.europeana.metis.mongo.utils.ObjectIdSerializer;
import eu.europeana.metis.utils.CommonStringValues;
import org.bson.types.ObjectId;
import tools.jackson.databind.annotation.JsonSerialize;

import java.util.Date;

/**
 * Class to represent a scheduled workflow.
 * The {@link ScheduleFrequence} {@link #scheduleFrequence} will be used in conjunction with the {@link #pointerDate} to determine when a scheduled execution is ready to be ran.
 */
@Entity
@Indexes({
    @Index(fields = {@Field("datasetId")}),
    @Index(fields = {@Field("pointerDate")})})
public class ScheduledWorkflow implements HasMongoObjectId {

  @Id
  @JsonSerialize(using = ObjectIdSerializer.class)
  private ObjectId id;
  private String datasetId;
  @JsonFormat(pattern = CommonStringValues.DATE_FORMAT_FOR_SCHEDULING)
  private Date pointerDate;
  private ScheduleFrequence scheduleFrequence;

  public ScheduledWorkflow() {
    //Required for json serialization
  }

  /**
   * Constructor for creating a scheduled workflow
   *
   * @param pointerDate the {@link Date} that will be used as a pointer Date
   * @param datasetId identifier of the dataset for the scheduled workflow
   * @param scheduleFrequence the {@link ScheduleFrequence} for the workflow
   */
  public ScheduledWorkflow(Date pointerDate, String datasetId, ScheduleFrequence scheduleFrequence) {
    this.pointerDate = pointerDate == null ? null : new Date(pointerDate.getTime());
    this.datasetId = datasetId;
    this.scheduleFrequence = scheduleFrequence;
  }

  @Override
  public ObjectId getId() {
    return id;
  }

  @Override
  public void setId(ObjectId id) {
    this.id = id;
  }

  public String getDatasetId() {
    return datasetId;
  }

  public void setDatasetId(String datasetId) {
    this.datasetId = datasetId;
  }

  public Date getPointerDate() {
    return pointerDate == null?null:new Date(pointerDate.getTime());
  }

  public void setPointerDate(Date pointerDate) {
    this.pointerDate = pointerDate == null?null:new Date(pointerDate.getTime());
  }

  public ScheduleFrequence getScheduleFrequence() {
    return scheduleFrequence;
  }

  public void setScheduleFrequence(ScheduleFrequence scheduleFrequence) {
    this.scheduleFrequence = scheduleFrequence;
  }
}
