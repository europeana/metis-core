package eu.europeana.metis.core.rest.controller;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.text.StringEscapeUtils.escapeJava;

import eu.europeana.metis.core.engine.base.task.report.EngineTaskErrors;
import eu.europeana.metis.core.exceptions.NoWorkflowExecutionFoundException;
import eu.europeana.metis.core.rest.ListOfIds;
import eu.europeana.metis.core.rest.Record;
import eu.europeana.metis.core.rest.RecordsResponse;
import eu.europeana.metis.core.rest.stats.NodePathStatisticsDTO;
import eu.europeana.metis.core.rest.stats.RecordStatisticsDTO;
import eu.europeana.metis.core.service.ProxiesService;
import eu.europeana.metis.core.workflow.plugins.ExecutablePluginType;
import eu.europeana.metis.core.workflow.plugins.PluginType;
import eu.europeana.metis.exception.GenericMetisException;
import eu.europeana.metis.utils.RestEndpoints;
import java.util.Collections;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Proxies Controller which encapsulates functionality that has to be proxied to an external resource.
 */
@Slf4j
@RestController
public class ProxiesController {

  private static final int NUMBER_OF_RECORDS = 5;
  private final ProxiesService proxiesService;

  /**
   * Constructor with required parameters
   *
   * @param proxiesService {@link ProxiesService}
   */
  @Autowired
  public ProxiesController(ProxiesService proxiesService) {
    this.proxiesService = proxiesService;
  }

  /**
   * Checks if a final report is available for the specified external task.
   *
   * @param topologyName the name of the topology associated with the task
   * @param engineTaskId the identifier of the external task
   * @return a map with a single entry where the key is "existsExternalTaskReport" and the value is true if the final report is
   * available, or false otherwise.
   * @throws GenericMetisException can be one of:
   * <ul>
   * <li>{@link NoWorkflowExecutionFoundException} if no
   * workflow execution exists for the provided external task identifier</li>
   * </ul>
   */
  @GetMapping(value = RestEndpoints.ORCHESTRATOR_PROXIES_TOPOLOGY_TASK_REPORT_EXISTS,
      produces = {MediaType.APPLICATION_JSON_VALUE})
  @ResponseStatus(HttpStatus.OK)
  public Map<String, Boolean> existsEngineTaskReport(
      @PathVariable("topologyName") String topologyName,
      @PathVariable("engineTaskId") String engineTaskId) throws GenericMetisException {
    log.info("Requesting proxy call to check if task report exists for topologyName: {}, engineTaskId: {}",
        escapeJava(topologyName), escapeJava(engineTaskId));
    return Collections.singletonMap("existsEngineTaskReport",
        proxiesService.existsEngineTaskReport(topologyName, engineTaskId));
  }

  /**
   * Get the final report that includes all the errors grouped. The number of ids per error can be specified through the
   * parameters.
   *
   * @param topologyName the topology name of the task
   * @param engineTaskId the task identifier
   * @param idsPerError the number of ids that should be displayed per error group
   * @return the list of errors grouped
   * @throws GenericMetisException can be one of:
   * <ul>
   * <li>{@link eu.europeana.cloud.service.dps.exception.DpsException} if an error occurred while
   * retrieving the report from the external resource</li>
   * <li>{@link eu.europeana.metis.core.exceptions.NoWorkflowExecutionFoundException} if no
   * workflow execution exists for the provided external task identifier</li>
   * </ul>
   */
  @GetMapping(value = RestEndpoints.ORCHESTRATOR_PROXIES_TOPOLOGY_TASK_REPORT,
      produces = {MediaType.APPLICATION_JSON_VALUE})
  @ResponseStatus(HttpStatus.OK)
  public EngineTaskErrors getEngineTaskReport(
      @PathVariable("topologyName") String topologyName,
      @PathVariable("engineTaskId") String engineTaskId,
      @RequestParam("idsPerError") int idsPerError) throws GenericMetisException {
    log.info("Requesting proxy call task reports for topologyName: {}, engineTaskId: {}",
        escapeJava(topologyName), escapeJava(engineTaskId));
    return proxiesService.getExternalTaskReport(topologyName, engineTaskId, idsPerError);
  }

