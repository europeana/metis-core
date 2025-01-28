package eu.europeana.metis.core.rest.security;

import static java.lang.String.format;

import eu.europeana.metis.exception.BadContentException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Utility class for authentication.
 */
public class AuthenticationUtils {

  private AuthenticationUtils() {
  }

  public static String getSubClaim(Authentication authentication) throws BadContentException {
    return getPrincipalClaim(authentication, "sub");
  }

  public static String getPrincipalClaim(Authentication authentication, String claimName) throws BadContentException {
    final String claimValue;
    if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
      claimValue = jwt.getClaimAsString(claimName);
    } else {
      throw new BadContentException(format("Jwt does not contain claim %s of user", claimName));
    }
    return claimValue;
  }
}
