package eu.europeana.metis.core.dao;

import com.mongodb.WriteResult;
import eu.europeana.metis.core.mongo.MorphiaDatastoreProvider;
import eu.europeana.metis.core.workflow.ScheduleFrequence;
import eu.europeana.metis.core.workflow.ScheduledWorkflow;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.bson.types.ObjectId;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * @author Simon Tzanakis (Simon.Tzanakis@europeana.eu)
 * @since 2017-09-25
 */
@Repository
public class ScheduledWorkflowDao implements MetisDao<ScheduledWorkflow, String> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledWorkflowDao.class);
  private static final String DATASET_ID = "datasetId";
  private int scheduledWorkflowPerRequest = 5;
  private final MorphiaDatastoreProvider morphiaDatastoreProvider;

  @Autowired
  public ScheduledWorkflowDao(MorphiaDatastoreProvider morphiaDatastoreProvider) {
    this.morphiaDatastoreProvider = morphiaDatastoreProvider;
  }

  @Override
  public String create(ScheduledWorkflow scheduledWorkflow) {
    Key<ScheduledWorkflow> scheduledWorkflowKey = morphiaDatastoreProvider.getDatastore().save(
        scheduledWorkflow);
    LOGGER.debug(
        "ScheduledWorkflow for datasetName: '{}' with workflowName: '{}' and owner: '{}' created in Mongo",
        scheduledWorkflow.getDatasetId(), scheduledWorkflow.getWorkflowName(),
        scheduledWorkflow.getWorkflowOwner());
    return scheduledWorkflowKey.getId().toString();
  }

  @Override
  public String update(ScheduledWorkflow scheduledWorkflow) {
    Key<ScheduledWorkflow> scheduledWorkflowKey = morphiaDatastoreProvider.getDatastore().save(
        scheduledWorkflow);
    LOGGER.debug(
        "ScheduledWorkflow with datasetId: '{}', workflowName: '{}' and workflowOwner '{}' updated in Mongo",
        scheduledWorkflow.getDatasetId(), scheduledWorkflow.getWorkflowName(),
        scheduledWorkflow.getWorkflowOwner());
    return scheduledWorkflowKey.getId().toString();
  }

  @Override
  public ScheduledWorkflow getById(String id) {
    Query<ScheduledWorkflow> query = morphiaDatastoreProvider.getDatastore()
        .find(ScheduledWorkflow.class)
        .field("_id").equal(new ObjectId(id));
    return query.get();
  }

  @Override
  public boolean delete(ScheduledWorkflow scheduledWorkflow) {
    return false;
  }

  public ScheduledWorkflow getScheduledWorkflow(String datasetId, String workflowOwner,
      String workflowName) {
    return morphiaDatastoreProvider.getDatastore()
        .find(ScheduledWorkflow.class).field(DATASET_ID)
        .equal(datasetId).field("workflowOwner")
        .equal(workflowOwner).field("workflowName")
        .equal(workflowName).get();
  }

  public ScheduledWorkflow getScheduledWorkflowByDatasetId(String datasetId) {
    return morphiaDatastoreProvider.getDatastore()
        .find(ScheduledWorkflow.class).field(DATASET_ID)
        .equal(datasetId).get();
  }

  public boolean exists(ScheduledWorkflow scheduledWorkflow) {
    return morphiaDatastoreProvider.getDatastore()
        .find(ScheduledWorkflow.class).field(DATASET_ID)
        .equal(scheduledWorkflow.getDatasetId()).field("workflowOwner")
        .equal(scheduledWorkflow.getWorkflowOwner()).field("workflowName")
        .equal(scheduledWorkflow.getWorkflowName())
        .project("_id", true).get() != null;
  }

  public String existsForDatasetId(String datasetId) {
    ScheduledWorkflow storedScheduledWorkflow = morphiaDatastoreProvider.getDatastore()
        .find(ScheduledWorkflow.class).field(DATASET_ID)
        .equal(datasetId).project("_id", true).get();
    return storedScheduledWorkflow != null ? storedScheduledWorkflow.getId().toString()
        : null;
  }

  public boolean deleteScheduledWorkflow(String datasetId) {
    Query<ScheduledWorkflow> query = morphiaDatastoreProvider.getDatastore()
        .createQuery(ScheduledWorkflow.class);
    query.field(DATASET_ID).equal(datasetId);
    WriteResult delete = morphiaDatastoreProvider.getDatastore().delete(query);
    LOGGER.debug(
        "ScheduledWorkflow with datasetId: {} deleted from Mongo",
        datasetId);
    return delete.getN() == 1;
  }

  public boolean deleteAllByDatasetName(String datasetId) {
    Query<ScheduledWorkflow> query = morphiaDatastoreProvider.getDatastore()
        .createQuery(ScheduledWorkflow.class);
    query.field(DATASET_ID).equal(datasetId);
    WriteResult delete = morphiaDatastoreProvider.getDatastore().delete(query);
    LOGGER.debug(
        "ScheduledWorkflows with datasetId: {} deleted from Mongo", datasetId);
    return delete.getN() >= 1;
  }

  public boolean deleteAllByDatasetId(String datasetId) {
    Query<ScheduledWorkflow> query = morphiaDatastoreProvider.getDatastore()
        .createQuery(ScheduledWorkflow.class);
    query.field(DATASET_ID).equal(datasetId);
    WriteResult delete = morphiaDatastoreProvider.getDatastore().delete(query);
    LOGGER.debug(
        "ScheduledWorkflows with datasetId: {} deleted from Mongo", datasetId);
    return delete.getN() >= 1;
  }

  public void updateAllDatasetNames(String datasetId, String newDatasetName) {
    UpdateOperations<ScheduledWorkflow> scheduledWorkflowUpdateOperations = morphiaDatastoreProvider
        .getDatastore()
        .createUpdateOperations(ScheduledWorkflow.class);
    Query<ScheduledWorkflow> query = morphiaDatastoreProvider.getDatastore().find(ScheduledWorkflow.class)
        .filter(DATASET_ID, datasetId);
    scheduledWorkflowUpdateOperations.set(DATASET_ID, newDatasetName);
    UpdateResults updateResults = morphiaDatastoreProvider.getDatastore()
        .update(query, scheduledWorkflowUpdateOperations);
    LOGGER.debug(
        "ScheduledWorkflow with datasetId '{}' renamed to '{}'. (UpdateResults: {})",
        datasetId, newDatasetName, updateResults.getUpdatedCount());
  }

  public List<ScheduledWorkflow> getAllScheduledWorkflows(
      ScheduleFrequence scheduleFrequence, String nextPage) {
    Query<ScheduledWorkflow> query = morphiaDatastoreProvider.getDatastore()
        .createQuery(ScheduledWorkflow.class);
    if (scheduleFrequence != null && scheduleFrequence != ScheduleFrequence.NULL) {
      query.field("scheduleFrequence").equal(scheduleFrequence);
    }
    query.order("_id");
    if (StringUtils.isNotEmpty(nextPage)) {
      query.field("_id").greaterThan(new ObjectId(nextPage));
    }
    return query.asList(new FindOptions().limit(scheduledWorkflowPerRequest));
  }

  public List<ScheduledWorkflow> getAllScheduledWorkflowsByDateRangeONCE(
      LocalDateTime lowerBound,
      LocalDateTime upperBound, String nextPage) {
    Query<ScheduledWorkflow> query = morphiaDatastoreProvider.getDatastore()
        .createQuery(ScheduledWorkflow.class);
    query.criteria("scheduleFrequence").equal(ScheduleFrequence.ONCE).and(
        query.criteria("pointerDate").greaterThanOrEq(
            Date.from(lowerBound.atZone(ZoneId.systemDefault()).toInstant()))).and(
        query.criteria("pointerDate")
            .lessThan(Date.from(upperBound.atZone(ZoneId.systemDefault()).toInstant())));
    query.order("_id");
    if (StringUtils.isNotEmpty(nextPage)) {
      query.field("_id").greaterThan(new ObjectId(nextPage));
    }
    return query.asList(new FindOptions().limit(scheduledWorkflowPerRequest));
  }

  public int getScheduledWorkflowPerRequest() {
    return scheduledWorkflowPerRequest;
  }

  public void setScheduledWorkflowPerRequest(int scheduledWorkflowPerRequest) {
    this.scheduledWorkflowPerRequest = scheduledWorkflowPerRequest;
  }
}
