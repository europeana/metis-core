package eu.europeana.metis.core.workflow.plugins;

import eu.europeana.cloud.common.model.dps.TaskState;
import eu.europeana.metis.core.engine.base.ProcessingEngineTask;
import eu.europeana.metis.core.engine.base.ProcessingEngineTaskClient;
import eu.europeana.metis.core.engine.base.ProcessingEngineTaskSettings;
import eu.europeana.metis.exception.ExternalTaskException;
import eu.europeana.metis.exception.UnrecoverableExternalTaskException;

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
  void execute(String datasetId, String previousTaskId, ProcessingEngineTaskClient<S, T> processingEngineTaskClient)
      throws ExternalTaskException;

  <S extends ProcessingEngineTaskSettings, T extends ProcessingEngineTask>
  MonitorResult monitor(ProcessingEngineTaskClient<S, T> processingEngineTaskClient) throws ExternalTaskException, UnrecoverableExternalTaskException;

  <S extends ProcessingEngineTaskSettings, T extends ProcessingEngineTask>
  void cancel(ProcessingEngineTaskClient<S, T> processingEngineTaskClient, String cancelledById) throws ExternalTaskException;

  /**
   * This object represents the result of a monitor call. It contains the information that monitoring processes need.
   */
    record MonitorResult(TaskState taskState, String taskInfo) {}
}
