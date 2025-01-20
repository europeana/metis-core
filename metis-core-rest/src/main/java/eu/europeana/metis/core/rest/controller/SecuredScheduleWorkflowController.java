package eu.europeana.metis.core.rest.controller;

import static eu.europeana.metis.utils.CommonStringValues.CRLF_PATTERN;
import static eu.europeana.metis.utils.CommonStringValues.sanitizeCRLF;

import eu.europeana.metis.authentication.user.AccountRole;
import eu.europeana.metis.authentication.user.MetisUser;
import eu.europeana.metis.authentication.user.MetisUserView;
import eu.europeana.metis.core.exceptions.NoDatasetFoundException;
import eu.europeana.metis.core.exceptions.NoScheduledWorkflowFoundException;
import eu.europeana.metis.core.exceptions.NoWorkflowFoundException;
import eu.europeana.metis.core.exceptions.ScheduledWorkflowAlreadyExistsException;
import eu.europeana.metis.core.rest.ResponseListWrapper;
import eu.europeana.metis.core.service.ScheduleWorkflowService;
import eu.europeana.metis.core.workflow.ScheduleFrequence;
import eu.europeana.metis.core.workflow.ScheduledWorkflow;
import eu.europeana.metis.exception.BadContentException;
import eu.europeana.metis.exception.GenericMetisException;
import eu.europeana.metis.exception.UserUnauthorizedException;
import eu.europeana.metis.utils.CommonStringValues;
import eu.europeana.metis.utils.RestEndpoints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contains all the calls that are related to scheduling workflows.
 * <p>The {@link ScheduleWorkflowService} has control on how to schedule workflows</p>
 *
 * @author Simon Tzanakis (Simon.Tzanakis@europeana.eu)
 * @since 2018-04-05
 */
@RestController
@RequestMapping("/secured")
public class SecuredScheduleWorkflowController {

  private static final Logger LOGGER = LoggerFactory.getLogger(SecuredScheduleWorkflowController.class);
  private final ScheduleWorkflowService scheduleWorkflowService;
  //TODO: 2025-01-17 - Remove when in-code authorization complete.
  //Temp static user so that the service methods will still work.
  private final MetisUserView metisUserView;

  /**
   * Constructor.
   *
   * @param scheduleWorkflowService the scheduled workflow service
   */
  public SecuredScheduleWorkflowController(ScheduleWorkflowService scheduleWorkflowService) {
    this.scheduleWorkflowService = scheduleWorkflowService;

    MetisUser metisUser = new MetisUser();
    metisUser.setAccountRole(AccountRole.EUROPEANA_DATA_OFFICER);
    metisUser.setOrganizationId("1482250000001617026");
    metisUser.setOrganizationName("Europeana Foundation");
    metisUserView = new MetisUserView(metisUser);
  }

