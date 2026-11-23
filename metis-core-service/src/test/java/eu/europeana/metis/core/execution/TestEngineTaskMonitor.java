package eu.europeana.metis.core.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import eu.europeana.metis.core.engine.base.EngineTask;
import eu.europeana.metis.core.engine.base.EngineTaskClient;
import eu.europeana.metis.core.engine.base.EngineTaskSettings;
import eu.europeana.metis.core.engine.base.task.report.EngineTaskProgress;
import eu.europeana.metis.core.engine.base.task.report.EngineTaskState;
import eu.europeana.metis.core.workflow.plugins.OaipmhHarvestPlugin;
import org.junit.jupiter.api.Test;

class TestEngineTaskMonitor {

  @Test
  @SuppressWarnings("unchecked")
  void updateExecutionProgressRecalculatesPercentageWithCurrentDepublishCounters() {
    OaipmhHarvestPlugin plugin = new OaipmhHarvestPlugin();
    plugin.getExecutionProgress().setExpectedDepublishRecords(9);
    plugin.getExecutionProgress().setProcessedDepublishRecords(2);

    EngineTaskProgress engineTaskProgress = new EngineTaskProgress();
    engineTaskProgress.setEngineTaskState(EngineTaskState.PROCESSED);
    engineTaskProgress.setExpectedRecords(1);
    engineTaskProgress.setProcessedRecords(1);
    engineTaskProgress.setExpectedDepublishRecords(9);
    engineTaskProgress.setProcessedDepublishRecords(9);

    EngineTaskClient<EngineTaskSettings, EngineTask> engineTaskClient = mock(EngineTaskClient.class);
    EngineTaskMonitor<EngineTaskSettings, EngineTask> engineTaskMonitor =
        new EngineTaskMonitor<>(plugin, engineTaskClient);

    engineTaskMonitor.updateExecutionProgress(engineTaskProgress);

    assertEquals(100, plugin.getExecutionProgress().getProgressPercentage());
  }
}
