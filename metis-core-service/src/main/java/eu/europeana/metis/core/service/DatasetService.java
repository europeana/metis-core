package eu.europeana.metis.core.service;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static java.util.function.Predicate.not;

import eu.europeana.metis.core.common.TransformationParameters;
import eu.europeana.metis.core.dao.DatasetDao;
import eu.europeana.metis.core.dao.DatasetXsltDao;
import eu.europeana.metis.core.dao.PluginWithExecutionId;
import eu.europeana.metis.core.dao.WorkflowDao;
import eu.europeana.metis.core.dao.WorkflowExecutionDao;
import eu.europeana.metis.core.dataset.Dataset;
import eu.europeana.metis.core.dataset.DatasetConverter;
import eu.europeana.metis.core.dataset.DatasetDTO;
import eu.europeana.metis.core.dataset.DatasetSearchView;
import eu.europeana.metis.core.dataset.DatasetXslt;
import eu.europeana.metis.core.dataset.DatasetXslt.XsltType;
import eu.europeana.metis.core.exceptions.DatasetAlreadyExistsException;
import eu.europeana.metis.core.exceptions.NoDatasetFoundException;
import eu.europeana.metis.core.exceptions.NoXsltFoundException;
import eu.europeana.metis.core.exceptions.XsltSetupException;
import eu.europeana.metis.core.rest.ListOfIds;
import eu.europeana.metis.core.rest.Record;
import eu.europeana.metis.core.workflow.WorkflowExecution;
import eu.europeana.metis.core.workflow.plugins.ExecutablePlugin;
import eu.europeana.metis.core.workflow.plugins.ExecutablePluginType;
import eu.europeana.metis.core.workflow.plugins.TransformationPlugin;
import eu.europeana.metis.exception.BadContentException;
import eu.europeana.metis.exception.GenericMetisException;
import eu.europeana.metis.transformation.service.EuropeanaGeneratedIdsMap;
import eu.europeana.metis.transformation.service.EuropeanaIdCreator;
import eu.europeana.metis.transformation.service.EuropeanaIdException;
import eu.europeana.metis.transformation.service.TransformationException;
import eu.europeana.metis.transformation.service.XsltTransformer;
import eu.europeana.metis.utils.CommonStringValues;
import eu.europeana.metis.utils.RestEndpoints;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Contains business logic of how to manipulate datasets in the system using several components. The functionality in this class
 * is checked for user authentication.
 */
@Service
public class DatasetService {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String DATASET_CREATION_LOCK = "datasetCreationLock";
  private static final int MINIMUM_WORD_LENGTH = 3;

  private final DatasetDao datasetDao;
  private final DatasetXsltDao datasetXsltDao;
  private final WorkflowDao workflowDao;
  private final WorkflowExecutionDao workflowExecutionDao;
  private final RedissonClient redissonClient;
  private final UserService userService;
  private String metisCoreUrl; //Initialize with setter


  /**
   * Constructs the service.
   *
   * @param datasetDao the Dao instance to access the Dataset database
   * @param datasetXsltDao the Dao instance to access the DatasetXslt database
   * @param workflowDao the Dao instance to access the Workflow database
   * @param workflowExecutionDao the Dao instance to access the WorkflowExecution database
   * @param redissonClient the redisson client used for distributed locks
   * @param userService the user service
   */
  @Autowired
  public DatasetService(DatasetDao datasetDao, DatasetXsltDao datasetXsltDao,
      WorkflowDao workflowDao, WorkflowExecutionDao workflowExecutionDao, RedissonClient redissonClient, UserService userService) {
    this.datasetDao = datasetDao;
    this.datasetXsltDao = datasetXsltDao;
    this.workflowDao = workflowDao;
    this.workflowExecutionDao = workflowExecutionDao;
    this.redissonClient = redissonClient;
    this.userService = userService;
  }