  /**
   * Schedules a provided workflow.
   *
   * @param authorization the authorization header with the access token
   * @param scheduledWorkflow the scheduled workflow information
   * @throws GenericMetisException which can be one of:
   * <ul>
   * <li>{@link NoDatasetFoundException} if the dataset does not exist</li>
   * <li>{@link UserUnauthorizedException} if the user is unauthorized</li>
   * <li>{@link BadContentException} if some content send was not acceptable</li>
   * <li>{@link NoWorkflowFoundException} if the workflow for a dataset was not found</li>
   * <li>{@link ScheduledWorkflowAlreadyExistsException} if a scheduled workflow already exists</li>
   * </ul>
   */
  @PostMapping(value = RestEndpoints.ORCHESTRATOR_WORKFLOWS_SCHEDULE, consumes = {
      MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE}, produces = {
      MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
  @ResponseStatus(HttpStatus.CREATED)
  public void scheduleWorkflowExecution(@RequestHeader("Authorization") String authorization,
      @RequestBody ScheduledWorkflow scheduledWorkflow) throws GenericMetisException {
    scheduleWorkflowService.scheduleWorkflow(metisUserView, scheduledWorkflow);
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info(
          "ScheduledWorkflowExecution for datasetId '{}', pointerDate at '{}', scheduled '{}'",
          CRLF_PATTERN.matcher(scheduledWorkflow.getDatasetId()), scheduledWorkflow.getPointerDate(),
          CRLF_PATTERN.matcher(scheduledWorkflow.getScheduleFrequence().name()).replaceAll(""));
    }
  }

  /**
   * Get a scheduled workflow based on datasets identifier.
   *
   * @param authorization the authorization header with the access token
   * @param datasetId the dataset identifier of which a scheduled workflow is to be retrieved
   * @return the scheduled workflow
   * @throws GenericMetisException which can be one of:
   * <ul>
   * <li>{@link UserUnauthorizedException} if user is unauthorized to access the scheduled
   * workflow</li>
   * <li>{@link NoDatasetFoundException} if dataset identifier does not exist</li>
   * </ul>
   */
  @GetMapping(value = RestEndpoints.ORCHESTRATOR_WORKFLOWS_SCHEDULE_DATASETID, produces = {
      MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
  @ResponseStatus(HttpStatus.OK)
  public ScheduledWorkflow getScheduledWorkflow(
      @RequestHeader("Authorization") String authorization,
      @PathVariable("datasetId") String datasetId) throws GenericMetisException {
    ScheduledWorkflow scheduledWorkflow = scheduleWorkflowService
        .getScheduledWorkflowByDatasetId(metisUserView, datasetId);
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("ScheduledWorkflow with with datasetId '{}' found",
          datasetId.replaceAll(CommonStringValues.REPLACEABLE_CRLF_CHARACTERS_REGEX, ""));
    }
    return scheduledWorkflow;
  }

  /**
   * Get all scheduled workflows.
   *
   * @param authorization the authorization token
   * @param nextPage the next page to retrieve
   * @return the list of scheduled workflows
   * @throws GenericMetisException which can be one of:
   * <ul>
   * <li>{@link UserUnauthorizedException} if user is unauthorized to access the scheduled
   * workflow</li>
   * </ul>
   */
  @GetMapping(value = RestEndpoints.ORCHESTRATOR_WORKFLOWS_SCHEDULE, produces = {
      MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
  @ResponseStatus(HttpStatus.OK)
  public ResponseListWrapper<ScheduledWorkflow> getAllScheduledWorkflows(
      @RequestHeader("Authorization") String authorization,
      @RequestParam(value = "nextPage", required = false, defaultValue = "0") int nextPage)
      throws GenericMetisException {

    if (nextPage < 0) {
      throw new BadContentException(CommonStringValues.NEXT_PAGE_CANNOT_BE_NEGATIVE);
    }
    ResponseListWrapper<ScheduledWorkflow> responseListWrapper = new ResponseListWrapper<>();
    responseListWrapper.setResultsAndLastPage(scheduleWorkflowService
            .getAllScheduledWorkflows(metisUserView, ScheduleFrequence.NULL, nextPage),
        scheduleWorkflowService.getScheduledWorkflowsPerRequest(), nextPage);
    LOGGER.info("Batch of: {} scheduledWorkflows returned, using batch nextPage: {}",
        responseListWrapper.getListSize(), nextPage);
    return responseListWrapper;
  }

  /**
   * Update a scheduled workflow
   * @param authorization the authorization token
   * @param scheduledWorkflow the scheduled workflow
   * @throws GenericMetisException which can be one of:
   * <ul>
   * <li>{@link UserUnauthorizedException} if user is unauthorized to access the scheduled
   * workflow</li>
   * <li>{@link NoDatasetFoundException} if dataset identifier does not exist</li>
   * <li>{@link NoScheduledWorkflowFoundException} if the workflow for a dataset was not found</li>
   * <li>{@link BadContentException} if some content send was not acceptable</li>
   * <li>{@link NoScheduledWorkflowFoundException} if scheduled workflow does not exist</li>
   * </ul>
   */
  @PutMapping(value = RestEndpoints.ORCHESTRATOR_WORKFLOWS_SCHEDULE, produces = {
      MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void updateScheduledWorkflow(@RequestHeader("Authorization") String authorization,
      @RequestBody ScheduledWorkflow scheduledWorkflow) throws GenericMetisException {
    scheduleWorkflowService.updateScheduledWorkflow(metisUserView, scheduledWorkflow);
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("ScheduledWorkflow with with datasetId '{}' updated",
          CRLF_PATTERN.matcher(scheduledWorkflow.getDatasetId()).replaceAll(""));
    }
  }

  /**
   * Delete a scheduled workflow.
   *
   * @param authorization the authorization token
   * @param datasetId the dataset identifier of which a scheduled workflow is to be deleted
   * @throws GenericMetisException which can be one of:
   * <ul>
   * <li>{@link UserUnauthorizedException} if user is unauthorized to access the scheduled
   * workflow</li>
   * <li>{@link NoDatasetFoundException} if dataset identifier does not exist</li>
   * </ul>
   */
  @DeleteMapping(value = RestEndpoints.ORCHESTRATOR_WORKFLOWS_SCHEDULE_DATASETID, produces = {
      MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteScheduledWorkflowExecution(@RequestHeader("Authorization") String authorization,
      @PathVariable("datasetId") String datasetId) throws GenericMetisException {
    authorization = sanitizeCRLF(authorization);
    datasetId = sanitizeCRLF(datasetId);

    scheduleWorkflowService.deleteScheduledWorkflow(metisUserView, datasetId);
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("ScheduledWorkflowExecution for datasetId '{}' deleted",
          datasetId.replaceAll(CommonStringValues.REPLACEABLE_CRLF_CHARACTERS_REGEX, ""));
    }
  }
}
