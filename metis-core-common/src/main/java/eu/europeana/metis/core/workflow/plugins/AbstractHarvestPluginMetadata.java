package eu.europeana.metis.core.workflow.plugins;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * This abstract class is the base implementation of {@link ExecutablePluginMetadata} for harvest tasks. All executable harvest
 * plugins should inherit from it.
 */
@Setter
@Getter
@NoArgsConstructor
@Slf4j
public abstract class AbstractHarvestPluginMetadata extends AbstractExecutablePluginMetadata {

  public static final int DEFAULT_STEP_SIZE = 1;

  /**
   * Default false. If false, it indicates that the ProvidedCHO rdf:about should be used to set the identifier for ECloud
   */
  private boolean useDefaultIdentifiers;

  private Integer stepSize;

  public abstract boolean isIncrementalHarvest();

  /**
   * Normalizes the configured step size for the harvest plugin.
   *
   * <p>
   * If the `stepSize` property is null or contains a non-positive value, it defaults to `DEFAULT_STEP_SIZE` to ensure a valid and
   * properly configured step size. A warning is logged if normalization is required, indicating the invalid value and the
   * fallback to the default.
   */
  public void normalizeStepSize() {
    if (stepSize == null || stepSize <= 0) {
      stepSize = DEFAULT_STEP_SIZE;
      log.warn("Invalid stepSize {}, using default {}", stepSize, DEFAULT_STEP_SIZE);
    }
  }
}