  /**
   * Creates a dataset.
   *
   * @param userId the userId of the user
   * @param datasetDTO the dataset to be created
   * @return the created {@link DatasetDTO} including the extra fields generated from the system
   * @throws GenericMetisException which can be one of:
   * <ul>
   * <li>{@link DatasetAlreadyExistsException} if the dataset for the datasetName already exists in the system.</li>
   * <li>{@link BadContentException} if some contents were invalid</li>
   * </ul>
   */
  public DatasetDTO createDataset(String userId, DatasetDTO datasetDTO) throws GenericMetisException {
    //Lock required for find in the next empty datasetId
    RLock lock = redissonClient.getFairLock(DATASET_CREATION_LOCK);
    lock.lock();

    Dataset createdDataset;
    try {
      Dataset storedDataset = datasetDao.getDatasetByDatasetName(datasetDTO.getDatasetName());
      if (storedDataset != null) {
        lock.unlock();
        throw new DatasetAlreadyExistsException(
            format("Dataset with datasetName: %s already exists..", datasetDTO.getDatasetName()));
      }

      datasetDTO.setCreatedByUserId(userId);
      datasetDTO.setId(null);
      datasetDTO.setUpdatedDate(null);
      datasetDTO.setCreatedDate(new Date());
      //Add fake ecloudDatasetId to avoid null errors in the database
      datasetDTO.setEcloudDatasetId(format("NOT_CREATED_YET-%s", UUID.randomUUID()));

      int nextInSequenceDatasetId = datasetDao.findNextInSequenceDatasetId();
      datasetDTO.setDatasetId(Integer.toString(nextInSequenceDatasetId));
      verifyReferencesToOldDatasetIds(datasetDTO);
      createdDataset = datasetDao.create(DatasetConverter.fromDTO(datasetDTO));
    } finally {
      lock.unlock();
    }
    return DatasetConverter.toDTO(createdDataset, userService.getUserFromCache(userId));
  }

  /**
   * Update an already existent dataset.
   *
   * @param datasetDTO the provided dataset with the changes and the datasetId included in the {@link Dataset}
   * @param xsltString the text of the String representation
   * @param xsltExternal
   * @throws GenericMetisException which can be one of:
   * <ul>
   * <li>{@link NoDatasetFoundException} if the dataset for datasetId was not found.</li>
   * <li>{@link BadContentException} if the dataset has an execution running, contents are invalid.</li>
   * <li>{@link DatasetAlreadyExistsException} if the request contains a datasetName change and that datasetName already exists.</li>
   * </ul>
   */
  public void updateDataset(DatasetDTO datasetDTO, String xsltString, String xsltExternal)
      throws GenericMetisException {

    // Find existing dataset and check authentication.
    final Dataset storedDataset = datasetDao.getDatasetOrThrow(datasetDTO.getDatasetId());

    // Check that the new dataset name does not already exist.
    final String newDatasetName = datasetDTO.getDatasetName();
    if (!storedDataset.getDatasetName().equals(newDatasetName)
        && datasetDao.getDatasetByDatasetName(newDatasetName) != null) {
      throw new DatasetAlreadyExistsException(format(
          "Trying to change dataset with datasetName: %s but dataset with datasetName: %s already exists",
          storedDataset.getDatasetName(), newDatasetName));
    }

    // Check that there is no workflow execution pending for the given dataset.
    if (workflowExecutionDao.existsAndNotCompleted(datasetDTO.getDatasetId()) != null) {
      throw new BadContentException(format("Workflow execution is active for datasetId %s", datasetDTO.getDatasetId()));
    }

    // Set/overwrite dataset properties that the user may not determine.
    datasetDTO.setCreatedByUserId(storedDataset.getCreatedByUserId());
    datasetDTO.setEcloudDatasetId(storedDataset.getEcloudDatasetId());
    datasetDTO.setCreatedDate(storedDataset.getCreatedDate());
    datasetDTO.setCreatedByUserId(storedDataset.getCreatedByUserId());
    datasetDTO.setId(storedDataset.getId().toString());

    verifyReferencesToOldDatasetIds(datasetDTO);

    if (xsltString == null) {
      datasetDTO.setXsltId(ofNullable(storedDataset.getXsltId()).map(ObjectId::toString).orElse(null));
    } else {
      cleanDatasetXslt(storedDataset.getXsltId());
      ObjectId xsltId = datasetXsltDao.create(new DatasetXslt(datasetDTO.getDatasetId(), XsltType.INTERNAL, xsltString)).getId();
      datasetDTO.setXsltId(xsltId.toString());
    }

    if (xsltExternal == null) {
      datasetDTO.setXsltIdExternal(ofNullable(storedDataset.getXsltIdExternal()).map(ObjectId::toString).orElse(null));
    } else {
      cleanDatasetXslt(storedDataset.getXsltIdExternal());
      ObjectId xsltId = datasetXsltDao.create(new DatasetXslt(datasetDTO.getDatasetId(), XsltType.EXTERNAL, xsltExternal)).getId();
      datasetDTO.setXsltIdExternal(xsltId.toString());
    }

    // Update the dataset
    datasetDTO.setUpdatedDate(new Date());
    datasetDao.update(DatasetConverter.fromDTO(datasetDTO));
  }

