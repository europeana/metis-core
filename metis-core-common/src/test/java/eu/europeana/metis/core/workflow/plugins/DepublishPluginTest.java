package eu.europeana.metis.core.workflow.plugins;

import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.europeana.cloud.service.dps.DpsTask;
import eu.europeana.cloud.service.dps.PluginParameterKeys;
import eu.europeana.metis.utils.DepublicationReason;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DepublishPluginTest {

  DepublishPlugin plugin = new DepublishPlugin();

  @Test
  void getTopologyName() {
    assertEquals("depublication", plugin.getTopologyName());
  }

  @Test
  void prepareDpsTask() {
    ThrottlingValues throttlingValues = new ThrottlingValues(8, 4, 2);
    DpsTaskSettings dpsTaskSettings = new DpsTaskSettings("ecloudurl",
        "ecloudDatasetId",
        "previousTaskId",
        "previousExternalTaskId",
        "metisCoreBaseUrl", throttlingValues);
    DepublishPluginMetadata pluginMetadata = new DepublishPluginMetadata();
    pluginMetadata.setDatasetDepublish(true);
    pluginMetadata.setDepublicationReason(DepublicationReason.GENERIC);
    pluginMetadata.setRecordIdsToDepublish(Set.of("1"));
    plugin.setPluginMetadata(pluginMetadata);
    DpsTask expectedDpsTask = new DpsTask();
    expectedDpsTask.setParameters(Map.of(PluginParameterKeys.METIS_DATASET_ID, "datasetId",
        PluginParameterKeys.DEPUBLICATION_REASON, plugin.getPluginMetadata().getDepublicationReason().name()));

    assertEquals(expectedDpsTask.getParameters(), plugin.prepareDpsTask("datasetId", dpsTaskSettings).getParameters());
  }
}
