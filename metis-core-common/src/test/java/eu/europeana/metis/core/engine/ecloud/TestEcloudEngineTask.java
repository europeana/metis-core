package eu.europeana.metis.core.engine.ecloud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import eu.europeana.cloud.service.dps.BatchInfo;
import eu.europeana.cloud.service.dps.DpsTask;
import org.junit.jupiter.api.Test;

class TestEcloudEngineTask {

  @Test
  void usesReturnedBatchRatherThanTaskId() {
    DpsTask dpsTask = new DpsTask();
    dpsTask.setTaskId(123);
    dpsTask.setResultsBatch(new BatchInfo("provider", "outputBatchId"));
    EcloudEngineTask ecloudEngineTask = new EcloudEngineTask(dpsTask);
    assertEquals("123", ecloudEngineTask.getEngineTaskId());
    assertEquals("outputBatchId", ecloudEngineTask.getEngineBatchId());
  }

  @Test
  void noOutputHasNoBatch() {
    DpsTask dpsTask = new DpsTask();
    dpsTask.setTaskId(123);
    EcloudEngineTask ecloudEngineTask = new EcloudEngineTask(dpsTask);
    assertEquals("123", ecloudEngineTask.getEngineTaskId());
    assertNull(ecloudEngineTask.getEngineBatchId());
  }
}
