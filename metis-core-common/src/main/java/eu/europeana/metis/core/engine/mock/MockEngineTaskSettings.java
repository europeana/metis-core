package eu.europeana.metis.core.engine.mock;

import eu.europeana.metis.core.engine.base.EngineTaskSettings;
import eu.europeana.metis.core.workflow.plugins.ThrottlingValues;

/**
 * Adapts DpsTaskSettings to the ExternalTaskSettings interface.
 */
public final class MockEngineTaskSettings extends EngineTaskSettings {

  public MockEngineTaskSettings(
      String baseUrl,
      String provider,
      String metisCoreBaseUrl,
      ThrottlingValues throttlingValues) {
    super(baseUrl, provider, metisCoreBaseUrl, throttlingValues);
  }

}
