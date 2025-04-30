package eu.europeana.metis.core.engine.ecloud;

import eu.europeana.metis.core.engine.base.ProcessingEngineTaskSettings;
import eu.europeana.metis.core.workflow.plugins.DpsTaskSettings;
import eu.europeana.metis.core.workflow.plugins.ThrottlingValues;
import java.util.function.Supplier;

/**
 * Adapts DpsTaskSettings to the ExternalTaskSettings interface.
 */
public class DpsProcessingEngineTaskSettings implements ProcessingEngineTaskSettings<DpsProcessingEngineTask> {

  private final DpsTaskSettings dpsTaskSettings;

  public DpsProcessingEngineTaskSettings(DpsTaskSettings dpsTaskSettings) {
    this.dpsTaskSettings = dpsTaskSettings;
  }

  @Override
  public String getBaseUrl() {
    return dpsTaskSettings.ecloudBaseUrl();
  }

  @Override
  public String getProvider() {
    return dpsTaskSettings.ecloudProvider();
  }

  @Override
  public String getDatasetId() {
    return dpsTaskSettings.ecloudDatasetId();
  }

  @Override
  public String getPreviousTaskId() {
    return dpsTaskSettings.previousExternalTaskId();
  }

  @Override
  public String getMetisCoreBaseUrl() {
    return dpsTaskSettings.metisCoreBaseUrl();
  }

  @Override
  public ThrottlingValues getThrottlingValues() {
    return dpsTaskSettings.throttlingValues();
  }

  @Override
  public Supplier<DpsProcessingEngineTask> getTaskCreator() {
    return DpsProcessingEngineTask::new;
  }
}