  private void verifyReferencesToOldDatasetIds(DatasetDTO datasetDTO) throws BadContentException {
    if (datasetDTO.getDatasetIdsToRedirectFrom() != null) {
      for (String datasetId : datasetDTO.getDatasetIdsToRedirectFrom()) {
        if (datasetDao.getDatasetByDatasetId(datasetId) == null) {
          throw new BadContentException(
              format("Old datasetId for redirection %s doesn't exist", datasetId));
        }
        if (datasetDTO.getDatasetId().equals(datasetId)) {
          throw new BadContentException(
              format("datasetId for redirection %s cannot be the same as the current datasetId", datasetId));
        }
      }
    }
  }

  private void cleanDatasetXslt(ObjectId xsltId) {
    if (xsltId != null) {
      //Check if it's referenced
      final WorkflowExecution workflowExecution = workflowExecutionDao
          .getAnyByXsltId(xsltId.toString());
      if (workflowExecution == null) {
        final DatasetXslt datasetXslt = datasetXsltDao
            .getById(xsltId.toString());
        if (datasetXslt != null) {
          datasetXsltDao.delete(datasetXslt);
        }
      }
    }
  }

  /**
   * Delete a dataset from the system
   *
   * @param datasetId the identifier to find the dataset with
   * @throws GenericMetisException which can be one of:
   * <ul>
   * <li>{@link BadContentException} if the dataset has an execution running.</li>
   * <li>{@link NoDatasetFoundException} if the dataset was not found.</li>
   * </ul>
   */
  public void deleteDatasetByDatasetId(String datasetId)
      throws GenericMetisException {
    datasetDao.getDatasetOrThrow(datasetId);

    // Check that there is no workflow execution pending for the given dataset.
    if (workflowExecutionDao.existsAndNotCompleted(datasetId) != null) {
      throw new BadContentException(
          format("Workflow execution is active for datasteId %s", datasetId));
    }

    //Are there datasets that have a reference to the datasetId that is to be removed
    final List<Dataset> datasetsThatHaveAReference = datasetDao.getAllDatasetsByDatasetIdsToRedirectFrom(datasetId);
    //Clear references of the datasetId
    datasetsThatHaveAReference.forEach(ds -> {
      final List<String> datasetIdsToRedirectFrom = ds.getDatasetIdsToRedirectFrom();
      ds.setDatasetIdsToRedirectFrom(datasetIdsToRedirectFrom.stream().filter(not(id -> id.equals(datasetId))).toList());
      datasetDao.update(ds);
    });

    // Delete the dataset.
    datasetDao.deleteByDatasetId(datasetId);

    // Clean up dataset leftovers
    datasetXsltDao.deleteAllByDatasetId(datasetId);
    workflowDao.deleteWorkflow(datasetId);
    workflowExecutionDao.deleteAllByDatasetId(datasetId);
  }

  /**
   * Get a dataset from the system using a datasetName
   *
   * @param datasetName the string used to find the dataset with
   * @return {@link Dataset}
   * @throws GenericMetisException which can be one of:
   * <ul>
   * <li>{@link NoDatasetFoundException} if the dataset is not found in the system.</li>
   * </ul>
   */
  public DatasetDTO getDatasetByDatasetName(String datasetName)
      throws GenericMetisException {
    final Dataset dataset = datasetDao.getDatasetByDatasetName(datasetName);
    if (dataset == null) {
      throw new NoDatasetFoundException(
          format("No dataset found with datasetName: '%s' in METIS", datasetName));
    }
    return DatasetConverter.toDTO(dataset, userService.getUserFromCache(dataset.getCreatedByUserId()));
  }

  /**
   * Get a dataset from the system using a datasetId.
   *
   * @param datasetId the identifier to find the dataset with
   * @return {@link Dataset}
   * @throws GenericMetisException which can be one of:
   * <ul>
   * <li>{@link NoDatasetFoundException} if the dataset was not found.</li>
   * </ul>
   */
  public DatasetDTO getDatasetByDatasetId(String datasetId)
      throws GenericMetisException {
    Dataset storedDataset = datasetDao.getDatasetOrThrow(datasetId);
    return DatasetConverter.toDTO(storedDataset, userService.getUserFromCache(storedDataset.getCreatedByUserId()));
  }

