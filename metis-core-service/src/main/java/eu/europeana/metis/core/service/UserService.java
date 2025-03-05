package eu.europeana.metis.core.service;

import eu.europeana.metis.core.dao.UserDao;
import eu.europeana.metis.core.user.User;
import jakarta.annotation.PostConstruct;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This class provides business logic for managing users in the system. It encapsulates the data access object (DAO) for users and
 * provides methods for creating, updating, and retrieving user information.
 */
@Service
public class UserService {

  //  private final Keycloak keycloak;
  //  private final String realm;
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final ConcurrentHashMap<String, User> userCache = new ConcurrentHashMap<>();
  private final UserDao userDao;

  /**
   * Constructs a new UserService instance.
   *
   * @param keycloak the Keycloak instance
   * @param realm the realm name
   * @param userDao the UserDao instance
   */
  //  @Autowired
  //  public UserService(Keycloak keycloak, String realm, UserDao userDao) {
  //    this.keycloak = keycloak;
  //    this.realm = realm;
  //    this.userDao = userDao;
  //  }
  @Autowired
  public UserService(UserDao userDao) {
    this.userDao = userDao;
  }

  /**
   * Retrieves a user from the in-memory cache.
   *
   * @param userId the ID of the user to retrieve
   * @return the User object associated with the given user ID, or null if not found in the cache
   */
  public User getUserFromCache(String userId) {
    return userCache.get(userId);
  }

  /**
   * Retrieves all users from the database and puts them into the in-memory cache. Any existing entries in the cache are replaced
   * by the retrieved users.
   */
  public void fillInMemoryCacheFromDatabase() {
    List<User> allUsers = userDao.getAllUsers();
    for (User user : allUsers) {
      userCache.put(user.getUserId(), user);
    }
  }

  /**
   * Saves the current in-memory user cache to the database.
   * <p>
   * Iterates over the entries in the in-memory cache and updates each user in the database.
   */
  public void saveCacheToDatabase() {
    for (Map.Entry<String, User> entry : userCache.entrySet()) {
      User user = entry.getValue();
      this.userDao.update(user);
    }
    LOGGER.info("Cache saved to Mongo");
  }

  /**
   * Inserts a user into the in-memory cache.
   *
   * @param user the user to insert into the cache
   */
  public static void insertToInMemoryCache(User user) {
    userCache.put(user.getUserId(), user);
  }

  /**
   * Initializes the in-memory cache on application startup by retrieving all users from the database.
   * <p>
   * This method is annotated with {@link PostConstruct} so that it is executed immediately after the application is started.
   */
  @PostConstruct
  public void fillInMemoryCacheFromDatabaseOnStartup() {
    fillInMemoryCacheFromDatabase();
  }

  /**
   * Refreshes the user in the database. If the user does not exist, the user is added. If the user does exist, the user is
   * updated.
   *
   * @param userId the ID of the user in Keycloak
   */
  //  public void refreshUserCache(String userId) {
  //    UserRepresentation userRepresentation = keycloak.realm(realm).users().get(userId).toRepresentation();
  //    User user = new User();
  //    user.setUserId(userRepresentation.getId());
  //    user.setUserName(userRepresentation.getUsername());
  //    user.setFirstName(userRepresentation.getFirstName());
  //    user.setLastName(userRepresentation.getLastName());
  //
  //    User userInDb = userDao.getByUserId(userId);
  //    if (userInDb == null) {
  //      userDao.create(user);
  //    } else {
  //      user.setId(userInDb.getId());
  //      userDao.update(user);
  //    }
  //  }
}
