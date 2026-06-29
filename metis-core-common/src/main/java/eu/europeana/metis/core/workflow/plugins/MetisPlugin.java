package eu.europeana.metis.core.workflow.plugins;

import java.time.Instant;
import java.util.Optional;

/**
 * This interface represents a plugin. It contains the minimum a plugin should support so that it
 * can be plugged in the Metis workflow registry and can be accessible via the REST API of Metis.
 */
public interface MetisPlugin {

  String REPRESENTATION_NAME = "metadataRecord";

  static String getRepresentationName() {
    return REPRESENTATION_NAME;
  }

  String getId();

  /**
   * @return {@link PluginType}
   */
  PluginType getPluginType();

  /**
   * The metadata corresponding to this plugin.
   *
   * @return {@link MetisPluginMetadata}
   */
  MetisPluginMetadata getPluginMetadata();


  /**
   * Retrieves the date and time when the plugin execution started.
   *
   * @return an {@link Instant} representing the start date and time of the plugin execution
   */
  Instant getStartedDate();

  /**
   * Retrieves the date and time when the plugin was last updated.
   *
   * @return an {@link Instant} representing the last update date and time of the plugin
   */
  Instant getUpdatedDate();

  /**
   * Retrieves the date and time when the plugin execution finished.
   *
   * @return an {@link Instant} representing the finish date and time of the plugin execution
   */
  Instant getFinishedDate();

  /**
   * @return status {@link PluginStatus} of the execution of the plugin
   */
  PluginStatus getPluginStatus();

  /**
   * Retrieves the failure message associated with the plugin execution, if any.
   *
   * @return a String representing the failure message of the plugin execution, or null if no failure
   * occurred or no message is available.
   */
  String getFailMessage();

  /**
   * @return The data status of this plugin. If null, this should be interpreted as being equal to
   * {@link DataStatus#VALID} (due to backwards-compatibility).
   */
  DataStatus getDataStatus();

  /**
   * Returns the data state for the plugin taking into account the default value.
   *
   * @param plugin The plugin.
   * @return The data status of the given plugin. Is not null.
   */
  static DataStatus getDataStatus(ExecutablePlugin plugin) {
    return Optional.ofNullable(plugin.getDataStatus()).orElse(DataStatus.VALID);
  }
}
