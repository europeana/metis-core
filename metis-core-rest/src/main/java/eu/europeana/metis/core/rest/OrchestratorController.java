package eu.europeana.metis.core.rest;

import eu.europeana.metis.RestEndpoints;
import eu.europeana.metis.core.exceptions.BadContentException;
import eu.europeana.metis.core.exceptions.NoDatasetFoundException;
import eu.europeana.metis.core.exceptions.NoScheduledWorkflowFoundException;
import eu.europeana.metis.core.exceptions.NoWorkflowExecutionFoundException;
import eu.europeana.metis.core.exceptions.NoWorkflowFoundException;
import eu.europeana.metis.core.exceptions.ScheduledWorkflowAlreadyExistsException;
import eu.europeana.metis.core.exceptions.WorkflowAlreadyExistsException;
import eu.europeana.metis.core.exceptions.WorkflowExecutionAlreadyExistsException;
import eu.europeana.metis.core.service.OrchestratorService;
import eu.europeana.metis.core.workflow.ScheduleFrequence;
import eu.europeana.metis.core.workflow.ScheduledWorkflow;
import eu.europeana.metis.core.workflow.Workflow;
import eu.europeana.metis.core.workflow.WorkflowExecution;
import eu.europeana.metis.core.workflow.WorkflowStatus;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Simon Tzanakis (Simon.Tzanakis@europeana.eu)
 * @since 2017-05-28
 */
@Controller
public class OrchestratorController {

  private static final Logger LOGGER = LoggerFactory.getLogger(OrchestratorController.class);
  private final OrchestratorService orchestratorService;

  @Autowired
  public OrchestratorController(OrchestratorService orchestratorService) {
    this.orchestratorService = orchestratorService;
  }

