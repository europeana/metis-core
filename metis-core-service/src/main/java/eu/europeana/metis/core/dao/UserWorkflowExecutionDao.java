package eu.europeana.metis.core.dao;

import com.mongodb.WriteResult;
import eu.europeana.metis.core.mongo.MorphiaDatastoreProvider;
import eu.europeana.metis.core.workflow.UserWorkflowExecution;
import eu.europeana.metis.core.workflow.WorkflowStatus;
import java.util.Date;
import java.util.Iterator;
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
 * @since 2017-05-26
 */
@Repository
public class UserWorkflowExecutionDao implements MetisDao<UserWorkflowExecution, String> {

  private static final Logger LOGGER = LoggerFactory.getLogger(UserWorkflowExecutionDao.class);
  private static final String WORKFLOW_STATUS = "workflowStatus";
  private static final String DATASET_NAME = "datasetName";
  private final MorphiaDatastoreProvider morphiaDatastoreProvider;
  private int userWorkflowExecutionsPerRequest = 5;

  @Autowired
  public UserWorkflowExecutionDao(MorphiaDatastoreProvider morphiaDatastoreProvider) {
    this.morphiaDatastoreProvider = morphiaDatastoreProvider;
  }

  @Override
  public String create(UserWorkflowExecution userWorkflowExecution) {
    Key<UserWorkflowExecution> userWorkflowExecutionKey = morphiaDatastoreProvider.getDatastore().save(
        userWorkflowExecution);
    LOGGER.debug(
        "UserWorkflowExecution for datasetName '{}' with workflowOwner '{}' and workflowName '{}' created in Mongo",
        userWorkflowExecution.getDatasetName(), userWorkflowExecution.getWorkflowOwner(),
        userWorkflowExecution.getWorkflowName());
    return userWorkflowExecutionKey.getId().toString();
  }

  @Override
  public String update(UserWorkflowExecution userWorkflowExecution) {
    Key<UserWorkflowExecution> userWorkflowExecutionKey = morphiaDatastoreProvider.getDatastore().save(
        userWorkflowExecution);
    LOGGER.debug(
        "UserWorkflowExecution for datasetName '{}' with workflowOwner '{}' and workflowName '{}' updated in Mongo",
        userWorkflowExecution.getDatasetName(), userWorkflowExecution.getWorkflowOwner(),
        userWorkflowExecution.getWorkflowName());
    return userWorkflowExecutionKey.getId().toString();
  }

  public void updateWorkflowPlugins(UserWorkflowExecution userWorkflowExecution) {
    UpdateOperations<UserWorkflowExecution> userWorkflowExecutionUpdateOperations = morphiaDatastoreProvider
        .getDatastore()
        .createUpdateOperations(UserWorkflowExecution.class);
    Query<UserWorkflowExecution> query = morphiaDatastoreProvider.getDatastore().find(UserWorkflowExecution.class)
        .filter("_id", userWorkflowExecution.getId());
    userWorkflowExecutionUpdateOperations
        .set("metisPlugins", userWorkflowExecution.getMetisPlugins());
    UpdateResults updateResults = morphiaDatastoreProvider.getDatastore()
        .update(query, userWorkflowExecutionUpdateOperations);
    LOGGER.debug(
        "UserWorkflowExecution metisPlugins for datasetName '{}' with workflowOwner '{}' and workflowName '{}' updated in Mongo. (UpdateResults: {})",
        userWorkflowExecution.getDatasetName(), userWorkflowExecution.getWorkflowOwner(),
        userWorkflowExecution.getWorkflowName(), updateResults.getUpdatedCount());
  }

  public void updateMonitorInformation(UserWorkflowExecution userWorkflowExecution) {
    UpdateOperations<UserWorkflowExecution> userWorkflowExecutionUpdateOperations = morphiaDatastoreProvider
        .getDatastore()
        .createUpdateOperations(UserWorkflowExecution.class);
    Query<UserWorkflowExecution> query = morphiaDatastoreProvider.getDatastore().find(UserWorkflowExecution.class)
        .filter("_id", userWorkflowExecution.getId());
    userWorkflowExecutionUpdateOperations
        .set(WORKFLOW_STATUS, userWorkflowExecution.getWorkflowStatus());
    if (userWorkflowExecution.getStartedDate() != null) {
      userWorkflowExecutionUpdateOperations
          .set("startedDate", userWorkflowExecution.getStartedDate());
    }
    if (userWorkflowExecution.getUpdatedDate() != null) {
      userWorkflowExecutionUpdateOperations
          .set("updatedDate", userWorkflowExecution.getUpdatedDate());
    }
    userWorkflowExecutionUpdateOperations
        .set("metisPlugins", userWorkflowExecution.getMetisPlugins());
    UpdateResults updateResults = morphiaDatastoreProvider.getDatastore()
        .update(query, userWorkflowExecutionUpdateOperations);
    LOGGER.debug(
        "UserWorkflowExecution monitor information for datasetName '{}' with workflowOwner '{}' and workflowName '{}' updated in Mongo. (UpdateResults: {})",
        userWorkflowExecution.getDatasetName(), userWorkflowExecution.getWorkflowOwner(),
        userWorkflowExecution.getWorkflowName(), updateResults.getUpdatedCount());
  }

