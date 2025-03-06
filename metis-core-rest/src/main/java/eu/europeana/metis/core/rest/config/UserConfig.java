package eu.europeana.metis.core.rest.config;

import eu.europeana.metis.core.dao.DatasetDao;
import eu.europeana.metis.core.dao.UserDao;
import eu.europeana.metis.core.dao.WorkflowExecutionDao;
import eu.europeana.metis.core.mongo.MorphiaDatastoreProvider;
import eu.europeana.metis.core.rest.config.properties.KeycloakConfigurationProperties;
import eu.europeana.metis.core.service.UserService;
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
@EnableConfigurationProperties(KeycloakConfigurationProperties.class)
@EnableScheduling
public class UserConfig {

  private UserService userService;

  /**
   * Return an instance of the Keycloak admin client, which is configured from the given configuration properties.

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
   * @param keycloak the Keycloak instance used for authentication and authorization
   * @param keycloakConfigurationProperties the Keycloak configuration properties
   * @param userDao the UserDao instance to use for user related database operations
   * @param datasetDao the DatasetDao instance to use for dataset related database operations
   * @param workflowExecutionDao the WorkflowExecutionDao instance to use for workflow execution related database operations
   * @return the UserService instance
   */
  @Bean
  public UserService getUserService(Keycloak keycloak, KeycloakConfigurationProperties keycloakConfigurationProperties,
      UserDao userDao, DatasetDao datasetDao, WorkflowExecutionDao workflowExecutionDao) {
    userService = new UserService(keycloak, keycloakConfigurationProperties.realm(), userDao, datasetDao, workflowExecutionDao);
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
   * This method is called periodically to update the user cache in mongo. It is used to ensure that the user cache is up to date
   * even if the application is restarted.
   */
  @Scheduled(fixedRate = 20000)
  public void saveCacheToMongo() {
    userService.saveCacheToDatabase();
  }
}
