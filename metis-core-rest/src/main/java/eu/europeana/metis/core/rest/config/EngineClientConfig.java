package eu.europeana.metis.core.rest.config;

import eu.europeana.cloud.client.dps.rest.DpsClient;
import eu.europeana.cloud.client.uis.rest.UISClient;
import eu.europeana.cloud.mcs.driver.DataSetServiceClient;
import eu.europeana.cloud.mcs.driver.FileServiceClient;
import eu.europeana.cloud.mcs.driver.RecordServiceClient;
import eu.europeana.metis.common.config.properties.ecloud.EcloudConfigurationProperties;
import eu.europeana.metis.core.engine.base.EngineTaskClient;
import eu.europeana.metis.core.engine.ecloud.EcloudEngineDatasetRecordClient;
import eu.europeana.metis.core.engine.ecloud.EcloudEngineTaskClient;
import eu.europeana.metis.core.engine.ecloud.EcloudEngineTaskSettings;
import eu.europeana.metis.core.engine.sandbox.SandboxEngineTaskClient;
import eu.europeana.metis.core.engine.sandbox.SandboxEngineTaskSettings;
import eu.europeana.metis.core.rest.config.properties.EngineConfigurationProperties;
import eu.europeana.metis.core.rest.config.properties.MetisCoreConfigurationProperties;
import eu.europeana.metis.core.rest.config.properties.MetisCoreConfigurationProperties.EngineType;
import eu.europeana.metis.core.workflow.plugins.ThrottlingValues;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Configuration class responsible for providing clients and settings required for interacting with processing engines.
 * <p>
 * Determines which type of client should be initialized based on configuration properties.
 */
@Configuration
@Slf4j
@EnableConfigurationProperties({MetisCoreConfigurationProperties.class, EcloudConfigurationProperties.class,
    EngineConfigurationProperties.class})
public class EngineClientConfig {

  private DpsClient dpsClient;
  private DataSetServiceClient dataSetServiceClient;
  private RecordServiceClient recordServiceClient;
  private FileServiceClient fileServiceClient;
  private UISClient uisClient;

  @Bean
  public RestClient engineRestClient(EngineConfigurationProperties engineConfigurationProperties) {
    return RestClient.builder()
                     .baseUrl(engineConfigurationProperties.baseUrl())
                     .build();
  }

  /**
   * Configures and returns an instance of EngineTaskClient based on the engine type.
   * <p>
   * If the engine type is ECLOUD, initializes an EcloudEngineTaskClient; otherwise, initializes a MockEngineTaskClient.
   *
   * @param metisCoreConfigurationProperties The core configuration properties for the Metis engine.
   * @param ecloudConfigurationProperties The configuration properties specific to the ECLOUD engine.
   * @param throttlingValues The throttling values for managing concurrency levels.
   * @return An instance of EngineTaskClient configured based on the specified properties and engine type.
   */
  @Bean(destroyMethod = "close")
  @SuppressWarnings("java:S1452") //This is acceptable and the only way (at the time of writing) that spring allows.
  public EngineTaskClient<?, ?> engineTaskClient(
      MetisCoreConfigurationProperties metisCoreConfigurationProperties,
      EcloudConfigurationProperties ecloudConfigurationProperties,
      EngineConfigurationProperties engineConfigurationProperties,
      ThrottlingValues throttlingValues,
      RestClient restClient
  ) {
    if (EngineType.ECLOUD.equals(metisCoreConfigurationProperties.engineType())) {
      log.info("Initializing DPS Engine Task Client");
      return ecloudEngineTaskClient(metisCoreConfigurationProperties, ecloudConfigurationProperties, throttlingValues);
    } else if (EngineType.SANDBOX.equals(metisCoreConfigurationProperties.engineType())) {
      return sandboxEngineTaskClient(metisCoreConfigurationProperties, engineConfigurationProperties, restClient,
          throttlingValues);
    } else {
      throw new IllegalArgumentException("Invalid engine type: " + metisCoreConfigurationProperties.engineType());
    }
  }

  private EngineTaskClient<?, ?> sandboxEngineTaskClient(
      MetisCoreConfigurationProperties metisCoreConfigurationProperties,
      EngineConfigurationProperties engineConfigurationProperties,
      RestClient restClient, ThrottlingValues throttlingValues) {
    SandboxEngineTaskSettings sandboxEngineTaskSettings =
        new SandboxEngineTaskSettings(engineConfigurationProperties.baseUrl(), null,
            metisCoreConfigurationProperties.baseUrl(), throttlingValues);
    return new SandboxEngineTaskClient(sandboxEngineTaskSettings, restClient);
  }

  private EngineTaskClient<?, ?> ecloudEngineTaskClient(
      MetisCoreConfigurationProperties metisCoreConfigurationProperties,
      EcloudConfigurationProperties ecloudConfigurationProperties,
      ThrottlingValues throttlingValues) {

    dpsClient = dpsClient(metisCoreConfigurationProperties, ecloudConfigurationProperties);
    dataSetServiceClient = dataSetServiceClient(metisCoreConfigurationProperties, ecloudConfigurationProperties);
    recordServiceClient = recordServiceClient(metisCoreConfigurationProperties, ecloudConfigurationProperties);
    fileServiceClient = fileServiceClient(metisCoreConfigurationProperties, ecloudConfigurationProperties);
    uisClient = uisClient(metisCoreConfigurationProperties, ecloudConfigurationProperties);
    EcloudEngineTaskSettings ecloudEngineTaskSettings = new EcloudEngineTaskSettings(
        ecloudConfigurationProperties.getBaseUrl(),
        ecloudConfigurationProperties.getProvider(),
        metisCoreConfigurationProperties.baseUrl(),
        throttlingValues
    );
    final EcloudEngineDatasetRecordClient ecloudEngineDatasetRecordClient = new EcloudEngineDatasetRecordClient(
        dataSetServiceClient,
        recordServiceClient, fileServiceClient, uisClient);
    return new EcloudEngineTaskClient(dpsClient, ecloudEngineTaskSettings, ecloudEngineDatasetRecordClient);
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

  /**
   * Closes all initialized service clients, releasing their resources to prevent potential memory leaks. If a client is null, no
   * action is performed for that client.
   */
  @PreDestroy
  public void close() {
    if (dataSetServiceClient != null) {
      dataSetServiceClient.close();
    }
    if (recordServiceClient != null) {
      recordServiceClient.close();
    }
    if (fileServiceClient != null) {
      fileServiceClient.close();
    }
    if (dpsClient != null) {
      dpsClient.close();
    }
    if (uisClient != null) {
      uisClient.close();
    }
  }
}
