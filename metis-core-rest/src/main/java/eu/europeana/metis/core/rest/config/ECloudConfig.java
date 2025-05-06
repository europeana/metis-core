package eu.europeana.metis.core.rest.config;

import eu.europeana.cloud.mcs.driver.DataSetServiceClient;
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

  /**
   * Close all open clients.
   */
  @PreDestroy
  public void close() {
    if (dataSetServiceClient != null) {
      dataSetServiceClient.close();
    }
  }
}
