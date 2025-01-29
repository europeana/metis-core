package eu.europeana.metis.core.rest.security;

import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Utility class for authentication.
 */
public class AuthenticationUtils {

  private AuthenticationUtils() {
  }

  public static String getUserId(Jwt jwt) {
    return jwt.getClaimAsString("sub");
  }
}
