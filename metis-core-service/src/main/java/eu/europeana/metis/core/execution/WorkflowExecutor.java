package eu.europeana.metis.core.execution;

import eu.europeana.cloud.service.dps.exception.DpsException;
import eu.europeana.metis.core.dao.WorkflowExecutionDao;
import eu.europeana.metis.core.engine.base.EngineTask;
import eu.europeana.metis.core.engine.base.EngineTaskClient;
import eu.europeana.metis.core.engine.base.EngineTaskSettings;
import eu.europeana.metis.core.engine.base.task.report.EngineTaskProgress;
import eu.europeana.metis.core.engine.base.task.report.EngineTaskState;
import eu.europeana.metis.core.exceptions.InvalidIndexPluginException;
import eu.europeana.metis.core.workflow.WorkflowExecution;
import eu.europeana.metis.core.workflow.WorkflowExecutionHelper;
import eu.europeana.metis.core.workflow.WorkflowStatus;
import eu.europeana.metis.core.workflow.plugins.AbstractExecutablePlugin;
import eu.europeana.metis.core.workflow.plugins.ExecutablePluginType;
import eu.europeana.metis.core.workflow.plugins.PluginStatus;
import eu.europeana.metis.core.workflow.plugins.PluginType;
import eu.europeana.metis.exception.BadContentException;
import eu.europeana.metis.exception.ExternalTaskException;
import eu.europeana.metis.exception.UnrecoverableExternalTaskException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * This class is a {@link Callable} class that accepts a {@link WorkflowExecution}. It starts that WorkflowExecution given to it
 * and will continue monitoring and updating its progress until it ends either by user interaction or by the end of the Workflow.
 * When the WorkflowExecution is received, there is a chance that the execution is already being handled from another
 * WorkflowExecutor in another instance, and if that is the case, the WorkflowExecution will be dropped.
 *
 * @param <S> The type representing the task settings required for the engine tasks.
 * @param <T> The type representing the tasks to be managed by the engine.
 */
@Slf4j
public class WorkflowExecutor<S extends EngineTaskSettings, T extends EngineTask> implements Callable<WorkflowExecution> {

  private static final String EXECUTION_ERROR_PREFIX = "Execution of external task presented with an error. ";
  private static final String MONITOR_ERROR_PREFIX = "An error occurred while monitoring the external task. ";
  private static final String POSTPROCESS_ERROR_PREFIX = "An error occurred while post-processing the external task. ";
  private static final String DETAILED_EXCEPTION_FORMAT = "%s%nDetailed exception:%s";

  protected static final int MAX_CANCEL_OR_MONITOR_FAILURES = 10;

  private final WorkflowExecutionHelper workflowExecutionHelper = new WorkflowExecutionHelper();
  private final WorkflowExecutionDao workflowExecutionDao;
  private final WorkflowPostProcessor workflowPostProcessor;
  private final Duration monitorCheckInterval;
  private final Duration periodOfNoProcessedRecordsChange;
  private final EngineTaskClient<S, T> engineTaskClient;
  private final PluginExecutor<S, T> pluginExecutor;
  private WorkflowExecution workflowExecution;

  /**
   * Constructor.
   *
   * @param workflowExecution The object representing the workflow to execute.
   * @param workflowExecutorSettings The settings instance containing configuration values and dependencies required for
   * execution, such as semaphores, DAOs, and post-processing utilities.
   */
  public WorkflowExecutor(WorkflowExecution workflowExecution, WorkflowExecutorSettings<S, T> workflowExecutorSettings) {
    this.workflowExecution = workflowExecution;
    this.workflowExecutionDao = workflowExecutorSettings.workflowExecutionDao();
    this.workflowPostProcessor = workflowExecutorSettings.workflowPostProcessor();
    this.engineTaskClient = workflowExecutorSettings.engineTaskClient();
    this.monitorCheckInterval = workflowExecutorSettings.monitorCheckInterval();
    this.periodOfNoProcessedRecordsChange = workflowExecutorSettings.noChangeInProcessedRecordsTimeout();
    this.pluginExecutor = new PluginExecutor<>(
        workflowExecutorSettings.engineTaskClient(),
        workflowExecutorSettings.workflowExecutionDao());
  }

