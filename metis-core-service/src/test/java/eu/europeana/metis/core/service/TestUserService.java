package eu.europeana.metis.core.service;

import eu.europeana.metis.core.user.User;
import eu.europeana.metis.core.user.User.UserBuilder;
import jakarta.ws.rs.NotFoundException;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestUserService {

  @Mock
  private Keycloak keycloak;

  @Mock
  private RealmResource realmResource;

  @Mock
  private UsersResource usersResource;

  @Mock
  private UserResource userResource;

  private UserService userService;

  private static final String REALM = "realm";
  private static final String USER_ID = "userId";
  private static final String USERNAME = "userName";
  private static final String FIRST_NAME = "fistName";
  private static final String LAST_NAME = "lastName";
  private static final Instant ISSUED_AT = Instant.now();
  private static final User user =
      new UserBuilder().userId(USER_ID).userName(USERNAME).firstName(FIRST_NAME).lastName(LAST_NAME).issuedAt(ISSUED_AT).build();

  @BeforeEach
  void setUp() {
    userService = new UserService(keycloak, REALM);
    userService.clearCache();
    reset(keycloak, realmResource, usersResource, userResource);
  }

  @Test
  void getUserFromCache() {
    when(keycloak.realm(REALM)).thenReturn(realmResource);
    when(realmResource.users()).thenReturn(usersResource);
    when(usersResource.get(USER_ID)).thenReturn(userResource);
    getUserFromKeycloakAndInsertInCache();

    User userFromCache = userService.getUserFromCache(USER_ID);
    assertUser(userFromCache);
    verifyNoMoreInteractions(keycloak);
  }

  @Test
  void getUserFromCache_NullUserId() {
    assertNull(userService.getUserFromCache(null));
    verifyNoMoreInteractions(keycloak);
  }

  @Test
  void getUserFromCacheDefaultForNonExistingUserId() {
    when(keycloak.realm(REALM)).thenReturn(realmResource);
    when(realmResource.users()).thenReturn(usersResource);
    when(usersResource.get(USER_ID)).thenThrow(NotFoundException.class);
    User userFromCache = userService.getUserFromCache(USER_ID);
    verifyNoMoreInteractions(keycloak);
    assertNotNull(userFromCache);
    assertEquals(USER_ID, userFromCache.getUserId());
    assertEquals(USER_ID, userFromCache.getUserName());
    assertNotNull(userFromCache.getIssuedAt());
  }

  @Test
  void getUserFromKeycloak() {
    when(keycloak.realm(REALM)).thenReturn(realmResource);
    when(realmResource.users()).thenReturn(usersResource);
    when(usersResource.get(USER_ID)).thenReturn(userResource);
    User userFromCache = getUserFromKeycloakAndInsertInCache();
    assertUser(userFromCache);
    verify(keycloak, times(1)).realm(REALM);
    verify(realmResource, times(1)).users();
    verify(usersResource, times(1)).get(USER_ID);
    verify(userResource, times(1)).toRepresentation();
    verifyNoMoreInteractions(keycloak);
  }

  private User getUserFromKeycloakAndInsertInCache() {
    UserRepresentation userRepresentation = new UserRepresentation();
    userRepresentation.setId(USER_ID);
    userRepresentation.setUsername(USERNAME);
    userRepresentation.setFirstName(FIRST_NAME);
    userRepresentation.setLastName(LAST_NAME);

    when(userResource.toRepresentation()).thenReturn(userRepresentation);

    return userService.getUserFromCache(USER_ID);
  }

  @Test
  void clearCache_ShouldGetDefault() {
    //Fill cache
    getUserFromKeycloak();
    userService.clearCache();
    //Get default after clear
    getUserFromCacheDefaultForNonExistingUserId();
  }

  @Test
  void insertToInMemoryCache_IfExists_ShouldUpdateIfNewIssuedAtIsLater() {
    //Fill cache
    getUserFromKeycloak();
    final String newFirstName = "newFirstName";
    User userUpdated = new UserBuilder().userId(USER_ID).userName(USERNAME).firstName(newFirstName).lastName(LAST_NAME)
                                        .issuedAt(ISSUED_AT.plusSeconds(10)).build();

    userService.insertToInMemoryCacheIfExists(userUpdated);
    User userFromCache = userService.getUserFromCache(USER_ID);
    assertEquals(newFirstName, userFromCache.getFirstName());
    verifyNoMoreInteractions(keycloak);
  }

  @Test
  void insertToInMemoryCache_IfExists_ShouldNotUpdateIfNewIssuedAtIsEarlier() {
    //Fill cache
    getUserFromKeycloak();
    final String newFirstName = "newFirstName";
    User userUpdated = new UserBuilder().userId(USER_ID).userName(USERNAME).firstName(newFirstName).lastName(LAST_NAME)
                                        .issuedAt(ISSUED_AT.minusSeconds(10)).build();

    userService.insertToInMemoryCacheIfExists(userUpdated);
    User userFromCache = userService.getUserFromCache(USER_ID);
    assertEquals(FIRST_NAME, userFromCache.getFirstName());
    verifyNoMoreInteractions(keycloak);
  }

  private static void assertUser(User user) {
    assertNotNull(user);
    assertEquals(USER_ID, user.getUserId());
    assertEquals(USERNAME, user.getUserName());
    assertEquals(FIRST_NAME, user.getFirstName());
    assertEquals(LAST_NAME, user.getLastName());
    assertNotNull(user.getIssuedAt());
  }
}