  /**
   * Get the statistics on the given task.
   *
   * @param topologyName the topology name of the task
   * @param engineTaskId the task identifier
   * @return the task statistics
   * @throws GenericMetisException can be one of:
   * <ul>
   * <li>{@link eu.europeana.cloud.service.dps.exception.DpsException} if an error occurred while
   * retrieving the statistics from the external resource</li>
   * <li>{@link eu.europeana.metis.core.exceptions.NoWorkflowExecutionFoundException} if no
   * workflow execution exists for the provided external task identifier</li>
   * </ul>
   */
  @GetMapping(value = RestEndpoints.ORCHESTRATOR_PROXIES_TOPOLOGY_TASK_STATISTICS,
      produces = {MediaType.APPLICATION_JSON_VALUE})
  @ResponseStatus(HttpStatus.OK)
  public RecordStatisticsDTO getEngineTaskStatistics(
      @PathVariable("topologyName") String topologyName,
      @PathVariable("engineTaskId") String engineTaskId) throws GenericMetisException {
    log.info("Requesting proxy call task statistics for topologyName: {}, engineTaskId: {}",
        escapeJava(topologyName), escapeJava(engineTaskId));
    return proxiesService.getExternalTaskStatistics(topologyName, engineTaskId);
  }

  /**
   * Get additional statistics on a node. This method can be used to elaborate on one of the items returned by
   * {@link #getEngineTaskStatistics(String, String)}.
   *
   * @param topologyName the topology name of the task
   * @param engineTaskId the task identifier
   * @param nodePath the path of the node for which this request is made
   * @return the list of errors grouped
   * @throws GenericMetisException can be one of:
   * <ul>
   * <li>{@link eu.europeana.cloud.service.dps.exception.DpsException} if an error occurred while
   * retrieving the statistics from the external resource</li>
   * <li>{@link eu.europeana.metis.core.exceptions.NoWorkflowExecutionFoundException} if no
   * workflow execution exists for the provided external task identifier</li>
   * </ul>
   */
  @GetMapping(value = RestEndpoints.ORCHESTRATOR_PROXIES_TOPOLOGY_TASK_NODE_STATISTICS,
      produces = {MediaType.APPLICATION_JSON_VALUE})
  @ResponseStatus(HttpStatus.OK)
  public NodePathStatisticsDTO getAdditionalNodeStatistics(
      @PathVariable("topologyName") String topologyName,
      @PathVariable("engineTaskId") String engineTaskId,
      @RequestParam("nodePath") String nodePath) throws GenericMetisException {
    log.info("Requesting proxy call additional node statistics for topologyName: {}, engineTaskId: {}",
        escapeJava(topologyName), escapeJava(engineTaskId));
    return proxiesService.getAdditionalNodeStatistics(topologyName, engineTaskId, nodePath);
  }

  /**
   * Get a list with record contents from the external resource based on an workflow execution and {@link PluginType}.
   *
   * @param workflowExecutionId the execution identifier of the workflow
   * @param pluginType the {@link PluginType} that is to be located inside the workflow
   * @param nextPage the string representation of the next page which is provided from the response and can be used to get the
   * next page of results.
   * TODO: The nextPage parameter is currently ignored and we should decide if we would support it again in the future.
   * @return the list of records from the external resource
   * @throws GenericMetisException can be one of:
   * <ul>
   * <li>{@link eu.europeana.metis.exception.ExternalTaskException} if an error occurred while
   * retrieving the records from the external resource</li>
   * <li>{@link eu.europeana.metis.core.exceptions.NoWorkflowExecutionFoundException} if no
   * workflow execution exists for the provided identifier</li>
   * </ul>
   */
  @GetMapping(value = RestEndpoints.ORCHESTRATOR_PROXIES_RECORDS,
      produces = {MediaType.APPLICATION_JSON_VALUE})
  @ResponseStatus(HttpStatus.OK)
  public RecordsResponse getListOfFileContentsFromPluginExecution(
      @RequestParam("workflowExecutionId") String workflowExecutionId,
      @RequestParam("pluginType") ExecutablePluginType pluginType,
      @RequestParam(value = "nextPage", required = false) String nextPage
  ) throws GenericMetisException {
    return proxiesService.getListOfFileContentsFromPluginExecution(workflowExecutionId, pluginType,
        isEmpty(nextPage) ? null : nextPage, NUMBER_OF_RECORDS);
  }

