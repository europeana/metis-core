package eu.europeana.metis.core.rest.config;

import eu.europeana.metis.core.dao.UserDao;
import eu.europeana.metis.core.mongo.MorphiaDatastoreProvider;
import eu.europeana.metis.core.rest.config.properties.KeycloakConfigurationProperties;
import eu.europeana.metis.core.service.UserService;
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
@EnableConfigurationProperties(KeycloakConfigurationProperties.class)
@EnableScheduling
public class UserConfig {

  private UserService userService;

  //  @Bean
  //  public Keycloak getKeycloak(KeycloakConfigurationProperties keycloakConfigurationProperties) {
  //    return KeycloakBuilder.builder()
  //                          .serverUrl(keycloakConfigurationProperties.authServerUrl())
  //                          .realm(keycloakConfigurationProperties.realm())
  //                          .clientId(keycloakConfigurationProperties.clientId())
  //                          .clientSecret(keycloakConfigurationProperties.clientSecret())
  //                          .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
  //                          .build();
  //  }
  //
  //  @Bean
  //  public UserService getUserService(Keycloak keycloak, KeycloakConfigurationProperties keycloakConfigurationProperties, UserDao userDao) {
  //    return new UserService(keycloak, keycloakConfigurationProperties.realm(), userDao);
  //  }

  /**
   * Get the DAO for users.
   *
   * @param morphiaDatastoreProvider the provider for accessing the Morphia datastore
   * @return an instance of UserDao configured with the given datastore provider
   */
  @Bean
  public UserDao getUserDao(MorphiaDatastoreProvider morphiaDatastoreProvider) {
    return new UserDao(morphiaDatastoreProvider);
  }

  /**
   * Create a UserService instance.
   *
   * @param userDao the UserDao instance to use for user related database operations
   * @return the UserService instance
   */
  @Bean
  public UserService getUserService(UserDao userDao) {
    userService = new UserService(userDao);
    return userService;
  }

  //  @Bean
  //  public String getRealm(KeycloakConfigurationProperties keycloakConfigurationProperties){
  //    return keycloakConfigurationProperties.realm();
  //  }

  /**
   * This method is called periodically to update the user cache in mongo. It is used to ensure that the user cache is up to date
   * even if the application is restarted.
   */
  @Scheduled(fixedRate = 20000)
  public void saveCacheToMongo() {
    userService.saveCacheToDatabase();
  }
}
