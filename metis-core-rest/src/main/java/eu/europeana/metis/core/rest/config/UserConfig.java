package eu.europeana.metis.core.rest.config;

import metis.common.config.properties.security.KeycloakConfigurationProperties;
import eu.europeana.metis.core.service.UserService;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for user-related settings and beans.
 * <p>
 * This class provides configuration for the user service and data access objects.
 */
@Configuration
@EnableConfigurationProperties({KeycloakConfigurationProperties.class})
public class UserConfig {

  /**
   * Return an instance of the Keycloak admin client, which is configured from the given configuration properties.
   *
   * @param keycloakConfigurationProperties the configuration properties for Keycloak
   * @return an instance of the Keycloak admin client
   */
  @Bean
  public Keycloak getKeycloak(KeycloakConfigurationProperties keycloakConfigurationProperties) {
    return KeycloakBuilder.builder()
                          .serverUrl(keycloakConfigurationProperties.authServerUrl())
                          .realm(keycloakConfigurationProperties.realm())
                          .clientId(keycloakConfigurationProperties.clientId())
                          .clientSecret(keycloakConfigurationProperties.clientSecret())
                          .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                          .build();
  }

  /**
   * Create a UserService instance.
   *
   * @param keycloak the Keycloak instance used for authentication and authorization
   * @param keycloakConfigurationProperties the Keycloak configuration properties
   * @return the UserService instance
   */
  @Bean
  public UserService getUserService(Keycloak keycloak, KeycloakConfigurationProperties keycloakConfigurationProperties) {
    return new UserService(keycloak, keycloakConfigurationProperties.realm());
  }

  /**
   * Return the Keycloak realm name, which is used to authenticate and authorize users.
   *
   * @param keycloakConfigurationProperties the Keycloak configuration properties
   * @return the Keycloak realm name
   */
  @Bean
  public String getRealm(KeycloakConfigurationProperties keycloakConfigurationProperties) {
    return keycloakConfigurationProperties.realm();
  }
}
