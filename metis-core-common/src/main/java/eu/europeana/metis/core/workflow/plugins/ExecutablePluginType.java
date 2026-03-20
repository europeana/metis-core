package eu.europeana.metis.core.workflow.plugins;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This denotes a plugin type that is executable (i.e. can be run by Metis). This is a subset of the list in {@link PluginType},
 * which contains all plugin types.
 */
public enum ExecutablePluginType {

  OAIPMH_HARVEST(PluginType.OAIPMH_HARVEST, ExecutablePluginTypeGroup.HARVEST),

  HTTP_HARVEST(PluginType.HTTP_HARVEST, ExecutablePluginTypeGroup.HARVEST),

  ENRICHMENT(PluginType.ENRICHMENT, ExecutablePluginTypeGroup.CURATE),

  MEDIA_PROCESS(PluginType.MEDIA_PROCESS, ExecutablePluginTypeGroup.CURATE),

  LINK_CHECKING(PluginType.LINK_CHECKING, ExecutablePluginTypeGroup.CURATE),

  VALIDATION_EXTERNAL(PluginType.VALIDATION_EXTERNAL, ExecutablePluginTypeGroup.CURATE),

  TRANSFORMATION(PluginType.TRANSFORMATION, ExecutablePluginTypeGroup.CURATE),

  VALIDATION_INTERNAL(PluginType.VALIDATION_INTERNAL, ExecutablePluginTypeGroup.CURATE),

  NORMALIZATION(PluginType.NORMALIZATION, ExecutablePluginTypeGroup.CURATE),

  PREVIEW(PluginType.PREVIEW, ExecutablePluginTypeGroup.INDEX),

  PUBLISH(PluginType.PUBLISH, ExecutablePluginTypeGroup.INDEX),

  DEPUBLISH(PluginType.DEPUBLISH, ExecutablePluginTypeGroup.DEPUBLISH);

  private final PluginType pluginType;
  private final ExecutablePluginTypeGroup executablePluginTypeGroup;

  ExecutablePluginType(PluginType pluginType, ExecutablePluginTypeGroup executablePluginTypeGroup) {
    this.pluginType = pluginType;
    this.executablePluginTypeGroup = executablePluginTypeGroup;
  }

  /**
   * @return the corresponding instance of {@link PluginType}.
   */
  public PluginType toPluginType() {
    return pluginType;
  }

  /**
   * Get the corresponding {@link ExecutablePluginType} by providing a {@link PluginType} or null if no match found
   *
   * @param pluginType the provided plugin type
   * @return the executable plugin type or null if no match found
   */
  public static ExecutablePluginType getExecutablePluginFromPluginType(PluginType pluginType) {
    for (ExecutablePluginType executablePluginType : values()) {
      if (executablePluginType.pluginType == pluginType) {
        return executablePluginType;
      }
    }
    return null;
  }

  /**
   * Lookup of a {@link ExecutablePluginType} enum from a provided enum String representation of the enum value.
   *
   * @param enumName the String representation of an enum value
   * @return the {@link ExecutablePluginType} that represents the provided value or null if not found
   */
  @JsonCreator
  public static ExecutablePluginType getPluginTypeFromEnumName(
      @JsonProperty("pluginName") String enumName) {
    for (ExecutablePluginType pluginType : values()) {
      if (pluginType.name().equalsIgnoreCase(enumName)) {
        return pluginType;
      }
    }
    return null;
  }

  public ExecutablePluginTypeGroup getExecutablePluginTypeGroup() {
    return executablePluginTypeGroup;
  }

  /**
   * Enum representing groups of executable plugin types.
   * <p>
   * These groups categorize functionality types that can be executed as part of the workflow.
   */
  public enum ExecutablePluginTypeGroup {
    HARVEST, CURATE, INDEX, DEPUBLISH
  }
}