  public void setCancellingState(UserWorkflowExecution userWorkflowExecution) {
    UpdateOperations<UserWorkflowExecution> userWorkflowExecutionUpdateOperations = morphiaDatastoreProvider
        .getDatastore()
        .createUpdateOperations(UserWorkflowExecution.class);
    Query<UserWorkflowExecution> query = morphiaDatastoreProvider.getDatastore().find(UserWorkflowExecution.class)
        .filter("_id", userWorkflowExecution.getId());
    userWorkflowExecutionUpdateOperations.set("cancelling", Boolean.TRUE);
    UpdateResults updateResults = morphiaDatastoreProvider.getDatastore()
        .update(query, userWorkflowExecutionUpdateOperations);
    LOGGER.debug(
        "UserWorkflowExecution cancelling for datasetName '{}' with workflowOwner '{}' and workflowName '{}' set to true in Mongo. (UpdateResults: {})",
        userWorkflowExecution.getDatasetName(), userWorkflowExecution.getWorkflowOwner(),
        userWorkflowExecution.getWorkflowName(), updateResults.getUpdatedCount());
  }

  @Override
  public UserWorkflowExecution getById(String id) {
    Query<UserWorkflowExecution> query = morphiaDatastoreProvider.getDatastore()
        .find(UserWorkflowExecution.class)
        .field("_id").equal(new ObjectId(id));
    return query.get();
  }

  @Override
  public boolean delete(UserWorkflowExecution userWorkflowExecution) {
    return false;
  }

  public UserWorkflowExecution getRunningOrInQueueExecution(String datasetName) {
    Query<UserWorkflowExecution> query = morphiaDatastoreProvider.getDatastore()
        .find(UserWorkflowExecution.class)
        .field(DATASET_NAME).equal(
            datasetName);
    query.or(query.criteria(WORKFLOW_STATUS).equal(WorkflowStatus.INQUEUE),
        query.criteria(WORKFLOW_STATUS).equal(WorkflowStatus.RUNNING));
    return query.get();
  }

  public boolean exists(UserWorkflowExecution userWorkflowExecution) {
    return morphiaDatastoreProvider.getDatastore().find(UserWorkflowExecution.class)
        .field(DATASET_NAME).equal(
            userWorkflowExecution.getDatasetName()).field("workflowOwner").equal(
            userWorkflowExecution.getWorkflowOwner()).field("workflowName")
        .equal(userWorkflowExecution.getWorkflowName())
        .project("_id", true).get() != null;
  }

  public String existsAndNotCompleted(String datasetName) {
    Query<UserWorkflowExecution> query = morphiaDatastoreProvider.getDatastore()
        .find(UserWorkflowExecution.class)
        .field(DATASET_NAME).equal(
            datasetName);
    query.or(query.criteria(WORKFLOW_STATUS).equal(WorkflowStatus.INQUEUE),
        query.criteria(WORKFLOW_STATUS).equal(WorkflowStatus.RUNNING));
    query.project("_id", true);
    query.project(WORKFLOW_STATUS, true);

    UserWorkflowExecution storedUserWorkflowExecution = query.get();
    if (storedUserWorkflowExecution != null) {
      return storedUserWorkflowExecution.getId().toString();
    }
    return null;
  }

  public UserWorkflowExecution getRunningUserWorkflowExecution(String datasetName) {
    Query<UserWorkflowExecution> query = morphiaDatastoreProvider.getDatastore()
        .createQuery(UserWorkflowExecution.class);
    query.field(DATASET_NAME).equal(
        datasetName)
        .field(WORKFLOW_STATUS).equal(WorkflowStatus.RUNNING);
    return query.get();
  }

  public List<UserWorkflowExecution> getAllUserWorkflowExecutions(String datasetName,
      String workflowOwner,
      String workflowName,
      WorkflowStatus workflowStatus, String nextPage) {
    Query<UserWorkflowExecution> query = morphiaDatastoreProvider.getDatastore()
        .createQuery(UserWorkflowExecution.class);
    query.field(DATASET_NAME).equal(datasetName)
        .field("workflowOwner").equal(workflowOwner)
        .field("workflowName").equal(workflowName);
    if (workflowStatus != null && workflowStatus != WorkflowStatus.NULL) {
      query.field(WORKFLOW_STATUS).equal(workflowStatus);
    }
    query.order("_id");
    if (StringUtils.isNotEmpty(nextPage)) {
      query.field("_id").greaterThan(new ObjectId(nextPage));
    }
    return query.asList(new FindOptions().limit(userWorkflowExecutionsPerRequest));
  }

