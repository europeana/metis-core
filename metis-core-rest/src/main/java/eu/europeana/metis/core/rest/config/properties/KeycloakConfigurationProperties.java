package eu.europeana.metis.core.rest.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Class using {@link ConfigurationProperties} loading.
 */
@ConfigurationProperties(prefix = "keycloak")
public record KeycloakConfigurationProperties(String authServerUrl, String clientId, String clientSecret, String realm) {}
