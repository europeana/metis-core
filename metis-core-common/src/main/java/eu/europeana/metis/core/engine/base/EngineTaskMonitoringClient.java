package eu.europeana.metis.core.engine.base;

import eu.europeana.metis.core.engine.base.item.report.DataItemStatus;
import eu.europeana.metis.core.engine.base.task.report.EngineTaskErrors;
import eu.europeana.metis.core.engine.base.task.report.EngineTaskProgress;
import eu.europeana.metis.core.workflow.plugins.PluginType;
import eu.europeana.metis.exception.ExternalTaskException;
import java.util.List;

/**
 * Provides methods for monitoring execution progress, retrieving error reports, and managing task-related data for tasks in a
 * processing engine.
 */
public interface EngineTaskMonitoringClient {

  /**
   * Retrieves the progress of a specific task running in the engine.
   *
   * @param topologyName The name of the topology associated with the task.
   * @param taskId The unique identifier of the task whose progress is to be retrieved.
   * @param pluginType
   * @return An instance of {@link EngineTaskProgress} containing details about the task's progress.
   * @throws ExternalTaskException If an error occurs while accessing the external resource.
   */
  EngineTaskProgress getEngineTaskProgress(String topologyName, String taskId, PluginType pluginType) throws ExternalTaskException;

  /**
   * Retrieves a list of data item statuses based on the specified parameters.
   *
   * @param topologyName The name of the topology to query.
   * @param taskId The ID of the task associated with the data items.
   * @param from The starting index for the data items to retrieve.
   * @param to The ending index for the data items to retrieve.
   * @return A list of DataItemStatus objects representing the statuses of the data items.
   * @throws ExternalTaskException If an error occurs while retrieving the data item statuses.
   */
  List<DataItemStatus> getDataItemStatuses(String topologyName, String taskId, int from, int to) throws ExternalTaskException;

  /**
   * Checks if there is an error report for the specified engine task.
   *
   * @param topologyName The name of the topology to check the task for.
   * @param taskId The unique identifier of the task.
   * @return True if an error report exists for the task, false otherwise.
   * @throws ExternalTaskException If there is an error accessing the task information.
   */
  boolean hasEngineTaskErrorReport(String topologyName, String taskId) throws ExternalTaskException;

  /**
   * Retrieves a list of errors reported for a specific task in the processing engine.
   *
   * @param topologyName The name of the topology associated with the task.
   * @param taskId The unique identifier of the task whose errors are to be retrieved.
   * @param maxEntries The maximum number of error entries to retrieve.
   * @return An instance of EngineTaskErrors containing details of the task errors.
   * @throws ExternalTaskException If an error occurs while accessing the external resource.
   */
  EngineTaskErrors getEngineTaskErrors(String topologyName, String taskId, int maxEntries) throws ExternalTaskException;

}
