package eu.europeana.metis.core.utils;

import static java.lang.Long.parseLong;

import eu.europeana.cloud.common.model.dps.ErrorDetails;
import eu.europeana.cloud.common.model.dps.NodeStatistics;
import eu.europeana.cloud.common.model.dps.RecordState;
import eu.europeana.cloud.common.model.dps.StatisticsReport;
import eu.europeana.cloud.common.model.dps.SubTaskInfo;
import eu.europeana.cloud.common.model.dps.TaskErrorInfo;
import eu.europeana.cloud.common.model.dps.TaskErrorsInfo;
import eu.europeana.metis.core.common.Language;
import eu.europeana.metis.core.dao.WorkflowExecutionDao.ExecutionDatasetPair;
import eu.europeana.metis.core.dataset.Dataset;
import eu.europeana.metis.core.dataset.Dataset.PublicationFitness;
import eu.europeana.metis.core.dataset.DatasetDTO;
import eu.europeana.metis.core.dataset.DatasetXslt;
import eu.europeana.metis.core.engine.base.item.report.DataItemState;
import eu.europeana.metis.core.engine.base.item.report.DataItemStatus;
import eu.europeana.metis.core.engine.base.task.report.EngineTaskErrorDetails;
import eu.europeana.metis.core.engine.base.task.report.EngineTaskErrorInfo;
import eu.europeana.metis.core.engine.base.task.report.EngineTaskErrors;
import eu.europeana.metis.core.rest.Record;
import eu.europeana.metis.core.user.User;
import eu.europeana.metis.core.user.User.UserBuilder;
import eu.europeana.metis.core.workflow.Workflow;
import eu.europeana.metis.core.workflow.WorkflowExecution;
import eu.europeana.metis.core.workflow.WorkflowStatus;
import eu.europeana.metis.core.workflow.plugins.AbstractExecutablePlugin;
import eu.europeana.metis.core.workflow.plugins.AbstractExecutablePluginMetadata;
import eu.europeana.metis.core.workflow.plugins.AbstractMetisPlugin;
import eu.europeana.metis.core.workflow.plugins.DepublishPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.EnrichmentPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.ExecutablePluginFactory;
import eu.europeana.metis.core.workflow.plugins.ExecutablePluginType;
import eu.europeana.metis.core.workflow.plugins.HTTPHarvestPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.IndexToPreviewPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.IndexToPublishPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.LinkCheckingPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.MediaProcessPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.NormalizationPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.OaipmhHarvestPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.TransformationPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.ValidationExternalPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.ValidationInternalPluginMetadata;
import eu.europeana.metis.utils.Country;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.bson.types.ObjectId;

public class TestObjectFactory {

  public static final int DATASETID = 100;
  public static final DatasetXslt DATASET_XSLT = new DatasetXslt();
  public static final String EXECUTIONID = "5a5dc67ba458bb00083d49e3";
  public static final String DATASETNAME = "datasetName";
  public static final String USER_ID = "userId";
  public static final String EXTERNAL_TASK_ID = "2070373127078497810";
  private static final int OCCURRENCES = 2;

  static {
    DATASET_XSLT.setId(new ObjectId("5a9821af34f04b794dcf63df"));
  }


  private TestObjectFactory() {
  }

  /**
   * Create a dummy workflow
   *
   * @return the created workflow
   */
  public static Workflow createWorkflowObject() {
    Workflow workflow = new Workflow();
    workflow.setDatasetId(Integer.toString(DATASETID));
    List<AbstractExecutablePluginMetadata> abstractExecutablePluginMetadata = List.of(
        enableMetadata(new OaipmhHarvestPluginMetadata()),
        enableMetadata(new ValidationExternalPluginMetadata()),
        enableMetadata(new TransformationPluginMetadata()),
        enableMetadata(new ValidationInternalPluginMetadata()),
        enableMetadata(new NormalizationPluginMetadata()),
        enableMetadata(new LinkCheckingPluginMetadata()),
        enableMetadata(new EnrichmentPluginMetadata())
    );
    workflow.setMetisPluginsMetadata(abstractExecutablePluginMetadata);

    return workflow;
  }

  private static <T extends AbstractExecutablePluginMetadata> T enableMetadata(T abstractExecutablePluginMetadata) {
    abstractExecutablePluginMetadata.setEnabled(true);
    return abstractExecutablePluginMetadata;
  }

