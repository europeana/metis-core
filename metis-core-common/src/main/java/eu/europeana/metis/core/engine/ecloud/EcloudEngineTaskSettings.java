package eu.europeana.metis.core.engine.ecloud;

import eu.europeana.metis.core.engine.base.AbstractEngineTaskSettings;
import eu.europeana.metis.core.workflow.plugins.ThrottlingValues;

/**
 * Adapts DpsTaskSettings to the ExternalTaskSettings interface.
 */
public final class EcloudEngineTaskSettings extends AbstractEngineTaskSettings {

  public EcloudEngineTaskSettings(
      String baseUrl,
      String provider,
      String metisCoreBaseUrl,
      ThrottlingValues throttlingValues) {
    super(baseUrl, provider, metisCoreBaseUrl, throttlingValues);
  }

}
