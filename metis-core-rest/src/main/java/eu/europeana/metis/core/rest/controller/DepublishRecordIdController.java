package eu.europeana.metis.core.rest.controller;

import static eu.europeana.metis.core.rest.security.AuthenticationUtils.getUserId;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.metis.core.exceptions.NoDatasetFoundException;
import eu.europeana.metis.core.rest.DepublicationInfoView;
import eu.europeana.metis.core.service.DepublishRecordIdService;
import eu.europeana.metis.core.util.DepublishRecordIdSortField;
import eu.europeana.metis.core.util.SortDirection;
import eu.europeana.metis.core.workflow.WorkflowExecution;
import eu.europeana.metis.exception.BadContentException;
import eu.europeana.metis.exception.GenericMetisException;
import eu.europeana.metis.utils.CommonStringValues;
import eu.europeana.metis.utils.DepublicationReason;
import eu.europeana.metis.utils.RestEndpoints;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controller for calls related to depublish record ids.
 */
@RestController
public class DepublishRecordIdController {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final Pattern CRLF_PATTERN = Pattern
      .compile(CommonStringValues.REPLACEABLE_CRLF_CHARACTERS_REGEX);

  private final DepublishRecordIdService depublishRecordIdService;

  /**
   * Autowired constructor with all required parameters.
   *
   * @param depublishRecordIdService the service for depublished records.
   */
  @Autowired
  public DepublishRecordIdController(DepublishRecordIdService depublishRecordIdService) {
    this.depublishRecordIdService = depublishRecordIdService;
  }

