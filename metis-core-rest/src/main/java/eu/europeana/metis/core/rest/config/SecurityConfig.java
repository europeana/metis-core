package eu.europeana.metis.core.rest.config;

import static eu.europeana.metis.core.common.AccountRole.ADMIN;
import static eu.europeana.metis.core.common.AccountRole.DATA_OFFICER;
import static eu.europeana.metis.utils.RestEndpoints.DATASETS_XSLT_DEFAULT;
import static eu.europeana.metis.utils.RestEndpoints.DATASETS_XSLT_XSLTID;
import static eu.europeana.metis.utils.RestEndpoints.DEPUBLISH_REASONS;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  public static final String SECURED = "/secured";
  @Value("${spring.security.oauth2.resourceserver.jwt.resourceNames}")
  private String[] resourceNames;

  @Bean
  public SecurityFilterChain configure(HttpSecurity httpSecurity) throws Exception {
    httpSecurity
        .authorizeHttpRequests(registry -> registry
            .requestMatchers(HttpMethod.GET, SECURED + DATASETS_XSLT_DEFAULT).permitAll()
            .requestMatchers(HttpMethod.POST, SECURED + DATASETS_XSLT_DEFAULT).hasRole(ADMIN.name())
            .requestMatchers(HttpMethod.GET, SECURED + DATASETS_XSLT_XSLTID).permitAll()
            .requestMatchers(HttpMethod.GET, SECURED + DEPUBLISH_REASONS).permitAll()
            .requestMatchers(SECURED + "/**").hasAnyRole(ADMIN.name(), DATA_OFFICER.name())
            .anyRequest().authenticated()
        )
        .oauth2ResourceServer(oauth2Configurer -> oauth2Configurer
            .jwt(jwtConfigurer -> jwtConfigurer.jwtAuthenticationConverter(new KeycloakJwtGrantedAuthoritiesConverter())
            )
        ).securityMatcher(SECURED + "/**");

    return httpSecurity.build();
  }

  private class KeycloakJwtGrantedAuthoritiesConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    public static final String REALM_ACCESS = "realm_access";
    public static final String RESOURCE_ACCESS = "resource_access";
    public static final String ROLES = "roles";
    private final JwtGrantedAuthoritiesConverter defaultGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
      Collection<GrantedAuthority> grantedAuthorities = Optional.of(defaultGrantedAuthoritiesConverter.convert(jwt)).orElseGet(List::of);

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
                                      .map(role -> "ROLE_" + role)
                                      .map(SimpleGrantedAuthority::new)
                                      .toList();
    }
  }
}

