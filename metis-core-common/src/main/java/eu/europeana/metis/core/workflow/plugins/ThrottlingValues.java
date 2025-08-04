package eu.europeana.metis.core.workflow.plugins;

import java.util.Map;

/**
 * Class encapsulating all possible throttling levels tuples.
 */
public class ThrottlingValues {

  private final Map<ThrottlingLevel, Integer> throttlingMap;

  /**
   * Constructor
   *
   * @param weak The throttling details to represent level weak
   * @param medium The throttling details to represent level medium
   * @param strong The throttling details to represent level strong
   */
  public ThrottlingValues(int weak, int medium, int strong) {
    throttlingMap = Map.of(
        ThrottlingLevel.WEAK, weak,
        ThrottlingLevel.MEDIUM, medium,
        ThrottlingLevel.STRONG, strong
    );
  }

  /**
   * Returns the number of threads associated with the given throttling level.
   * If the given throttling level is not found in the map, it returns the number of threads
   * associated with the weak throttling level.
   *
   * @param throttlingLevel The throttling level for which to get the number of threads.
   * @return The number of threads associated with the given throttling level, or the number of threads
   *         associated with the weak throttling level if the given throttling level is not found.
   */
  public int getThreadNumberFromThrottlingLevel(ThrottlingLevel throttlingLevel) {
    return throttlingMap.getOrDefault(throttlingLevel, throttlingMap.get(ThrottlingLevel.WEAK));
  }
}
