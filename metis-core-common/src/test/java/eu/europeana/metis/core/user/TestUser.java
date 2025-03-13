package eu.europeana.metis.core.user;

import eu.europeana.metis.core.user.User.UserBuilder;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class TestUser {

  private static final String USER_ID = "userId";
  private static final String USER_NAME = "userName";
  private static final String FIRST_NAME = "fistName";
  private static final String LAST_NAME = "lastName";
  private static final Instant ISSUED_AT = Instant.now();

  @Test
  void testGetUserIdWithValidUserId() {
    User user = new User.UserBuilder()
        .userId(USER_ID)
        .userName(USER_NAME)
        .firstName(FIRST_NAME)
        .lastName(LAST_NAME)
        .issuedAt(ISSUED_AT)
        .build();
    assertEquals(USER_ID, user.getUserId());
    assertEquals(USER_NAME, user.getUserName());
    assertEquals(FIRST_NAME, user.getFirstName());
    assertEquals(LAST_NAME, user.getLastName());
    assertEquals(ISSUED_AT, user.getIssuedAt());
  }

  @Test
  void testGetUserIdWhenUserIdIsNull() {
    UserBuilder userBuilder1 = new UserBuilder().userId(USER_ID).userName(USER_NAME);
    assertThrows(NullPointerException.class, userBuilder1::build);
    UserBuilder userBuilder2 = new UserBuilder().userId(USER_ID);
    assertThrows(NullPointerException.class, userBuilder2::build);
    UserBuilder userBuilder3 = new UserBuilder();
    assertThrows(NullPointerException.class, userBuilder3::build);
  }
}