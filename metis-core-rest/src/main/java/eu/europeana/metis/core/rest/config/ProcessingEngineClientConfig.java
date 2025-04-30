package eu.europeana.metis.core.rest.config;

import eu.europeana.cloud.client.dps.rest.DpsClient;
import eu.europeana.metis.common.config.properties.ecloud.EcloudConfigurationProperties;
import eu.europeana.metis.core.engine.base.ProcessingEngineTask;
import eu.europeana.metis.core.engine.base.ProcessingEngineTaskClient;
import eu.europeana.metis.core.engine.base.ProcessingEngineTaskSettings;
import eu.europeana.metis.core.engine.ecloud.DpsProcessingEngineTaskClient;
import eu.europeana.metis.core.engine.ecloud.DpsProcessingEngineTaskSettings;
import eu.europeana.metis.core.rest.config.properties.MetisCoreConfigurationProperties;
import eu.europeana.metis.core.workflow.plugins.ThrottlingValues;
import jakarta.annotation.PreDestroy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProcessingEngineClientConfig {
  private DpsClient dpsClient;
  private ProcessingEngineTaskClient<? extends ProcessingEngineTaskSettings, ? extends ProcessingEngineTask> processingEngineTaskClient;

  @Bean
  public ProcessingEngineTaskClient<? extends ProcessingEngineTaskSettings, ? extends ProcessingEngineTask> externalTaskClient(
      MetisCoreConfigurationProperties metisCoreConfigurationProperties,
      EcloudConfigurationProperties ecloudConfigurationProperties,
      ThrottlingValues throttlingValues) {
    String type = "ECLOUD";
    return switch (type) {
      case "ECLOUD" -> {
        dpsClient = dpsClient(metisCoreConfigurationProperties, ecloudConfigurationProperties);
        DpsProcessingEngineTaskSettings dpsProcessingEngineTaskSettings = new DpsProcessingEngineTaskSettings(
            ecloudConfigurationProperties.getBaseUrl(),
            ecloudConfigurationProperties.getProvider(),
            metisCoreConfigurationProperties.baseUrl(),
            throttlingValues
        );
        processingEngineTaskClient = new DpsProcessingEngineTaskClient(dpsClient, dpsProcessingEngineTaskSettings);
        yield processingEngineTaskClient;
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
    if (processingEngineTaskClient != null) {
      processingEngineTaskClient.close();
    }
    if (dpsClient != null) {
      dpsClient.close();
    }
  }

}
