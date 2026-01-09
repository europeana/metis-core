package eu.europeana.metis.core.rest.config;

import eu.europeana.metis.core.rest.config.properties.MetisCoreConfigurationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

  private final MetisCoreConfigurationProperties metisCoreConfigurationProperties;

  /**
   * Constructor.
   *
   * @param metisCoreConfigurationProperties The properties.
   */
  @Autowired
  public WebMvcConfig(MetisCoreConfigurationProperties metisCoreConfigurationProperties) {
    this.metisCoreConfigurationProperties = metisCoreConfigurationProperties;
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/**").allowedMethods("GET", "HEAD", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedOrigins(metisCoreConfigurationProperties.allowedCorsHosts().toArray(String[]::new));
  }
}
