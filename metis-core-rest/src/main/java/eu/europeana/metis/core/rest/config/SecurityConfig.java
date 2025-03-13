package eu.europeana.metis.core.rest.config;

import eu.europeana.metis.core.rest.config.properties.SecurityConfigurationProperties;
import eu.europeana.metis.core.rest.security.UserInformationClaimsExtractorFilter;
import eu.europeana.metis.core.service.UserService;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

import static eu.europeana.metis.core.common.AccountRole.ADMIN;
import static eu.europeana.metis.core.common.AccountRole.DATA_OFFICER;
import static eu.europeana.metis.utils.RestEndpoints.DATASETS_XSLT_DEFAULT;
import static eu.europeana.metis.utils.RestEndpoints.DATASETS_XSLT_XSLTID;
import static eu.europeana.metis.utils.RestEndpoints.DEPUBLISH_REASONS;

/**
 * Spring security configuration class.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(SecurityConfigurationProperties.class)
public class SecurityConfig {

  private final List<String> resourceNames;

  /**
   * Constructs a SecurityConfig object using the provided SecurityConfigurationProperties.
   *
   * @param securityConfigurationProperties the configuration properties containing resource names for security configuration.
   * This parameter is used to initialize the internal resources required for the security setup.
   */
  @Autowired
  public SecurityConfig(SecurityConfigurationProperties securityConfigurationProperties) {
    this.resourceNames = securityConfigurationProperties.getResourceNames();
  }

  /**
   * Configures the security filter chain for the application. It disables CSRF (as the API is stateless and uses JWT for
   * authentication), enables CORS, sets up authorization rules, and configures the OAuth2 resource server with JWT
   * authentication.
   *
   * @param httpSecurity the HttpSecurity to be configured with the security settings
   * @param userService the UserService instance used to authenticate and authorize users
   * @return the configured SecurityFilterChain
   * @throws Exception if an error occurs during the security configuration
   */
  @SuppressWarnings("squid:S4502")
  @Bean
  public SecurityFilterChain configure(HttpSecurity httpSecurity, UserService userService) throws Exception {
    httpSecurity.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(registry -> registry
                    .requestMatchers(HttpMethod.GET, "/actuator/**").permitAll()
                    .requestMatchers(HttpMethod.GET, DATASETS_XSLT_DEFAULT).permitAll()
                    .requestMatchers(HttpMethod.POST, DATASETS_XSLT_DEFAULT).hasRole(ADMIN.toString())
                    .requestMatchers(HttpMethod.GET, DATASETS_XSLT_XSLTID).permitAll()
                    .requestMatchers(HttpMethod.GET, DEPUBLISH_REASONS).permitAll()
                    .requestMatchers("/**").hasAnyRole(ADMIN.toString(), DATA_OFFICER.toString())
                    .anyRequest().denyAll())
                .addFilterAfter(new UserInformationClaimsExtractorFilter(userService::insertToInMemoryCacheIfExists), BearerTokenAuthenticationFilter.class)
                .oauth2ResourceServer(oauth2Configurer -> oauth2Configurer
                    .jwt(jwtConfigurer -> jwtConfigurer.jwtAuthenticationConverter(new KeycloakJwtGrantedAuthoritiesConverter())
                    )
                ).securityMatcher("/**");

    return httpSecurity.build();
  }

  private class KeycloakJwtGrantedAuthoritiesConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    public static final String REALM_ACCESS = "realm_access";
    public static final String RESOURCE_ACCESS = "resource_access";
    public static final String ROLES = "roles";
    public static final String ROLE_PREFIX = "ROLE_";
    private final JwtGrantedAuthoritiesConverter defaultGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

    @Override
    public AbstractAuthenticationToken convert(@NotNull Jwt jwt) {
      Collection<GrantedAuthority> grantedAuthorities = Optional.of(defaultGrantedAuthoritiesConverter.convert(jwt))
                                                                .orElseGet(List::of);

      final Map<String, List<String>> realmAccess = jwt.getClaim(REALM_ACCESS);
      final List<String> realmRoles = (realmAccess == null) ? List.of() : realmAccess.getOrDefault(ROLES, List.of());

      final Map<String, Map<String, List<String>>> resourceAccess = jwt.getClaim(RESOURCE_ACCESS);

      for (String resourceName : resourceNames) {
        final List<String> resourceRoles = (resourceAccess == null) ? List.of()
            : resourceAccess.getOrDefault(resourceName, Map.of()).getOrDefault(ROLES, List.of());
        grantedAuthorities.addAll(getAuthorities(resourceRoles));

      }
      grantedAuthorities.addAll(getAuthorities(realmRoles));
      return new JwtAuthenticationToken(jwt, grantedAuthorities);
    }

    private static List<SimpleGrantedAuthority> getAuthorities(List<String> resourceRoles) {
      return resourceRoles.stream()
                          .map(role -> ROLE_PREFIX + role)
                          .map(SimpleGrantedAuthority::new)
                          .toList();
    }
  }
}

