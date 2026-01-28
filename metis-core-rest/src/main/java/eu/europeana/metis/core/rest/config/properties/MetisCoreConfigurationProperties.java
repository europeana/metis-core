package eu.europeana.metis.core.rest.config.properties;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Metis Core.
 * <p>
 * This record encapsulates application-level configuration settings for the Metis Core service, which can be configured using
 * properties prefixed with "metis-core".
 */
@ConfigurationProperties(prefix = "metis-core")
public record MetisCoreConfigurationProperties(
    EngineType engineType,
    int maxConcurrentThreads,
    int dpsMonitorCheckIntervalInSeconds,
    int dpsConnectTimeoutInMilliseconds,
    int dpsReadTimeoutInMilliseconds,
    int failsafeMarginOfInactivityInSeconds,
//    int periodicFailsafeCheckInMilliseconds,
    int periodicSchedulerCheckInMilliseconds,
    int pollingTimeoutForCleaningCompletionServiceInMilliseconds,
    int userCacheClearIntervalInMinutes,
    int periodOfNoProcessedRecordsChangeInMinutes,
    int threadLimitThrottlingLevelWeak,
    int threadLimitThrottlingLevelMedium,
    int threadLimitThrottlingLevelStrong,
    String baseUrl,
    int maxServedExecutionListLength,
    int maxDepublishRecordIdsPerDataset,
    int linkCheckingDefaultSamplingSize,
    int solrCommitPeriodInMinutes,
    List<String> allowedCorsHosts
) {

  /**
   * Enum representing types of engines available for configuration.
   */
  public enum EngineType {
    ECLOUD,
    MOCK;
  }
}
