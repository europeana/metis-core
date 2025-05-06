package eu.europeana.metis.core.rest.config;

import eu.europeana.cloud.client.dps.rest.DpsClient;
import eu.europeana.cloud.client.uis.rest.UISClient;
import eu.europeana.cloud.mcs.driver.DataSetServiceClient;
import eu.europeana.cloud.mcs.driver.FileServiceClient;
import eu.europeana.cloud.mcs.driver.RecordServiceClient;
import eu.europeana.metis.common.config.properties.ecloud.EcloudConfigurationProperties;
import eu.europeana.metis.core.engine.base.EngineTaskClient;
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
  private DataSetServiceClient dataSetServiceClient;
  private RecordServiceClient recordServiceClient;
  private FileServiceClient fileServiceClient;
  private UISClient uisClient;

  @Bean(destroyMethod = "close")
  @ConditionalOnProperty(name = "engine.mode", havingValue = "real")
  public EngineTaskClient<?, ?> engineTaskClient(
      MetisCoreConfigurationProperties metisCoreConfigurationProperties,
      EcloudConfigurationProperties ecloudConfigurationProperties,
      ThrottlingValues throttlingValues) {

    dpsClient = dpsClient(metisCoreConfigurationProperties, ecloudConfigurationProperties);
    dataSetServiceClient = dataSetServiceClient(metisCoreConfigurationProperties, ecloudConfigurationProperties);
    recordServiceClient = recordServiceClient(metisCoreConfigurationProperties, ecloudConfigurationProperties);
    fileServiceClient = fileServiceClient(metisCoreConfigurationProperties, ecloudConfigurationProperties);
    uisClient = uisClient(metisCoreConfigurationProperties, ecloudConfigurationProperties);
    DpsEngineTaskSettings dpsProcessingEngineTaskSettings = new DpsEngineTaskSettings(
        ecloudConfigurationProperties.getBaseUrl(),
        ecloudConfigurationProperties.getProvider(),
        metisCoreConfigurationProperties.baseUrl(),
        throttlingValues
    );
    return new DpsEngineTaskClient(dpsClient, dataSetServiceClient, recordServiceClient, fileServiceClient, uisClient, dpsProcessingEngineTaskSettings);
  }

  //todo not really a mock yet
  @Bean(destroyMethod = "close")
  @ConditionalOnProperty(name = "engine.mode", havingValue = "mock", matchIfMissing = true)
  public EngineTaskClient<?, ?> mockEngineTaskClient(
      MetisCoreConfigurationProperties metisCoreConfigurationProperties,
      EcloudConfigurationProperties ecloudConfigurationProperties,
      ThrottlingValues throttlingValues) {

    dpsClient = dpsClient(metisCoreConfigurationProperties, ecloudConfigurationProperties);
    dataSetServiceClient = dataSetServiceClient(metisCoreConfigurationProperties, ecloudConfigurationProperties);
    recordServiceClient = recordServiceClient(metisCoreConfigurationProperties, ecloudConfigurationProperties);
    fileServiceClient = fileServiceClient(metisCoreConfigurationProperties, ecloudConfigurationProperties);
    uisClient = uisClient(metisCoreConfigurationProperties, ecloudConfigurationProperties);
    MockEngineTaskSettings mockEngineTaskSettings = new MockEngineTaskSettings(
        ecloudConfigurationProperties.getBaseUrl(),
        ecloudConfigurationProperties.getProvider(),
        metisCoreConfigurationProperties.baseUrl(),
        throttlingValues
    );
    return new MockEngineTaskClient(dpsClient, dataSetServiceClient, recordServiceClient, fileServiceClient, uisClient, mockEngineTaskSettings);
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

  private DataSetServiceClient dataSetServiceClient(
      MetisCoreConfigurationProperties metisCoreConfigurationProperties,
      EcloudConfigurationProperties ecloudConfigurationProperties) {
    return new DataSetServiceClient(
        ecloudConfigurationProperties.getBaseUrl(),
        ecloudConfigurationProperties.getUsername(),
        ecloudConfigurationProperties.getPassword(),
        metisCoreConfigurationProperties.dpsConnectTimeoutInMilliseconds(),
        metisCoreConfigurationProperties.dpsReadTimeoutInMilliseconds());
  }

  private RecordServiceClient recordServiceClient(
      MetisCoreConfigurationProperties metisCoreConfigurationProperties,
      EcloudConfigurationProperties ecloudConfigurationProperties) {
    return new RecordServiceClient(
        ecloudConfigurationProperties.getBaseUrl(),
        ecloudConfigurationProperties.getUsername(),
        ecloudConfigurationProperties.getPassword(),
        metisCoreConfigurationProperties.dpsConnectTimeoutInMilliseconds(),
        metisCoreConfigurationProperties.dpsReadTimeoutInMilliseconds());
  }

  private FileServiceClient fileServiceClient(
      MetisCoreConfigurationProperties metisCoreConfigurationProperties,
      EcloudConfigurationProperties ecloudConfigurationProperties) {
    return new FileServiceClient(
        ecloudConfigurationProperties.getBaseUrl(),
        ecloudConfigurationProperties.getUsername(),
        ecloudConfigurationProperties.getPassword(),
        metisCoreConfigurationProperties.dpsConnectTimeoutInMilliseconds(),
        metisCoreConfigurationProperties.dpsReadTimeoutInMilliseconds());
  }

  private UISClient uisClient(
      MetisCoreConfigurationProperties metisCoreConfigurationProperties,
      EcloudConfigurationProperties ecloudConfigurationProperties) {
    return new UISClient(
        ecloudConfigurationProperties.getBaseUrl(),
        ecloudConfigurationProperties.getUsername(),
        ecloudConfigurationProperties.getPassword(),
        metisCoreConfigurationProperties.dpsConnectTimeoutInMilliseconds(),
        metisCoreConfigurationProperties.dpsReadTimeoutInMilliseconds());
  }

  @PreDestroy
  public void close() {
    if (dpsClient != null) {
      dpsClient.close();
    }
    if (dataSetServiceClient != null) {
      dataSetServiceClient.close();
    }
    if (recordServiceClient != null) {
      recordServiceClient.close();
    }
    if (fileServiceClient != null) {
      fileServiceClient.close();
    }
    if (uisClient != null) {
      uisClient.close();
    }
  }
}
