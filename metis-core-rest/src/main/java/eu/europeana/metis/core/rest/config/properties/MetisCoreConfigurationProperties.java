package eu.europeana.metis.core.rest.config.properties;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Class using {@link ConfigurationProperties} loading.
 */
@ConfigurationProperties(prefix = "metis-core")
public record MetisCoreConfigurationProperties(
    int maxConcurrentThreads,
    int dpsMonitorCheckIntervalInSeconds,
    int dpsConnectTimeoutInMilliseconds,
    int dpsReadTimeoutInMilliseconds,
    int failsafeMarginOfInactivityInSeconds,
    int periodicFailsafeCheckInMilliseconds,
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
) {}
