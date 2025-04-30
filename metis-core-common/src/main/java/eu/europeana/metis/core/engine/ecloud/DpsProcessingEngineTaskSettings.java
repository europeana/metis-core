package eu.europeana.metis.core.engine.ecloud;

import eu.europeana.metis.core.engine.base.ProcessingEngineTaskSettings;
import eu.europeana.metis.core.workflow.plugins.ThrottlingValues;

/**
 * Adapts DpsTaskSettings to the ExternalTaskSettings interface.
 */
public record DpsProcessingEngineTaskSettings(String baseUrl, String provider, String metisCoreBaseUrl,
                                              ThrottlingValues throttlingValues) implements ProcessingEngineTaskSettings {

}
