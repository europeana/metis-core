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
 * Contains business logic of how to manipulate users.
 */
@Service
public class UserService {

//  private final Keycloak keycloak;
//  private final String realm;
  private final UserDao userDao;
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final ConcurrentHashMap<String, User> userCache = new ConcurrentHashMap<>();

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

  public User getUserFromCache(String userId) {
    return userCache.get(userId);
  }

  public void populateCache() {
    List<User> allUsers = userDao.getAllUsers();
    for (User user : allUsers) {
      userCache.put(user.getUserId(), user);
    }
  }

  public void saveCacheToMongo() {
    for (Map.Entry<String, User> entry : userCache.entrySet()) {
      User user = entry.getValue();
      this.userDao.update(user);
    }
    LOGGER.info("Cache saved to Mongo");
  }

  public static void insertInCache(User user) {
    userCache.put(user.getUserId(), user);
  }

  @PostConstruct
  public void populateCacheOnStartup() {
    populateCache();
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
