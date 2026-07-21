package eu.europeana.metis.core.engine.ecloud;

import eu.europeana.cloud.service.dps.DpsTask;
import eu.europeana.metis.core.engine.base.EngineTask;

public record EcloudEngineTask(DpsTask dpsTask) implements EngineTask {

  @Override
  public String getExternalTaskId() {
    return String.valueOf(dpsTask.getTaskId());
  }

  @Override
  public String getBatchId() {
    return String.valueOf(dpsTask.getTaskId());
  }
}
