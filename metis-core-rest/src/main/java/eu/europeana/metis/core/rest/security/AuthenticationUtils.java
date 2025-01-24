package eu.europeana.metis.core.rest.security;

import eu.europeana.metis.exception.BadContentException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Utility class for authentication.
 */
public class AuthenticationUtils {

  private AuthenticationUtils() {
  }

  public static String getUserEmail(Authentication authentication) throws BadContentException {
    final String email;
    if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
      email = jwt.getClaimAsString("email");
    } else {
      throw new BadContentException("Jwt does not contain email address of user");
    }
    return email;
  }
}
