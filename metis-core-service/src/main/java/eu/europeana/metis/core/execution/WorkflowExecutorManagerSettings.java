package eu.europeana.metis.core.execution;

/**
 * These are settings that are all related to the actual execution of workflows, and used mostly by
 * the classes {@link WorkflowExecutor}.
 */
public class WorkflowExecutorManagerSettings {

  private static final int DEFAULT_MONITOR_CHECK_INTERVAL_IN_SECS = 5;
  private static final int DEFAULT_PERIOD_OF_NO_PROCESSED_RECORDS_CHANGE_IN_MINUTES = 30;
  private int dpsMonitorCheckIntervalInSecs = DEFAULT_MONITOR_CHECK_INTERVAL_IN_SECS; //Use setter otherwise default
  private int periodOfNoProcessedRecordsChangeInMinutes = DEFAULT_PERIOD_OF_NO_PROCESSED_RECORDS_CHANGE_IN_MINUTES; //Use setter otherwise default

  public int getDpsMonitorCheckIntervalInSecs() {
    return dpsMonitorCheckIntervalInSecs;
  }

  public void setDpsMonitorCheckIntervalInSecs(int interval) {
    this.dpsMonitorCheckIntervalInSecs = interval;
  }

  public int getPeriodOfNoProcessedRecordsChangeInMinutes() {
    return periodOfNoProcessedRecordsChangeInMinutes;
  }

  public void setPeriodOfNoProcessedRecordsChangeInMinutes(int period) {
    this.periodOfNoProcessedRecordsChangeInMinutes = period;
  }
}
