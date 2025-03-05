package eu.europeana.metis.core.dao;

import com.mongodb.client.result.UpdateResult;
import dev.morphia.UpdateOptions;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import dev.morphia.query.filters.Filters;
import dev.morphia.query.updates.UpdateOperator;
import dev.morphia.query.updates.UpdateOperators;
import eu.europeana.metis.core.mongo.MorphiaDatastoreProvider;
import eu.europeana.metis.core.user.User;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import static eu.europeana.metis.core.common.DaoFieldNames.USER_ID;
import static eu.europeana.metis.mongo.utils.MorphiaUtils.getListOfQueryRetryable;
import static eu.europeana.metis.network.ExternalRequestUtil.retryableExternalRequestForNetworkExceptions;

/**
 * User Access Object for users using Mongo.
 */
@Repository
public class UserDao implements MetisDao<User, String> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final MorphiaDatastoreProvider morphiaDatastoreProvider;

  /**
   * Constructs the DAO
   *
   * @param morphiaDatastoreProvider {@link MorphiaDatastoreProvider} used to access Mongo
   */
  @Autowired
  public UserDao(MorphiaDatastoreProvider morphiaDatastoreProvider) {
    this.morphiaDatastoreProvider = morphiaDatastoreProvider;
  }

  @Override
  public User create(User user) {
    User userSaved = retryableExternalRequestForNetworkExceptions(
        () -> morphiaDatastoreProvider.getDatastore().save(user));
    LOGGER.debug("User with userId: '{}', userName: '{}' saved in Mongo", user.getUserId(), user.getUserName());
    return userSaved;
  }

  @Override
  public String update(User user) {
    UpdateResult updateResult = retryableExternalRequestForNetworkExceptions(
        () -> {
          Query<User> query = morphiaDatastoreProvider.getDatastore().find(User.class)
                                                      .filter(Filters.eq("userId", user.getUserId()));
          return query.update(new UpdateOptions().upsert(true),
              getSetOperators(user).toArray(UpdateOperator[]::new));
        });
    LOGGER.debug("User with userId: '{}', userName: '{}' updated in Mongo", user.getUserId(), user.getUserName());
    return updateResult.getModifiedCount() > 0 ? user.getUserId() : null;
  }

  /**
   * Retrieve a user by the user ID.
   *
   * @param userId the ID of the user
   * @return the user or null if not found
   */
  @Override
  public User getById(String userId) {
    return retryableExternalRequestForNetworkExceptions(
        () -> morphiaDatastoreProvider.getDatastore().find(User.class)
                                      .filter(Filters.eq(USER_ID.getFieldName(), userId)).first());
  }

  @Override
  public boolean delete(User user) {
    retryableExternalRequestForNetworkExceptions(
        () -> morphiaDatastoreProvider.getDatastore().find(User.class)
                                      .filter(Filters.eq(USER_ID.getFieldName(), user.getUserId())).delete());
    LOGGER.debug("User with userId: '{}', userName: '{}' deleted in Mongo", user.getUserId(), user.getUserName());
    return true;
  }

  public List<User> getAllUsers() {
    Query<User> query = morphiaDatastoreProvider.getDatastore().find(User.class);
    return getListOfQueryRetryable(query, new FindOptions());
  }

  private List<UpdateOperator> getSetOperators(User user) {
    List<UpdateOperator> operators = new ArrayList<>();
    operators.add(UpdateOperators.set("userId", user.getUserId()));
    operators.add(UpdateOperators.set("userName", user.getUserName()));
    operators.add(UpdateOperators.set("firstName", user.getFirstName()));
    operators.add(UpdateOperators.set("lastName", user.getLastName()));
    return operators;
  }
}
