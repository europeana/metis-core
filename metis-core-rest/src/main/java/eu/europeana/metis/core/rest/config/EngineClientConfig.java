package eu.europeana.metis.core.rest.config;

import eu.europeana.cloud.client.dps.rest.DpsClient;
import eu.europeana.metis.common.config.properties.ecloud.EcloudConfigurationProperties;
import eu.europeana.metis.core.engine.base.EngineTask;
import eu.europeana.metis.core.engine.base.EngineTaskClient;
import eu.europeana.metis.core.engine.base.EngineTaskSettings;
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
  private EngineTaskClient<? extends EngineTaskSettings, ? extends EngineTask> engineTaskClient;

  @Bean
  public EngineTaskClient<? extends EngineTaskSettings, ? extends EngineTask> engineTaskClient(
      MetisCoreConfigurationProperties metisCoreConfigurationProperties,
      EcloudConfigurationProperties ecloudConfigurationProperties,
      ThrottlingValues throttlingValues) {
    String type = "ECLOUD";
    return switch (type) {
      case "ECLOUD" -> {
        dpsClient = dpsClient(metisCoreConfigurationProperties, ecloudConfigurationProperties);
        DpsEngineTaskSettings dpsProcessingEngineTaskSettings = new DpsEngineTaskSettings(
            ecloudConfigurationProperties.getBaseUrl(),
            ecloudConfigurationProperties.getProvider(),
            metisCoreConfigurationProperties.baseUrl(),
            throttlingValues
        );
        engineTaskClient = new DpsEngineTaskClient(dpsClient, dpsProcessingEngineTaskSettings);
        yield engineTaskClient;
      }
      default -> throw new IllegalArgumentException("Unknown task client type: " + type);
    };
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
