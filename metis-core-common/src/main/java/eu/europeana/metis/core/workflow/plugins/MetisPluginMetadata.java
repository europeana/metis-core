package eu.europeana.metis.core.workflow.plugins;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import java.time.Instant;

/**
 * This interface represents plugin metadata
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = As.EXISTING_PROPERTY, property = "pluginType")
@JsonSubTypes({
    @JsonSubTypes.Type(value = OaipmhHarvestPluginMetadata.class, name = "OAIPMH_HARVEST"),
    @JsonSubTypes.Type(value = HTTPHarvestPluginMetadata.class, name = "HTTP_HARVEST"),
    @JsonSubTypes.Type(value = TransformationExternalPluginMetadata.class, name = "TRANSFORMATION_EXTERNAL"),
    @JsonSubTypes.Type(value = ValidationExternalPluginMetadata.class, name = "VALIDATION_EXTERNAL"),
    @JsonSubTypes.Type(value = TransformationPluginMetadata.class, name = "TRANSFORMATION"),
    @JsonSubTypes.Type(value = ValidationInternalPluginMetadata.class, name = "VALIDATION_INTERNAL"),
    @JsonSubTypes.Type(value = NormalizationPluginMetadata.class, name = "NORMALIZATION"),
    @JsonSubTypes.Type(value = EnrichmentPluginMetadata.class, name = "ENRICHMENT"),
    @JsonSubTypes.Type(value = MediaProcessPluginMetadata.class, name = "MEDIA_PROCESS"),
    @JsonSubTypes.Type(value = LinkCheckingPluginMetadata.class, name = "LINK_CHECKING"),
    @JsonSubTypes.Type(value = IndexToPreviewPluginMetadata.class, name = "PREVIEW"),
    @JsonSubTypes.Type(value = IndexToPublishPluginMetadata.class, name = "PUBLISH")
})
public interface MetisPluginMetadata {

  PluginType getPluginType();

  String getPredecessorPluginName();

  Instant getPredecessorPluginStartedDate();

}
