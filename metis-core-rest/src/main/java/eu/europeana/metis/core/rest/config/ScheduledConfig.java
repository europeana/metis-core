package eu.europeana.metis.core.rest.config;

import eu.europeana.metis.core.execution.QueueConsumer;
import eu.europeana.metis.core.execution.SchedulerExecutor;
import eu.europeana.metis.core.execution.WorkflowExecutionMonitor;
import eu.europeana.metis.core.rest.config.properties.MetisCoreConfigurationProperties;
import eu.europeana.metis.core.service.UserService;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Configuration class for scheduling tasks and defining beans related to scheduling intervals.
 */
@Configuration
@EnableConfigurationProperties({MetisCoreConfigurationProperties.class})
public class ScheduledConfig {

  /**
   * Retrieves the periodic failsafe check interval in milliseconds from the provided MetisCoreConfigurationProperties.
   *
   * @param metisCoreConfigurationProperties Configuration properties for the Metis Core.
   * @return The periodic failsafe check interval in milliseconds.
   */
  @Bean
  public long getPeriodicFailsafeCheckInMilliseconds(MetisCoreConfigurationProperties metisCoreConfigurationProperties) {
    return metisCoreConfigurationProperties.periodicFailsafeCheckInMilliseconds();
  }

  /**
   * Retrieves the periodic scheduler check-in interval in milliseconds from the provided MetisCoreConfigurationProperties.
   *
   * @param metisCoreConfigurationProperties Configuration properties for the Metis Core.
   * @return The periodic scheduler check-in interval in milliseconds.
   */
  @Bean
  public long getPeriodicSchedulerCheckInMilliseconds(MetisCoreConfigurationProperties metisCoreConfigurationProperties) {
    return metisCoreConfigurationProperties.periodicSchedulerCheckInMilliseconds();
  }

  /**
   * Retrieves the polling timeout for the cleaning completion service in milliseconds from the provided
   * MetisCoreConfigurationProperties.
   *
   * @param metisCoreConfigurationProperties Configuration properties for the Metis Core.
   * @return The polling timeout for the cleaning completion service in milliseconds.
   */
  @Bean
  public long getPollingTimeoutForCleaningCompletionServiceInMilliseconds(
      MetisCoreConfigurationProperties metisCoreConfigurationProperties) {
    return metisCoreConfigurationProperties.pollingTimeoutForCleaningCompletionServiceInMilliseconds();
  }

  /**
   * Bean to fetch the user cache clear interval in minutes from MetisCoreConfigurationProperties.
   *
   * @param metisCoreConfigurationProperties Configuration properties for the Metis Core.
   * @return The user cache clear interval in minutes.
   */
  @Bean
  public long getUserCacheClearIntervalInMinutes(MetisCoreConfigurationProperties metisCoreConfigurationProperties) {
    return metisCoreConfigurationProperties.userCacheClearIntervalInMinutes();
  }

  /**
   * Inner class containing methods with @Scheduled annotation.
   * <p>
   * This is a helper class so that we can use shorter references on the SPEL on the @Scheduled annotation with values from
   * {@link ScheduledConfig}.
   */
  @Configuration
  @EnableScheduling
  static class ScheduledTasks {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final WorkflowExecutionMonitor workflowExecutionMonitor;
    private final SchedulerExecutor schedulerExecutor;
    private final QueueConsumer queueConsumer;
    private final UserService userService;

    @Autowired
    public ScheduledTasks(WorkflowExecutionMonitor workflowExecutionMonitor, SchedulerExecutor schedulerExecutor,
        QueueConsumer queueConsumer, UserService userService) {
      this.userService = userService;
      this.workflowExecutionMonitor = workflowExecutionMonitor;
      this.queueConsumer = queueConsumer;
      this.schedulerExecutor = schedulerExecutor;
    }

    /**
     * Failsafe periodic thread.
     * <p>It will find stale executions and will re-submit them in the distributed queue.</p>
     */
    @Scheduled(fixedDelayString = "#{@getPeriodicFailsafeCheckInMilliseconds}")
    public void runFailsafeExecutor() {
      this.workflowExecutionMonitor.performFailsafe();
      LOGGER.info("Failsafe task finished.");
    }

    /**
     * Scheduling periodic thread.
     * <p>Checks if scheduled workflows are valid for starting and sends them to the distributed
     * queue.</p>
     */
    @Scheduled(
        initialDelayString = "#{@getPeriodicSchedulerCheckInMilliseconds}",
        fixedDelayString = "#{@getPeriodicSchedulerCheckInMilliseconds}"
    )
    public void runSchedulingExecutor() {
      this.schedulerExecutor.performScheduling();
      LOGGER.info("Scheduler task finished.");
    }

    /**
     * Queue consumer cleanup periodic thread.
     * <p>
     * Periodically runs a cleanup operation on the queue consumer's completion service. This method logs the completion of the
     * cleanup operation and may throw an {@code InterruptedException} if interrupted during execution.
     *
     * @throws InterruptedException if the execution of this method is interrupted
     */
    @Scheduled(
        initialDelayString = "#{@getPollingTimeoutForCleaningCompletionServiceInMilliseconds}",
        fixedDelayString = "#{@getPollingTimeoutForCleaningCompletionServiceInMilliseconds}"
    )
    public void runQueueConsumerCleanup() throws InterruptedException {
      this.queueConsumer.checkAndCleanCompletionService();
      LOGGER.debug("Queue consumer cleanup finished.");
    }

    /**
     * User cache cleanup periodic thread.
     * <p>
     * This method is called once a day and clears the cache for the user service. This is necessary to refresh the cache when the
     * user information in Keycloak changes.
     */
    @Scheduled(timeUnit = TimeUnit.MINUTES,
        initialDelayString = "#{@getUserCacheClearIntervalInMinutes}",
        fixedDelayString = "#{@getUserCacheClearIntervalInMinutes}"
    )
    public void clearCache() {
      userService.clearCache();
    }
  }
}
