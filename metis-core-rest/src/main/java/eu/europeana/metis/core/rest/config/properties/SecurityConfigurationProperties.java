package eu.europeana.metis.core.rest.config.properties;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Class using {@link ConfigurationProperties} loading for custom security properties.
 */
@ConfigurationProperties(prefix = "security.oauth2")
public class SecurityConfigurationProperties {
  private List<String> resourceNames;

  public List<String> getResourceNames() {
    return resourceNames == null ? List.of() : List.copyOf(resourceNames);
  }

  public void setResourceNames(List<String> resourceNames) {
    this.resourceNames = resourceNames == null ? null : List.copyOf(resourceNames);
  }
}
