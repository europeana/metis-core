package eu.europeana.metis.core.rest.config;

import jakarta.annotation.PreDestroy;
import java.lang.invoke.MethodHandles;
import java.net.MalformedURLException;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import eu.europeana.metis.common.config.properties.TruststoreConfigurationProperties;
import eu.europeana.metis.common.config.properties.redis.RedisConfigurationProperties;
import eu.europeana.metis.common.config.properties.redis.RedissonConfigurationProperties;
import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redis configuration class.
 */
@Configuration
public class RedisConfig {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private RedissonClient redissonClient;

  @Bean
  RedissonClient getRedissonClient(
      TruststoreConfigurationProperties truststoreConfigurationProperties,
      RedisConfigurationProperties redisConfigurationProperties)
      throws MalformedURLException {
    Config config = new Config();

    SingleServerConfig singleServerConfig;
    if (redisConfigurationProperties.isEnableSsl()) {
      singleServerConfig = config.useSingleServer().setAddress(String
          .format("rediss://%s:%s", redisConfigurationProperties.getHost(),
              redisConfigurationProperties.getPort()));
      LOGGER.info("Redis enabled SSL");
      if (redisConfigurationProperties.isEnableCustomTruststore()) {
        singleServerConfig
            .setSslTruststore(Paths.get(truststoreConfigurationProperties.getPath()).toUri().toURL());
        singleServerConfig.setSslTruststorePassword(truststoreConfigurationProperties.getPassword());
        LOGGER.info("Redis enabled SSL using custom Truststore");
      }
    } else {
      singleServerConfig = config.useSingleServer().setAddress(String
          .format("redis://%s:%s", redisConfigurationProperties.getHost(),
              redisConfigurationProperties.getPort()));
      LOGGER.info("Redis disabled SSL");
    }
    if (StringUtils.isNotEmpty(redisConfigurationProperties.getUsername())) {
      singleServerConfig.setUsername(redisConfigurationProperties.getUsername());
    }
    if (StringUtils.isNotEmpty(redisConfigurationProperties.getPassword())) {
      singleServerConfig.setPassword(redisConfigurationProperties.getPassword());
    }

    RedissonConfigurationProperties redisson = redisConfigurationProperties.getRedisson();
    singleServerConfig.setConnectionPoolSize(redisson.getConnectionPoolSize())
                      .setConnectionMinimumIdleSize(redisson.getConnectionPoolSize())
                      .setConnectTimeout((int) TimeUnit.SECONDS.toMillis(redisson.getConnectTimeoutInSeconds()))
                      .setDnsMonitoringInterval((int) TimeUnit.SECONDS.toMillis(redisson.getDnsMonitorIntervalInSeconds()))
                      .setIdleConnectionTimeout((int) TimeUnit.SECONDS.toMillis(redisson.getIdleConnectionTimeoutInSeconds()))
                      .setRetryAttempts(redisson.getRetryAttempts());
    //Give some secs to unlock if connection lost, or if too long to unlock
    config.setLockWatchdogTimeout(TimeUnit.SECONDS.toMillis(redisson.getLockWatchdogTimeoutInSeconds()));
    redissonClient = Redisson.create(config);
    return redissonClient;
  }

  /**
   * Close resources
   */
  @PreDestroy
  public void close() {
    if (redissonClient != null && !redissonClient.isShuttingDown()) {
      redissonClient.shutdown();
    }
  }
}
