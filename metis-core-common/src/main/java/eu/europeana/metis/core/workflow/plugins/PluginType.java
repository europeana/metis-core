package eu.europeana.metis.core.workflow.plugins;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Contains all Plugin types.
 */
public enum PluginType {

  HTTP_HARVEST,

  OAIPMH_HARVEST,

  ENRICHMENT,

  MEDIA_PROCESS,

  LINK_CHECKING,

  VALIDATION_EXTERNAL,

  TRANSFORMATION,

  VALIDATION_INTERNAL,

  NORMALIZATION,

  PREVIEW,

  PUBLISH,

  DEPUBLISH,

  REINDEX_TO_PREVIEW,

  REINDEX_TO_PUBLISH;

  /**
   * Lookup of a {@link PluginType} enum from a provided enum String representation of the enum
   * value.
   *
   * @param enumName the String representation of an enum value
   * @return the {@link PluginType} that represents the provided value or null if not found
   */
  @JsonCreator
  public static PluginType getPluginTypeFromEnumName(String enumName) {
    for (PluginType pluginType : PluginType.values()) {
      if (pluginType.name().equalsIgnoreCase(enumName)) {
        return pluginType;
      }
    }
    return null;
  }
}
