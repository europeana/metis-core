package eu.europeana.metis.core.service;

import eu.europeana.metis.core.user.User;
import eu.europeana.metis.core.user.User.UserBuilder;
import eu.europeana.metis.core.workflow.execution.SystemId;
import jakarta.ws.rs.NotFoundException;
import java.lang.invoke.MethodHandles;
import java.time.Instant;
import java.util.Optional;
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
  private static final String UNKNOWN_USER_NAME = "Unknown user";
  private static final String SYSTEM_MINUTE_CAP_EXPIRE_NAME = "Metis system initiated expiration";
  private static final String STARTED_BY_SYSTEM_NAME = "Metis system initiated";
  private static final ConcurrentHashMap<String, User> USER_CACHE = new ConcurrentHashMap<>();
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

    User user = USER_CACHE.get(userId);
    if (user == null) {
      User keycloakUserInformation = getKeycloakUserInformationOrDefault(userId);
      USER_CACHE.put(keycloakUserInformation.getUserId(), keycloakUserInformation);
      user = USER_CACHE.get(userId);
    }
    return user;
  }

  /**
   * Clears all entries from the in-memory user cache. This method is used to ensure that the cache is completely emptied,
   * removing all stored user information.
   */
  public void clearCache() {
    USER_CACHE.clear();
  }

  /**
   * Inserts a user into the in-memory cache.
   *
   * @param user the user to insert into the cache
   */
  public void insertToInMemoryCacheIfExists(User user) {
    USER_CACHE.computeIfPresent(user.getUserId(), (key, cachedUser) ->
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
  private User getKeycloakUserInformationOrDefault(String userId) {
    Optional<UserRepresentation> userRepresentation = getKeycloakUserRepresentation(userId);
    UserBuilder userBuilder = new UserBuilder();

    if (userRepresentation.isEmpty()) {
      String userName;
      if (userId.equals(SystemId.STARTED_BY_SYSTEM.name())) {
        userName = STARTED_BY_SYSTEM_NAME;
      } else if (userId.equals(SystemId.SYSTEM_MINUTE_CAP_EXPIRE.name())) {
        userName = SYSTEM_MINUTE_CAP_EXPIRE_NAME;
      } else {
        userName = UNKNOWN_USER_NAME;
      }
      userBuilder.userId(userId)
                 .userName(userName)
                 .issuedAt(Instant.now());
    } else {
      userBuilder.userId(userRepresentation.get().getId())
                 .userName(userRepresentation.get().getUsername())
                 .firstName(userRepresentation.get().getFirstName())
                 .lastName(userRepresentation.get().getLastName())
                 .issuedAt(Instant.now());
    }
    return userBuilder.build();
  }

  /**
   * Retrieves the Keycloak user representation for a given user ID.
   * This method attempts to fetch user information from the Keycloak server based
   * on the user ID provided. If the user is not found or an error occurs during
   * the retrieval, the method returns an empty Optional.
   *
   * @param userId the unique identifier of the user in Keycloak
   * @return an Optional containing the UserRepresentation if found, or an empty
   *         Optional if the user is not found or an error occurs
   */
  private Optional<UserRepresentation> getKeycloakUserRepresentation(String userId) {
    Optional<UserRepresentation> userRepresentation = Optional.empty();
    try {
      userRepresentation = Optional.ofNullable(keycloak.realm(realm).users().get(userId).toRepresentation());
    } catch (NotFoundException e) {
      LOGGER.warn("User with ID {} not found. This can be normal e.g. if the user identifier is an old one", userId);
      LOGGER.debug("Exception details:", e);
    } catch (RuntimeException e) {
      //We don't want to fail in case the service is down
      LOGGER.error("Unexpected exception while retrieving user information for user with ID {}", userId, e);
    }
    return userRepresentation;
  }


}
