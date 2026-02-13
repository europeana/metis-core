package eu.europeana.metis.core.rest.config;

import eu.europeana.metis.core.engine.base.EngineTask;
import eu.europeana.metis.core.engine.base.EngineTaskSettings;
import eu.europeana.metis.core.execution.WorkflowExecutionDispatcher;
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
   * Retrieves the workflow dispatch check-in interval in milliseconds from the provided MetisCoreConfigurationProperties.
   *
   * @param metisCoreConfigurationProperties Configuration properties for the Metis Core.
   * @return The queue polling check-in interval in milliseconds.
   */
  @Bean
  public long workflowDispatchPeriodCheckInMilliseconds(MetisCoreConfigurationProperties metisCoreConfigurationProperties) {
    return metisCoreConfigurationProperties.workflowDispatchPeriodCheckInMilliseconds();
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
  static class ScheduledTasks<S extends EngineTaskSettings, T extends EngineTask> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final WorkflowExecutionDispatcher<S, T> workflowExecutionDispatcher;
    private final UserService userService;

    @Autowired
    public ScheduledTasks(WorkflowExecutionDispatcher<S, T> workflowExecutionDispatcher, UserService userService) {
      this.workflowExecutionDispatcher = workflowExecutionDispatcher;
      this.userService = userService;
    }

    /**
     * Periodically polls for workflow executions and dispatches them for processing.
     *
     * This method is scheduled with a fixed delay to continuously check for available
     * workflow executions and submit them for processing. The polling interval is
     * configured dynamically using Spring Expression Language (SpEL) and is retrieved
     * from the method {@code getQueuePollingCheckInMilliseconds} in the configuration.
     *
     * The actual polling and dispatching logic is delegated to the
     * {@code WorkflowExecutionDispatcher#pollAndDispatch()} method, which handles the
     * claiming and submission of workflow executions.
     *
     * Log messages are generated to track the execution of this method, providing
     * insight into operational behavior and successful runs.
     */
    @Scheduled(fixedDelayString = "#{@workflowDispatchPeriodCheckInMilliseconds}")
    public void pollAndDispatchWorkflowExecutions() {
      this.workflowExecutionDispatcher.pollAndSubmit();
      LOGGER.info("Run executions.");
    }

    /**
     * Mongo poller cleanup periodic thread.
     * <p>
     * Periodically runs a cleanup operation on the mongo poller's completion service. This method logs the completion of the
     * cleanup operation and may throw an {@code InterruptedException} if interrupted during execution.
     *
     * @throws InterruptedException if the execution of this method is interrupted
     */
    @Scheduled(
        initialDelayString = "#{@getPollingTimeoutForCleaningCompletionServiceInMilliseconds}",
        fixedDelayString = "#{@getPollingTimeoutForCleaningCompletionServiceInMilliseconds}"
    )
    public void runExecutorCompletionServiceCleanup() throws InterruptedException {
      this.workflowExecutionDispatcher.cleanup();
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
