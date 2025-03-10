package eu.europeana.metis.core.service;

import eu.europeana.metis.core.user.User;
import eu.europeana.metis.core.user.User.UserBuilder;
import jakarta.ws.rs.NotFoundException;
import java.lang.invoke.MethodHandles;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.jvnet.hk2.annotations.Service;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This class provides business logic for managing users in the system. It encapsulates the data access object (DAO) for users and
 * provides methods for creating, updating, and retrieving user information.
 */
@Service
public class UserService {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final ConcurrentHashMap<String, User> userCache = new ConcurrentHashMap<>();
  private final Keycloak keycloak;
  private final String realm;

  /**
   * Constructs a new UserService instance.
   *
   * @param keycloak the Keycloak instance
   * @param realm the realm name
   */
  @Autowired
  public UserService(Keycloak keycloak, String realm) {
    this.keycloak = keycloak;
    this.realm = realm;
  }

  /**
   * Retrieves a user from the in-memory cache.
   *
   * @param userId the ID of the user to retrieve
   * @return the User object associated with the given user ID, or null if not found in the cache
   */
  public User getUserFromCache(String userId) {
    //Return early if userId provided is already null
    if (userId == null) {
      return null;
    }

    User user = userCache.get(userId);
    if (user == null) {
      User keycloakUserInformation = getKeycloakUserInformationOrDefault(userId);
      user = userCache.put(keycloakUserInformation.getUserId(), keycloakUserInformation);
    }
    return user;
  }

  /**
   * Clears all entries from the in-memory user cache. This method is used to ensure that the cache is completely emptied,
   * removing all stored user information.
   */
  public void clearCache() {
    userCache.clear();
  }

  /**
   * Inserts a user into the in-memory cache.
   *
   * @param user the user to insert into the cache
   */
  public void insertToInMemoryCache(User user) {
    userCache.computeIfPresent(user.getUserId(), (key, cachedUser) ->
        (user.getIssuedAt().isAfter(cachedUser.getIssuedAt())) ? user : cachedUser
    );
  }

  /**
   * Refreshes the user in the database. If the user does not exist, the user is added. If the user does exist, the user is
   * updated.
   *
   * @param userId the ID of the user in Keycloak
   * @return an Optional containing the User object if the user information is found, or an empty Optional if not found
   */
  public User getKeycloakUserInformationOrDefault(String userId) {
    UserRepresentation userRepresentation = null;
    try {
      userRepresentation = keycloak.realm(realm).users().get(userId).toRepresentation();
    } catch (NotFoundException e) {
      LOGGER.warn("User with ID {} not found. This can be normal e.g. if the user identifier is an old one", userId);
      LOGGER.debug("Exception details:", e);
    }
    UserBuilder userBuilder = new UserBuilder();

    if (userRepresentation == null) {
      userBuilder.userId(userId)
                 .userName(userId)
                 .issuedAt(Instant.now());
    } else {
      userBuilder.userId(userRepresentation.getId())
                 .userName(userRepresentation.getUsername())
                 .firstName(userRepresentation.getFirstName())
                 .lastName(userRepresentation.getLastName())
                 .issuedAt(Instant.now());
    }
    return userBuilder.build();
  }
}
