package eu.europeana.metis.core.rest.config;

import eu.europeana.cloud.client.uis.rest.UISClient;
import eu.europeana.cloud.mcs.driver.DataSetServiceClient;
import eu.europeana.cloud.mcs.driver.FileServiceClient;
import eu.europeana.cloud.mcs.driver.RecordServiceClient;
import eu.europeana.metis.common.config.properties.ecloud.EcloudConfigurationProperties;
import eu.europeana.metis.core.rest.config.properties.MetisCoreConfigurationProperties;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * ECloud configuration class.
 */
@Configuration
@EnableConfigurationProperties({
    MetisCoreConfigurationProperties.class, EcloudConfigurationProperties.class})
@ComponentScan(basePackages = {"eu.europeana.metis.core.rest.controller"})
public class ECloudConfig {

  private DataSetServiceClient dataSetServiceClient;
  private RecordServiceClient recordServiceClient;
  private FileServiceClient fileServiceClient;
  private UISClient uisClient;


  @Bean
  DataSetServiceClient dataSetServiceClient(
      MetisCoreConfigurationProperties metisCoreConfigurationProperties,
      EcloudConfigurationProperties ecloudConfigurationProperties) {
    dataSetServiceClient = new DataSetServiceClient(
        ecloudConfigurationProperties.getBaseUrl(),
        ecloudConfigurationProperties.getUsername(),
        ecloudConfigurationProperties.getPassword(),
        metisCoreConfigurationProperties.dpsConnectTimeoutInMilliseconds(),
        metisCoreConfigurationProperties.dpsReadTimeoutInMilliseconds());
    return dataSetServiceClient;
  }

  @Bean
  RecordServiceClient recordServiceClient(
      MetisCoreConfigurationProperties metisCoreConfigurationProperties,
      EcloudConfigurationProperties ecloudConfigurationProperties) {
    recordServiceClient = new RecordServiceClient(
        ecloudConfigurationProperties.getBaseUrl(),
        ecloudConfigurationProperties.getUsername(),
        ecloudConfigurationProperties.getPassword(),
        metisCoreConfigurationProperties.dpsConnectTimeoutInMilliseconds(),
        metisCoreConfigurationProperties.dpsReadTimeoutInMilliseconds());
    return recordServiceClient;
  }

  @Bean
  FileServiceClient fileServiceClient(
      MetisCoreConfigurationProperties metisCoreConfigurationProperties,
      EcloudConfigurationProperties ecloudConfigurationProperties) {
    fileServiceClient = new FileServiceClient(
        ecloudConfigurationProperties.getBaseUrl(),
        ecloudConfigurationProperties.getUsername(),
        ecloudConfigurationProperties.getPassword(),
        metisCoreConfigurationProperties.dpsConnectTimeoutInMilliseconds(),
        metisCoreConfigurationProperties.dpsReadTimeoutInMilliseconds());
    return fileServiceClient;
  }

  @Bean
  UISClient uisClient(
      MetisCoreConfigurationProperties metisCoreConfigurationProperties,
      EcloudConfigurationProperties ecloudConfigurationProperties) {
    uisClient = new UISClient(
        ecloudConfigurationProperties.getBaseUrl(),
        ecloudConfigurationProperties.getUsername(),
        ecloudConfigurationProperties.getPassword(),
        metisCoreConfigurationProperties.dpsConnectTimeoutInMilliseconds(),
        metisCoreConfigurationProperties.dpsReadTimeoutInMilliseconds());
    return uisClient;
  }

  /**
   * Close all open clients.
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
    if (uisClient != null) {
      uisClient.close();
    }
  }
}