  /**
   * Get the xslt object containing the escaped xslt string using a dataset identifier.
   *
   * @param datasetId the identifier to find the xslt with
   * @return the {@link DatasetXslt} object containing the xslt as an escaped string
   * @throws GenericMetisException which can be one of:
   * <ul>
   * <li>{@link NoXsltFoundException} if the xslt was not found.</li>
   * <li>{@link NoDatasetFoundException} if the dataset was not found.</li>
   * </ul>
   */
  public DatasetXslt getDatasetXsltByDatasetId(String datasetId) throws GenericMetisException {
    final Dataset dataset = datasetDao.getDatasetOrThrow(datasetId);
    DatasetXslt datasetXslt = datasetXsltDao.getById(dataset.getXsltId() == null ? null : dataset.getXsltId().toString());
    if (datasetXslt == null) {
      throw new NoXsltFoundException(format(
          "No datasetXslt found for dataset with datasetId: '%s' and xsltId: '%s' in METIS",
          datasetId, dataset.getXsltId()));
    }
    return datasetXslt;
  }

  /**
   * Get the xslt object containing the escaped xslt string using an xslt identifier.
   * <p>
   * It is a method that does not require authentication.
   * </p>
   *
   * @param xsltId the identifier to find the xslt with
   * @return the {@link DatasetXslt} object containing the xslt as an escaped string
   * @throws GenericMetisException which can be one of:
   * <ul>
   * <li>{@link NoXsltFoundException} if the xslt was not found.</li>
   * </ul>
   */
  public DatasetXslt getDatasetXsltByXsltId(String xsltId) throws GenericMetisException {
    DatasetXslt datasetXslt = datasetXsltDao.getById(xsltId);
    if (datasetXslt == null) {
      throw new NoXsltFoundException(format("No datasetXslt found with xsltId: '%s' in METIS", xsltId));
    }
    return datasetXslt;
  }

  /**
   * Create a new default xslt in the database.
   * <p>
   * Each dataset can have it's own custom xslt but a default xslt should always be available. Creating a new default xslt will
   * create a new {@link DatasetXslt} object and the older one will still be available. The created {@link DatasetXslt} will have
   * {@link DatasetXslt#getDatasetId()} equal to -1 to indicate that it is not related to a specific dataset.
   * </p>
   *
   * @param xsltString the text of the String representation non escaped
   * @return the created {@link DatasetXslt}
   */
  public DatasetXslt createDefaultXslt(String xsltString) {
    DatasetXslt datasetXslt = null;
    if (xsltString != null) {
      final DatasetXslt latestDefaultXslt = datasetXsltDao.getLatestDefaultXslt();
      if (latestDefaultXslt != null) {
        cleanDatasetXslt(latestDefaultXslt.getId());
      }
      datasetXslt = datasetXsltDao.create(new DatasetXslt(xsltString));
    }
    return datasetXslt;
  }

  /**
   * Get the latest default xslt.
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
  public DatasetXslt getLatestDefaultXslt() throws GenericMetisException {
    DatasetXslt datasetXslt = datasetXsltDao.getLatestDefaultXslt();
    if (datasetXslt == null) {
      throw new NoXsltFoundException("No default datasetXslt found");
    }
    return datasetXslt;
  }

  /**
   * Transform a list of records using the latest default xslt stored.
   * <p>
   * This method can be used, for example, after a response from
   * {@link ProxiesService#getListOfFileContentsFromPluginExecution(String, ExecutablePluginType, ListOfIds)} to try a
   * transformation on a list of records just after validation external to preview an example result.
   * </p>
   *
   * @param datasetId the dataset identifier, it is required for authentication and for the dataset fields xslt injection
   * @param records the list of {@link Record} for which {@link Record#getXmlRecord()} returns a non-null value
   * @return a list of {@link Record}s with {@link Record#getXmlRecord()} returning the transformed XML
   * @throws GenericMetisException which can be one of:
   * <ul>
   * <li>{@link NoDatasetFoundException} if the dataset was not found.</li>
   * <li>{@link NoXsltFoundException} if there is no xslt found</li>
   * <li>{@link XsltSetupException} if the XSL transform could not be set up</li>
   * </ul>
   */
  public List<Record> transformRecordsUsingLatestDefaultXslt(String datasetId,
      List<Record> records) throws GenericMetisException {
    final Dataset dataset = datasetDao.getDatasetOrThrow(datasetId);
    //Using default dataset identifier
    DatasetXslt datasetXslt = datasetXsltDao.getLatestDefaultXslt();
    if (datasetXslt == null) {
      throw new NoXsltFoundException("Could not find default xslt");
    }
    String xsltUrl;
    synchronized (this) {
      xsltUrl = metisCoreUrl +
          RestEndpoints.resolve(RestEndpoints.DATASETS_XSLT_XSLTID, Collections.singletonList(datasetXslt.getId().toString()));
    }
    return transformRecords(dataset, records, xsltUrl);
  }