  public static WorkflowExecution createWorkflowExecutionObject(ExecutablePluginType executablePluginType) {
    Dataset dataset = createDataset(DATASETNAME);
    ArrayList<AbstractMetisPlugin<?>> abstractMetisPlugins = new ArrayList<>();
    AbstractExecutablePlugin<?> executablePlugin = createExecutablePlugin(executablePluginType);
    abstractMetisPlugins.add(executablePlugin);

    WorkflowExecution workflowExecution = new WorkflowExecution();
    workflowExecution.setNextExecutablePluginType(executablePluginType);
    workflowExecution.setDatasetId(dataset.getDatasetId());
    workflowExecution.setEcloudDatasetId(dataset.getEcloudDatasetId());
    workflowExecution.setMetisPlugins(abstractMetisPlugins);
    workflowExecution.setId(new ObjectId());
    workflowExecution.setWorkflowStatus(WorkflowStatus.INQUEUE);
    workflowExecution.setCreatedDate(new Date());

    return workflowExecution;
  }

  public static AbstractExecutablePlugin<?> createExecutablePlugin(ExecutablePluginType type) {
    return ExecutablePluginFactory.createPlugin(switch (type) {
      case HTTP_HARVEST -> new HTTPHarvestPluginMetadata();
      case OAIPMH_HARVEST -> new OaipmhHarvestPluginMetadata();
      case ENRICHMENT -> new EnrichmentPluginMetadata();
      case MEDIA_PROCESS -> new MediaProcessPluginMetadata();
      case LINK_CHECKING -> new LinkCheckingPluginMetadata();
      case VALIDATION_EXTERNAL -> new ValidationExternalPluginMetadata();
      case TRANSFORMATION -> new TransformationPluginMetadata();
      case VALIDATION_INTERNAL -> new ValidationInternalPluginMetadata();
      case NORMALIZATION -> new NormalizationPluginMetadata();
      case PREVIEW -> new IndexToPreviewPluginMetadata();
      case PUBLISH -> new IndexToPublishPluginMetadata();
      case DEPUBLISH -> new DepublishPluginMetadata();
    });
  }

  /**
   * Create dummy workflow execution
   *
   * @return the created workflow execution
   */
  public static WorkflowExecution createWorkflowExecutionObject() {
    Dataset dataset = createDataset(DATASETNAME);
    ArrayList<AbstractMetisPlugin<?>> abstractMetisPlugins = new ArrayList<>();
    AbstractMetisPlugin<?> oaipmhHarvestPlugin = ExecutablePluginFactory
        .createPlugin(new OaipmhHarvestPluginMetadata());
    abstractMetisPlugins.add(oaipmhHarvestPlugin);
    AbstractMetisPlugin<?> validationExternalPlugin = ExecutablePluginFactory
        .createPlugin(new ValidationExternalPluginMetadata());
    abstractMetisPlugins.add(validationExternalPlugin);

    WorkflowExecution workflowExecution = createWorkflowExecutionObject(dataset);
    workflowExecution.setNextExecutablePluginType(ExecutablePluginType.OAIPMH_HARVEST);
    workflowExecution.setMetisPlugins(abstractMetisPlugins);
    return workflowExecution;
  }

  public static WorkflowExecution createWorkflowExecutionObject(Dataset dataset) {
    WorkflowExecution execution = new WorkflowExecution();
    execution.setDatasetId(dataset.getDatasetId());
    execution.setEcloudDatasetId(dataset.getEcloudDatasetId());
    execution.setWorkflowStatus(WorkflowStatus.INQUEUE);
    execution.setCreatedDate(new Date());
    execution.setId(new ObjectId());
    return execution;
  }

  /**
   * Create a list of dummy workflow executions. The dataset name will have a suffix number for each dataset.
   *
   * @param size the number of dummy workflow executions to create
   * @return the created list
   */
  public static List<WorkflowExecution> createListOfWorkflowExecutions(int size) {
    return createExecutionsWithDatasets(size).stream().map(ExecutionDatasetPair::getExecution)
                                             .toList();
  }

