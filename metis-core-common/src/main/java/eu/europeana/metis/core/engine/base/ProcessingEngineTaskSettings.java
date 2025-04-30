package eu.europeana.metis.core.engine.base;

import eu.europeana.metis.core.workflow.plugins.ThrottlingValues;
import java.util.function.Supplier;

/**
 * Basic task settings needed for executing tasks in external systems.
 */
public interface ProcessingEngineTaskSettings<T extends ProcessingEngineTask> {

  String getBaseUrl();

  String getProvider();

  String getDatasetId();

  String getPreviousTaskId();

  String getMetisCoreBaseUrl();

  ThrottlingValues getThrottlingValues();

  Supplier<T> getTaskCreator();

}
