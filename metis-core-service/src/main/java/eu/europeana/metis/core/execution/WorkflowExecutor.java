package eu.europeana.metis.core.execution;

import static java.lang.Thread.currentThread;

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
import eu.europeana.metis.core.workflow.plugins.AbstractMetisPlugin;
import eu.europeana.metis.core.workflow.plugins.ExecutablePluginType;
import eu.europeana.metis.core.workflow.plugins.PluginStatus;
import eu.europeana.metis.core.workflow.plugins.PluginType;
import eu.europeana.metis.exception.BadContentException;
import eu.europeana.metis.exception.ExternalTaskException;
import eu.europeana.metis.exception.UnrecoverableExternalTaskException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

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
public class WorkflowExecutor<S extends EngineTaskSettings, T extends EngineTask>
    implements Callable<Pair<WorkflowExecution, Boolean>> {

  private static final String EXECUTION_ERROR_PREFIX = "Execution of external task presented with an error. ";
  private static final String MONITOR_ERROR_PREFIX = "An error occurred while monitoring the external task. ";
  private static final String POSTPROCESS_ERROR_PREFIX = "An error occurred while post-processing the external task. ";
  private static final String DETAILED_EXCEPTION_FORMAT = "%s%nDetailed exception:%s";

  protected static final int MAX_CANCEL_OR_MONITOR_FAILURES = 10;

  private final SemaphoresPerPluginManager semaphoresPerPluginManager;
  private final WorkflowExecutionDao workflowExecutionDao;
  private final WorkflowPostProcessor workflowPostProcessor;
  private final Duration monitorCheckInterval;
  private final Duration periodOfNoProcessedRecordsChange;
  private final EngineTaskClient<S, T> engineTaskClient;
  private final WorkflowExecutionHelper workflowExecutionHelper = new WorkflowExecutionHelper();
  private final PluginExecutor<S, T> pluginExecutor;
  private WorkflowExecution workflowExecution;

  /**
   * Constructor.
   *
   * @param workflowExecution The object representing the workflow to execute.
   * @param workflowExecutorSettings The settings instance containing configuration values and dependencies required for
   * execution, such as semaphores, DAOs, and post-processing utilities.
   */
  public WorkflowExecutor(WorkflowExecution workflowExecution,
      WorkflowExecutorSettings<S, T> workflowExecutorSettings) {
    this.workflowExecution = workflowExecution;
    this.semaphoresPerPluginManager = workflowExecutorSettings.semaphoresPerPluginManager();
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
  public Pair<WorkflowExecution, Boolean> call() {
    // Perform the work - run the workflow.
    log.info("workflowExecutionId: {}- Starting workflow execution", workflowExecution.getId());
    final Pair<Date, Boolean> didPluginRunDatePair = runWorkflowExecution();
    final Boolean anyPluginRan = handleWorkflowCompletion(didPluginRunDatePair);

    // The only full update is used here. The rest of the execution uses partial updates to avoid
    // losing the cancelling state field
    workflowExecutionDao.update(workflowExecution);
    return new ImmutablePair<>(workflowExecution, anyPluginRan);
  }

  /**
   * Will determine from which plugin of the workflow to start execution from and will iterate through the plugins of the workflow
   * and run them one by one.
   * <p>It returns a {@link Pair} of a finished {@link Date} and a {@link Boolean} flag.
   * <ul>
   *   <li>
   *     The Date represents the finished date of the workflow or null if it did not finish
   *     as expected. That can happen if an error occurred or some plugin was not permitted to
   *     run.
   *   </li>
   *   <li>
   *     The Boolean flag represents true if all plugins were allowed to run or false if one of
   *     the plugins was not allowed to run.
   *   </li>
   * </ul>
   * </p>
   *
   * @return The pair of date and boolean flag
   */
  private Pair<Date, Boolean> runWorkflowExecution() {

    // Find the first plugin to continue execution from
    int firstPluginPositionToStart = getFirstPluginPositionToStart();

    boolean anyPluginRan = false;
    boolean continueNextPlugin = true;
    List<AbstractMetisPlugin> metisPlugins = workflowExecution.getMetisPlugins();
    // One by one start the plugins of the workflow
    for (int i = firstPluginPositionToStart; i < metisPlugins.size() && continueNextPlugin; i++) {
      final AbstractMetisPlugin<?> plugin = metisPlugins.get(i);

      //Run plugin if available space
      boolean didPluginRun = runMetisPluginWithSemaphoreAllocation(i, plugin);
      anyPluginRan |= didPluginRun;
      continueNextPlugin = shouldContinueNextPlugin(plugin, didPluginRun);
    }

    // Compute the finished date
    final AbstractMetisPlugin<?> lastPlugin = metisPlugins.getLast();
    final Date finishDate;
    if (lastPlugin.getPluginStatus() == PluginStatus.FINISHED) {
      finishDate = lastPlugin.getFinishedDate();
    } else {
      finishDate = null;
    }
    return new ImmutablePair<>(finishDate, anyPluginRan);
  }

  private boolean shouldContinueNextPlugin(AbstractMetisPlugin<?> plugin, boolean didPluginRun) {
    boolean notInterrupted = !currentThread().isInterrupted();
    boolean notFailed = plugin.getPluginStatus() != PluginStatus.FAILED;
    boolean cancelling = workflowExecutionDao.isCancelling(workflowExecution.getId());
    boolean pluginFinished = plugin.getFinishedDate() != null;
    boolean notCancellingOrPluginFinished = !cancelling || pluginFinished;

    return notInterrupted && didPluginRun && notFailed && notCancellingOrPluginFinished;
  }

  private Boolean handleWorkflowCompletion(Pair<Date, Boolean> anyPluginRanDatePair) {
    final Date finishDate = anyPluginRanDatePair.getLeft();
    final Boolean anyPluginRan = anyPluginRanDatePair.getRight();

    // Process the results if we were not interrupted
    if (!currentThread().isInterrupted()) {
      if (finishDate == null && workflowExecutionDao.isCancelling(workflowExecution.getId())) {
        // If the workflow was canceled before it had the chance to finish, we cancel all remaining plugins.
        workflowExecutionHelper.setWorkflowAndAllQualifiedPluginsToCancelled(workflowExecution);
        // Make sure the cancelledBy information is not lost
        String cancelledBy = workflowExecutionDao.getById(workflowExecution.getId().toString()).getCancelledBy();
        workflowExecution.setCancelledBy(cancelledBy);
        log.info("workflowExecutionId: {} - Cancelled running workflow execution", workflowExecution.getId());
      } else if (finishDate == null && anyPluginRan) {
        // Workflow stopped midway (a plugin failed, was cancelled, or monitoring stopped)
        workflowExecutionHelper.checkAndSetAllRunningAndInqueuePluginsToCancelledIfOnePluginHasFailed(workflowExecution);
      } else if (finishDate == null) {
        log.info("workflowExecution: {} - Stop workflow execution a plugin was not allowed to run", workflowExecution.getId());
      } else {
        // If the workflow finished successfully, we record this.
        workflowExecution.setFinishedDate(finishDate);
        workflowExecution.setWorkflowStatus(WorkflowStatus.FINISHED);
        workflowExecution.setCancelling(false);
        log.info("workflowExecutionId: {} - Finished workflow execution", workflowExecution.getId());
      }
    }
    return anyPluginRan;
  }

  private int getFirstPluginPositionToStart() {
    int firstPluginPositionToStart = 0;
    List<AbstractMetisPlugin> metisPlugins = workflowExecution.getMetisPlugins();
    for (int i = 0; i < metisPlugins.size(); i++) {
      AbstractMetisPlugin<?> metisPlugin = metisPlugins.get(i);
      if (metisPlugin.getPluginStatus() == PluginStatus.INQUEUE
          || metisPlugin.getPluginStatus() == PluginStatus.RUNNING
          || metisPlugin.getPluginStatus() == PluginStatus.CLEANING
          || metisPlugin.getPluginStatus() == PluginStatus.PENDING
          || metisPlugin.getPluginStatus() == PluginStatus.IDENTIFYING_DELETED_RECORDS) {
        firstPluginPositionToStart = i;
        break;
      }
    }
    return firstPluginPositionToStart;
  }

  /**
   * Tries to acquire a semaphore permission corresponding to the provided plugin's type.
   * <ol>
   *   <li>If semaphore permission granted then there is space for that plugin and the plugin
   *   starts</li>
   *   <li>If semaphore permission NOT granted then the plugin din not run and a false flag is
   *   send back as a return result</li>
   * </ol>
   *
   * @param i the index of the plugin in the list of plugins inside the workflow execution
   * @param plugin the provided plugin to run
   * @return true if plugin ran, false if plugin did not run
   */
  private boolean runMetisPluginWithSemaphoreAllocation(int i, AbstractMetisPlugin<?> plugin) {
    // Sanity check
    if (plugin == null) {
      throw new IllegalStateException("Plugin cannot be null.");
    }
    // Check the plugin: it has to be executable
    AbstractExecutablePlugin<?> executablePlugin = expectExecutablePlugin(plugin);

    final ExecutablePluginType executablePluginType = ExecutablePluginType
        .getExecutablePluginFromPluginType(executablePlugin.getPluginType());
    if (executablePluginType == null) {
      throw new IllegalStateException("Plugin type cannot be null.");
    }

    //Try to acquire semaphore and run the plugin. Remember to release.
    boolean acquired = semaphoresPerPluginManager.tryAcquireForExecutablePluginType(executablePluginType);
    if (acquired) {
      try {
        log.debug("workflowExecutionId: {}, executablePluginType: {} - Acquired semaphore",
            workflowExecution.getId(), executablePluginType);
        final Date startDateToUse = i == 0 ? workflowExecution.getStartedDate() : new Date();
        pluginExecutor.execute(executablePlugin, startDateToUse, workflowExecution);
        periodicCheckingLoop(executablePlugin, workflowExecution.getDatasetId());
      } finally {
        semaphoresPerPluginManager.releaseForPluginType(executablePluginType);
        log.debug("workflowExecutionId: {}, executablePluginType: {} - Released semaphore",
            workflowExecution.getId(), executablePluginType);
      }
    }
    return acquired;
  }

  private AbstractExecutablePlugin<?> expectExecutablePlugin(AbstractMetisPlugin<?> plugin) {
    if (plugin == null) {
      return null;
    }
    if (plugin instanceof AbstractExecutablePlugin<?> abstractExecutablePlugin) {
      return abstractExecutablePlugin;
    }
    throw new IllegalStateException(String.format(
        "workflowExecutionId: %s, pluginId: %s - Found plugin that is not an executable plugin.",
        workflowExecution.getId(), plugin.getId()));
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

  private boolean applyPostProcessing(EngineTaskProgress engineTaskProgress,
      AbstractExecutablePlugin<?> plugin,
      String datasetId) {
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
    // A plugin with a CLEANING state is NOT cancellable, it will be when the state is updated
    final boolean notCleaningAndCancelling =
        plugin.getPluginStatus() != PluginStatus.CLEANING
            && workflowExecutionDao.isCancelling(workflowExecution.getId());
    // A cleaning or a pending task should not be canceled by exceeding the minute cap
    final boolean notCleaningOrPending = plugin.getPluginStatus() != PluginStatus.CLEANING
        && plugin.getPluginStatus() != PluginStatus.PENDING;
    final boolean isMinuteCapExceeded = isMinuteCapOverWithoutChangeInProcessedRecords(plugin, progressState);
    return (notCleaningAndCancelling || (notCleaningOrPending && isMinuteCapExceeded));
  }

  private boolean isMinuteCapOverWithoutChangeInProcessedRecords(AbstractExecutablePlugin<?> plugin,
      ProgressState progressState) {
    //If CLEANING is in progress, then just reset the values to be sure and return false. Or if we have progress
    if (plugin.getPluginStatus() == PluginStatus.CLEANING
        || plugin.getPluginStatus() == PluginStatus.PENDING
        || progressState.hasChanged(plugin)) {
      progressState.updateFrom(plugin);
      return false;
    }

    final boolean isMinuteCapOverWithoutChangeInProcessedRecords =
        progressState.timeSinceLastChange().compareTo(periodOfNoProcessedRecordsChange) >= 0;
    if (isMinuteCapOverWithoutChangeInProcessedRecords) {
      workflowExecutionDao.setCancellingStateSystem(workflowExecution);
    }
    return isMinuteCapOverWithoutChangeInProcessedRecords;
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
