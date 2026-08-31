package eu.europeana.metis.core.workflow.plugins;


import eu.europeana.metis.utils.DepublicationReason;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Index to Publish Plugin Metadata.
 */
@Getter
@Setter
@NoArgsConstructor
public class DepublishPluginMetadata extends AbstractExecutablePluginMetadata {

  private static final ExecutablePluginType pluginType = ExecutablePluginType.DEPUBLISH;
  private boolean datasetDepublish;
  private Set<String> recordIdsToDepublish;
  private DepublicationReason depublicationReason;

  @Override
  public ExecutablePluginType getExecutablePluginType() {
    return pluginType;
  }

  public Set<String> getRecordIdsToDepublish() {
    return Optional.ofNullable(recordIdsToDepublish).map(Collections::unmodifiableSet)
            .orElseGet(Collections::emptySet);
  }

  public void setRecordIdsToDepublish(Set<String> recordIdsToDepublish) {
    this.recordIdsToDepublish = new HashSet<>(recordIdsToDepublish);
  }
}
