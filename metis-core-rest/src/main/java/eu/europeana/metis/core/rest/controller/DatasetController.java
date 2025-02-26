package eu.europeana.metis.core.rest.controller;

import static eu.europeana.metis.core.rest.security.AuthenticationUtils.getUserId;
import static eu.europeana.metis.utils.CommonStringValues.CRLF_PATTERN;
import static eu.europeana.metis.utils.CommonStringValues.sanitizeCRLF;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.metis.core.common.CountrySerializer;
import eu.europeana.metis.core.common.Language;
import eu.europeana.metis.core.dataset.Dataset;
import eu.europeana.metis.core.dataset.DatasetSearchView;
import eu.europeana.metis.core.dataset.DatasetXslt;
import eu.europeana.metis.core.dataset.DatasetXsltStringWrapper;
import eu.europeana.metis.core.exceptions.DatasetAlreadyExistsException;
import eu.europeana.metis.core.exceptions.NoDatasetFoundException;
import eu.europeana.metis.core.exceptions.NoXsltFoundException;
import eu.europeana.metis.core.exceptions.XsltSetupException;
import eu.europeana.metis.core.rest.Record;
import eu.europeana.metis.core.rest.ResponseListWrapper;
import eu.europeana.metis.core.service.DatasetService;
import eu.europeana.metis.core.workflow.plugins.ExecutablePluginType;
import eu.europeana.metis.core.workflow.plugins.TransformationPlugin;
import eu.europeana.metis.exception.BadContentException;
import eu.europeana.metis.exception.GenericMetisException;
import eu.europeana.metis.utils.CommonStringValues;
import eu.europeana.metis.utils.Country;
import eu.europeana.metis.utils.RestEndpoints;
import java.util.List;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contains all the calls that are related to Datasets.
 * <p>The {@link DatasetService} has control on how to manipulate a dataset</p>
 */
@RestController
public class DatasetController {

  private static final Logger LOGGER = LoggerFactory.getLogger(DatasetController.class);
  private final DatasetService datasetService;

  /**
   * Autowired constructor with all required parameters.
   *
   * @param datasetService the datasetService
   */
  @Autowired
  public DatasetController(DatasetService datasetService) {
    this.datasetService = datasetService;
  }