  /**
   * Get a list with record contents from the external resource for a specific list of IDS based on a workflow execution and
   * {@link PluginType}.
   *
   * @param workflowExecutionId the execution identifier of the workflow
   * @param pluginType the {@link ExecutablePluginType} that is to be located inside the workflow
   * @param ecloudIds the list of ecloud IDs of the records we wish to obtain
   * @return the list of records from the external resource matching the input ID list. If no record with the matching ID was
   * found in the given workflow step, no entry for this record will appear in the result list.
   * @throws GenericMetisException can be one of:
   * <ul>
   * <li>{@link eu.europeana.metis.exception.ExternalTaskException} if an error occurred while
   * retrieving the records from the external resource</li>
   * <li>{@link eu.europeana.metis.core.exceptions.NoWorkflowExecutionFoundException} if no workflow
   * execution exists for the provided identifier</li>
   * </ul>
   */
  @PostMapping(value = RestEndpoints.ORCHESTRATOR_PROXIES_RECORDS_BY_IDS,
      consumes = {MediaType.APPLICATION_JSON_VALUE},
      produces = {MediaType.APPLICATION_JSON_VALUE})
  @ResponseStatus(HttpStatus.OK)
  public RecordsResponse getListOfFileContentsFromPluginExecution(
      @RequestParam("workflowExecutionId") String workflowExecutionId,
      @RequestParam("pluginType") ExecutablePluginType pluginType,
      @RequestBody ListOfIds ecloudIds
  ) throws GenericMetisException {
    return proxiesService.getListOfFileContentsFromPluginExecution(workflowExecutionId,
        pluginType, ecloudIds);
  }

  /**
   * Get an eCloudId from the external resource for a specific searchId.
   *
   * @param workflowExecutionId the execution identifier of the workflow
   * @param pluginType the plugin from the execution
   * @param idToSearch the ID we are searching for and for which we want to find a record
   * @return the CloudId from the external resource matching the input ID. If no record with the matching ID was found, it will
   * return an empty string.
   * @throws GenericMetisException can be one of:
   * <ul>
   * <li>{@link eu.europeana.metis.exception.ExternalTaskException} if an error occurred while
   * retrieving the records from the external resource</li>
   * <li>{@link eu.europeana.metis.core.exceptions.NoWorkflowExecutionFoundException} if no workflow
   * execution exists for the provided identifier</li>
   * </ul>
   */
  @PostMapping(value = RestEndpoints.ORCHESTRATOR_PROXIES_RECORD_SEARCH_BY_ID,
      produces = {MediaType.APPLICATION_JSON_VALUE})
  @ResponseStatus(HttpStatus.OK)
  public Record searchRecordByIdFromPluginExecution(
      @RequestParam("workflowExecutionId") String workflowExecutionId,
      @RequestParam("pluginType") ExecutablePluginType pluginType,
      @RequestParam("idToSearch") String idToSearch
  ) throws GenericMetisException {
    return proxiesService.searchRecordByIdFromPluginExecution(workflowExecutionId, pluginType, idToSearch);
  }

  /**
   * Get a list with record contents from the external resource for a specific list of IDS based on a workflow execution and the
   * predecessor of the given {@link PluginType}.
   *
   * @param workflowExecutionId the execution identifier of the workflow
   * @param pluginType the {@link ExecutablePluginType} that is to be located inside the workflow
   * @param ecloudIds the list of ecloud IDs of the records we wish to obtain
   * @return the list of records from the external resource matching the input ID list. If no record with the matching ID was
   * found in the given workflow step, no entry for this record will appear in the result list.
   * @throws GenericMetisException can be one of:
   * <ul>
   * <li>{@link eu.europeana.metis.exception.ExternalTaskException} if an error occurred while
   * retrieving the records from the external resource</li>
   * <li>{@link eu.europeana.metis.core.exceptions.NoWorkflowExecutionFoundException} if no workflow
   * execution exists for the provided identifier</li>
   * </ul>
   */
  @PostMapping(value = RestEndpoints.ORCHESTRATOR_PROXIES_RECORDS_FROM_PREDECESSOR_PLUGIN,
      consumes = {MediaType.APPLICATION_JSON_VALUE},
      produces = {MediaType.APPLICATION_JSON_VALUE})
  @ResponseStatus(HttpStatus.OK)
  public RecordsResponse getListOfFileContentsFromPredecessorOfPluginExecution(
      @RequestParam("workflowExecutionId") String workflowExecutionId,
      @RequestParam("pluginType") ExecutablePluginType pluginType,
      @RequestBody ListOfIds ecloudIds
  ) throws GenericMetisException {
    return proxiesService.getListOfFileContentsFromPredecessorPluginExecution(workflowExecutionId, pluginType,
        ecloudIds);
  }

}
