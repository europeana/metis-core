package eu.europeana.metis.core.engine.sandbox;

import eu.europeana.metis.core.engine.base.EngineTaskSettings;
import eu.europeana.metis.core.workflow.plugins.ThrottlingValues;

/**
 * Adapts SandboxEngineTaskSettings to the ExternalTaskSettings interface.
 */
public class SandboxEngineTaskSettings extends EngineTaskSettings {

  public SandboxEngineTaskSettings(String baseUrl, String provider, String metisCoreBaseUrl,
      ThrottlingValues throttlingValues) {
    super(baseUrl, provider, metisCoreBaseUrl, throttlingValues);
  }
}