  @Override
  public WorkflowExecution call() {
    if (workflowExecution.getStartedDate() == null) {
      workflowExecution.setStartedDate(new Date());
    }
    AbstractExecutablePlugin<?> plugin = findPluginToExecute();

    if (plugin == null) {
      log.warn("workflowExecutionId: {} - No runnable plugin found", workflowExecution.getId());
      return workflowExecution;
    }

    runPlugin(plugin);
    handlePluginCompletion(plugin);
    workflowExecutionDao.update(workflowExecution);
    return workflowExecution;
  }

  private AbstractExecutablePlugin<?> findPluginToExecute() {
    ExecutablePluginType expectedType = workflowExecution.getNextExecutablePluginType();
    if (expectedType == null) {
      return null;
    }

    return workflowExecutionHelper.getExecutablePlugins(workflowExecution).stream()
                                  .filter(p -> p.getPluginMetadata().getExecutablePluginType() == expectedType)
                                  .filter(p -> p.getPluginStatus().isRunnable())
                                  .findFirst()
                                  .orElse(null);
  }

  private void runPlugin(AbstractExecutablePlugin<?> plugin) {
    boolean startedSuccessfully = pluginExecutor.execute(plugin, workflowExecution);
    if (startedSuccessfully) {
      periodicCheckingLoop(plugin, workflowExecution.getDatasetId());
    }
  }

  private void handlePluginCompletion(AbstractExecutablePlugin<?> plugin) {
    boolean cancelling = workflowExecutionDao.isCancelling(workflowExecution.getId());

    if (cancelling && plugin.getPluginStatus().isRunnable()) {
      workflowExecutionHelper.setWorkflowAndAllQualifiedPluginsToCancelled(workflowExecution);
      String cancelledBy = workflowExecutionDao.getById(workflowExecution.getId().toString()).getCancelledBy();
      workflowExecution.setCancelledBy(cancelledBy);
      log.info("workflowExecutionId: {} - Cancelled workflow execution", workflowExecution.getId());
      return;
    }

    if (plugin.getPluginStatus() == PluginStatus.FAILED) {
      workflowExecutionHelper.checkAndSetAllRunningAndInqueuePluginsToCancelledIfOnePluginHasFailed(workflowExecution);
      log.info("workflowExecutionId: {} - Workflow execution failed", workflowExecution.getId());
      return;
    }

    if (plugin.getPluginStatus() == PluginStatus.FINISHED) {
      workflowExecutionHelper.moveToNextPlugin(workflowExecution);
      if (workflowExecution.getNextExecutablePluginType() == null) {
        workflowExecution.setWorkflowStatus(WorkflowStatus.FINISHED);
        workflowExecution.setFinishedDate(plugin.getFinishedDate());
        log.info("workflowExecutionId: {} - Finished workflow execution", workflowExecution.getId());
      } else {
        workflowExecution.setClaimedByInstance(null);
        log.info("workflowExecutionId: {} - Plugin finished, workflow returned to queue", workflowExecution.getId());
      }
    }
  }

  private static class ProgressState {

    @Getter
    private Instant lastProgressChange = Instant.now();
    private long expected;
    private long processed;
    private long deleted;
    private long ignored;
    private long errors;
    private long total;

    void updateFrom(AbstractExecutablePlugin<?> plugin) {
      lastProgressChange = Instant.now();
      this.processed = plugin.getExecutionProgress().getProcessedRecords();
      this.deleted = plugin.getExecutionProgress().getDeletedRecords();
      this.expected = plugin.getExecutionProgress().getExpectedRecords();
      this.ignored = plugin.getExecutionProgress().getIgnoredRecords();
      this.errors = plugin.getExecutionProgress().getErrors();
      this.total = plugin.getExecutionProgress().getTotalDatabaseRecords();
    }

    boolean hasChanged(AbstractExecutablePlugin<?> plugin) {
      var progress = plugin.getExecutionProgress();
      return processed != progress.getProcessedRecords()
          || deleted != progress.getDeletedRecords()
          || expected != progress.getExpectedRecords()
          || ignored != progress.getIgnoredRecords()
          || errors != progress.getErrors()
          || total != progress.getTotalDatabaseRecords();
    }

    Duration timeSinceLastChange() {
      return Duration.between(lastProgressChange, Instant.now());
    }
  }

