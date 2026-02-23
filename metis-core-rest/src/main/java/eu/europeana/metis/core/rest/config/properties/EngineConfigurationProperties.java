package eu.europeana.metis.core.rest.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "engine")
public record EngineConfigurationProperties(
    String baseUrl
) {

}