  /**
   * Adds a list of record ids to be depublished for the dataset - the version for a simple text body.
   *
   * @param datasetId The dataset ID to which the depublish record ids belong.
   * @param recordIdsInSeparateLines The string containing the record IDs in separate lines.
   * @throws GenericMetisException which can be one of:
   * <ul>
   * <li>{@link NoDatasetFoundException} if the dataset for datasetId was not found.</li>
   * <li>{@link BadContentException} if some content or the operation were invalid</li>
   * </ul>
   */
  @PostMapping(value = RestEndpoints.DEPUBLISH_RECORDIDS_DATASETID, consumes = {
      MediaType.TEXT_PLAIN_VALUE})
  @ResponseStatus(HttpStatus.CREATED)
  public void createRecordIdsToBeDepublished(@PathVariable("datasetId") String datasetId,
      @RequestBody String recordIdsInSeparateLines
  ) throws GenericMetisException {
    final int added = depublishRecordIdService
        .addRecordIdsToBeDepublished(datasetId, recordIdsInSeparateLines);
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("{} Depublish record ids added to dataset with datasetId: {}", added,
          CRLF_PATTERN.matcher(datasetId).replaceAll(""));
    }
  }

  /**
   * Adds a list of record ids to be depublished for the dataset - the version for a multipart file.
   *
   * @param datasetId The dataset ID to which the depublish record ids belong.
   * @param recordIdsFile The file containing the record IDs in separate lines.
   * @throws GenericMetisException which can be one of:
   * <ul>
   * <li>{@link NoDatasetFoundException} if the dataset for datasetId was not found.</li>
   * <li>{@link BadContentException} if some content or the operation were invalid</li>
   * </ul>
   */
  @PostMapping(value = RestEndpoints.DEPUBLISH_RECORDIDS_DATASETID, consumes = {
      MediaType.MULTIPART_FORM_DATA_VALUE})
  @ResponseStatus(HttpStatus.CREATED)
  public void createRecordIdsToBeDepublished(@PathVariable("datasetId") String datasetId,
      @RequestPart("depublicationFile") MultipartFile recordIdsFile) throws GenericMetisException {
    final String fileContent;
    try {
      fileContent = new String(recordIdsFile.getBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new GenericMetisException("Failed to read the request body", e);
    }
    createRecordIdsToBeDepublished(datasetId, fileContent);
  }

  /**
   * Deletes a list of record ids from the database. Only record ids that are in a
   * {@link eu.europeana.metis.core.dataset.DepublishRecordId.DepublicationStatus#PENDING_DEPUBLICATION} state will be removed.
   *
   * @param datasetId The dataset ID to which the depublish record ids belong.
   * @param recordIdsInSeparateLines The string containing the record IDs in separate lines.
   * @throws GenericMetisException which can be one of:
   * <ul>
   * <li>{@link NoDatasetFoundException} if the dataset for datasetId was not found.</li>
   * <li>{@link BadContentException} if some content or the operation were invalid</li>
   * </ul>
   */
  @DeleteMapping(value = RestEndpoints.DEPUBLISH_RECORDIDS_DATASETID, consumes = {
      MediaType.TEXT_PLAIN_VALUE})
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deletePendingRecordIds(@PathVariable("datasetId") String datasetId, @RequestBody String recordIdsInSeparateLines
  ) throws GenericMetisException {
    final Long removedRecordIds = depublishRecordIdService.deletePendingRecordIds(datasetId, recordIdsInSeparateLines);
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("{} Depublish record ids removed from database with datasetId: {}",
          removedRecordIds, CRLF_PATTERN.matcher(datasetId).replaceAll(""));
    }
  }

  /**
   * Retrieve the list of depublish record ids for a specific dataset.
   *
   * @param datasetId The ID of the dataset for which to retrieve the records.
   * @param page The page to retrieve.
   * @param sortField The field on which to sort.
   * @param sortAscending The direction in which to sort.
   * @param searchQuery Search query for the record ID.
   * @return The list of records along with some other information regarding the depublication.
   * @throws GenericMetisException which can be one of:
   * <ul>
   * <li>{@link NoDatasetFoundException} if the dataset for datasetId was not found.</li>
   * </ul>
   */
  @GetMapping(value = RestEndpoints.DEPUBLISH_RECORDIDS_DATASETID, produces = {
      MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
  @ResponseStatus(HttpStatus.OK)
  public DepublicationInfoView getDepublishRecordIds(
      @PathVariable("datasetId") String datasetId,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "sortField", required = false) DepublishRecordIdSortField sortField,
      @RequestParam(value = "sortAscending", defaultValue = "" + true) boolean sortAscending,
      @RequestParam(value = "searchQuery", required = false) String searchQuery
  ) throws GenericMetisException {
    final var recordIds = depublishRecordIdService.getDepublishRecordIds(datasetId, page,
        sortField == null ? DepublishRecordIdSortField.RECORD_ID : sortField,
        sortAscending ? SortDirection.ASCENDING : SortDirection.DESCENDING, searchQuery);
    final var canDepublish = depublishRecordIdService.canTriggerDepublication(datasetId);
    return new DepublicationInfoView(recordIds, canDepublish);
  }

  /**
   * Does checking, prepares and adds a WorkflowExecution with a single Depublish step in the queue. That means it updates the
   * status of the WorkflowExecution to {@link eu.europeana.metis.core.workflow.WorkflowStatus#INQUEUE}, adds it to the database
   * and also it's identifier goes into the distributed queue of WorkflowExecutions.
   *
   * @param jwtPrincipal the jwt principal
   * @param datasetId the dataset identifier for which the execution will take place
   * @param datasetDepublish true for dataset depublication, false for record depublication
   * @param depublicationReason the reason of depublication
   * @param priority the priority of the execution in case the system gets overloaded, 0 lowest, 10 highest
   * @param recordIdsInSeparateLines the specific pending record ids to depublish. Only record ids that are marked as
   * {@link eu.europeana.metis.core.dataset.DepublishRecordId.DepublicationStatus#PENDING_DEPUBLICATION} in the database will be
   * attempted for depublication.
   * @return the WorkflowExecution object that was generated
   * @throws GenericMetisException which can be one of:
   * <ul>
   * <li>{@link BadContentException} if the workflow is empty or no plugin enabled</li>
   * <li>{@link NoDatasetFoundException} if the dataset
   * identifier provided does not exist</li>
   * <li>{@link eu.europeana.metis.exception.ExternalTaskException} if there was an exception when
   * contacting the external resource(ECloud)</li>
   * <li>{@link eu.europeana.metis.core.exceptions.PluginExecutionNotAllowed} if the execution of
   * the first plugin was not allowed, because a valid source plugin could not be found</li>
   * <li>{@link eu.europeana.metis.core.exceptions.WorkflowExecutionAlreadyExistsException} if a
   * workflow execution for the generated execution identifier already exists, almost impossible to
   * happen since ids are UUIDs</li>
   * </ul>
   */
  @PostMapping(value = RestEndpoints.DEPUBLISH_EXECUTE_DATASETID, produces = {
      MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
  @ResponseStatus(HttpStatus.CREATED)
  public WorkflowExecution addDepublishWorkflowInQueueOfWorkflowExecutions(
      @AuthenticationPrincipal Jwt jwtPrincipal,
      @PathVariable("datasetId") String datasetId,
      @RequestParam(value = "datasetDepublish", defaultValue = "" + true) boolean datasetDepublish,
      @RequestParam(value = "depublicationReason") DepublicationReason depublicationReason,
      @RequestParam(value = "priority", defaultValue = "0") int priority,
      @RequestBody(required = false) String recordIdsInSeparateLines)
      throws GenericMetisException {
    final String userId = getUserId(jwtPrincipal);
    return depublishRecordIdService
        .createAndAddInQueueDepublishWorkflowExecution(datasetId,
            datasetDepublish, priority, recordIdsInSeparateLines, depublicationReason, userId);
  }

  /**
   * API to return all possible values of depublication reasons
   *
   * @return All possible values of depublication reasons
   */
  @GetMapping(value = RestEndpoints.DEPUBLISH_REASONS, produces = {
      MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
  @ResponseStatus(HttpStatus.OK)
  public List<DepublicationReasonView> getAllDepublicationReasons() {
    return Arrays.stream(DepublicationReason.values()).filter(value -> value != DepublicationReason.LEGACY)
                 .map(DepublicationReasonView::new).toList();
  }

  /**
   * The view class for a depublication reason
   */
  public static class DepublicationReasonView {

    @JsonProperty("name")
    private final String name;
    @JsonProperty("valueAsString")
    private final String valueAsString;

    /**
     * Instantiates a new DepublicationReason view.
     *
     * @param depublicationReason the depublication reason
     */
    DepublicationReasonView(DepublicationReason depublicationReason) {
      this.name = depublicationReason.name();
      this.valueAsString = depublicationReason.toString();
    }
  }
}
