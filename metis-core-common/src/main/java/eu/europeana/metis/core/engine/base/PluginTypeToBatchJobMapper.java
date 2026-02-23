package eu.europeana.metis.core.engine.base;

import eu.europeana.metis.core.workflow.plugins.PluginType;
import eu.europeana.metis.sandbox.batch.common.FullBatchJobType;

public final class PluginTypeToBatchJobMapper {

  private PluginTypeToBatchJobMapper() {
  }

  public static FullBatchJobType map(PluginType pluginType) {

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
      case LINK_CHECKING, DEPUBLISH, REINDEX_TO_PUBLISH, REINDEX_TO_PREVIEW -> null;
    };
  }
}
