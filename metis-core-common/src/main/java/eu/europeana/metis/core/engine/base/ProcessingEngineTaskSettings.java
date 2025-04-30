package eu.europeana.metis.core.engine.base;

import eu.europeana.metis.core.workflow.plugins.ThrottlingValues;

/**
 * Basic task settings needed for executing tasks in external systems.
 */
public interface ProcessingEngineTaskSettings {

  String baseUrl();

  String provider();

  String metisCoreBaseUrl();

  ThrottlingValues throttlingValues();
}
