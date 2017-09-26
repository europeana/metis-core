package eu.europeana.metis.core.dao;

import com.mongodb.WriteResult;
import eu.europeana.metis.core.mongo.MorphiaDatastoreProvider;
import eu.europeana.metis.core.workflow.UserWorkflow;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.bson.types.ObjectId;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * @author Simon Tzanakis (Simon.Tzanakis@europeana.eu)
 * @since 2017-05-29
 */
@Repository
public class UserWorkflowDao implements MetisDao<UserWorkflow, String> {
  private static final Logger LOGGER = LoggerFactory.getLogger(UserWorkflowDao.class);
  private int userWorkflowsPerRequest = 5;
  private final MorphiaDatastoreProvider provider;

  @Autowired
  public UserWorkflowDao(MorphiaDatastoreProvider provider) {
    this.provider = provider;
  }

  @Override
  public String create(UserWorkflow userWorkflow) {
    Key<UserWorkflow> userWorkflowKey = provider.getDatastore().save(
        userWorkflow);
    LOGGER.info("UserWorkflow '" + userWorkflow.getWorkflowName() + "' created with workflowOwner '" + userWorkflow
        .getWorkflowOwner() + "' in Mongo");
    return userWorkflowKey.getId().toString();
  }

  @Override
  public String update(UserWorkflow userWorkflow) {
    Key<UserWorkflow> userWorkflowKey = provider.getDatastore().save(
        userWorkflow);
    LOGGER.info("UserWorkflow '" + userWorkflow.getWorkflowName() + "' updated with workflowOwner '" + userWorkflow
        .getWorkflowOwner() + "' in Mongo");
    return userWorkflowKey.getId().toString();
  }

  @Override
  public UserWorkflow getById(String id) {
    return null;
  }

  @Override
  public boolean delete(UserWorkflow userWorkflow) {
    return false;
  }


  public boolean deleteUserWorkflow(String workflowOwner, String workflowName){
    Query<UserWorkflow> query = provider.getDatastore().createQuery(UserWorkflow.class);
    query.field("workflowOwner").equal(workflowOwner);
    query.field("workflowName").equal(workflowName);
    WriteResult delete = provider.getDatastore().delete(query);
    LOGGER.info("UserWorkflow with workflowOwner: %s, and workflowName %s, deleted from Mongo", workflowOwner, workflowName);
    return delete.getN() == 1;
  }

  public String exists(UserWorkflow userWorkflow)
  {
    UserWorkflow storedUserWorkflow = provider.getDatastore().find(UserWorkflow.class).field("workflowOwner")
        .equal(
            userWorkflow.getWorkflowOwner()).field("workflowName").equal(userWorkflow.getWorkflowName())
        .project("_id", true).get();
    return storedUserWorkflow!=null?storedUserWorkflow.getId().toString():null;
  }

  public UserWorkflow getUserWorkflow(String workflowOwner, String workflowName) {
    return provider.getDatastore().find(UserWorkflow.class).field("workflowOwner").equal(workflowOwner)
        .field("workflowName").equal(workflowName)
        .get();
  }

  public List<UserWorkflow> getAllUserWorkflows(String workflowOwner, String nextPage) {
    Query<UserWorkflow> query = provider.getDatastore()
        .createQuery(UserWorkflow.class);
    query.field("workflowOwner").equal(workflowOwner);
    query.order("_id");
    if (StringUtils.isNotEmpty(nextPage)) {
      query.field("_id").greaterThan(new ObjectId(nextPage));
    }
    return query.asList(new FindOptions().limit(userWorkflowsPerRequest));
  }

  public int getUserWorkflowsPerRequest() {
    return userWorkflowsPerRequest;
  }

  public void setUserWorkflowsPerRequest(int userWorkflowsPerRequest) {
    this.userWorkflowsPerRequest = userWorkflowsPerRequest;
  }
}

