package eu.europeana.metis.core.workflow.plugins;

import eu.europeana.metis.core.engine.base.ProcessingEngineTask;
import eu.europeana.metis.core.engine.base.ProcessingEngineTaskClient;
import eu.europeana.metis.core.engine.base.ProcessingEngineTaskSettings;
import eu.europeana.metis.exception.ExternalTaskException;

/**
 * This interface represents plugins that are executable by Metis.
 */
public interface ExecutablePlugin extends MetisPlugin {

  /**
   * The metadata corresponding to this plugin.
   *
   * @return {@link ExecutablePluginMetadata}
   */
  @Override
  ExecutablePluginMetadata getPluginMetadata();

  /**
   * @return String representation of the external task identifier of the execution
   */
  String getExternalTaskId();

  /**
   * Progress information of the execution of the plugin
   *
   * @return {@link ExecutionProgress}
   */
  ExecutionProgress getExecutionProgress();

  /**
   * It is required as an abstract method to have proper serialization on the api level.
   *
   * @return the topologyName string coming from {@link Topology}
   */
  String getTopologyName();

  <S extends ProcessingEngineTaskSettings, T extends ProcessingEngineTask>
  void cancel(ProcessingEngineTaskClient<S, T> processingEngineTaskClient, String cancelledById) throws ExternalTaskException;

}
