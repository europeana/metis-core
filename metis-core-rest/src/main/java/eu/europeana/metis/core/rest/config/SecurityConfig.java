package eu.europeana.metis.core.rest.config;

import static eu.europeana.metis.security.AccountRole.ADMIN;
import static eu.europeana.metis.security.AccountRole.DATA_OFFICER;
import static eu.europeana.metis.security.KeycloakJwtGrantedAuthoritiesConverter.buildResourceRoles;
import static eu.europeana.metis.utils.RestEndpoints.DATASETS_XSLT_DEFAULT;
import static eu.europeana.metis.utils.RestEndpoints.DATASETS_XSLT_XSLTID;
import static eu.europeana.metis.utils.RestEndpoints.DEPUBLISH_REASONS;
import static java.util.Objects.requireNonNull;

import eu.europeana.metis.core.rest.security.UserInformationClaimsExtractorFilter;
import eu.europeana.metis.core.service.UserService;
import eu.europeana.metis.security.KeycloakJwtGrantedAuthoritiesConverter;
import java.util.List;
import eu.europeana.metis.common.config.properties.security.SecurityConfigurationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

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
    this.resourceNames = securityConfigurationProperties.resourceNames();
    requireNonNull(this.resourceNames, "The resourceNames property must be set in the security configuration.");
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
    KeycloakJwtGrantedAuthoritiesConverter keycloakJwtGrantedAuthoritiesConverter =
        new KeycloakJwtGrantedAuthoritiesConverter(resourceNames, false);
    httpSecurity.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(registry -> registry
                    .requestMatchers(HttpMethod.GET, "/actuator/**").permitAll()
                    .requestMatchers(HttpMethod.GET, DATASETS_XSLT_DEFAULT).permitAll()
                    .requestMatchers(HttpMethod.POST, DATASETS_XSLT_DEFAULT)
                    .hasAnyRole(buildResourceRoles(resourceNames, List.of(ADMIN.toString())))
                    .requestMatchers(HttpMethod.GET, DATASETS_XSLT_XSLTID).permitAll()
                    .requestMatchers(HttpMethod.GET, DEPUBLISH_REASONS).permitAll()
                    .requestMatchers("/**")
                    .hasAnyRole(buildResourceRoles(resourceNames, List.of(ADMIN.toString(), DATA_OFFICER.toString())))
                    .anyRequest().denyAll())
                .addFilterAfter(new UserInformationClaimsExtractorFilter(userService::insertToInMemoryCacheIfExists), BearerTokenAuthenticationFilter.class)
                .oauth2ResourceServer(oauth2Configurer -> oauth2Configurer
                    .jwt(jwtConfigurer -> jwtConfigurer.jwtAuthenticationConverter(keycloakJwtGrantedAuthoritiesConverter)
                    )
                ).securityMatcher("/**");

    return httpSecurity.build();
  }
}

