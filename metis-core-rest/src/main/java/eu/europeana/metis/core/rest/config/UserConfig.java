package eu.europeana.metis.core.rest.config;

import eu.europeana.metis.core.rest.config.properties.KeycloakConfigurationProperties;
import eu.europeana.metis.core.service.UserService;
import java.util.concurrent.TimeUnit;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Configuration class for user-related settings and beans.
 * <p>
 * This class provides configuration for the user service and data access objects.
 */
@Configuration
@EnableConfigurationProperties({KeycloakConfigurationProperties.class})
@EnableScheduling
public class UserConfig {

  private UserService userService;

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
    userService = new UserService(keycloak, keycloakConfigurationProperties.realm());
    return userService;
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

  /**
   * This method is called once a day and clears the cache for the user service. This is necessary to refresh the cache when the
   * user information in Keycloak changes.
   */
  @Scheduled(timeUnit = TimeUnit.MINUTES,
      initialDelayString = "#{@'metis-core-eu.europeana.metis.core.rest.config.properties.MetisCoreConfigurationProperties'.getUserCacheClearIntervalInMinutes()}",
      fixedDelayString = "#{@'metis-core-eu.europeana.metis.core.rest.config.properties.MetisCoreConfigurationProperties'.getUserCacheClearIntervalInMinutes()}")
  public void clearCache() {
    userService.clearCache();
  }
}