  public List<UserWorkflowExecution> getAllUserWorkflowExecutions(WorkflowStatus workflowStatus,
      String nextPage) {
    Query<UserWorkflowExecution> query = morphiaDatastoreProvider.getDatastore()
        .createQuery(UserWorkflowExecution.class);
    if (workflowStatus != null && workflowStatus != WorkflowStatus.NULL) {
      query.field(WORKFLOW_STATUS).equal(workflowStatus);
    }
    query.order("_id");
    if (StringUtils.isNotEmpty(nextPage)) {
      query.field("_id").greaterThan(new ObjectId(nextPage));
    }
    return query.asList(new FindOptions().limit(userWorkflowExecutionsPerRequest));
  }

  public int getUserWorkflowExecutionsPerRequest() {
    return userWorkflowExecutionsPerRequest;
  }

  public void setUserWorkflowExecutionsPerRequest(int userWorkflowExecutionsPerRequest) {
    this.userWorkflowExecutionsPerRequest = userWorkflowExecutionsPerRequest;
  }

  public boolean isCancelled(ObjectId id) {
    return morphiaDatastoreProvider.getDatastore().find(UserWorkflowExecution.class).field("_id").equal(id)
        .project(WORKFLOW_STATUS, true).get().getWorkflowStatus() == WorkflowStatus.CANCELLED;
  }

  public boolean isCancelling(ObjectId id) {
    return morphiaDatastoreProvider.getDatastore().find(UserWorkflowExecution.class).field("_id").equal(id)
        .project("cancelling", true).get().isCancelling();
  }

  public boolean isExecutionActive(UserWorkflowExecution userWorkflowExecutionToCheck,
      int monitorCheckInSecs) {
    try {
      Date updatedDateBefore = userWorkflowExecutionToCheck.getUpdatedDate();
      Thread.sleep(2 * monitorCheckInSecs * 1000L);
      UserWorkflowExecution userWorkflowExecution = this
          .getById(userWorkflowExecutionToCheck.getId().toString());
      return (updatedDateBefore != null && updatedDateBefore.compareTo(userWorkflowExecution.getUpdatedDate()) < 0) ||
          (updatedDateBefore == null && userWorkflowExecution.getUpdatedDate() != null)
          || userWorkflowExecution.getFinishedDate() != null;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();  // set interrupt flag
      LOGGER.warn("Thread was interrupted", e);
      return true;
    }
  }

  public void removeActiveExecutionsFromList(List<UserWorkflowExecution> userWorkflowExecutions,
      int monitorCheckInSecs) {
    try {
      Thread.sleep(2 * monitorCheckInSecs * 1000L);
      for (Iterator<UserWorkflowExecution> iterator = userWorkflowExecutions.iterator();
          iterator.hasNext(); ) {
        UserWorkflowExecution userWorkflowExecutionToCheck = iterator.next();
        UserWorkflowExecution userWorkflowExecution = this
            .getById(userWorkflowExecutionToCheck.getId().toString());
        if (userWorkflowExecutionToCheck.getUpdatedDate() != null
            && userWorkflowExecutionToCheck.getUpdatedDate()
            .compareTo(userWorkflowExecution.getUpdatedDate()) < 0) {
          iterator.remove();
        }
      }

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();  // set interrupt flag
      LOGGER.warn("Thread was interruped", e);
    }
  }

  public boolean deleteAllByDatasetName(String datasetName) {
    Query<UserWorkflowExecution> query = morphiaDatastoreProvider.getDatastore()
        .createQuery(UserWorkflowExecution.class);
    query.field(DATASET_NAME).equal(datasetName);
    WriteResult delete = morphiaDatastoreProvider.getDatastore().delete(query);
    LOGGER.debug("UserWorkflowExecution with datasetName: {}, deleted from Mongo", datasetName);
    return delete.getN() >= 1;
  }

  public void updateAllDatasetNames(String datasetName, String newDatasetName) {
    UpdateOperations<UserWorkflowExecution> userWorkflowExecutionUpdateOperations = morphiaDatastoreProvider
        .getDatastore()
        .createUpdateOperations(UserWorkflowExecution.class);
    Query<UserWorkflowExecution> query = morphiaDatastoreProvider.getDatastore().find(UserWorkflowExecution.class)
        .filter(DATASET_NAME, datasetName);
    userWorkflowExecutionUpdateOperations.set(DATASET_NAME, newDatasetName);
    UpdateResults updateResults = morphiaDatastoreProvider.getDatastore()
        .update(query, userWorkflowExecutionUpdateOperations);
    LOGGER.debug(
        "UserWorkflowExecution with datasetName '{}' renamed to '{}'. (UpdateResults: {})",
        datasetName, newDatasetName, updateResults.getUpdatedCount());
  }
}
