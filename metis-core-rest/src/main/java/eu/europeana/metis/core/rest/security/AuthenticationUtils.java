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

  public static String getUserName(Jwt jwt) {
    return jwt.getClaimAsString("preferred_username");
  }

  public static String getFirstName(Jwt jwt) {
    return jwt.getClaimAsString("given_name");
  }

  public static String getLastName(Jwt jwt) {
    return jwt.getClaimAsString("family_name");
  }
}