  private void periodicCheckingLoop(AbstractExecutablePlugin<?> plugin, String datasetId) {
    final EngineTaskMonitor<S, T> engineTaskMonitor = new EngineTaskMonitor<>(plugin, engineTaskClient);
    EngineTaskProgress engineTaskProgress = null;
    int consecutiveCancelOrMonitorFailures = 0;
    AtomicBoolean externalCancelCallSent = new AtomicBoolean(false);
    ProgressState progressState = new ProgressState();
    progressState.updateFrom(plugin);
    boolean updateSuccess = true;
    while (updateSuccess && isContinueMonitor(engineTaskProgress)) {
      try {
        if (!sleepMonitorInterval()) {
          return;
        }

        sendExternalCancelCallIfNeeded(
            externalCancelCallSent,
            engineTaskMonitor,
            plugin,
            progressState
        );

        engineTaskProgress = engineTaskMonitor.monitor();
        consecutiveCancelOrMonitorFailures = 0;

        applyRuntimePluginState(plugin, engineTaskProgress);
      } catch (ExternalTaskException | RuntimeException e) {
        if (e.getCause() instanceof UnrecoverableExternalTaskException) {
          log.warn(String
              .format("workflowExecutionId: %s, pluginType: %s - UnrecoverableExternalTaskException"
                  + " occurred. Setting task state failed ", workflowExecution.getId(), plugin.getPluginType()), e);
          // Set the plugin to FAILED and return immediately
          plugin.setFinishedDate(null);
          plugin.setPluginStatusAndResetFailMessage(PluginStatus.FAILED);
          plugin.setFailMessage(String.format(DETAILED_EXCEPTION_FORMAT, MONITOR_ERROR_PREFIX, ExceptionUtils.getStackTrace(e)));
          return;
        }

        consecutiveCancelOrMonitorFailures++;
        handleRecoverableMonitorFailure(plugin, e, consecutiveCancelOrMonitorFailures);

      } finally {
        Date now = new Date();
        plugin.setUpdatedDate(now);
        workflowExecution.setUpdatedDate(now);
        updateSuccess = workflowExecutionDao.updateMonitorInformation(workflowExecution);
      }
    }

    // Perform post-processing if needed.
    if (engineTaskProgress == null || !applyPostProcessing(engineTaskProgress, plugin, datasetId)) {
      return;
    }

    // Set the status of the task.
    preparePluginStateAndFinishedDate(plugin, engineTaskProgress);
  }

  private boolean sleepMonitorInterval() {
    try {
      Thread.sleep(monitorCheckInterval);
      return true;
    } catch (InterruptedException e) {
      log.warn("Thread interrupted during monitoring sleep for workflowExecutionId {}", workflowExecution.getId(), e);
      Thread.currentThread().interrupt();
      return false;
    }
  }

  private void handleRecoverableMonitorFailure(AbstractExecutablePlugin<?> plugin, Exception e,
      int consecutiveCancelOrMonitorFailures) {
    log.warn(String.format(
        "workflowExecutionId: %s, pluginType: %s - Monitoring of external task failed %s "
            + "consecutive times. After exceeding %s retries, pending status will be set",
        workflowExecution.getId(), plugin.getPluginType(), consecutiveCancelOrMonitorFailures,
        MAX_CANCEL_OR_MONITOR_FAILURES), e);
    if (consecutiveCancelOrMonitorFailures >= MAX_CANCEL_OR_MONITOR_FAILURES) {
      plugin.setPluginStatusAndResetFailMessage(PluginStatus.PENDING);
    }
  }

  private void applyRuntimePluginState(AbstractExecutablePlugin<?> plugin, EngineTaskProgress engineTaskProgress) {
    EngineTaskState engineTaskState = engineTaskProgress.getEngineTaskState();
    if (engineTaskState == EngineTaskState.REMOVING_FROM_SOLR_AND_MONGO ||
        isIndexingInPostProcessing(engineTaskState, plugin)) {
      plugin.setPluginStatusAndResetFailMessage(PluginStatus.CLEANING);

    } else if (isHarvestingInPostProcessing(engineTaskState, plugin)) {
      plugin.setPluginStatusAndResetFailMessage(PluginStatus.IDENTIFYING_DELETED_RECORDS);

    } else {
      plugin.setPluginStatusAndResetFailMessage(PluginStatus.RUNNING);
    }
  }

  private boolean isIndexingInPostProcessing(EngineTaskState engineTaskState,
      AbstractExecutablePlugin<?> plugin) {
    return engineTaskState == EngineTaskState.IN_POST_PROCESSING &&
        (plugin.getPluginType() == PluginType.REINDEX_TO_PREVIEW ||
            plugin.getPluginType() == PluginType.REINDEX_TO_PUBLISH);
  }

