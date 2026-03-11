package eu.europeana.metis.core.engine.base;

import eu.europeana.metis.core.workflow.plugins.ExecutablePluginType;
import eu.europeana.metis.core.workflow.plugins.PluginType;
import eu.europeana.metis.sandbox.common.batch.FullBatchJobType;
import org.jetbrains.annotations.UnknownNullability;

/**
 * Utility class for mapping plugin types to their corresponding full batch job types. This class provides a static method to
 * determine the appropriate {@link FullBatchJobType} based on the provided {@link PluginType}.
 */
public final class PluginTypeToBatchJobMapper {

  private PluginTypeToBatchJobMapper() {
  }

  /**
   * Maps a given {@link PluginType} to its corresponding {@link FullBatchJobType}.
   * This method determines the appropriate job type that aligns with the semantics of the provided plugin type.
   * Certain plugin types may not yet be supported, in which case this method returns {@code null}.
   *
   * @param pluginType the plugin type
   * @return the corresponding {@link FullBatchJobType} if a match is found; otherwise, {@code null}.
   */
  public static FullBatchJobType map(@UnknownNullability ExecutablePluginType pluginType) {

    return switch (pluginType) {

      case HTTP_HARVEST -> FullBatchJobType.HARVEST_FILE;
      case OAIPMH_HARVEST -> FullBatchJobType.HARVEST_OAI;
      case VALIDATION_EXTERNAL -> FullBatchJobType.VALIDATE_EXTERNAL;
      case VALIDATION_INTERNAL -> FullBatchJobType.VALIDATE_INTERNAL;
      //External not supported yet.
      case TRANSFORMATION -> FullBatchJobType.TRANSFORM_INTERNAL;
      case NORMALIZATION -> FullBatchJobType.NORMALIZE;
      case ENRICHMENT -> FullBatchJobType.ENRICH;
      case MEDIA_PROCESS -> FullBatchJobType.MEDIA;

      //Preview and publish here redo the samething.
      case PREVIEW, PUBLISH -> FullBatchJobType.INDEX_PUBLISH;
      case LINK_CHECKING, DEPUBLISH -> null;
    };
  }
}
