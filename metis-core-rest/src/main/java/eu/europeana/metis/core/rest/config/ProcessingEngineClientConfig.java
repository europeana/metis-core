package eu.europeana.metis.core.rest.config;

import eu.europeana.cloud.client.dps.rest.DpsClient;
import eu.europeana.metis.common.config.properties.ecloud.EcloudConfigurationProperties;
import eu.europeana.metis.core.engine.ecloud.DpsProcessingEngineTaskClient;
import eu.europeana.metis.core.engine.base.ProcessingEngineTask;
import eu.europeana.metis.core.engine.base.ProcessingEngineTaskClient;
import eu.europeana.metis.core.rest.config.properties.MetisCoreConfigurationProperties;
import jakarta.annotation.PreDestroy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProcessingEngineClientConfig {
  private DpsClient dpsClient;
  private ProcessingEngineTaskClient<? extends ProcessingEngineTask> processingEngineTaskClient;

  @Bean
  public ProcessingEngineTaskClient<? extends ProcessingEngineTask> externalTaskClient(
      MetisCoreConfigurationProperties metisCoreConfigurationProperties,
      EcloudConfigurationProperties ecloudConfigurationProperties) {
    String type = "ECLOUD";
    return switch (type) {
      case "ECLOUD" -> {
        processingEngineTaskClient = new DpsProcessingEngineTaskClient(dpsClient(metisCoreConfigurationProperties, ecloudConfigurationProperties));
        yield processingEngineTaskClient;
      }
      default -> throw new IllegalArgumentException("Unknown task client type: " + type);
    };
  }

  private DpsClient dpsClient(
      MetisCoreConfigurationProperties metisCoreConfigurationProperties,
      EcloudConfigurationProperties ecloudConfigurationProperties) {
    dpsClient = new DpsClient(
        ecloudConfigurationProperties.getDpsBaseUrl(),
        ecloudConfigurationProperties.getUsername(),
        ecloudConfigurationProperties.getPassword(),
        metisCoreConfigurationProperties.dpsConnectTimeoutInMilliseconds(),
        metisCoreConfigurationProperties.dpsReadTimeoutInMilliseconds());
    return dpsClient;
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
