package eu.europeana.metis.core.dao;

import eu.europeana.metis.core.workflow.plugins.MetisPlugin;
import eu.europeana.metis.core.workflow.plugins.MetisPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.PluginType;
import java.time.Instant;
import java.util.Objects;

/**
 * Instances of this class uniquely define a plugin that has been completed. It can be used to test
 * equality of executed plugins.
 */
public class ExecutedMetisPluginId {

  private final Instant pluginStartedDate;
  private final PluginType pluginType;

  ExecutedMetisPluginId(Instant pluginStartedDate, PluginType pluginType) {
    this.pluginStartedDate = pluginStartedDate;
    this.pluginType = pluginType;
    if (this.pluginStartedDate == null || this.pluginType == null) {
      throw new IllegalArgumentException();
    }
  }

  /**
   * Creates the ID of this plugin.
   *
   * @param plugin The pluign for which to create the ID.
   * @return The ID of this plugin, or null if this plugin has not been started yet.
   */
  public static ExecutedMetisPluginId forPlugin(MetisPlugin plugin) {
    final Instant startedDate = plugin.getStartedDate();
    if (startedDate == null) {
      return null;
    }
    return new ExecutedMetisPluginId(startedDate, plugin.getPluginType());
  }

  /**
   * Extracts the ID of the predecessor plugin of this plugin.
   *
   * @param plugin The plugin for which to extract the predecessor ID.
   * @return The ID of the predecessor, or null if no predecessor defined.
   */
  public static ExecutedMetisPluginId forPredecessor(MetisPlugin plugin) {
    return forPredecessor(plugin.getPluginMetadata());
  }

  /**
   * Extracts the ID of the predecessor plugin of this plugin.
   *
   * @param metadata The metadata of the plugin for which to extract the predecessor ID.
   * @return The ID of the predecessor, or null if no predecessor defined.
   */
  public static ExecutedMetisPluginId forPredecessor(MetisPluginMetadata metadata) {
    final Instant predecessorPluginStartedDate = metadata.getPredecessorPluginStartedDate();
    final PluginType previousPluginType = PluginType.getPluginTypeFromEnumName(metadata.getPredecessorPluginName());
    if (predecessorPluginStartedDate == null || previousPluginType == null) {
      return null;
    }
    return new ExecutedMetisPluginId(predecessorPluginStartedDate, previousPluginType);
  }

  public Instant getPluginStartedDate() {
    return pluginStartedDate;
  }

  public PluginType getPluginType() {
    return pluginType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final ExecutedMetisPluginId that = (ExecutedMetisPluginId) o;
    return Objects.equals(pluginStartedDate, that.getPluginStartedDate()) &&
            getPluginType() == that.getPluginType();
  }

  @Override
  public int hashCode() {
    return Objects.hash(pluginStartedDate, getPluginType());
  }
}
