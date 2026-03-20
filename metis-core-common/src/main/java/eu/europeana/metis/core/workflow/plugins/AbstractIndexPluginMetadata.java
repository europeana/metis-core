package eu.europeana.metis.core.workflow.plugins;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * This abstract class is the base implementation of {@link ExecutablePluginMetadata} for index tasks. All executable index
 * plugins should inherit from it.
 */
@Getter
@Setter
@NoArgsConstructor
public abstract class AbstractIndexPluginMetadata extends AbstractExecutablePluginMetadata {

  private boolean preserveTimestamps;
  private boolean performRedirects;
  private List<String> datasetIdsToRedirectFrom = new ArrayList<>();
  private boolean incrementalIndexing; // Default: false (i.e. full processing)
  private Date harvestDate;

  public List<String> getDatasetIdsToRedirectFrom() {
    return List.copyOf(datasetIdsToRedirectFrom);
  }

  public void setDatasetIdsToRedirectFrom(List<String> datasetIdsToRedirectFrom) {
    this.datasetIdsToRedirectFrom =
        datasetIdsToRedirectFrom == null ? List.of() : List.copyOf(datasetIdsToRedirectFrom);
  }

  public Date getHarvestDate() {
    return harvestDate == null ? null : new Date(harvestDate.getTime());
  }

  public void setHarvestDate(Date harvestDate) {
    this.harvestDate = harvestDate == null ? null : new Date(harvestDate.getTime());
  }
}
