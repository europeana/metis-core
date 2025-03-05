package eu.europeana.metis.core.rest.security;

import eu.europeana.metis.core.user.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.function.Consumer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

public class UserInformationClaimsExtractorFilter extends OncePerRequestFilter {

  private final Consumer<User> cacheInsertConsumer;

  public UserInformationClaimsExtractorFilter(Consumer<User> insertInCache) {
    cacheInsertConsumer = insertInCache;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
      String userId = AuthenticationUtils.getUserId(jwtAuthentication.getToken());
      String userName = AuthenticationUtils.getUserName(jwtAuthentication.getToken());
      String firstName = AuthenticationUtils.getFirstName(jwtAuthentication.getToken());
      String lastName = AuthenticationUtils.getLastName(jwtAuthentication.getToken());

      if (userId != null) {
        User user = new User();
        user.setUserId(userId);
        user.setUserName(userName);
        user.setFirstName(firstName);
        user.setLastName(lastName);

        cacheInsertConsumer.accept(user);
      }
    }
    filterChain.doFilter(request, response);
  }
}