  /**
   * Create a list of dummy execution overviews. The dataset name will have a suffix number for each dataset.
   *
   * @param size the number of dummy execution overviews to create
   * @return the created list
   */
  public static List<ExecutionDatasetPair> createExecutionsWithDatasets(int size) {
    final List<ExecutionDatasetPair> result = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      Dataset dataset = createDataset(String.format("%s%s", DATASETNAME, i));
      dataset.setId(new ObjectId(new Date(i)));
      dataset.setDatasetId(Integer.toString(DATASETID + i));
      WorkflowExecution workflowExecution = createWorkflowExecutionObject(dataset);
      workflowExecution.setId(new ObjectId());
      result.add(new ExecutionDatasetPair(dataset, workflowExecution));
    }
    return result;
  }

  /**
   * Create a dummy dataset
   *
   * @param datasetName the dataset name to be used
   * @return the created dataset
   */
  public static DatasetDTO createDatasetDTO(String datasetName) {
    DatasetDTO ds = new DatasetDTO();
    ds.setEcloudDatasetId("NOT_CREATED_YET-f525f64c-fea0-44bf-8c56-88f30962734c");
    ds.setDatasetId(Integer.toString(DATASETID));
    ds.setDatasetName(datasetName);
    final String providerId = "1234567890";
    ds.setProvider(providerId);
    ds.setIntermediateProvider(providerId);
    ds.setDataProvider(providerId);
    ds.setCreatedByUserId("userId");
    ds.setCreatedDate(new Date());
    ds.setUpdatedDate(new Date());
    ds.setReplacedBy("replacedBy");
    ds.setReplaces("12345");
    ds.setCountry(Country.GREECE);
    ds.setLanguage(Language.AR);
    ds.setDescription("description");
    ds.setPublicationFitness(PublicationFitness.PARTIALLY_FIT);
    ds.setNotes("Notes");
    return ds;
  }

  /**
   * Create a dummy dataset
   *
   * @param datasetName the dataset name to be used
   * @return the created dataset
   */
  public static Dataset createDataset(String datasetName) {
    Dataset ds = new Dataset();
    ds.setId(new ObjectId());
    ds.setEcloudDatasetId("NOT_CREATED_YET-f525f64c-fea0-44bf-8c56-88f30962734c");
    ds.setDatasetId(Integer.toString(DATASETID));
    ds.setDatasetName(datasetName);
    final String providerId = "1234567890";
    ds.setProvider(providerId);
    ds.setIntermediateProvider(providerId);
    ds.setDataProvider(providerId);
    ds.setCreatedByUserId("userId");
    ds.setCreatedDate(new Date());
    ds.setUpdatedDate(new Date());
    ds.setReplacedBy("replacedBy");
    ds.setReplaces("12345");
    ds.setCountry(Country.GREECE);
    ds.setLanguage(Language.AR);
    ds.setDescription("description");
    ds.setPublicationFitness(PublicationFitness.PARTIALLY_FIT);
    ds.setNotes("Notes");
    return ds;
  }

  public static User createUser(String userId) {
    return new UserBuilder()
        .userId(userId)
        .userName("userName")
        .firstName("firstName")
        .lastName("lastName")
        .issuedAt(Instant.now())
        .build();
  }

  /**
   * Create a dummy subtask info
   *
   * @return the created subtask info
   */
  public static List<SubTaskInfo> createListOfSubTaskInfo() {
    SubTaskInfo subTaskInfo1 = new SubTaskInfo(1, "some_resource_id1", RecordState.SUCCESS, "info",
        "additional info", "europeanaId", 0L);
    SubTaskInfo subTaskInfo2 = new SubTaskInfo(2, "some_resource_id2", RecordState.SUCCESS, "info",
        "additional info", "europeanaId", 0L);
    return List.of(subTaskInfo1, subTaskInfo2);
  }

  public static List<DataItemStatus> createExternalRecordStatusList() {
    return createListOfSubTaskInfo().stream()
                                    .map(subTaskInfo -> new DataItemStatus(
                                        subTaskInfo.getResourceNum(),
                                        subTaskInfo.getResource(),
                                        DataItemState.valueOf(subTaskInfo.getRecordState().name()),
                                        subTaskInfo.getInfo(),
                                        subTaskInfo.getEuropeanaId(),
                                        subTaskInfo.getProcessingTime(),
                                        subTaskInfo.getResultResource()))
                                    .toList();
  }

  /**
   * Create a task errors info object, which contains a list of {@link TaskErrorInfo} objects.
   *
   * @param numberOfErrorTypes the number of dummy error types
   * @return the created task errors info
   */
  public static TaskErrorsInfo createTaskErrorsInfoListWithoutIdentifiers(int numberOfErrorTypes) {
    ArrayList<TaskErrorInfo> taskErrorInfos = new ArrayList<>();
    for (int i = 0; i < numberOfErrorTypes; i++) {
      TaskErrorInfo taskErrorInfo = new TaskErrorInfo("be39ef50-f77d-11e7-af0f-fa163e77119a",
          String.format("Error%s", i), OCCURRENCES);
      taskErrorInfos.add(taskErrorInfo);
    }
    return new TaskErrorsInfo(parseLong(EXTERNAL_TASK_ID), taskErrorInfos);
  }

  /**
   * Create a task errors info object, which contains a list of {@link TaskErrorInfo} objects. These will also contain a list of
   * {@link ErrorDetails} that in turn contain dummy identifiers.
   *
   * @param numberOfErrorTypes the number of dummy error types
   * @return the created task errors info
   */
  public static TaskErrorsInfo createTaskErrorsInfoListWithIdentifiers(int numberOfErrorTypes) {
    ArrayList<TaskErrorInfo> taskErrorInfos = new ArrayList<>();
    for (int i = 0; i < numberOfErrorTypes; i++) {
      TaskErrorInfo taskErrorInfo = new TaskErrorInfo("be39ef50-f77d-11e7-af0f-fa163e77119a",
          String.format("Error%s", i), OCCURRENCES);
      ArrayList<ErrorDetails> errorDetails = new ArrayList<>();
      errorDetails.add(new ErrorDetails("identifier1", "error1"));
      errorDetails.add(new ErrorDetails("identifier2", "error2"));
      taskErrorInfo.setErrorDetails(errorDetails);
      taskErrorInfos.add(taskErrorInfo);
    }
    return new TaskErrorsInfo(parseLong(EXTERNAL_TASK_ID), taskErrorInfos);
  }

  /**
   * Create a task errors info object, which contains a list of {@link TaskErrorInfo} objects. These will also contain a list of
   * {@link ErrorDetails} that in turn contain dummy identifiers.
   *
   * @param errorType the error type to be used for the internal {@link TaskErrorInfo}
   * @param message the message type to be used for the internal {@link TaskErrorInfo}
   * @return the created task errors info
   */
  public static TaskErrorsInfo createTaskErrorsInfoWithIdentifiers(String errorType,
      String message) {
    ArrayList<ErrorDetails> errorDetails = new ArrayList<>();
    errorDetails.add(new ErrorDetails("identifier1", "error1"));
    errorDetails.add(new ErrorDetails("identifier2", "error2"));
    TaskErrorInfo taskErrorInfo1 = new TaskErrorInfo(errorType,
        message, OCCURRENCES, errorDetails);
    ArrayList<TaskErrorInfo> taskErrorInfos = new ArrayList<>();
    taskErrorInfos.add(taskErrorInfo1);

    return new TaskErrorsInfo(parseLong(EXTERNAL_TASK_ID), taskErrorInfos);
  }

  public static EngineTaskErrors createTaskErrorsInfoWithIdentifiersExternal(String errorType, String message) {
    TaskErrorsInfo taskErrorsInfo = createTaskErrorsInfoWithIdentifiers(errorType, message);

    List<EngineTaskErrorInfo> engineTaskErrorInfoList = taskErrorsInfo.getErrors().stream().map(taskErrorInfo -> {
      List<EngineTaskErrorDetails> engineTaskErrorDetailsList = new ArrayList<>();
      for (ErrorDetails errorDetail : taskErrorInfo.getErrorDetails()) {
        EngineTaskErrorDetails engineTaskErrorDetails = new EngineTaskErrorDetails(errorDetail.getIdentifier(),
            errorDetail.getAdditionalInfo());
        engineTaskErrorDetailsList.add(engineTaskErrorDetails);
      }
      return new EngineTaskErrorInfo(taskErrorInfo.getErrorType(), taskErrorInfo.getMessage(),
          taskErrorInfo.getOccurrences(), engineTaskErrorDetailsList);
    }).toList();
    return new EngineTaskErrors(Long.toString(taskErrorsInfo.getId()), engineTaskErrorInfoList);
  }

  /**
   * Create a dummy {@link StatisticsReport}
   *
   * @return the created report
   */
  public static StatisticsReport createTaskStatisticsReport() {
    List<NodeStatistics> nodeStatistics = new ArrayList<>();
    nodeStatistics.add(new NodeStatistics("parentpath1", "path1", "value1", 1));
    nodeStatistics.add(new NodeStatistics("parentpath2", "path2", "value2", OCCURRENCES));
    return new StatisticsReport(parseLong(EXTERNAL_TASK_ID), nodeStatistics);
  }

  /**
   * Create a dummy dataset xslt. The xslt copies a record.
   *
   * @param dataset the dataset to be used for the creation of the {@link DatasetXslt}
   * @return the created dataset xslt
   */
  public static DatasetXslt createXslt(Dataset dataset) {
    DatasetXslt datasetXslt = new DatasetXslt(dataset.getDatasetId(),
        """
            <?xml version="1.0" encoding="UTF-8"?>
            <xsl:stylesheet version="2.0"
            xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
            <xsl:template match="/">
            <xsl:copy-of select="node()"/>
            </xsl:template>
            </xsl:stylesheet>""");
    datasetXslt.setId(new ObjectId());
    return datasetXslt;
  }

  /**
   * Create a dummy list of {@link Record}s
   *
   * @param numberOfRecords the number of records to create
   * @return the created list of records
   */
  public static List<Record> createListOfRecords(int numberOfRecords) {
    List<Record> records = new ArrayList<>(numberOfRecords);
    for (int i = 0; i < numberOfRecords; i++) {
      String domain = String.format("http://some.domain.com/id/path/%s", i);
      records.add(new Record(UUID.randomUUID().toString(),
          "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
              + "<rdf:RDF xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
              + "\txmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:edm=\"http://www.europeana.eu/schemas/edm/\">\n"
              + "\t<edm:ProvidedCHO rdf:about=\"" + domain + "\">\n"
              + "\t</edm:ProvidedCHO>\n"
              + "</rdf:RDF>\n"));
    }
    return records;
  }

}
