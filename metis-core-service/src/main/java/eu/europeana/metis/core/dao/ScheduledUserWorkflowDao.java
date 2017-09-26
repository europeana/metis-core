package eu.europeana.metis.core.dao;

import com.mongodb.WriteResult;
import eu.europeana.metis.core.mongo.MorphiaDatastoreProvider;
import eu.europeana.metis.core.workflow.ScheduledUserWorkflow;
import org.bson.types.ObjectId;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * @author Simon Tzanakis (Simon.Tzanakis@europeana.eu)
 * @since 2017-09-25
 */
@Repository
public class ScheduledUserWorkflowDao implements MetisDao<ScheduledUserWorkflow, String> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledUserWorkflowDao.class);
  private int scheduledUserWorkflowPerRequest = 5;
  private final MorphiaDatastoreProvider provider;

  @Autowired
  public ScheduledUserWorkflowDao(MorphiaDatastoreProvider provider) {
    this.provider = provider;
  }

  @Override
  public String create(ScheduledUserWorkflow scheduledUserWorkflow) {
    Key<ScheduledUserWorkflow> scheduledUserWorkflowKey = provider.getDatastore().save(
        scheduledUserWorkflow);
    LOGGER.info(
        "ScheduledUserWorkflow for datasetName: '{}' with workflowName: '{}' and owner: '{}' created in Mongo",
        scheduledUserWorkflow.getDatasetName(), scheduledUserWorkflow.getWorkflowName(),
        scheduledUserWorkflow.getWorkflowOwner());
    return scheduledUserWorkflowKey.getId().toString();
  }

  @Override
  public String update(ScheduledUserWorkflow scheduledUserWorkflow) {
    Key<ScheduledUserWorkflow> scheduledUserWorkflowKey = provider.getDatastore().save(
        scheduledUserWorkflow);
    LOGGER.info(
        "ScheduledUserWorkflow with datasetName: '{}', workflowName: '{}' and workflowOwner '{}' updated in Mongo",
        scheduledUserWorkflow.getDatasetName(), scheduledUserWorkflow.getWorkflowName(),
        scheduledUserWorkflow.getWorkflowOwner());
    return scheduledUserWorkflowKey.getId().toString();
  }

  @Override
  public ScheduledUserWorkflow getById(String id) {
    Query<ScheduledUserWorkflow> query = provider.getDatastore()
        .find(ScheduledUserWorkflow.class)
        .field("_id").equal(new ObjectId(id));
    return query.get();
  }

  @Override
  public boolean delete(ScheduledUserWorkflow scheduledUserWorkflow) {
    return false;
  }

  public ScheduledUserWorkflow getScheduledUserWorkflow(String datasetName, String workflowOwner,
      String workflowName) {
    return provider.getDatastore()
        .find(ScheduledUserWorkflow.class).field("datasetName")
        .equal(datasetName).field("workflowOwner")
        .equal(workflowOwner).field("workflowName")
        .equal(workflowName).get();
  }

  public String exists(ScheduledUserWorkflow scheduledUserWorkflow) {
    ScheduledUserWorkflow storedScheduledUserWorkflow = provider.getDatastore()
        .find(ScheduledUserWorkflow.class).field("datasetName")
        .equal(scheduledUserWorkflow.getDatasetName()).field("workflowOwner")
        .equal(scheduledUserWorkflow.getWorkflowOwner()).field("workflowName")
        .equal(scheduledUserWorkflow.getWorkflowName())
        .project("_id", true).get();
    return storedScheduledUserWorkflow != null ? storedScheduledUserWorkflow.getId().toString()
        : null;
  }

  public String existsForDatasetName(String datasetName) {
    ScheduledUserWorkflow storedScheduledUserWorkflow = provider.getDatastore()
        .find(ScheduledUserWorkflow.class).field("datasetName")
        .equal(datasetName).project("_id", true).get();
    return storedScheduledUserWorkflow != null ? storedScheduledUserWorkflow.getId().toString()
        : null;
  }

  public boolean deleteScheduledUserWorkflow(String datasetName, String workflowOwner,
      String workflowName) {
    Query<ScheduledUserWorkflow> query = provider.getDatastore()
        .createQuery(ScheduledUserWorkflow.class);
    query.field("datasetName").equal(datasetName);
    query.field("workflowOwner").equal(workflowOwner);
    query.field("workflowName").equal(workflowName);
    WriteResult delete = provider.getDatastore().delete(query);
    LOGGER.info(
        "ScheduledUserWorkflow with datasetName: {} workflowOwner: {}, and workflowName {}, deleted from Mongo",
        datasetName, workflowOwner, workflowName);
    return delete.getN() == 1;
  }
}
