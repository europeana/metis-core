package eu.europeana.metis.core.workflow.execution;

import eu.europeana.metis.core.workflow.plugins.AbstractExecutablePlugin;
import eu.europeana.metis.core.workflow.plugins.AbstractMetisPlugin;
import org.junit.jupiter.api.Test;

import static eu.europeana.metis.core.workflow.execution.TestMetisPluginUtils.getAbstractExecutablePluginUsingSetters;
import static eu.europeana.metis.core.workflow.execution.TestMetisPluginUtils.getAbstractMetisPluginUsingSetters;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TestMetisPluginConverter {

  @Test
  void testToDTO() {
    AbstractExecutablePlugin<?> abstractExecutablePlugin = getAbstractExecutablePluginUsingSetters();
    MetisPluginDTO metisPluginDTO = MetisPluginConverter.toDTO(abstractExecutablePlugin, true);
    assertMetisPluginEquals(abstractExecutablePlugin, metisPluginDTO);
  }

  @Test
  void testToDTO_NonExecutablePlugin() {
    AbstractMetisPlugin<?> abstractMetisPlugin = getAbstractMetisPluginUsingSetters();
    MetisPluginDTO metisPluginDTO = MetisPluginConverter.toDTO(abstractMetisPlugin, true);
    assertMetisPluginEquals(abstractMetisPlugin, metisPluginDTO);
  }

  static void assertMetisPluginEquals(AbstractMetisPlugin<?> abstractMetisPlugin, MetisPluginDTO metisPluginDTO) {
    assertEquals(abstractMetisPlugin.getId(), metisPluginDTO.getId());
    assertEquals(abstractMetisPlugin.getPluginStatus(), metisPluginDTO.getPluginStatus());
    assertEquals(abstractMetisPlugin.getDataStatus(), metisPluginDTO.getDataStatus());
    assertEquals(abstractMetisPlugin.getFailMessage(), metisPluginDTO.getFailMessage());
    assertEquals(abstractMetisPlugin.getStartedDate(), metisPluginDTO.getStartedDate());
    assertEquals(abstractMetisPlugin.getUpdatedDate(), metisPluginDTO.getUpdatedDate());
    assertEquals(abstractMetisPlugin.getFinishedDate(), metisPluginDTO.getFinishedDate());
    assertEquals(abstractMetisPlugin.getPluginMetadata(), metisPluginDTO.getPluginMetadata());
    if (abstractMetisPlugin instanceof AbstractExecutablePlugin<?> abstractExecutablePlugin) {
      assertEquals(abstractExecutablePlugin.getEngineTaskId(), metisPluginDTO.getEngineTaskId());
      TestExecutionProgressConverter.assertExecutionProgressEquals(abstractExecutablePlugin.getExecutionProgress(),
          metisPluginDTO.getExecutionProgress());
    }
  }
}