  /**
   * Transform a list of records using the latest dataset xslt stored.
   * <p>
   * This method can be used, for example, after a response from
   * {@link ProxiesService#getListOfFileContentsFromPluginExecution(String, ExecutablePluginType, String, int)} to try a
   * transformation on a list of records just after validation external to preview an example result.
   * </p>
   *
   * @param datasetId the dataset identifier, it is required for authentication and for the dataset fields xslt injection
   * @param records the list of {@link Record} for which {@link Record#getXmlRecord()} returns a non-null value
   * @return a list of {@link Record}s with {@link Record#getXmlRecord()} returning the transformed XML
   * @throws GenericMetisException which can be one of:
   * <ul>
   * <li>{@link NoDatasetFoundException} if the dataset was not found.</li>
   * <li>{@link NoXsltFoundException} if there is no xslt found</li>
   * <li>{@link XsltSetupException} if the XSL transform could not be set up</li>
   * </ul>
   */
  public List<Record> transformRecordsUsingLatestDatasetXslt(String datasetId,
      List<Record> records) throws GenericMetisException {
    //Used for authentication and dataset existence
    final Dataset dataset = datasetDao.getDatasetOrThrow(datasetId);
    if (dataset.getXsltId() == null) {
      throw new NoXsltFoundException(
          format("Could not find xslt for datasetId %s", datasetId));
    }
    DatasetXslt datasetXslt = datasetXsltDao.getById(dataset.getXsltId().toString());

    String xsltUrl;
    synchronized (this) {
      xsltUrl = metisCoreUrl + RestEndpoints
          .resolve(RestEndpoints.DATASETS_XSLT_XSLTID, Collections.singletonList(datasetXslt.getId().toString()));
    }

    return transformRecords(dataset, records, xsltUrl);
  }

  private List<Record> transformRecords(Dataset dataset, Collection<Record> records, String xsltUrl)
      throws XsltSetupException {

    // Set up transformer.
    final EuropeanaIdCreator europeanIdCreator;
    final TransformationParameters transformationParameters = new TransformationParameters(dataset);
    try (XsltTransformer transformer = new XsltTransformer(xsltUrl, transformationParameters.getDatasetName(),
        transformationParameters.getEdmCountry(), transformationParameters.getEdmLanguage())) {
      europeanIdCreator = new EuropeanaIdCreator();

      // Transform the records.
      return records.stream().map(ecloudIdXmlRecord -> {
        try {
          EuropeanaGeneratedIdsMap europeanaGeneratedIdsMap = europeanIdCreator
              .constructEuropeanaId(ecloudIdXmlRecord.xmlRecord(), dataset.getDatasetId());
          return new Record(ecloudIdXmlRecord.ecloudId(),
              transformer.transform(ecloudIdXmlRecord.xmlRecord().getBytes(StandardCharsets.UTF_8), europeanaGeneratedIdsMap)
                         .toString());
        } catch (TransformationException e) {
          LOGGER.info("Record from list failed transformation", e);
          return new Record(ecloudIdXmlRecord.ecloudId(), e.getMessage());
        } catch (EuropeanaIdException e) {
          LOGGER.info(CommonStringValues.EUROPEANA_ID_CREATOR_INITIALIZATION_FAILED, e);
          return new Record(ecloudIdXmlRecord.ecloudId(), e.getMessage());
        }
      }).toList();
    } catch (TransformationException e) {
      throw new XsltSetupException("Could not setup XSL transformation.", e);
    } catch (EuropeanaIdException e) {
      throw new XsltSetupException(CommonStringValues.EUROPEANA_ID_CREATOR_INITIALIZATION_FAILED,
          e);
    }
  }

