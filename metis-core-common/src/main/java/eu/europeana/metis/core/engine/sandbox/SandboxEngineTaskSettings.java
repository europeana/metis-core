package eu.europeana.metis.core.engine.sandbox;

import eu.europeana.metis.core.engine.base.EngineTaskSettings;
import eu.europeana.metis.core.workflow.plugins.ThrottlingValues;

/**
 * Adapts SandboxEngineTaskSettings to the ExternalTaskSettings interface.
 */
public class SandboxEngineTaskSettings extends EngineTaskSettings {

  /**
   * Constructor.
   *
   * @param baseUrl The base URL for the engine.
   * @param provider The provider identifier for the engine task. Not really applicable for metis-sandbox.
   * @param metisCoreBaseUrl The base URL of the Metis core service.
   * @param throttlingValues Throttling settings to manage task processing levels.
   */
  public SandboxEngineTaskSettings(String baseUrl, String provider, String metisCoreBaseUrl,
      ThrottlingValues throttlingValues) {
    super(baseUrl, provider, metisCoreBaseUrl, throttlingValues);
  }
}
