package eu.europeana.metis.core.rest.security;

import eu.europeana.metis.core.rest.utils.TestJwtUtils;
import eu.europeana.metis.core.user.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TestUserInformationClaimsExtractorFilter {

  @Mock
  private Consumer<User> cacheInsertConsumerMock;
  @Mock
  private HttpServletRequest request;
  @Mock
  private HttpServletResponse response;
  @Mock
  private FilterChain filterChain;
  private UserInformationClaimsExtractorFilter filter = new UserInformationClaimsExtractorFilter(cacheInsertConsumerMock);
  private final TestJwtUtils testJwtUtils = new TestJwtUtils(List.of("resource1"));

  @BeforeEach
  void setup() {
    filter = new UserInformationClaimsExtractorFilter(cacheInsertConsumerMock);
  }

  @Test
  void testDoFilterInternal_withValidJwtToken_callsCacheInsertConsumer() throws ServletException, IOException {
    final JwtAuthenticationToken jwtAuthentication = new JwtAuthenticationToken(testJwtUtils.getDataOfficerJwt());
    SecurityContextHolder.getContext().setAuthentication(jwtAuthentication);
    filter.doFilterInternal(request, response, filterChain);
    verify(cacheInsertConsumerMock).accept(notNull());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void testDoFilterInternal_withNullUserId() throws ServletException, IOException {
    final JwtAuthenticationToken jwtAuthentication = new JwtAuthenticationToken(testJwtUtils.getJwtNoUserId());
    SecurityContextHolder.getContext().setAuthentication(jwtAuthentication);
    filter.doFilterInternal(request, response, filterChain);
    verify(cacheInsertConsumerMock, never()).accept(any());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void testDoFilterInternal_withEmptyUserId() throws ServletException, IOException {
    final JwtAuthenticationToken jwtAuthentication = new JwtAuthenticationToken(testJwtUtils.getJwtWithEmptyStringUserId());
    SecurityContextHolder.getContext().setAuthentication(jwtAuthentication);
    filter.doFilterInternal(request, response, filterChain);
    verify(cacheInsertConsumerMock, never()).accept(any());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void testDoFilterInternal_withNoAuthentication() throws ServletException, IOException {
    SecurityContextHolder.clearContext();
    filter.doFilterInternal(request, response, filterChain);
    verify(cacheInsertConsumerMock, never()).accept(any());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void testDoFilterInternal_withInvalidAuthentication() throws ServletException, IOException {
    Authentication authentication = mock(Authentication.class);
    SecurityContextHolder.getContext().setAuthentication(authentication);
    filter.doFilterInternal(request, response, filterChain);
    verify(cacheInsertConsumerMock, never()).accept(any());
    verify(filterChain).doFilter(request, response);
  }
}