  /**
   * Create a provided dataset.
   * <p>Dataset is provided as json or xml.</p>
   *
   * <p> The expected input should follow the rule Bearer
   * accessTokenHere </p>
   *
   * @param jwtPrincipal the jwt principal
   * @param dataset the provided dataset to be created
   * @return the dataset created including all other fields that are auto generated
   * @throws GenericMetisException which can be one of:
   * <ul>
   * <li>{@link DatasetAlreadyExistsException} if the dataset already exists for the organizationId and datasetName.</li>
   * </ul>
   */
  @PostMapping(value = RestEndpoints.DATASETS, consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
  @ResponseStatus(HttpStatus.CREATED)
  public Dataset createDataset(@AuthenticationPrincipal Jwt jwtPrincipal, @RequestBody Dataset dataset) throws GenericMetisException {
    final String userId = getUserId(jwtPrincipal);
    Dataset createdDataset = datasetService.createDataset(userId, dataset);
    LOGGER.info("Dataset with datasetId: {}, datasetName: {} and organizationId {} created",
        createdDataset.getDatasetId(), createdDataset.getDatasetName(),
        createdDataset.getOrganizationId());
    return createdDataset;
  }

  /**
   * Update a provided dataset including an xslt string.
   * <p>
   * Non allowed fields, to be manually updated, will be ignored. Updating a dataset with a new xslt will only overwrite the
   * {@code Dataset#xsltId} and a new {@link DatasetXslt} object will be stored. The older {@link DatasetXslt} will still be
   * accessible.
   * </p>
   *
   * <p> The expected input should follow the rule Bearer
   * accessTokenHere </p>
   *
   * @param datasetXsltStringWrapper {@link DatasetXsltStringWrapper}
   * @throws GenericMetisException which can be one of:
   * <ul>
   * <li>{@link NoDatasetFoundException} if the dataset was not found for the datasetId.</li>
   * <li>{@link DatasetAlreadyExistsException} if a datasetName change is requested and the datasetName for that organizationId already exists.</li>
   * </ul>
   */
  @PutMapping(value = RestEndpoints.DATASETS, consumes = {
      MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void updateDataset(@RequestBody DatasetXsltStringWrapper datasetXsltStringWrapper)
      throws GenericMetisException {
    datasetService.updateDataset(datasetXsltStringWrapper.getDataset(), datasetXsltStringWrapper.getXslt());
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("Dataset with datasetId {} updated",
          CRLF_PATTERN.matcher(datasetXsltStringWrapper.getDataset().getDatasetId()).replaceAll(""));
    }
  }

  /**
   * Delete a dataset using a datasetId.
   *
   * <p> The expected input should follow the rule Bearer
   * accessTokenHere </p>
   *
   * @param datasetId the identifier used to find and delete the dataset
   * @throws GenericMetisException which can be one of:
   * <ul>
   * <li>{@link NoDatasetFoundException} if the dataset was not found for datasetId</li>
   * </ul>
   */
  @DeleteMapping(value = RestEndpoints.DATASETS_DATASETID)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteDataset(@PathVariable("datasetId") String datasetId) throws GenericMetisException {
    datasetId = sanitizeCRLF(datasetId);

    datasetService.deleteDatasetByDatasetId(datasetId);
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("Dataset with datasetId '{}' deleted",
          datasetId.replaceAll(CommonStringValues.REPLACEABLE_CRLF_CHARACTERS_REGEX, ""));
    }
  }

  /**
   * Get a dataset based on its datasetId
   *
   * <p> The expected input should follow the rule Bearer
   * accessTokenHere </p>
   *
   * @param datasetId the identifier used to find a dataset
   * @return {@link Dataset}
   * @throws GenericMetisException which can be one of:
   * <ul>
   * <li>{@link NoDatasetFoundException} if the dataset was not found.</li>
   * </ul>
   */
  @GetMapping(value = RestEndpoints.DATASETS_DATASETID, produces = {
      MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
  @ResponseStatus(HttpStatus.OK)
  public Dataset getByDatasetId(@PathVariable("datasetId") String datasetId)
      throws GenericMetisException {

    Dataset storedDataset = datasetService.getDatasetByDatasetId(datasetId);
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("Dataset with datasetId '{}' found",
          datasetId.replaceAll(CommonStringValues.REPLACEABLE_CRLF_CHARACTERS_REGEX, ""));
    }
    return storedDataset;
  }

  /**
   * Get the xslt object containing the escaped xslt string using a dataset identifier.
   *
   * <p> The expected input should follow the rule Bearer
   * accessTokenHere </p>
   *
   * @param datasetId the identifier used to find a dataset
   * @return the {@link DatasetXslt} object containing the xslt as an escaped string
   * @throws GenericMetisException which can be one of:
   * <ul>
   * <li>{@link NoXsltFoundException} if the xslt was not found.</li>
   * <li>{@link NoDatasetFoundException} if the dataset was not found.</li>
   * </ul>
   */
  @GetMapping(value = RestEndpoints.DATASETS_DATASETID_XSLT, produces = {
      MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
  @ResponseStatus(HttpStatus.OK)
  public DatasetXslt getDatasetXsltByDatasetId(@PathVariable("datasetId") String datasetId) throws GenericMetisException {
    datasetId = sanitizeCRLF(datasetId);

    DatasetXslt datasetXslt = datasetService.getDatasetXsltByDatasetId(datasetId);
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("Dataset XSLT with datasetId '{}' and xsltId: '{}' found", sanitizeCRLF(datasetId), datasetXslt.getId());
    }
    return datasetXslt;
  }

  /**
   * Get the xslt string as non escaped text using an xslt identifier.
   * <p>
   * It is a method that does not require authentication and it is meant to be used from external service to download the
   * corresponding xslt. At the point of writing, ECloud transformation topology is using it. {@link TransformationPlugin}
   * </p>
   *
   * @param xsltId the xslt identifier
   * @return the text non escaped representation of the xslt string
   * @throws GenericMetisException which can be one of:
   * <ul>
   * <li>{@link NoXsltFoundException} if the xslt was not found.</li>
   * </ul>
   */
  @GetMapping(value = RestEndpoints.DATASETS_XSLT_XSLTID, produces = {
      MediaType.TEXT_PLAIN_VALUE})
  @ResponseStatus(HttpStatus.OK)
  public String getXsltByXsltId(@PathVariable("xsltId") String xsltId)
      throws GenericMetisException {
    DatasetXslt datasetXslt = datasetService.getDatasetXsltByXsltId(xsltId);
    LOGGER.info("XSLT with xsltId '{}' found", datasetXslt.getId());
    return datasetXslt.getXslt();
  }

  /**
   * Create a new default xslt in the database.
   * <p>
   * Each dataset can have its own custom xslt but a default xslt should always be available. Creating a new default xslt will
   * create a new {@link DatasetXslt} object and the older one will still be available. The created {@link DatasetXslt} will have
   * it's {@code DatasetXslt#datasetId} as -1 to indicate that it is not related to a specific dataset.
   * </p>
   *
   * <p> The expected input should follow the rule Bearer
   * accessTokenHere </p>
   *
   * @param xsltString the text of the String representation non escaped
   * @return the created {@link DatasetXslt}
   */
  @PostMapping(value = RestEndpoints.DATASETS_XSLT_DEFAULT, consumes = {
      MediaType.TEXT_PLAIN_VALUE}, produces = {
      MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
  @ResponseStatus(HttpStatus.CREATED)
  public DatasetXslt createDefaultXslt(@RequestBody String xsltString) {
    DatasetXslt defaultDatasetXslt = datasetService.createDefaultXslt(xsltString);
    LOGGER.info("New default xslt created with xsltId: {}", defaultDatasetXslt.getId());
    return defaultDatasetXslt;
  }

  /**
   * Get the latest created default xslt.
   * <p>
   * It is an method that does not require authentication and it is meant to be used from external service to download the
   * corresponding xslt. At the point of writing, ECloud transformation topology is using it. {@link TransformationPlugin}
   * </p>
   *
   * @return the text representation of the String xslt non escaped
   * @throws GenericMetisException which can be one of:
   * <ul>
   * <li>{@link NoXsltFoundException} if the xslt was not found.</li>
   * </ul>
   */
  @GetMapping(value = RestEndpoints.DATASETS_XSLT_DEFAULT, produces = {MediaType.TEXT_PLAIN_VALUE})
  @ResponseStatus(HttpStatus.OK)
  public String getLatestDefaultXslt() throws GenericMetisException {
    DatasetXslt datasetXslt = datasetService.getLatestDefaultXslt();
    LOGGER.info("Default XSLT with xsltId '{}' found", datasetXslt.getId());
    return datasetXslt.getXslt();
  }

  /**
   * Transform a list of xmls using the latest dataset xslt stored.
   * <p>
   * This method is meant to be used after a response from
   * {@link ProxiesController#getListOfFileContentsFromPluginExecution(String, String, ExecutablePluginType, String)} to try a
   * transformation on a list of xmls just after validation external to preview an example result.
   * </p>
   *
   * <p> The expected input should follow the rule Bearer
   * accessTokenHere </p>
   *
   * @param datasetId the dataset identifier, it is required for authentication and for the dataset fields xslt injection
   * @param records the list of {@link Record} that contain the xml fields {@code Record#xmlRecord}.
   * @return a list of {@link Record}s with the field {@code Record#xmlRecord} containing the transformed xml
   * @throws GenericMetisException which can be one of:
   * <ul>
   * <li>{@link NoDatasetFoundException} if the dataset was not found.</li>
   * <li>{@link NoXsltFoundException} if there is no xslt found</li>
   * <li>{@link XsltSetupException} if the XSL transform could not be set up</li>
   * </ul>
   */
  @PostMapping(value = RestEndpoints.DATASETS_DATASETID_XSLT_TRANSFORM, consumes = {
      MediaType.APPLICATION_JSON_VALUE}, produces = {MediaType.APPLICATION_JSON_VALUE})
  @ResponseStatus(HttpStatus.OK)
  public List<Record> transformRecordsUsingLatestDatasetXslt(@PathVariable("datasetId") String datasetId,
      @RequestBody List<Record> records) throws GenericMetisException {
    return datasetService.transformRecordsUsingLatestDatasetXslt(datasetId, records);
  }

  /**
   * Transform a list of xmls using the latest default xslt stored.
   * <p>
   * This method is meant to be used after a response from
   * {@link ProxiesController#getListOfFileContentsFromPluginExecution(String, String, ExecutablePluginType, String)} to try a
   * transformation on a list of xmls just after validation external to preview an example result.
   * </p>
   *
   * <p> The expected input should follow the rule Bearer
   * accessTokenHere </p>
   *
   * @param datasetId the dataset identifier, it is required for authentication and for the dataset fields xslt injection
   * @param records the list of {@link Record} that contain the xml fields {@code Record#xmlRecord}.
   * @return a list of {@link Record}s with the field {@code Record#xmlRecord} containing the transformed xml
   * @throws GenericMetisException which can be one of:
   * <ul>
   * <li>{@link NoDatasetFoundException} if the dataset was not found.</li>
   * <li>{@link NoXsltFoundException} if there is no xslt found</li>
   * <li>{@link XsltSetupException} if the XSL transform could not be set up</li>
   * </ul>
   */
  @PostMapping(value = RestEndpoints.DATASETS_DATASETID_XSLT_TRANSFORM_DEFAULT, consumes = {
      MediaType.APPLICATION_JSON_VALUE}, produces = {MediaType.APPLICATION_JSON_VALUE})
  @ResponseStatus(HttpStatus.OK)
  public List<Record> transformRecordsUsingLatestDefaultXslt(@PathVariable("datasetId") String datasetId,
      @RequestBody List<Record> records) throws GenericMetisException {
    return datasetService.transformRecordsUsingLatestDefaultXslt(datasetId, records);
  }

  /**
   * Get a dataset based on its datasetName
   *
   * <p> The expected input should follow the rule Bearer
   * accessTokenHere </p>
   *
   * @param datasetName the name of the dataset used to find a dataset
   * @return {@link Dataset}
   * @throws GenericMetisException which can be one of:
   * <ul>
   * <li>{@link NoDatasetFoundException} if the dataset was not found.</li>
   * </ul>
   */
  @GetMapping(value = RestEndpoints.DATASETS_DATASETNAME, produces = {
      MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
  @ResponseStatus(HttpStatus.OK)
  public Dataset getByDatasetName(@PathVariable("datasetName") String datasetName) throws GenericMetisException {
    Dataset dataset = datasetService.getDatasetByDatasetName(datasetName);
    LOGGER.info("Dataset with datasetName '{}' found", dataset.getDatasetName());
    return dataset;
  }

  /**
   * Get a list of all the datasets using the provider field for lookup.
   * <p>The results are paged and wrapped around {@link ResponseListWrapper}</p>
   *
   * <p> The expected input should follow the rule Bearer
   * accessTokenHere </p>
   *
   * @param provider the provider used to search
   * @param nextPage the nextPage number or -1
   * @return {@link ResponseListWrapper}
   * @throws GenericMetisException which can be one of:
   * <ul>
   * <li>{@link BadContentException} if the parameters provided are invalid.</li>
   * </ul>
   */
  @GetMapping(value = RestEndpoints.DATASETS_PROVIDER, produces = {
      MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
  @ResponseStatus(HttpStatus.OK)
  public ResponseListWrapper<Dataset> getAllDatasetsByProvider(
      @PathVariable("provider") String provider,
      @RequestParam(value = "nextPage", required = false, defaultValue = "0") int nextPage)
      throws GenericMetisException {
    if (nextPage < 0) {
      throw new BadContentException(CommonStringValues.NEXT_PAGE_CANNOT_BE_NEGATIVE);
    }
    ResponseListWrapper<Dataset> responseListWrapper = new ResponseListWrapper<>();
    responseListWrapper
        .setResultsAndLastPage(
            datasetService.getAllDatasetsByProvider(provider, nextPage),
            datasetService.getDatasetsPerRequestLimit(), nextPage);
    LOGGER.info(CommonStringValues.BATCH_OF_DATASETS_RETURNED,
        responseListWrapper.getListSize(), nextPage);
    return responseListWrapper;
  }

  /**
   * Get a list of all the datasets using the intermediateProvider field for lookup.
   * <p>The results are paged and wrapped around {@link ResponseListWrapper}</p>
   *
   * </p>
   *
   * @param intermediateProvider the intermediateProvider used to search
   * @param nextPage the nextPage number or -1
   * @return {@link ResponseListWrapper}
   * @throws GenericMetisException which can be one of:
   * <ul>
   * <li>{@link BadContentException} if the parameters provided are invalid.</li>
   * </ul>
   */
  @GetMapping(value = RestEndpoints.DATASETS_INTERMEDIATE_PROVIDER, produces = {
      MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
  @ResponseStatus(HttpStatus.OK)
  public ResponseListWrapper<Dataset> getAllDatasetsByIntermediateProvider(
      @PathVariable("intermediateProvider") String intermediateProvider,
      @RequestParam(value = "nextPage", required = false, defaultValue = "0") int nextPage)
      throws GenericMetisException {
    if (nextPage < 0) {
      throw new BadContentException(CommonStringValues.NEXT_PAGE_CANNOT_BE_NEGATIVE);
    }
    ResponseListWrapper<Dataset> responseListWrapper = new ResponseListWrapper<>();
    responseListWrapper
        .setResultsAndLastPage(
            datasetService
                .getAllDatasetsByIntermediateProvider(intermediateProvider, nextPage),
            datasetService.getDatasetsPerRequestLimit(), nextPage);
    LOGGER.info(CommonStringValues.BATCH_OF_DATASETS_RETURNED,
        responseListWrapper.getListSize(), nextPage);
    return responseListWrapper;
  }

  /**
   * Get a list of all the datasets using the dataProvider field for lookup.
   * <p>The results are paged and wrapped around {@link ResponseListWrapper}</p>
   *
   * <p> The expected input should follow the rule Bearer
   * accessTokenHere </p>
   *
   * @param dataProvider the dataProvider used to search
   * @param nextPage the nextPage number or -1
   * @return {@link ResponseListWrapper}
   * @throws GenericMetisException which can be one of:
   * <ul>
   * <li>{@link BadContentException} if the parameters provided are invalid.</li>
   * </ul>
   */
  @GetMapping(value = RestEndpoints.DATASETS_DATAPROVIDER, produces = {
      MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
  @ResponseStatus(HttpStatus.OK)
  public ResponseListWrapper<Dataset> getAllDatasetsByDataProvider(
      @PathVariable("dataProvider") String dataProvider,
      @RequestParam(value = "nextPage", required = false, defaultValue = "0") int nextPage)
      throws GenericMetisException {
    if (nextPage < 0) {
      throw new BadContentException(CommonStringValues.NEXT_PAGE_CANNOT_BE_NEGATIVE);
    }
    ResponseListWrapper<Dataset> responseListWrapper = new ResponseListWrapper<>();
    responseListWrapper
        .setResultsAndLastPage(
            datasetService.getAllDatasetsByDataProvider(dataProvider, nextPage),
            datasetService.getDatasetsPerRequestLimit(), nextPage);
    LOGGER.info(CommonStringValues.BATCH_OF_DATASETS_RETURNED,
        responseListWrapper.getListSize(), nextPage);
    return responseListWrapper;
  }

  /**
   * Get a list of all the datasets using the organizationId field for lookup.
   * <p>The results are paged and wrapped around {@link ResponseListWrapper}</p>
   *
   * <p> The expected input should follow the rule Bearer
   * accessTokenHere </p>
   *
   * @param organizationId the organizationId used to search
   * @param nextPage the nextPage number or -1
   * @return {@link ResponseListWrapper}
   * @throws GenericMetisException which can be one of:
   * <ul>
   * <li>{@link BadContentException} if the parameters provided are invalid.</li>
   * </ul>
   */
  @GetMapping(value = RestEndpoints.DATASETS_ORGANIZATION_ID, produces = {
      MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
  @ResponseStatus(HttpStatus.OK)
  public ResponseListWrapper<Dataset> getAllDatasetsByOrganizationId(
      @PathVariable("organizationId") String organizationId,
      @RequestParam(value = "nextPage", required = false, defaultValue = "0") int nextPage)
      throws GenericMetisException {
    if (nextPage < 0) {
      throw new BadContentException(CommonStringValues.NEXT_PAGE_CANNOT_BE_NEGATIVE);
    }
    ResponseListWrapper<Dataset> responseListWrapper = new ResponseListWrapper<>();
    responseListWrapper
        .setResultsAndLastPage(
            datasetService.getAllDatasetsByOrganizationId(organizationId, nextPage),
            datasetService.getDatasetsPerRequestLimit(), nextPage);
    LOGGER.info(CommonStringValues.BATCH_OF_DATASETS_RETURNED,
        responseListWrapper.getListSize(), nextPage);
    return responseListWrapper;
  }

  /**
   * Get a list of all the datasets using the organizationName field for lookup.
   * <p>The results are paged and wrapped around {@link ResponseListWrapper}</p>
   *
   * <p> The expected input should follow the rule Bearer
   * accessTokenHere </p>
   *
   * @param organizationName the organizationName used to search
   * @param nextPage the nextPage number or -1
   * @return {@link ResponseListWrapper}
   * @throws GenericMetisException which can be one of:
   * <ul>
   * <li>{@link BadContentException} if the parameters provided are invalid.</li>
   * </ul>
   */
  @GetMapping(value = RestEndpoints.DATASETS_ORGANIZATION_NAME, produces = {
      MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
  @ResponseStatus(HttpStatus.OK)
  public ResponseListWrapper<Dataset> getAllDatasetsByOrganizationName(
      @PathVariable("organizationName") String organizationName,
      @RequestParam(value = "nextPage", required = false, defaultValue = "0") int nextPage)
      throws GenericMetisException {
    if (nextPage < 0) {
      throw new BadContentException(CommonStringValues.NEXT_PAGE_CANNOT_BE_NEGATIVE);
    }
    ResponseListWrapper<Dataset> responseListWrapper = new ResponseListWrapper<>();
    responseListWrapper
        .setResultsAndLastPage(
            datasetService.getAllDatasetsByOrganizationName(organizationName, nextPage),
            datasetService.getDatasetsPerRequestLimit(), nextPage);
    LOGGER.info(CommonStringValues.BATCH_OF_DATASETS_RETURNED,
        responseListWrapper.getListSize(), nextPage);
    return responseListWrapper;
  }

  /**
   * Get all available countries that can be used.
   * <p>The list is retrieved based on an internal enum</p>
   *
   * <p> The expected input should follow the rule Bearer
   * accessTokenHere </p>
   *
   * @return The list of countries that are serialized based on {@link CountrySerializer}
   */
  @GetMapping(value = RestEndpoints.DATASETS_COUNTRIES, produces = {
      MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
  @ResponseStatus(HttpStatus.OK)
  public List<CountryView> getDatasetsCountries() {
    return Country.getCountryListSortedByName().stream().map(CountryView::new)
                  .toList();
  }

  /**
   * Get all available languages that can be used.
   * <p>The list is retrieved based on an internal enum</p>
   *
   * <p> The expected input should follow the rule Bearer accessTokenHere </p>
   *
   * @return The list of countries that are serialized based on {@link eu.europeana.metis.core.common.LanguageSerializer}
   */
  @GetMapping(value = RestEndpoints.DATASETS_LANGUAGES, produces = {
      MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
  @ResponseStatus(HttpStatus.OK)
  public List<LanguageView> getDatasetsLanguages() {
    return Language.getLanguageListSortedByName().stream().map(LanguageView::new)
                   .toList();
  }

  /**
   * Get the list of matching DatasetSearch using dataset
   *
   * <p> The expected input should follow the rule Bearer accessTokenHere </p>
   *
   * @param searchString a string that may contain multiple words separated by spaces.
   * <p>The search will be performed on the fields datasetId, datasetName, provider, dataProvider.
   * The words that start with a numeric character will be considered as part of the datasetId search and that field is searched
   * as a "starts with" operation. All words that from a certain length threshold and above e.g. 3 will be used, as AND
   * operations, for searching the fields datasetName, provider, dataProvider</p>
   * @param nextPage the nextPage number, must be positive
   * @return a list with the dataset search view results
   * @throws GenericMetisException which can be one of:
   * <ul>
   *   <li>{@link BadContentException} if the parameters provided are invalid.</li>
   * </ul>
   */
  @GetMapping(value = RestEndpoints.DATASETS_SEARCH, produces = {
      MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
  @ResponseStatus(HttpStatus.OK)
  public ResponseListWrapper<DatasetSearchView> getDatasetSearch(
      @RequestParam(value = "searchString") String searchString,
      @RequestParam(value = "nextPage", required = false, defaultValue = "0") int nextPage)
      throws GenericMetisException {
    if (nextPage < 0) {
      throw new BadContentException(CommonStringValues.NEXT_PAGE_CANNOT_BE_NEGATIVE);
    }
    ResponseListWrapper<DatasetSearchView> responseListWrapper = new ResponseListWrapper<>();
    responseListWrapper.setResultsAndLastPage(
        datasetService.searchDatasetsBasedOnSearchString(searchString, nextPage),
        datasetService.getDatasetsPerRequestLimit(), nextPage);
    LOGGER.info(CommonStringValues.BATCH_OF_DATASETS_RETURNED, responseListWrapper.getListSize(),
        nextPage);
    return responseListWrapper;
  }

  private static class CountryView {

    @JsonProperty("enum")
    private final String enumName;
    @JsonProperty
    private final String name;
    @JsonProperty
    private final String isoCode;

    CountryView(Country country) {
      this.enumName = country.name();
      this.name = country.getName();
      this.isoCode = country.getIsoCode();
    }
  }

  private static class LanguageView {

    @JsonProperty("enum")
    private final String enumName;
    @JsonProperty
    private final String name;

    LanguageView(Language language) {
      this.enumName = language.name();
      this.name = language.getName();
    }
  }
}