  //USER WORKFLOWS
  @RequestMapping(value = RestEndpoints.ORCHESTRATOR_USERWORKFLOWS, method = RequestMethod.POST, consumes = {
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML}, produces = {
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  public void createUserWorkflow(
      @RequestBody Workflow workflow)
      throws WorkflowAlreadyExistsException {
    orchestratorService.createUserWorkflow(workflow);
  }

  @RequestMapping(value = RestEndpoints.ORCHESTRATOR_USERWORKFLOWS, method = RequestMethod.PUT, produces = {
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @ResponseBody
  public void updateUserWorkflow(
      @RequestBody Workflow workflow) throws NoWorkflowFoundException {
    orchestratorService.updateUserWorkflow(workflow);
  }

  @RequestMapping(value = RestEndpoints.ORCHESTRATOR_USERWORKFLOWS, method = RequestMethod.DELETE)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteUserWorkflow(@QueryParam("workflowOwner") String workflowOwner,
      @QueryParam("workflowName") String workflowName) {
    orchestratorService.deleteUserWorkflow(workflowOwner, workflowName);
    LOGGER.info("Workflow with workflowOwner '{}' and workflowName '{}' deleted", workflowOwner,
        workflowName);
  }

  @RequestMapping(value = RestEndpoints.ORCHESTRATOR_USERWORKFLOWS, method = RequestMethod.GET, produces = {
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public Workflow getUserWorkflow(@QueryParam("workflowOwner") String workflowOwner,
      @QueryParam("workflowName") String workflowName) {
    Workflow workflow = orchestratorService
        .getUserWorkflow(workflowOwner, workflowName);
    LOGGER.info(
        "Workflow with workflowOwner '{}' and workflowName '{}' found", workflowOwner,
        workflowName);
    return workflow;
  }

  @RequestMapping(value = RestEndpoints.ORCHESTRATOR_USERWORKFLOWS_OWNER, method = RequestMethod.GET, produces = {
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public ResponseListWrapper<Workflow> getAllUserWorkflows(
      @PathVariable("workflowOwner") String workflowOwner,
      @QueryParam("nextPage") String nextPage) {
    ResponseListWrapper<Workflow> responseListWrapper = new ResponseListWrapper<>();
    responseListWrapper.setResultsAndLastPage(orchestratorService
            .getAllUserWorkflows(workflowOwner, nextPage),
        orchestratorService.getUserWorkflowsPerRequest());
    LOGGER.info("Batch of: {} userWorkflows returned, using batch nextPage: {}",
        responseListWrapper.getListSize(), nextPage);
    return responseListWrapper;
  }


  //USER WORKFLOW EXECUTIONS
  @RequestMapping(value = RestEndpoints.ORCHESTRATOR_USERWORKFLOWS_DATASETNAME_EXECUTE, method = RequestMethod.POST, produces = {
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  public void addUserWorkflowInQueueOfUserWorkflowExecutions(
      @PathVariable("datasetName") String datasetName,
      @QueryParam("workflowOwner") String workflowOwner,
      @QueryParam("workflowName") String workflowName, @QueryParam("priority") Integer priority)
      throws WorkflowExecutionAlreadyExistsException, NoDatasetFoundException, NoWorkflowFoundException {
    if (priority == null) {
      priority = 0;
    }
    orchestratorService
        .addUserWorkflowInQueueOfUserWorkflowExecutions(datasetName, workflowOwner,
            workflowName, priority);
    LOGGER.info(
        "WorkflowExecution for datasetName '{}' with workflowOwner '{}' and workflowName '{}' added to queue",
        datasetName, workflowOwner, workflowName);
  }

  @RequestMapping(value = RestEndpoints.ORCHESTRATOR_USERWORKFLOWS_DATASETNAME_EXECUTE_DIRECT, method = RequestMethod.POST, produces = {
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  public void addUserWorkflowInQueueOfUserWorkflowExecutions(
      @PathVariable("datasetName") String datasetName, @RequestBody Workflow workflow,
      @QueryParam("priority") Integer priority)
      throws WorkflowExecutionAlreadyExistsException, NoDatasetFoundException, WorkflowAlreadyExistsException {
    if (priority == null) {
      priority = 0;
    }
    orchestratorService
        .addUserWorkflowInQueueOfUserWorkflowExecutions(datasetName, workflow, priority);
    LOGGER.info(
        "WorkflowExecution for datasetName '{}' with workflowOwner '{}' started", datasetName,
        workflow.getWorkflowOwner());
  }

  @RequestMapping(value = RestEndpoints.ORCHESTRATOR_USERWORKFLOWS_EXECUTION_DATASETNAME, method = RequestMethod.DELETE, produces = {
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @ResponseBody
  public void cancelUserWorkflowExecution(
      @PathVariable("datasetName") String datasetName)
      throws NoWorkflowExecutionFoundException {
    orchestratorService.cancelUserWorkflowExecution(datasetName);
    LOGGER.info(
        "WorkflowExecution for datasetName '{}' is cancelling",
        datasetName);
  }

  @RequestMapping(value = RestEndpoints.ORCHESTRATOR_USERWORKFLOWS_EXECUTION_DATASETNAME, method = RequestMethod.GET, produces = {
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public WorkflowExecution getRunningUserWorkflowExecution(
      @PathVariable("datasetName") String datasetName) {
    WorkflowExecution workflowExecution = orchestratorService
        .getRunningUserWorkflowExecution(datasetName);
    LOGGER.info("WorkflowExecution with datasetName '{}' found", datasetName);
    return workflowExecution;
  }

  @RequestMapping(value = RestEndpoints.ORCHESTRATOR_USERWORKFLOWS_EXECUTIONS_DATASETNAME, method = RequestMethod.GET, produces = {
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public ResponseListWrapper<WorkflowExecution> getAllUserWorkflowExecutions(
      @PathVariable("datasetName") String datasetName,
      @QueryParam("workflowOwner") String workflowOwner,
      @QueryParam("workflowName") String workflowName,
      @QueryParam("workflowStatus") WorkflowStatus workflowStatus,
      @QueryParam("nextPage") String nextPage) {
    ResponseListWrapper<WorkflowExecution> responseListWrapper = new ResponseListWrapper<>();
    responseListWrapper.setResultsAndLastPage(orchestratorService
            .getAllUserWorkflowExecutions(datasetName, workflowOwner, workflowName, workflowStatus,
                nextPage),
        orchestratorService.getUserWorkflowExecutionsPerRequest());
    LOGGER.info("Batch of: {} userWorkflowExecutions returned, using batch nextPage: {}",
        responseListWrapper.getListSize(), nextPage);
    return responseListWrapper;
  }

  @RequestMapping(value = RestEndpoints.ORCHESTRATOR_USERWORKFLOWS_EXECUTIONS, method = RequestMethod.GET, produces = {
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public ResponseListWrapper<WorkflowExecution> getAllUserWorkflowExecutions(
      @QueryParam("workflowStatus") WorkflowStatus workflowStatus,
      @QueryParam("nextPage") String nextPage) {
    ResponseListWrapper<WorkflowExecution> responseListWrapper = new ResponseListWrapper<>();
    responseListWrapper.setResultsAndLastPage(orchestratorService
            .getAllUserWorkflowExecutions(workflowStatus, nextPage),
        orchestratorService.getUserWorkflowExecutionsPerRequest());
    LOGGER.info("Batch of: {} userWorkflowExecutions returned, using batch nextPage: {}",
        responseListWrapper.getListSize(), nextPage);
    return responseListWrapper;
  }

  //SCHEDULED USER WORKFLOWS
  @RequestMapping(value = RestEndpoints.ORCHESTRATOR_USERWORKFLOWS_SCHEDULE, method = RequestMethod.POST, consumes = {
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML}, produces = {
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  public void scheduleUserWorkflowExecution(
      @RequestBody ScheduledWorkflow scheduledWorkflow)
      throws BadContentException, ScheduledWorkflowAlreadyExistsException, NoWorkflowFoundException, NoDatasetFoundException {
    orchestratorService.scheduleUserWorkflow(scheduledWorkflow);
    LOGGER.info(
        "ScheduledUserWorkflowExecution for datasetName '{}', workflowOwner '{}', workflowName '{}', pointerDate at '{}', scheduled '{}'",
        scheduledWorkflow.getDatasetName(),
        scheduledWorkflow.getWorkflowOwner(), scheduledWorkflow.getWorkflowName(),
        scheduledWorkflow.getPointerDate(),
        scheduledWorkflow.getScheduleFrequence().name());
  }

  @RequestMapping(value = RestEndpoints.ORCHESTRATOR_USERWORKFLOWS_SCHEDULE_DATASETNAME, method = RequestMethod.GET, produces = {
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public ScheduledWorkflow getScheduledUserWorkflow(
      @PathVariable("datasetName") String datasetName) {
    ScheduledWorkflow scheduledWorkflow = orchestratorService
        .getScheduledUserWorkflowByDatasetName(datasetName);
    LOGGER.info("ScheduledWorkflow with with datasetName '{}' found", datasetName);
    return scheduledWorkflow;
  }

  @RequestMapping(value = RestEndpoints.ORCHESTRATOR_USERWORKFLOWS_SCHEDULE, method = RequestMethod.GET, produces = {
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public ResponseListWrapper<ScheduledWorkflow> getAllScheduledUserWorkflows(
      @QueryParam("nextPage") String nextPage) {
    ResponseListWrapper<ScheduledWorkflow> responseListWrapper = new ResponseListWrapper<>();
    responseListWrapper.setResultsAndLastPage(orchestratorService
            .getAllScheduledUserWorkflows(ScheduleFrequence.NULL, nextPage),
        orchestratorService.getScheduledUserWorkflowsPerRequest());
    LOGGER.info("Batch of: {} scheduledUserWorkflows returned, using batch nextPage: {}",
        responseListWrapper.getListSize(), nextPage);
    return responseListWrapper;
  }

  @RequestMapping(value = RestEndpoints.ORCHESTRATOR_USERWORKFLOWS_SCHEDULE, method = RequestMethod.PUT, produces = {
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @ResponseBody
  public void updateScheduledUserWorkflow(
      @RequestBody ScheduledWorkflow scheduledWorkflow)
      throws BadContentException, NoScheduledWorkflowFoundException, NoWorkflowFoundException {
    orchestratorService.updateScheduledUserWorkflow(scheduledWorkflow);
    LOGGER.info("ScheduledWorkflow with with datasetName '{}' updated",
        scheduledWorkflow.getDatasetName());
  }

  @RequestMapping(value = RestEndpoints.ORCHESTRATOR_USERWORKFLOWS_SCHEDULE_DATASETNAME, method = RequestMethod.DELETE, produces = {
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @ResponseBody
  public void deleteScheduledUserWorkflowExecution(
      @PathVariable("datasetName") String datasetName) {
    orchestratorService.deleteScheduledUserWorkflow(datasetName);
    LOGGER.info(
        "ScheduledUserWorkflowExecution for datasetName '{}' deleted",
        datasetName);
  }
}
