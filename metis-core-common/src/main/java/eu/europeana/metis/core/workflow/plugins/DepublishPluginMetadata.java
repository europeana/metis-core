package eu.europeana.metis.core.workflow.plugins;


import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Index to Publish Plugin Metadata.
 *
 * @author Simon Tzanakis (Simon.Tzanakis@europeana.eu)
 * @since 2020-06-16
 */
public class DepublishPluginMetadata extends AbstractExecutablePluginMetadata {

  private static final ExecutablePluginType pluginType = ExecutablePluginType.DEPUBLISH;
  private boolean useAlternativeIndexingEnvironment;
  private boolean datasetDepublish;
  private Set<String> recordIdsToDepublish;

  public DepublishPluginMetadata() {
    //Required for json serialization
  }

  @Override
  public ExecutablePluginType getExecutablePluginType() {
    return pluginType;
  }

  public boolean isUseAlternativeIndexingEnvironment() {
    return useAlternativeIndexingEnvironment;
  }

  public void setUseAlternativeIndexingEnvironment(boolean useAlternativeIndexingEnvironment) {
    this.useAlternativeIndexingEnvironment = useAlternativeIndexingEnvironment;
  }

  public boolean isDatasetDepublish() {
    return datasetDepublish;
  }

  public void setDatasetDepublish(boolean datasetDepublish) {
    this.datasetDepublish = datasetDepublish;
  }

  public Set<String> getRecordIdsToDepublish() {
    return Collections.unmodifiableSet(recordIdsToDepublish);
  }

  public void setRecordIdsToDepublish(Set<String> recordIdsToDepublish) {
    this.recordIdsToDepublish = new HashSet<>(recordIdsToDepublish);
  }
}