  private boolean isHarvestingInPostProcessing(EngineTaskState engineTaskState,
      AbstractExecutablePlugin<?> plugin) {
    return engineTaskState == EngineTaskState.IN_POST_PROCESSING &&
        (plugin.getPluginType() == PluginType.HTTP_HARVEST ||
            plugin.getPluginType() == PluginType.OAIPMH_HARVEST);
  }

  private void sendExternalCancelCallIfNeeded(AtomicBoolean externalCancelCallSent,
      EngineTaskMonitor<S, T> engineTaskMonitor, AbstractExecutablePlugin<?> plugin,
      ProgressState progressState) throws ExternalTaskException {
    if (!externalCancelCallSent.get() && shouldPluginBeCancelled(plugin, progressState)) {
      // Update workflowExecution first, to retrieve cancelling information from db
      workflowExecution = workflowExecutionDao.getById(workflowExecution.getId().toString());
      engineTaskMonitor.cancel(workflowExecution.getCancelledBy());
      externalCancelCallSent.set(true);
    }
  }

  private boolean applyPostProcessing(
      EngineTaskProgress engineTaskProgress, AbstractExecutablePlugin<?> plugin, String datasetId) {
    boolean processingAppliedOrNotRequired = true;
    if (engineTaskProgress.getEngineTaskState() == EngineTaskState.PROCESSED) {
      try {
        this.workflowPostProcessor.performPluginPostProcessing(plugin, datasetId);
      } catch (DpsException | InvalidIndexPluginException | BadContentException | RuntimeException | ExternalTaskException e) {
        processingAppliedOrNotRequired = false;
        log.warn("Problem occurred during Metis post-processing.", e);
        plugin.setFinishedDate(null);
        plugin.setPluginStatusAndResetFailMessage(PluginStatus.FAILED);
        plugin.setFailMessage(String.format(DETAILED_EXCEPTION_FORMAT, POSTPROCESS_ERROR_PREFIX,
            ExceptionUtils.getStackTrace(e)));
      }
    }
    return processingAppliedOrNotRequired;
  }

  private boolean isContinueMonitor(EngineTaskProgress engineTaskProgress) {
    return engineTaskProgress == null ||
        (engineTaskProgress.getEngineTaskState() != EngineTaskState.DROPPED
            && engineTaskProgress.getEngineTaskState() != EngineTaskState.PROCESSED);
  }

  private boolean shouldPluginBeCancelled(AbstractExecutablePlugin<?> plugin, ProgressState progressState) {
    if (workflowExecutionDao.isCancelling(workflowExecution.getId()) && plugin.getPluginStatus().isCancellable()) {
      return true;
    }
    return plugin.getPluginStatus().isCancellable() && hasExceededNoProgressTimeout(plugin, progressState);
  }

  private boolean hasExceededNoProgressTimeout(
      AbstractExecutablePlugin<?> plugin, ProgressState progressState) {
    if (progressState.hasChanged(plugin)) {
      progressState.updateFrom(plugin);
      return false;
    }

    final boolean hasExceededTimeout =
        progressState.timeSinceLastChange().compareTo(periodOfNoProcessedRecordsChange) >= 0;
    if (hasExceededTimeout) {
      workflowExecutionDao.setCancellingStateSystem(workflowExecution);
    }
    return hasExceededTimeout;
  }

  private void preparePluginStateAndFinishedDate(AbstractExecutablePlugin<?> plugin,
      EngineTaskProgress engineTaskProgress) {
    EngineTaskState engineTaskState = engineTaskProgress.getEngineTaskState();
    switch (engineTaskState) {
      case PROCESSED -> {
        plugin.setFinishedDate(new Date());
        plugin.setPluginStatusAndResetFailMessage(PluginStatus.FINISHED);
      }
      case DROPPED -> {
        boolean isNotCancelling = !workflowExecutionDao.isCancelling(workflowExecution.getId());
        if (isNotCancelling) {
          plugin.setPluginStatusAndResetFailMessage(PluginStatus.FAILED);
          String engineTaskStateInfo = engineTaskProgress.getEngineTaskStateInfo();
          String message = StringUtils.isBlank(engineTaskStateInfo)
              ? "No further information received."
              : engineTaskStateInfo;
          plugin.setFailMessage(EXECUTION_ERROR_PREFIX + message);
        }
      }
      default -> log.debug("No action for other states");
    }
    workflowExecutionDao.updateWorkflowPlugins(workflowExecution);
  }
}
