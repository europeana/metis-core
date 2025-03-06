package eu.europeana.metis.core.user;

import java.time.Instant;
import java.util.Objects;

/**
 * User model that contains all the required fields for User functionality.
 */
public final class User {

  private final String userId;
  private final String userName;
  private final String firstName;
  private final String lastName;
  private final Instant issuedAt;

  private User(UserBuilder builder) {
    this.userId = builder.userId;
    this.userName = builder.userName;
    this.firstName = builder.firstName;
    this.lastName = builder.lastName;
    this.issuedAt = builder.issuedAt;
  }

  public String getUserId() {
    return userId;
  }

  public String getUserName() {
    return userName;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public Instant getIssuedAt() {
    return issuedAt;
  }

  /**
   * Builder class for constructing a {@link User} instance.
   * <p>
   * This class provides a fluent API for setting the fields of the {@link User} object.
   * Once all required fields are set, the {@link #build()} method can be called to create the {@link User} object.
   */
  public static final class UserBuilder {

    private String userId;
    private String userName;
    private String firstName;
    private String lastName;
    private Instant issuedAt;

    /**
     * Sets the user ID.
     *
     * @param userId the user ID
     * @return the current {@link UserBuilder} instance
     */
    public UserBuilder userId(String userId) {
      this.userId = userId;
      return this;
    }

    /**
     * Sets the username.
     *
     * @param userName the username
     * @return the current {@link UserBuilder} instance
     */
    public UserBuilder userName(String userName) {
      this.userName = userName;
      return this;
    }

    /**
     * Sets the user's first name.
     *
     * @param firstName the user's first name
     * @return the current {@link UserBuilder} instance
     */
    public UserBuilder firstName(String firstName) {
      this.firstName = firstName;
      return this;
    }

    /**
     * Sets the user's last name.
     *
     * @param lastName the user's last name
     * @return the current {@link UserBuilder} instance
     */
    public UserBuilder lastName(String lastName) {
      this.lastName = lastName;
      return this;
    }

    /**
     * Sets the issued date and time.
     *
     * @param issuedAt the issued date and time
     * @return the current {@link UserBuilder} instance
     */
    public UserBuilder issuedAt(Instant issuedAt) {
      this.issuedAt = issuedAt;
      return this;
    }

    /**
     * Builds a new {@link User} instance using the provided values.
     * <p>
     * This method checks that all required fields (userId, userName, and issuedAt) are not null
     * before creating the {@link User} object.
     *
     * @return a new {@link User} instance
     * @throws NullPointerException if any required field is null
     */
    public User build() {
      Objects.requireNonNull(userId, "userId must not be null");
      Objects.requireNonNull(userName, "userName must not be null");
      Objects.requireNonNull(issuedAt, "issuedAt must not be null");

      return new User(this);
    }
  }

}

