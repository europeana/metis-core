package eu.europeana.metis.core.workflow.execution;

import eu.europeana.metis.core.workflow.plugins.AbstractExecutablePlugin;
import eu.europeana.metis.core.workflow.plugins.AbstractMetisPlugin;

/**
 * A utility class that provides methods for converting {@link AbstractMetisPlugin} into {@link MetisPluginDTO}.
 *
 * <p>This class is designed to act as a translator between the domain model
 * and the Data Transfer Object (DTO) for AbstractMetisPlugin, ensuring separation of concerns and easing data transfer between
 * layers.
 */
public final class MetisPluginConverter {

  /**
   * Private constructor to prevent instantiation of this utility class.
   */
  private MetisPluginConverter() {
  }

  /**
   * Converts an instance of {@link AbstractMetisPlugin} into a {@link MetisPluginDTO}.
   *
   * @param abstractMetisPlugin the plugin to be converted, containing information on its type, status, execution details, and
   * other metadata
   * @param canDisplayRawXml a flag indicating whether raw XML data can be displayed for this plugin
   * @return a {@link MetisPluginDTO} containing the data from the provided plugin
   */
  public static MetisPluginDTO toDTO(AbstractMetisPlugin<?> abstractMetisPlugin, boolean canDisplayRawXml) {
    MetisPluginDTO metisPluginDTO = new MetisPluginDTO();
    metisPluginDTO.setPluginType(abstractMetisPlugin.getPluginType());
    metisPluginDTO.setId(abstractMetisPlugin.getId());
    metisPluginDTO.setPluginStatus(abstractMetisPlugin.getPluginStatus());
    metisPluginDTO.setDataStatus(abstractMetisPlugin.getDataStatus());
    metisPluginDTO.setFailMessage(abstractMetisPlugin.getFailMessage());
    metisPluginDTO.setStartedDate(abstractMetisPlugin.getStartedDate());
    metisPluginDTO.setUpdatedDate(abstractMetisPlugin.getUpdatedDate());
    metisPluginDTO.setFinishedDate(abstractMetisPlugin.getFinishedDate());
    metisPluginDTO.setCanDisplayRawXml(canDisplayRawXml);
    metisPluginDTO.setPluginMetadata(abstractMetisPlugin.getPluginMetadata());
    if (abstractMetisPlugin instanceof AbstractExecutablePlugin<?> abstractExecutablePlugin) {
      metisPluginDTO.setEngineTaskId(abstractExecutablePlugin.getEngineTaskId());
      metisPluginDTO.setEngineBatchId(abstractExecutablePlugin.getEngineBatchId());
      metisPluginDTO.setExecutionProgress(ExecutionProgressConverter.toDTO(abstractExecutablePlugin.getExecutionProgress()));
      metisPluginDTO.setTopologyName(abstractExecutablePlugin.getTopologyName());
    }
    return metisPluginDTO;
  }
}
