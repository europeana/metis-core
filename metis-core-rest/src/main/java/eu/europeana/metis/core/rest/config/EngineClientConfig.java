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
import eu.europeana.metis.core.engine.mock.MockEngineTaskClient;
import eu.europeana.metis.core.engine.mock.MockEngineTaskSettings;
import eu.europeana.metis.core.rest.config.properties.MetisCoreConfigurationProperties;
import eu.europeana.metis.core.rest.config.properties.MetisCoreConfigurationProperties.EngineType;
import eu.europeana.metis.core.workflow.plugins.ThrottlingValues;
import jakarta.annotation.PreDestroy;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class responsible for providing clients and settings required for interacting with processing engines.
 * <p>
 * Determines which type of client should be initialized based on configuration properties.
 */
@Configuration
public class EngineClientConfig {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private DpsClient dpsClient;
  private DataSetServiceClient dataSetServiceClient;
  private RecordServiceClient recordServiceClient;
  private FileServiceClient fileServiceClient;
  private UISClient uisClient;

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
      ThrottlingValues throttlingValues
  ) {
    if (EngineType.ECLOUD.equals(metisCoreConfigurationProperties.engineType())) {
      LOGGER.info("Initializing DPS Engine Task Client");
      return ecloudEngineTaskClient(metisCoreConfigurationProperties, ecloudConfigurationProperties, throttlingValues);
    } else {
      LOGGER.info("Initializing Mock Engine Task Client");
      return mockEngineTaskClient(metisCoreConfigurationProperties, ecloudConfigurationProperties, throttlingValues);
    }
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

  //todo not really a mock yet
  private EngineTaskClient<?, ?> mockEngineTaskClient(
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
    final EcloudEngineDatasetRecordClient ecloudEngineDatasetRecordClient = new EcloudEngineDatasetRecordClient(
        dataSetServiceClient,
        recordServiceClient, fileServiceClient, uisClient);
    return new MockEngineTaskClient(dpsClient, mockEngineTaskSettings, ecloudEngineDatasetRecordClient);
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
