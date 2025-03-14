package eu.europeana.metis.core.rest.security;

import eu.europeana.metis.core.user.User;
import eu.europeana.metis.core.user.User.UserBuilder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * A filter that extracts user information claims from a JWT authentication token and inserts them into a cache.
 * <p>
 * This filter is designed to be used in a Spring-based web application to authenticate users and extract their information from a
 * JWT token.
 *
 * @see OncePerRequestFilter
 * @see JwtAuthenticationToken
 */
public class UserInformationClaimsExtractorFilter extends OncePerRequestFilter {

  private final Consumer<User> cacheInsertConsumer;

  /**
   * Constructs a new UserInformationClaimsExtractorFilter instance.
   *
   * @param insertInCache a Consumer that inserts the extracted user information into a cache
   */
  public UserInformationClaimsExtractorFilter(Consumer<User> insertInCache) {
    cacheInsertConsumer = insertInCache;
  }

  @Override
  protected void doFilterInternal(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response,
      @NotNull FilterChain filterChain)
      throws ServletException, IOException {

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
      String userId = AuthenticationUtils.getUserId(jwtAuthentication.getToken());
      String userName = AuthenticationUtils.getUserName(jwtAuthentication.getToken());
      String firstName = AuthenticationUtils.getFirstName(jwtAuthentication.getToken());
      String lastName = AuthenticationUtils.getLastName(jwtAuthentication.getToken());
      Instant issuedAt = AuthenticationUtils.getIssuedAt(jwtAuthentication.getToken());

      if (isNotBlank(userId)) {
        UserBuilder userBuilder = new UserBuilder()
            .userId(userId)
            .userName(userName)
            .firstName(firstName)
            .lastName(lastName)
            .issuedAt(issuedAt);

        cacheInsertConsumer.accept(userBuilder.build());
      }
    }
    filterChain.doFilter(request, response);
  }
}
