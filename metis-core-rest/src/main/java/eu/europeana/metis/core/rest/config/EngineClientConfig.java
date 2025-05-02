package eu.europeana.metis.core.rest.config;

import eu.europeana.cloud.client.dps.rest.DpsClient;
import eu.europeana.metis.common.config.properties.ecloud.EcloudConfigurationProperties;
import eu.europeana.metis.core.engine.ecloud.DpsEngineTaskClient;
import eu.europeana.metis.core.engine.ecloud.DpsEngineTaskSettings;
import eu.europeana.metis.core.engine.mock.MockEngineTaskClient;
import eu.europeana.metis.core.engine.mock.MockEngineTaskSettings;
import eu.europeana.metis.core.rest.config.properties.MetisCoreConfigurationProperties;
import eu.europeana.metis.core.workflow.plugins.ThrottlingValues;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EngineClientConfig {

  private DpsClient dpsClient;
  private DpsEngineTaskClient engineTaskClient;
  private MockEngineTaskClient mockEngineTaskClient;

  @Bean
  @ConditionalOnProperty(name = "engine.mode", havingValue = "real")
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

  @Bean
  @ConditionalOnProperty(name = "engine.mode", havingValue = "mock", matchIfMissing = true)
  public MockEngineTaskClient mockEngineTaskClient(
      MetisCoreConfigurationProperties metisCoreConfigurationProperties,
      EcloudConfigurationProperties ecloudConfigurationProperties,
      ThrottlingValues throttlingValues) {
    dpsClient = dpsClient(metisCoreConfigurationProperties, ecloudConfigurationProperties);
    MockEngineTaskSettings mockEngineTaskSettings = new MockEngineTaskSettings(
        ecloudConfigurationProperties.getBaseUrl(),
        ecloudConfigurationProperties.getProvider(),
        metisCoreConfigurationProperties.baseUrl(),
        throttlingValues
    );
    mockEngineTaskClient = new MockEngineTaskClient(dpsClient, mockEngineTaskSettings);

    return mockEngineTaskClient;
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
    if (mockEngineTaskClient != null) {
      mockEngineTaskClient.close();
    }
    if (dpsClient != null) {
      dpsClient.close();
    }
  }

}
