package eu.europeana.metis.core.engine.mock;

import eu.europeana.metis.core.engine.base.EngineTaskSettings;
import eu.europeana.metis.core.workflow.plugins.ThrottlingValues;

/**
 * Adapts DpsTaskSettings to the ExternalTaskSettings interface.
 */
public record MockEngineTaskSettings(
    String baseUrl,
    String provider,
    String metisCoreBaseUrl,
    ThrottlingValues throttlingValues) implements EngineTaskSettings {

}
