package eu.europeana.metis.core.rest.config;

import eu.europeana.cloud.client.dps.rest.DpsClient;
import eu.europeana.metis.common.config.properties.ecloud.EcloudConfigurationProperties;
import eu.europeana.metis.core.engine.ecloud.DpsEngineTaskClient;
import eu.europeana.metis.core.engine.ecloud.DpsEngineTaskSettings;
import eu.europeana.metis.core.rest.config.properties.MetisCoreConfigurationProperties;
import eu.europeana.metis.core.workflow.plugins.ThrottlingValues;
import jakarta.annotation.PreDestroy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EngineClientConfig {

  private DpsClient dpsClient;
  private DpsEngineTaskClient engineTaskClient;

  @Bean
  public DpsEngineTaskClient engineTaskClient(
      MetisCoreConfigurationProperties metisCoreConfigurationProperties,
      EcloudConfigurationProperties ecloudConfigurationProperties,
      ThrottlingValues throttlingValues) {
    dpsClient = dpsClient(metisCoreConfigurationProperties, ecloudConfigurationProperties);
    DpsEngineTaskSettings dpsProcessingEngineTaskSettings = new DpsEngineTaskSettings(
        ecloudConfigurationProperties.getBaseUrl(),
        ecloudConfigurationProperties.getProvider(),
        metisCoreConfigurationProperties.baseUrl(),
        throttlingValues
    );
    engineTaskClient = new DpsEngineTaskClient(dpsClient, dpsProcessingEngineTaskSettings);

    return engineTaskClient;
  }

  private DpsClient dpsClient(
      MetisCoreConfigurationProperties metisCoreConfigurationProperties,
      EcloudConfigurationProperties ecloudConfigurationProperties) {
    return new DpsClient(
        ecloudConfigurationProperties.getDpsBaseUrl(),
        ecloudConfigurationProperties.getUsername(),
        ecloudConfigurationProperties.getPassword(),
        metisCoreConfigurationProperties.dpsConnectTimeoutInMilliseconds(),
        metisCoreConfigurationProperties.dpsReadTimeoutInMilliseconds());
  }

  @PreDestroy
  public void close() {
    if (engineTaskClient != null) {
      engineTaskClient.close();
    }
    if (dpsClient != null) {
      dpsClient.close();
    }
  }

}