  /**
   * Get all datasets using the provider field.
   *
   * @param provider the provider string used to find the datasets
   * @param nextPage the nextPage token or -1
   * @return {@link List} of {@link Dataset}
   */
  public List<DatasetDTO> getAllDatasetsByProvider(String provider, int nextPage) {
    List<Dataset> allDatasetsByProvider = datasetDao.getAllDatasetsByProvider(provider, nextPage);
    return allDatasetsByProvider.stream()
                         .map(storedDataset -> DatasetConverter.toDTO(storedDataset,
                             userService.getUserFromCache(storedDataset.getCreatedByUserId())))
                         .toList();
  }

  /**
   * Get all datasets using the intermediateProvider field.
   *
   * @param intermediateProvider the intermediateProvider string used to find the datasets
   * @param nextPage the nextPage token or -1
   * @return {@link List} of {@link Dataset}
   */
  public List<DatasetDTO> getAllDatasetsByIntermediateProvider(String intermediateProvider, int nextPage) {
    List<Dataset> allDatasetsByIntermediateProvider = datasetDao.getAllDatasetsByIntermediateProvider(intermediateProvider, nextPage);
    return allDatasetsByIntermediateProvider.stream()
                                .map(storedDataset -> DatasetConverter.toDTO(storedDataset,
                                    userService.getUserFromCache(storedDataset.getCreatedByUserId())))
                                .toList();
  }

  /**
   * Get all datasets using the dataProvider field.
   *
   * @param dataProvider the dataProvider string used to find the datasets
   * @param nextPage the nextPage token or -1
   * @return {@link List} of {@link Dataset}
   */
  public List<DatasetDTO> getAllDatasetsByDataProvider(String dataProvider, int nextPage) {
    List<Dataset> allDatasetsByDataProvider = datasetDao.getAllDatasetsByDataProvider(dataProvider, nextPage);
    return allDatasetsByDataProvider.stream()
                                            .map(storedDataset -> DatasetConverter.toDTO(storedDataset,
                                                userService.getUserFromCache(storedDataset.getCreatedByUserId())))
                                            .toList();
  }

  /**
   * Get the list of of matching DatasetSearch using dataset
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
  public List<DatasetSearchView> searchDatasetsBasedOnSearchString(String searchString,
      int nextPage) throws GenericMetisException {
    if (StringUtils.isBlank(searchString)) {
      throw new BadContentException("Parameter searchString cannot be blank");
    }
    final String[] words = searchString.split("\\s+");
    final List<String> datasetIdWords = Arrays.stream(words)
                                              .filter(word -> Character.isDigit(word.charAt(0))).toList();

    final List<String> minimumLengthWords = Arrays.stream(words)
                                                  .filter(word -> word.length() >= MINIMUM_WORD_LENGTH).toList();

    List<Dataset> datasets = new ArrayList<>();
    if (!datasetIdWords.isEmpty() || !minimumLengthWords.isEmpty()) {
      datasets = datasetDao
          .searchDatasetsBasedOnSearchString(datasetIdWords, minimumLengthWords, nextPage);
    }

    return datasets.stream().map(dataset -> {
          final PluginWithExecutionId<ExecutablePlugin> latestSuccessfulExecutablePlugin = workflowExecutionDao
              .getLatestSuccessfulExecutablePlugin(dataset.getDatasetId(),
                  EnumSet.allOf(ExecutablePluginType.class), false);
          final DatasetSearchView datasetSearchView = new DatasetSearchView();
          datasetSearchView.setDatasetId(dataset.getDatasetId());
          datasetSearchView.setDatasetName(dataset.getDatasetName());
          datasetSearchView.setProvider(dataset.getProvider());
          datasetSearchView.setDataProvider(dataset.getDataProvider());
          if (latestSuccessfulExecutablePlugin != null) {
            datasetSearchView
                .setLastExecutionDate(latestSuccessfulExecutablePlugin.getPlugin().getStartedDate());
          }
          return datasetSearchView;
        }
    ).toList();
  }

  public int getDatasetsPerRequestLimit() {
    return datasetDao.getDatasetsPerRequest();
  }

  public void setMetisCoreUrl(String metisCoreUrl) {
    synchronized (this) {
      this.metisCoreUrl = metisCoreUrl;
    }
  }
}
