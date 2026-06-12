package eu.europeana.metis.core.execution;

import static java.util.Objects.requireNonNull;

import eu.europeana.metis.core.dao.DatasetXsltDao;
import eu.europeana.metis.core.dao.WorkflowExecutionDao;
import eu.europeana.metis.core.engine.base.EngineTask;
import eu.europeana.metis.core.engine.base.EngineTaskClient;
import eu.europeana.metis.core.engine.base.EngineTaskSettings;
import java.time.Duration;

/**
 * These are settings that are all related to the actual execution of workflows, and used mostly by the classes
 * {@link WorkflowExecutor}.
 *
 * @param <S> The type representing the task settings required for the engine tasks.
 * @param <T> The type representing the tasks to be managed by the engine.
 */
public record WorkflowExecutorSettings<S extends EngineTaskSettings, T extends EngineTask>(
    Duration monitorCheckInterval,
    Duration noChangeInProcessedRecordsTimeout,

    SemaphoresPerPluginManager semaphoresPerPluginManager,
    WorkflowExecutionDao workflowExecutionDao,
    WorkflowPostProcessor workflowPostProcessor,
    DatasetXsltDao datasetXsltDao,
    EngineTaskClient<S, T> engineTaskClient
) {

  private static final Duration DEFAULT_MONITOR_CHECK_INTERVAL = Duration.ofSeconds(5);
  private static final Duration DEFAULT_NO_CHANGE_IN_PROCESSED_RECORDS_TIMEOUT = Duration.ofMinutes(30);

  /**
   * Compact constructor for validation and defaults
   */
  public WorkflowExecutorSettings {
    if (monitorCheckInterval == null) {
      monitorCheckInterval = DEFAULT_MONITOR_CHECK_INTERVAL;
    }

    if (noChangeInProcessedRecordsTimeout == null) {
      noChangeInProcessedRecordsTimeout = DEFAULT_NO_CHANGE_IN_PROCESSED_RECORDS_TIMEOUT;
    }

    if (monitorCheckInterval.isZero() || monitorCheckInterval.isNegative()) {
      throw new IllegalArgumentException("dpsMonitorCheckInterval must be positive");
    }

    if (noChangeInProcessedRecordsTimeout.isZero() || noChangeInProcessedRecordsTimeout.isNegative()) {
      throw new IllegalArgumentException("periodOfNoProcessedRecordsChange must be positive");
    }

    requireNonNull(semaphoresPerPluginManager);
    requireNonNull(workflowExecutionDao);
    requireNonNull(workflowPostProcessor);
    requireNonNull(engineTaskClient);
  }
}

