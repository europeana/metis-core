package eu.europeana.metis.core.workflow.plugins;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import dev.morphia.annotations.Entity;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * This abstract class is the base implementation of {@link MetisPluginMetadata} and all other plugins should inherit from it.
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
@Entity
@Getter
@Setter
@NoArgsConstructor
public abstract class AbstractMetisPluginMetadata implements MetisPluginMetadata {

  private String predecessorPluginName;
  private Instant predecessorPluginStartedDate;

  /**
   * For the current plugin, set up the predecessor link.
   *
   * @param predecessor the predecessor plugin that the current plugin is based on. Is not null.
   */
  public void setPredecessorInformation(ExecutablePlugin predecessor) {
    this.setPredecessorPluginName(predecessor.getPluginType().name());
    this.setPredecessorPluginStartedDate(predecessor.getStartedDate());
  }
}
