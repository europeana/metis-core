package eu.europeana.metis.core.engine.base;

import eu.europeana.metis.core.workflow.plugins.ThrottlingValues;

/**
 * Basic task settings needed for executing tasks in a processing engine.
 */
public class EngineTaskSettings {

  protected final String baseUrl;
  protected final String provider;
  protected final String metisCoreBaseUrl;
  protected final ThrottlingValues throttlingValues;

  protected EngineTaskSettings(String baseUrl, String provider, String metisCoreBaseUrl, ThrottlingValues throttlingValues) {
    this.baseUrl = baseUrl;
    this.provider = provider;
    this.metisCoreBaseUrl = metisCoreBaseUrl;
    this.throttlingValues = throttlingValues;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public String getProvider() {
    return provider;
  }

  public String getMetisCoreBaseUrl() {
    return metisCoreBaseUrl;
  }

  public ThrottlingValues getThrottlingValues() {
    return throttlingValues;
  }
}
