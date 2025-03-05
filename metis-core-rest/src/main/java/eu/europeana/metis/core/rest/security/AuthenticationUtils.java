package eu.europeana.metis.core.rest.security;

import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Utility class for authentication.
 */
public final class AuthenticationUtils {

  private AuthenticationUtils() {
  }

  /**
   * Extracts the user ID from a JWT token by retrieving the "sub" claim.
   *
   * @param jwt the JWT token containing user information
   * @return the user ID extracted from the "sub" claim of the token
   */
  public static String getUserId(Jwt jwt) {
    return jwt.getClaimAsString("sub");
  }

  /**
   * Extracts the username from a JWT token by retrieving the "preferred_username" claim.
   *
   * @param jwt the JWT token containing user information
   * @return the username extracted from the "preferred_username" claim of the token
   */
  public static String getUserName(Jwt jwt) {
    return jwt.getClaimAsString("preferred_username");
  }

  /**
   * Extracts the user first name from a JWT token by retrieving the "given_name"
   * claim.
   *
   * @param jwt the JWT token containing user information
   * @return the user first name extracted from the "given_name" claim of the token
   */
  public static String getFirstName(Jwt jwt) {
    return jwt.getClaimAsString("given_name");
  }

  /**
   * Extracts the user last name from a JWT token by retrieving the "family_name" claim.
   *
   * @param jwt the JWT token containing user information
   * @return the user last name extracted from the "family_name" claim of the token
   */
  public static String getLastName(Jwt jwt) {
    return jwt.getClaimAsString("family_name");
  }
}
