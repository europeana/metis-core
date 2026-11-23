package eu.europeana.metis.core.engine.ecloud;

import eu.europeana.cloud.service.dps.BatchInfo;
import eu.europeana.cloud.service.dps.DpsTask;
import eu.europeana.metis.core.engine.base.EngineTask;

/**
 * Wraps an eCloud DPS task and exposes its task and output batch identifiers.
 *
 * @param dpsTask the eCloud task
 */
public record EcloudEngineTask(DpsTask dpsTask) implements EngineTask {

  @Override
  public String getEngineTaskId() {
    return String.valueOf(dpsTask.getTaskId());
  }

  @Override
  public String getEngineBatchId() {
    BatchInfo resultsBatch = dpsTask.getResultsBatch();
    return resultsBatch == null ? null : resultsBatch.getBatchId();
  }
}
