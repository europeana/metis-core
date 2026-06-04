package eu.europeana.metis.core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import eu.europeana.metis.core.common.DaoFieldNames;
import eu.europeana.metis.core.dao.DataEvolutionUtils;
import eu.europeana.metis.core.dao.DatasetDao;
import eu.europeana.metis.core.dao.DatasetXsltDao;
import eu.europeana.metis.core.dao.DepublishRecordIdDao;
import eu.europeana.metis.core.dao.PluginWithExecutionId;
import eu.europeana.metis.core.dao.WorkflowDao;
import eu.europeana.metis.core.dao.WorkflowExecutionDao;
import eu.europeana.metis.core.dao.WorkflowExecutionDao.ExecutionDatasetPair;
import eu.europeana.metis.core.dao.WorkflowExecutionDao.ResultList;
import eu.europeana.metis.core.dao.WorkflowValidationUtils;
import eu.europeana.metis.core.dataset.Dataset;
import eu.europeana.metis.core.dataset.DatasetExecutionInformation;
import eu.europeana.metis.core.dataset.DatasetXslt;
import eu.europeana.metis.core.engine.base.EngineTaskClient;
import eu.europeana.metis.core.exceptions.NoDatasetFoundException;
import eu.europeana.metis.core.exceptions.NoWorkflowExecutionFoundException;
import eu.europeana.metis.core.exceptions.NoWorkflowFoundException;
import eu.europeana.metis.core.exceptions.PluginExecutionNotAllowed;
import eu.europeana.metis.core.exceptions.WorkflowAlreadyExistsException;
import eu.europeana.metis.core.exceptions.WorkflowExecutionAlreadyExistsException;
import eu.europeana.metis.core.execution.WorkflowExecutorSettings;
import eu.europeana.metis.core.rest.ExecutionHistory;
import eu.europeana.metis.core.rest.PluginsWithDataAvailability;
import eu.europeana.metis.core.rest.VersionEvolution;
import eu.europeana.metis.core.rest.VersionEvolution.VersionEvolutionStep;
import eu.europeana.metis.core.rest.execution.overview.DatasetSummaryView;
import eu.europeana.metis.core.rest.execution.overview.ExecutionAndDatasetView;
import eu.europeana.metis.core.rest.execution.overview.ExecutionSummaryView;
import eu.europeana.metis.core.utils.TestObjectFactory;
import eu.europeana.metis.core.workflow.ValidationProperties;
import eu.europeana.metis.core.workflow.Workflow;
import eu.europeana.metis.core.workflow.WorkflowExecution;
import eu.europeana.metis.core.workflow.WorkflowStatus;
import eu.europeana.metis.core.workflow.execution.MetisPluginDTO;
import eu.europeana.metis.core.workflow.execution.WorkflowExecutionDTO;
import eu.europeana.metis.core.workflow.plugins.AbstractExecutablePlugin;
import eu.europeana.metis.core.workflow.plugins.AbstractExecutablePluginMetadata;
import eu.europeana.metis.core.workflow.plugins.AbstractMetisPlugin;
import eu.europeana.metis.core.workflow.plugins.ExecutablePluginFactory;
import eu.europeana.metis.core.workflow.plugins.ExecutablePluginType;
import eu.europeana.metis.core.workflow.plugins.ExecutionProgress;
import eu.europeana.metis.core.workflow.plugins.HTTPHarvestPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.IndexToPreviewPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.IndexToPublishPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.MediaProcessPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.OaipmhHarvestPlugin;
import eu.europeana.metis.core.workflow.plugins.OaipmhHarvestPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.PluginStatus;
import eu.europeana.metis.core.workflow.plugins.PluginType;
import eu.europeana.metis.core.workflow.plugins.ReindexToPreviewPlugin;
import eu.europeana.metis.core.workflow.plugins.ReindexToPreviewPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.TransformationPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.ValidationExternalPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.ValidationInternalPluginMetadata;
import eu.europeana.metis.exception.BadContentException;
import eu.europeana.metis.exception.ExternalTaskException;
import eu.europeana.metis.exception.GenericMetisException;
import eu.europeana.metis.utils.DateUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

@SuppressWarnings("unchecked")
class TestOrchestratorService {

  private static final int SOLR_COMMIT_PERIOD_IN_MINUTES = 15;
  private static WorkflowExecutionDao workflowExecutionDao;
  private static DataEvolutionUtils dataEvolutionUtils;
  private static WorkflowValidationUtils validationUtils;
  private static WorkflowDao workflowDao;
  private static DatasetDao datasetDao;
  private static DatasetXsltDao datasetXsltDao;
  private static WorkflowExecutorSettings<?, ?> workflowExecutorSettings;
  private static WorkflowExecutionFactory workflowExecutionFactory;
  private static OrchestratorService<?, ?> orchestratorService;
  private static RedissonClient redissonClient;

  @BeforeAll
  static void prepare() {
    workflowExecutionDao = mock(WorkflowExecutionDao.class);
    dataEvolutionUtils = mock(DataEvolutionUtils.class);
    validationUtils = mock(WorkflowValidationUtils.class);
    workflowDao = mock(WorkflowDao.class);
    datasetDao = mock(DatasetDao.class);
    datasetXsltDao = mock(DatasetXsltDao.class);
    DepublishRecordIdDao depublishRecordIdDao = mock(DepublishRecordIdDao.class);
    workflowExecutorSettings = mock(WorkflowExecutorSettings.class);
    redissonClient = mock(RedissonClient.class);
    UserService userService = mock(UserService.class);

    RedirectionInferrer redirectionInferrer = new RedirectionInferrer(workflowExecutionDao, dataEvolutionUtils);
    workflowExecutionFactory = spy(new WorkflowExecutionFactory(datasetXsltDao,
        depublishRecordIdDao, redirectionInferrer));
    workflowExecutionFactory.setValidationExternalProperties(
        new ValidationProperties("url-ext", "schema-ext", "schematron-ext"));
    workflowExecutionFactory.setValidationInternalProperties(
        new ValidationProperties("url-int", "schema-int", "schematron-int"));

    orchestratorService = spy(new OrchestratorService<>(workflowExecutionFactory, workflowDao,
        workflowExecutionDao, validationUtils, dataEvolutionUtils, datasetDao,
        workflowExecutorSettings, redissonClient, depublishRecordIdDao, userService));
    orchestratorService.setSolrCommitPeriodInMinutes(SOLR_COMMIT_PERIOD_IN_MINUTES);
  }

  @BeforeEach
  void cleanUp() throws ExternalTaskException {
    Mockito.reset(workflowExecutionDao);
    Mockito.reset(validationUtils);
    Mockito.reset(workflowDao);
    Mockito.reset(datasetDao);
    Mockito.reset(workflowExecutorSettings);
    Mockito.reset(redissonClient);
    Mockito.reset(workflowExecutionFactory);
    Mockito.reset(orchestratorService);

    //Stub for engine task dataset id creation
    EngineTaskClient<?, ?> mockEngineTaskClient = mock(EngineTaskClient.class);
    when(mockEngineTaskClient.createEngineDatasetId(any(Dataset.class))).thenReturn("");
    doReturn(mockEngineTaskClient).when(workflowExecutorSettings).engineTaskClient();
    when(datasetDao.update(any(Dataset.class))).thenReturn("");
  }

  @Test
  void createWorkflow() throws Exception {
    Workflow workflow = TestObjectFactory.createWorkflowObject();
    Dataset dataset = TestObjectFactory.createDataset("datasetName");
    workflow.setDatasetId(dataset.getDatasetId());
    when(datasetDao.getDatasetByDatasetId(dataset.getDatasetId())).thenReturn(dataset);
    orchestratorService.createWorkflow(workflow.getDatasetId(), workflow, null);

    InOrder inOrder = Mockito.inOrder(workflowDao);
    inOrder.verify(workflowDao, times(1)).workflowExistsForDataset(workflow.getDatasetId());
    inOrder.verify(workflowDao, times(1)).create(workflow);
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  void createWorkflowOrderOfPluginsNotAllowed() throws Exception {
    Workflow workflow = TestObjectFactory.createWorkflowObject();
    Dataset dataset = TestObjectFactory.createDataset("datasetName");
    workflow.setDatasetId(dataset.getDatasetId());
    when(datasetDao.getDatasetByDatasetId(dataset.getDatasetId())).thenReturn(dataset);
    doThrow(PluginExecutionNotAllowed.class).when(validationUtils).validateWorkflowPlugins(workflow, null);
    assertThrows(PluginExecutionNotAllowed.class,
        () -> orchestratorService.createWorkflow(workflow.getDatasetId(), workflow, null));

    verify(workflowDao, times(1)).workflowExistsForDataset(workflow.getDatasetId());
    verifyNoMoreInteractions(workflowDao);
  }

  @Test
  void createWorkflow_AlreadyExists() {
    Workflow workflow = TestObjectFactory.createWorkflowObject();
    Dataset dataset = TestObjectFactory.createDataset("datasetName");
    workflow.setDatasetId(dataset.getDatasetId());
    when(datasetDao.getDatasetByDatasetId(dataset.getDatasetId())).thenReturn(dataset);
    when(workflowDao.workflowExistsForDataset(workflow.getDatasetId())).thenReturn(true);

    assertThrows(WorkflowAlreadyExistsException.class,
        () -> orchestratorService.createWorkflow(workflow.getDatasetId(), workflow, null));

    InOrder inOrder = Mockito.inOrder(workflowDao);
    inOrder.verify(workflowDao, times(1)).workflowExistsForDataset(workflow.getDatasetId());
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  void updateWorkflow() throws Exception {
    Workflow workflow = TestObjectFactory.createWorkflowObject();
    Dataset dataset = TestObjectFactory.createDataset("datasetName");
    workflow.setDatasetId(dataset.getDatasetId());
    when(datasetDao.getDatasetByDatasetId(dataset.getDatasetId())).thenReturn(dataset);
    when(workflowDao.getWorkflow(dataset.getDatasetId())).thenReturn(workflow);
    orchestratorService.updateWorkflow(workflow.getDatasetId(), workflow, null);
    InOrder inOrder = Mockito.inOrder(workflowDao);
    inOrder.verify(workflowDao, times(1)).getWorkflow(dataset.getDatasetId());
    inOrder.verify(workflowDao, times(1)).update(workflow);
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  void updateUserWorkflow_NoUserWorkflowFound() {
    Workflow workflow = TestObjectFactory.createWorkflowObject();
    Dataset dataset = TestObjectFactory.createDataset("datasetName");
    workflow.setDatasetId(dataset.getDatasetId());
    when(datasetDao.getDatasetByDatasetId(dataset.getDatasetId())).thenReturn(dataset);
    assertThrows(NoWorkflowFoundException.class,
        () -> orchestratorService.updateWorkflow(workflow.getDatasetId(), workflow, null));
    InOrder inOrder = Mockito.inOrder(workflowDao);
    inOrder.verify(workflowDao, times(1)).getWorkflow(anyString());
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  void deleteWorkflow() throws GenericMetisException {
    Workflow workflow = TestObjectFactory.createWorkflowObject();
    orchestratorService.deleteWorkflow(workflow.getDatasetId());
    ArgumentCaptor<String> workflowDatasetIdArgumentCaptor = ArgumentCaptor.forClass(String.class);
    verify(workflowDao, times(1)).deleteWorkflow(workflowDatasetIdArgumentCaptor.capture());
    assertEquals(workflow.getDatasetId(), workflowDatasetIdArgumentCaptor.getValue());
  }

  @Test
  void getWorkflow() throws GenericMetisException {
    Workflow workflow = TestObjectFactory.createWorkflowObject();
    when(workflowDao.getWorkflow(workflow.getDatasetId())).thenReturn(workflow);

    Workflow retrievedWorkflow = orchestratorService.getWorkflow(workflow.getDatasetId());
    assertSame(workflow, retrievedWorkflow);
  }

  @Test
  void getWorkflowExecutionByExecutionId() throws GenericMetisException {

    // Create some objects
    final String workflowExecutionId = "workflow execution ID";
    final WorkflowExecution workflowExecution = TestObjectFactory.createWorkflowExecutionObject();

    // Test the happy flow
    when(workflowExecutionDao.getById(workflowExecutionId)).thenReturn(workflowExecution);
    when(workflowExecutionDao.getById(workflowExecution.getId().toString())).thenReturn(workflowExecution);
    WorkflowExecutionDTO workflowExecutionDTOResult = orchestratorService.getWorkflowExecutionDTOByExecutionId(
        workflowExecutionId);
    assertEquals(workflowExecution.getDatasetId(), workflowExecutionDTOResult.getDatasetId());

    // Test when the workflow execution does not exist
    when(workflowExecutionDao.getById(workflowExecutionId)).thenReturn(null);
    assertNull(orchestratorService.getWorkflowExecutionDTOByExecutionId(workflowExecutionId));
    when(workflowExecutionDao.getById(workflowExecutionId)).thenReturn(workflowExecution);
    workflowExecutionDTOResult = orchestratorService.getWorkflowExecutionDTOByExecutionId(
        workflowExecutionId);
    assertEquals(workflowExecution.getDatasetId(), workflowExecutionDTOResult.getDatasetId());
  }

  @Test
  void getWorkflowExecutionByExecutionId_NonExistingWorkflowExecution()
      throws GenericMetisException {
    final String workflowExecutionId = "workflow execution id";
    when(workflowExecutionDao.getById(workflowExecutionId)).thenReturn(null);
    orchestratorService.getWorkflowExecutionDTOByExecutionId(workflowExecutionId);
    InOrder inOrder = Mockito.inOrder(workflowExecutionDao);
    inOrder.verify(workflowExecutionDao, times(1)).getById(workflowExecutionId);
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  void addWorkflowInQueueOfWorkflowExecutions() throws Exception {

    // Create the test objects
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    Workflow workflow = TestObjectFactory.createWorkflowObject();
    when(datasetDao.getDatasetOrThrow(dataset.getDatasetId())).thenReturn(dataset);
    when(datasetDao.getDatasetByDatasetId(dataset.getDatasetId())).thenReturn(dataset);
    when(workflowDao.getWorkflow(workflow.getDatasetId())).thenReturn(workflow);
    RLock rlock = mock(RLock.class);
    when(redissonClient.getFairLock(anyString())).thenReturn(rlock);
    doNothing().when(rlock).lock();
    when(workflowExecutionDao.existsAndNotCompleted(dataset.getDatasetId())).thenReturn(null);
    ObjectId objectId = new ObjectId();
    DatasetXslt datasetXslt = TestObjectFactory.createXslt(dataset);
    datasetXslt.setId(TestObjectFactory.DATASET_XSLT.getId());
    when(datasetXsltDao.getLatestDefaultXslt()).thenReturn(datasetXslt);
    WorkflowExecution workflowExecutionTest = TestObjectFactory.createWorkflowExecutionObject(dataset);
    workflowExecutionTest.setId(objectId);
    when(workflowExecutionDao.create(any(WorkflowExecution.class))).thenReturn(workflowExecutionTest);
    doNothing().when(rlock).unlock();

    // Add the workflow
    orchestratorService.addWorkflowInQueueOfWorkflowExecutions(dataset.getDatasetId(), null, null, TestObjectFactory.USER_ID);

    // Verify the validation parameters
    final Map<ExecutablePluginType, AbstractExecutablePluginMetadata> pluginsByType = workflow
        .getMetisPluginsMetadata().stream().collect(Collectors
            .toMap(AbstractExecutablePluginMetadata::getExecutablePluginType, Function.identity(),
                (m1, m2) -> m1));
    final ValidationInternalPluginMetadata metadataInternal = (ValidationInternalPluginMetadata) pluginsByType.get(
        ExecutablePluginType.VALIDATION_INTERNAL);
    assertEquals(workflowExecutionFactory.getValidationInternalProperties().urlOfSchemasZip(),
        metadataInternal.getUrlOfSchemasZip());
    assertEquals(workflowExecutionFactory.getValidationInternalProperties().schemaRootPath(),
        metadataInternal.getSchemaRootPath());
    assertEquals(workflowExecutionFactory.getValidationInternalProperties().schematronRootPath(),
        metadataInternal.getSchematronRootPath());
    final ValidationExternalPluginMetadata metadataExternal = (ValidationExternalPluginMetadata) pluginsByType.get(
        ExecutablePluginType.VALIDATION_EXTERNAL);
    assertEquals(workflowExecutionFactory.getValidationExternalProperties().urlOfSchemasZip(),
        metadataExternal.getUrlOfSchemasZip());
    assertEquals(workflowExecutionFactory.getValidationExternalProperties().schemaRootPath(),
        metadataExternal.getSchemaRootPath());
    assertEquals(workflowExecutionFactory.getValidationExternalProperties().schematronRootPath(),
        metadataExternal.getSchematronRootPath());
  }

  @Test
  void addWorkflowInQueueOfWorkflowExecutions_TransformationUsesCustomXslt()
      throws Exception {
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    Workflow workflow = TestObjectFactory.createWorkflowObject();
    List<AbstractExecutablePluginMetadata> updatedMetadata =
        workflow.getMetisPluginsMetadata().stream()
                .peek(metadata -> {
                  if (metadata instanceof TransformationPluginMetadata transformationMetadata) {
                    transformationMetadata.setCustomXslt(true);
                  }
                }).toList();
    workflow.setMetisPluginsMetadata(updatedMetadata);
    when(datasetDao.getDatasetOrThrow(dataset.getDatasetId())).thenReturn(dataset);
    when(workflowDao.getWorkflow(workflow.getDatasetId())).thenReturn(workflow);
    RLock rlock = mock(RLock.class);
    when(redissonClient.getFairLock(anyString())).thenReturn(rlock);
    doNothing().when(rlock).lock();
    when(workflowExecutionDao.existsAndNotCompleted(dataset.getDatasetId())).thenReturn(null);
    DatasetXslt datasetXslt = TestObjectFactory.createXslt(dataset);
    datasetXslt.setId(TestObjectFactory.DATASET_XSLT.getId());
    dataset.setXsltId(datasetXslt.getId());
    when(datasetXsltDao.getById(dataset.getXsltId().toString())).thenReturn(datasetXslt);

    ObjectId objectId = new ObjectId();

    ArgumentCaptor<WorkflowExecution> workflowExecutionArgumentCaptor = ArgumentCaptor.forClass(WorkflowExecution.class);
    when(workflowExecutionDao.create(workflowExecutionArgumentCaptor.capture()))
        .thenAnswer(invocation -> {
          WorkflowExecution argument = invocation.getArgument(0);
          argument.setId(objectId);
          return argument;
        });
    doAnswer(invocation -> workflowExecutionArgumentCaptor.getValue())
        .when(workflowExecutionDao).getById(objectId.toString());
    doNothing().when(rlock).unlock();
    WorkflowExecutionDTO workflowExecutionDTO = orchestratorService.addWorkflowInQueueOfWorkflowExecutions(dataset.getDatasetId(),
        null, null, TestObjectFactory.USER_ID);
    Optional<TransformationPluginMetadata> transformationPluginMetadata = workflowExecutionDTO
        .getMetisPlugins().stream()
        .map(MetisPluginDTO::getPluginMetadata)
        .filter(TransformationPluginMetadata.class::isInstance)
        .map(TransformationPluginMetadata.class::cast)
        .findFirst();
    assertTrue(transformationPluginMetadata.isPresent());
    assertTrue(transformationPluginMetadata.get().isCustomXslt());
  }

  @Test
  void addWorkflowInQueueOfWorkflowExecutions_AddHTTPHarvest()
      throws Exception {
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    Workflow workflow = TestObjectFactory.createWorkflowObject();
    HTTPHarvestPluginMetadata httpHarvestPluginMetadata = new HTTPHarvestPluginMetadata();
    httpHarvestPluginMetadata.setUrl("http://harvest.url.org");
    httpHarvestPluginMetadata.setEnabled(true);
    List<AbstractExecutablePluginMetadata> metisPluginsMetadata = workflow.getMetisPluginsMetadata();
    metisPluginsMetadata.set(0, httpHarvestPluginMetadata);
    workflow.setMetisPluginsMetadata(metisPluginsMetadata);
    when(datasetDao.getDatasetOrThrow(dataset.getDatasetId())).thenReturn(dataset);
    when(workflowDao.getWorkflow(workflow.getDatasetId())).thenReturn(workflow);
    when(redissonClient.getFairLock(anyString())).thenReturn(Mockito.mock(RLock.class));
    when(workflowExecutionDao.existsAndNotCompleted(dataset.getDatasetId())).thenReturn(null);
    ObjectId objectId = new ObjectId();

    ArgumentCaptor<WorkflowExecution> workflowExecutionArgumentCaptor = ArgumentCaptor.forClass(WorkflowExecution.class);
    when(workflowExecutionDao.create(workflowExecutionArgumentCaptor.capture()))
        .thenAnswer(invocation -> {
          WorkflowExecution argument = invocation.getArgument(0);
          argument.setId(objectId);
          return argument;
        });
    doAnswer(invocation -> workflowExecutionArgumentCaptor.getValue())
        .when(workflowExecutionDao).getById(objectId.toString());
    WorkflowExecutionDTO workflowExecutionDTO = orchestratorService.addWorkflowInQueueOfWorkflowExecutions(dataset.getDatasetId(),
        null, null, TestObjectFactory.USER_ID);
    assertEquals(httpHarvestPluginMetadata.getUrl(),
        ((HTTPHarvestPluginMetadata) workflowExecutionDTO.getMetisPlugins().getFirst()
                                                         .getPluginMetadata()).getUrl());
  }

  @Test
  void addWorkflowInQueueOfWorkflowExecutions_NoHarvestPlugin() throws Exception {
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    Workflow workflow = TestObjectFactory.createWorkflowObject();
    workflow.getMetisPluginsMetadata().removeFirst();

    when(datasetDao.getDatasetOrThrow(dataset.getDatasetId())).thenReturn(dataset);
    when(workflowDao.getWorkflow(workflow.getDatasetId())).thenReturn(workflow);
    OaipmhHarvestPlugin oaipmhHarvestPlugin = (OaipmhHarvestPlugin) ExecutablePluginFactory.createPlugin(
        new OaipmhHarvestPluginMetadata());
    oaipmhHarvestPlugin.setPluginMetadata(new OaipmhHarvestPluginMetadata());
    oaipmhHarvestPlugin.setStartedDate(new Date());
    ExecutionProgress executionProgress = new ExecutionProgress();
    executionProgress.setProcessedRecords(5);
    oaipmhHarvestPlugin.setExecutionProgress(executionProgress);
    when(validationUtils.validateWorkflowPlugins(workflow, null)).thenReturn(
        new PluginWithExecutionId<>("execution id", oaipmhHarvestPlugin));
    RLock rlock = mock(RLock.class);
    when(redissonClient.getFairLock(anyString())).thenReturn(rlock);
    doNothing().when(rlock).lock();
    when(workflowExecutionDao.existsAndNotCompleted(dataset.getDatasetId())).thenReturn(null);
    ObjectId objectId = new ObjectId();
    WorkflowExecution workflowExecutionTest = TestObjectFactory.createWorkflowExecutionObject(dataset);
    workflowExecutionTest.setId(objectId);
    when(workflowExecutionDao.create(any(WorkflowExecution.class))).thenReturn(workflowExecutionTest);
    doNothing().when(rlock).unlock();
    orchestratorService.addWorkflowInQueueOfWorkflowExecutions(dataset.getDatasetId(), null, null, TestObjectFactory.USER_ID);
  }

  @Test
  void addWorkflowInQueueOfWorkflowExecutions_NoHarvestPlugin_NoProcessPlugin()
      throws Exception {
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    Workflow workflow = TestObjectFactory.createWorkflowObject();
    when(datasetDao.getDatasetOrThrow(dataset.getDatasetId())).thenReturn(dataset);
    when(workflowDao.getWorkflow(workflow.getDatasetId())).thenReturn(workflow);
    when(redissonClient.getFairLock(anyString())).thenReturn(Mockito.mock(RLock.class));
    when(validationUtils.validateWorkflowPlugins(workflow, null))
        .thenThrow(new PluginExecutionNotAllowed(""));
    assertThrows(PluginExecutionNotAllowed.class, () -> orchestratorService
        .addWorkflowInQueueOfWorkflowExecutions(dataset.getDatasetId(), null, null, TestObjectFactory.USER_ID));
  }

  @Test
  void addWorkflowInQueueOfWorkflowExecutions_EcloudDatasetAlreadyGenerated()
      throws Exception {
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    dataset.setEcloudDatasetId("f525f64c-fea0-44bf-8c56-88f30962734c");
    Workflow workflow = TestObjectFactory.createWorkflowObject();
    when(datasetDao.getDatasetOrThrow(dataset.getDatasetId())).thenReturn(dataset);
    when(workflowDao.getWorkflow(workflow.getDatasetId())).thenReturn(workflow);
    when(redissonClient.getFairLock(anyString())).thenReturn(Mockito.mock(RLock.class));
    when(workflowExecutionDao.existsAndNotCompleted(dataset.getDatasetId())).thenReturn(null);
    ObjectId objectId = new ObjectId();
    WorkflowExecution workflowExecutionTest = TestObjectFactory.createWorkflowExecutionObject(dataset);
    workflowExecutionTest.setId(objectId);
    when(workflowExecutionDao.create(any(WorkflowExecution.class))).thenReturn(workflowExecutionTest);
    orchestratorService.addWorkflowInQueueOfWorkflowExecutions(dataset.getDatasetId(), null, null, TestObjectFactory.USER_ID);
    verify(datasetDao, times(0)).update(any(Dataset.class));
  }

  @Test
  void addWorkflowInQueueOfWorkflowExecutions_EcloudDatasetCreationFails()
      throws Exception {
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    Workflow workflow = TestObjectFactory.createWorkflowObject();
    when(datasetDao.getDatasetOrThrow(dataset.getDatasetId())).thenReturn(dataset);
    when(workflowDao.getWorkflow(workflow.getDatasetId())).thenReturn(workflow);
    when(redissonClient.getFairLock(anyString())).thenReturn(Mockito.mock(RLock.class));
    when(workflowExecutionDao.existsAndNotCompleted(dataset.getDatasetId())).thenReturn(null);
    ObjectId objectId = new ObjectId();
    WorkflowExecution workflowExecutionTest = TestObjectFactory.createWorkflowExecutionObject(dataset);
    workflowExecutionTest.setId(objectId);
    when(workflowExecutionDao.create(any(WorkflowExecution.class))).thenReturn(workflowExecutionTest);

    EngineTaskClient<?, ?> mockEngineTaskClient = mock(EngineTaskClient.class);
    when(mockEngineTaskClient.createEngineDatasetId(any(Dataset.class))).thenThrow(new ExternalTaskException(""));
    doReturn(mockEngineTaskClient).when(workflowExecutorSettings).engineTaskClient();
    assertThrows(ExternalTaskException.class, () ->
        orchestratorService.addWorkflowInQueueOfWorkflowExecutions(dataset.getDatasetId(), null, null,
            TestObjectFactory.USER_ID));
  }

  @Test
  void addWorkflowInQueueOfWorkflowExecutions_NoDatasetFoundException()
      throws Exception {
    final String datasetId = Integer.toString(TestObjectFactory.DATASETID);
    when(datasetDao.getDatasetOrThrow(datasetId)).thenThrow(new NoDatasetFoundException(datasetId));
    assertThrows(NoDatasetFoundException.class, () -> orchestratorService
        .addWorkflowInQueueOfWorkflowExecutions(datasetId, null, null, TestObjectFactory.USER_ID));
  }

  @Test
  void addWorkflowInQueueOfWorkflowExecutions_NoWorkflowFoundException()
      throws Exception {
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    when(datasetDao.getDatasetOrThrow(dataset.getDatasetId())).thenReturn(dataset);
    when(workflowDao.getWorkflow(dataset.getDatasetId())).thenReturn(null);
    assertThrows(NoWorkflowFoundException.class,
        () -> orchestratorService.addWorkflowInQueueOfWorkflowExecutions(dataset.getDatasetId(), null, null,
            TestObjectFactory.USER_ID));
  }

  @Test
  void addWorkflowInQueueOfWorkflowExecutions_WorkflowIsEmpty() throws Exception {
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    Workflow workflow = new Workflow();
    workflow.setDatasetId(dataset.getDatasetId());
    when(datasetDao.getDatasetOrThrow(dataset.getDatasetId())).thenReturn(dataset);
    when(workflowDao.getWorkflow(dataset.getDatasetId())).thenReturn(workflow);
    when(validationUtils.validateWorkflowPlugins(workflow, null)).thenThrow(new BadContentException(""));
    assertThrows(BadContentException.class,
        () -> orchestratorService.addWorkflowInQueueOfWorkflowExecutions(dataset.getDatasetId(), null, null,
            TestObjectFactory.USER_ID));
  }

  @Test
  void addWorkflowInQueueOfWorkflowExecutions_WorkflowExecutionAlreadyExistsException()
      throws Exception {
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    Workflow workflow = TestObjectFactory.createWorkflowObject();
    when(datasetDao.getDatasetOrThrow(dataset.getDatasetId())).thenReturn(dataset);
    when(workflowDao.getWorkflow(dataset.getDatasetId())).thenReturn(workflow);
    when(redissonClient.getFairLock(anyString())).thenReturn(Mockito.mock(RLock.class));
    when(workflowExecutionDao.existsAndNotCompleted(dataset.getDatasetId())).thenReturn(new ObjectId().toString());
    assertThrows(WorkflowExecutionAlreadyExistsException.class,
        () -> orchestratorService.addWorkflowInQueueOfWorkflowExecutions(dataset.getDatasetId(), null, null,
            TestObjectFactory.USER_ID));
  }

  @Test
  void cancelWorkflowExecution() throws Exception {
    WorkflowExecution workflowExecution = TestObjectFactory.createWorkflowExecutionObject();
    when(workflowExecutionDao.getById(TestObjectFactory.EXECUTIONID)).thenReturn(workflowExecution);
    doNothing().when(workflowExecutionDao).setCancellingState(workflowExecution, TestObjectFactory.USER_ID);
    orchestratorService.cancelWorkflowExecution(TestObjectFactory.EXECUTIONID, TestObjectFactory.USER_ID);
    verify(workflowExecutionDao, times(1)).setCancellingState(workflowExecution, TestObjectFactory.USER_ID);
  }

  @Test
  void cancelWorkflowExecution_NoWorkflowExecutionFoundException() {
    when(workflowExecutionDao.getById(TestObjectFactory.EXECUTIONID)).thenReturn(null);
    assertThrows(NoWorkflowExecutionFoundException.class,
        () -> orchestratorService.cancelWorkflowExecution(TestObjectFactory.EXECUTIONID, TestObjectFactory.USER_ID));
    verifyNoMoreInteractions(workflowExecutorSettings);
  }

  @Test
  void getWorkflowExecutionsPerRequest() {
    orchestratorService.getWorkflowExecutionsPerRequest();
    verify(workflowExecutionDao, times(1)).getWorkflowExecutionsPerRequest();
  }

  @Test
  void getLatestSuccessfulFinishedPluginByDatasetIdIfPluginTypeAllowedForExecution_ProcessPlugin()
      throws Exception {
    final String datasetId = Integer.toString(TestObjectFactory.DATASETID);
    final AbstractExecutablePlugin<?> oaipmhHarvestPlugin = ExecutablePluginFactory
        .createPlugin(new OaipmhHarvestPluginMetadata());

    doReturn(new PluginWithExecutionId<>("execution ID", oaipmhHarvestPlugin))
        .when(dataEvolutionUtils)
        .computePredecessorPlugin(ExecutablePluginType.VALIDATION_EXTERNAL, null, datasetId);
    assertSame(oaipmhHarvestPlugin, orchestratorService
        .getLatestFinishedPluginByDatasetIdIfPluginTypeAllowedForExecution(datasetId,
            ExecutablePluginType.VALIDATION_EXTERNAL, null));
  }

  @Test
  void getLatestFinishedPluginByDatasetIdIfPluginTypeAllowedForExecution_PluginExecutionNotAllowed()
      throws PluginExecutionNotAllowed {
    final String datasetId = Integer.toString(TestObjectFactory.DATASETID);

    when(dataEvolutionUtils.computePredecessorPlugin(ExecutablePluginType.VALIDATION_EXTERNAL, null,
        datasetId)).thenThrow(new PluginExecutionNotAllowed(""));
    assertThrows(PluginExecutionNotAllowed.class, () -> orchestratorService
        .getLatestFinishedPluginByDatasetIdIfPluginTypeAllowedForExecution(datasetId, ExecutablePluginType.VALIDATION_EXTERNAL,
            null));
  }

  @Test
  void getAllWorkflowExecutionsByDatasetId() throws GenericMetisException {

    // Define some constants
    final int nextPage = 1;
    final String datasetId = Integer.toString(TestObjectFactory.DATASETID);
    final Set<WorkflowStatus> workflowStatuses = Collections.singleton(WorkflowStatus.INQUEUE);

    // Check with a specific dataset ID: should query only that dataset.
    doReturn(new ResultList<>(Collections.emptyList(), false)).when(workflowExecutionDao)
                                                              .getAllWorkflowExecutions(any(), any(), any(), anyBoolean(),
                                                                  anyInt(), anyInt(), anyBoolean());
    orchestratorService.getAllWorkflowExecutions(datasetId, workflowStatuses,
        DaoFieldNames.ID, false, nextPage);

    verify(workflowExecutionDao, times(1)).getAllWorkflowExecutions(
        Collections.singleton(datasetId), workflowStatuses, DaoFieldNames.ID, false,
        nextPage, 1, false);
    verify(workflowExecutionDao, times(1)).getWorkflowExecutionsPerRequest();
    verifyNoMoreInteractions(workflowExecutionDao);
  }

  @Test
  void getAllWorkflowExecutionsFor() throws GenericMetisException {

    // Define some constants
    final int nextPage = 1;
    final Set<String> datasetIds = new HashSet<>(Arrays.asList("A", "B", "C"));
    final List<Dataset> datasets = datasetIds.stream().map(id -> {
      final Dataset result = new Dataset();
      result.setDatasetId(id);
      return result;
    }).toList();
    final Set<WorkflowStatus> workflowStatuses = Collections.singleton(WorkflowStatus.INQUEUE);

    when(datasetDao.getAllDatasets()).thenReturn(datasets);
    doReturn(new ResultList<>(Collections.emptyList(), false)).when(workflowExecutionDao)
                                                              .getAllWorkflowExecutions(any(), any(), any(), anyBoolean(),
                                                                  anyInt(), anyInt(), anyBoolean());
    orchestratorService.getAllWorkflowExecutions(null, workflowStatuses,
        DaoFieldNames.CREATED_DATE, false, nextPage);
    verify(workflowExecutionDao, times(1)).getAllWorkflowExecutions(datasetIds,
        workflowStatuses, DaoFieldNames.CREATED_DATE, false, nextPage, 1, false);
    verify(workflowExecutionDao, times(1)).getWorkflowExecutionsPerRequest();
    verifyNoMoreInteractions(workflowExecutionDao);
  }

  @Test
  void getWorkflowExecutionOverview() {

    // Define some constants
    final int nextPage = 1;
    final int pageCount = 2;
    final Set<String> datasetIds = new HashSet<>(Arrays.asList("A", "B", "C"));
    final List<Dataset> datasets = datasetIds.stream().map(id -> {
      final Dataset result = new Dataset();
      result.setDatasetId(id);
      return result;
    }).toList();
    final List<ExecutionDatasetPair> data = TestObjectFactory.createExecutionsWithDatasets(4);

    when(datasetDao.getAllDatasets()).thenReturn(datasets);
    when(workflowExecutionDao
        .getWorkflowExecutionsOverview(eq(datasetIds), isNull(), isNull(), isNull(), isNull(),
            eq(nextPage), eq(pageCount)))
        .thenReturn(new ResultList<>(data, false));
    final List<ExecutionAndDatasetView> result = orchestratorService
        .getWorkflowExecutionsOverview(null, null, null, null, nextPage, pageCount)
        .getResults();
    verify(workflowExecutionDao, times(1))
        .getWorkflowExecutionsOverview(eq(datasetIds), isNull(), isNull(), isNull(), isNull(),
            eq(nextPage), eq(pageCount));
    verify(workflowExecutionDao, times(1)).getWorkflowExecutionsPerRequest();
    verifyNoMoreInteractions(workflowExecutionDao);
    assertEquals(data.size(), result.size());
    assertEquals(data.stream().map(ExecutionDatasetPair::getDataset).map(Dataset::getDatasetId).toList(),
        result.stream().map(ExecutionAndDatasetView::getDataset)
              .map(DatasetSummaryView::getDatasetId).toList());
    assertEquals(data.stream().map(ExecutionDatasetPair::getExecution).map(WorkflowExecution::getId)
                     .map(ObjectId::toString).toList(),
        result.stream().map(ExecutionAndDatasetView::getExecution)
              .map(ExecutionSummaryView::getId).toList());
  }

  @Test
  void getDatasetExecutionInformation() throws GenericMetisException {
    ExecutionProgress executionProgress = getExecutionProgress(100, 20);
    final Date longEnoughToBeValidDate = DateUtils.modifyDateByTimeUnitAmount(new Date(), -(SOLR_COMMIT_PERIOD_IN_MINUTES + 3),
        TimeUnit.MINUTES);
    final Date notLongEnoughToBeValidDate = DateUtils.modifyDateByTimeUnitAmount(new Date(), -(SOLR_COMMIT_PERIOD_IN_MINUTES + 2),
        TimeUnit.MINUTES);

    // Create preview plugin
    AbstractExecutablePlugin<IndexToPreviewPluginMetadata> previewPlugin = ExecutablePluginFactory.createPlugin(
        new IndexToPreviewPluginMetadata());
    previewPlugin.setFinishedDate(longEnoughToBeValidDate);
    previewPlugin.setDataStatus(null); // Is default status, means valid.
    previewPlugin.setExecutionProgress(executionProgress);

    // Create second publish plugin
    AbstractExecutablePlugin<IndexToPublishPluginMetadata> lastPublishPlugin = ExecutablePluginFactory.createPlugin(
        new IndexToPublishPluginMetadata());
    lastPublishPlugin.setFinishedDate(notLongEnoughToBeValidDate);
    lastPublishPlugin.setDataStatus(null); // Is default status, means valid.
    lastPublishPlugin.setExecutionProgress(executionProgress);

    boolean enableRunningPublish = true;
    getDatasetExecutionInformation(previewPlugin, lastPublishPlugin, enableRunningPublish, true, false);
    previewPlugin.getExecutionProgress().setTotalDatabaseRecords(100);
    lastPublishPlugin.getExecutionProgress().setTotalDatabaseRecords(100);
    lastPublishPlugin.setFinishedDate(longEnoughToBeValidDate);
    enableRunningPublish = false;
    getDatasetExecutionInformation(previewPlugin, lastPublishPlugin, enableRunningPublish, true, true);
    previewPlugin.getExecutionProgress().setTotalDatabaseRecords(0);
    lastPublishPlugin.getExecutionProgress().setTotalDatabaseRecords(0);
    getDatasetExecutionInformation(previewPlugin, lastPublishPlugin, enableRunningPublish, false, false);
  }

  private void getDatasetExecutionInformation(
      AbstractExecutablePlugin<IndexToPreviewPluginMetadata> previewPlugin,
      AbstractExecutablePlugin<IndexToPublishPluginMetadata> lastPublishPlugin, boolean enableRunningPublish,
      boolean previewReadyForViewing,
      boolean publishReadyForViewing) throws GenericMetisException {
    ExecutionProgress executionProgress = getExecutionProgress(100, 20);

    // Create a harvest plugin.
    AbstractExecutablePlugin<?> oaipmhHarvestPlugin = ExecutablePluginFactory
        .createPlugin(new OaipmhHarvestPluginMetadata());
    oaipmhHarvestPlugin.setFinishedDate(
        DateUtils.modifyDateByTimeUnitAmount(new Date(), -(SOLR_COMMIT_PERIOD_IN_MINUTES + 5),
            TimeUnit.MINUTES));
    oaipmhHarvestPlugin.setDataStatus(null); // Is default status, means valid.
    oaipmhHarvestPlugin.setExecutionProgress(executionProgress);

    // Create first publish plugin
    AbstractExecutablePlugin<?> firstPublishPlugin = ExecutablePluginFactory
        .createPlugin(new IndexToPublishPluginMetadata());
    firstPublishPlugin.setFinishedDate(
        DateUtils.modifyDateByTimeUnitAmount(new Date(), -(SOLR_COMMIT_PERIOD_IN_MINUTES + 4),
            TimeUnit.MINUTES));
    firstPublishPlugin.setDataStatus(null); // Is default status, means valid.
    firstPublishPlugin.setExecutionProgress(executionProgress);
    final WorkflowExecution executionWithFirstPublishPlugin = TestObjectFactory
        .createWorkflowExecutionObject();
    final List<AbstractMetisPlugin<?>> metisPluginsFirstPublish = executionWithFirstPublishPlugin
        .getMetisPlugins();
    metisPluginsFirstPublish.add(firstPublishPlugin);
    executionWithFirstPublishPlugin.setMetisPlugins(metisPluginsFirstPublish);

    final WorkflowExecution executionWithLastPublishPlugin = TestObjectFactory
        .createWorkflowExecutionObject();
    final List<AbstractMetisPlugin<?>> metisPluginsLastPublish = executionWithLastPublishPlugin
        .getMetisPlugins();
    metisPluginsLastPublish.add(lastPublishPlugin);
    executionWithLastPublishPlugin.setMetisPlugins(metisPluginsLastPublish);

    // Create reindex to preview plugin
    AbstractMetisPlugin<?> reindexToPreviewPlugin = new ReindexToPreviewPlugin(
        new ReindexToPreviewPluginMetadata());
    reindexToPreviewPlugin.setFinishedDate(
        DateUtils.modifyDateByTimeUnitAmount(new Date(), -(SOLR_COMMIT_PERIOD_IN_MINUTES + 1),
            TimeUnit.MINUTES));
    final WorkflowExecution executionWithReindexToPreview = TestObjectFactory
        .createWorkflowExecutionObject();
    executionWithReindexToPreview.setMetisPlugins(List.of(reindexToPreviewPlugin));

    // Create execution in progress with a publish plugin
    final WorkflowExecution workflowExecutionObject = TestObjectFactory
        .createWorkflowExecutionObject();
    workflowExecutionObject.setWorkflowStatus(WorkflowStatus.RUNNING);
    final List<AbstractMetisPlugin<?>> metisPlugins = workflowExecutionObject.getMetisPlugins();
    final AbstractExecutablePlugin<?> cleaningPublishPlugin = ExecutablePluginFactory
        .createPlugin(new IndexToPublishPluginMetadata());
    cleaningPublishPlugin.setPluginStatus(PluginStatus.CLEANING);
    metisPlugins.add(cleaningPublishPlugin);
    workflowExecutionObject.setMetisPlugins(metisPlugins);

    // Mock the workflow execution
    final String datasetId = Integer.toString(TestObjectFactory.DATASETID);
    when(workflowExecutionDao.getLatestSuccessfulExecutablePlugin(datasetId,
        EnumSet.of(ExecutablePluginType.HTTP_HARVEST, ExecutablePluginType.OAIPMH_HARVEST), false))
        .thenReturn(new PluginWithExecutionId<>("", oaipmhHarvestPlugin));
    when(workflowExecutionDao.getFirstSuccessfulPlugin(datasetId,
        EnumSet.of(PluginType.PUBLISH, PluginType.REINDEX_TO_PUBLISH)))
        .thenReturn(new PluginWithExecutionId<>(
            executionWithFirstPublishPlugin.getId().toString(), firstPublishPlugin));
    when(workflowExecutionDao.getLatestSuccessfulExecutablePlugin(datasetId,
        EnumSet.of(ExecutablePluginType.PREVIEW), false))
        .thenReturn(new PluginWithExecutionId<>("", previewPlugin));
    when(workflowExecutionDao.getLatestSuccessfulPlugin(datasetId,
        EnumSet.of(PluginType.PREVIEW, PluginType.REINDEX_TO_PREVIEW)))
        .thenReturn(new PluginWithExecutionId<>(executionWithReindexToPreview.getId().toString(), reindexToPreviewPlugin));
    when(workflowExecutionDao.getLatestSuccessfulExecutablePlugin(datasetId,
        EnumSet.of(ExecutablePluginType.PUBLISH), false))
        .thenReturn(new PluginWithExecutionId<>("", lastPublishPlugin));
    when(workflowExecutionDao.getLatestSuccessfulPlugin(datasetId,
        EnumSet.of(PluginType.PUBLISH, PluginType.REINDEX_TO_PUBLISH))).thenReturn(
        new PluginWithExecutionId<>(executionWithLastPublishPlugin.getId().toString(), lastPublishPlugin));
    if (enableRunningPublish) {
      when(workflowExecutionDao.getRunningOrInQueueExecution(datasetId)).thenReturn(workflowExecutionObject);
    } else {
      when(workflowExecutionDao.getRunningOrInQueueExecution(datasetId)).thenReturn(null);
    }

    DatasetExecutionInformation executionInfo = orchestratorService.getDatasetExecutionInformation(datasetId);

    assertEquals(oaipmhHarvestPlugin.getFinishedDate(), executionInfo.getLastHarvestedDate());
    assertEquals(reindexToPreviewPlugin.getFinishedDate(), executionInfo.getLastPreviewDate());
    assertEquals(firstPublishPlugin.getFinishedDate(), executionInfo.getFirstPublishedDate());
    assertEquals(lastPublishPlugin.getFinishedDate(), executionInfo.getLastPublishedDate());

    assertEquals(
        oaipmhHarvestPlugin.getExecutionProgress().getProcessedRecords() - oaipmhHarvestPlugin.getExecutionProgress().getFailRecords(),
        executionInfo.getLastHarvestedRecords());
    assertEquals(previewPlugin.getExecutionProgress().getProcessedRecords() - previewPlugin.getExecutionProgress().getFailRecords(),
        executionInfo.getLastPreviewRecords());
    assertEquals(
        lastPublishPlugin.getExecutionProgress().getProcessedRecords() - lastPublishPlugin.getExecutionProgress().getFailRecords(),
        executionInfo.getLastPublishedRecords());

    assertEquals(previewReadyForViewing, executionInfo.isLastPreviewRecordsReadyForViewing());
    assertEquals(publishReadyForViewing, executionInfo.isLastPublishedRecordsReadyForViewing());
  }

  @NotNull
  private ExecutionProgress getExecutionProgress(int processedRecords, int errors) {
    // Create execution progress object
    ExecutionProgress executionProgress = new ExecutionProgress();
    executionProgress.setProcessedRecords(processedRecords);
    executionProgress.setFailRecords(errors);
    return executionProgress;
  }

  @Test
  void testGetDatasetExecutionHistory() throws GenericMetisException {

    // Create plugins
    final AbstractExecutablePlugin<HTTPHarvestPluginMetadata> plugin1 = mock(AbstractExecutablePlugin.class);
    when(plugin1.getFinishedDate()).thenReturn(new Date());
    when(plugin1.getPluginType()).thenReturn(PluginType.OAIPMH_HARVEST);
    when(plugin1.getPluginMetadata()).thenReturn(new HTTPHarvestPluginMetadata());
    final ExecutionProgress progress1 = getExecutionProgress(10, 1);
    when(plugin1.getExecutionProgress()).thenReturn(progress1);
    final AbstractExecutablePlugin<TransformationPluginMetadata> plugin2 = mock(AbstractExecutablePlugin.class);
    final ExecutionProgress progress2 = new ExecutionProgress();
    when(plugin2.getPluginType()).thenReturn(PluginType.TRANSFORMATION);
    when(plugin2.getPluginMetadata()).thenReturn(new TransformationPluginMetadata());
    progress2.setProcessedRecords(10);
    progress2.setFailRecords(10);
    when(plugin2.getExecutionProgress()).thenReturn(progress2);
    final AbstractExecutablePlugin<MediaProcessPluginMetadata> plugin3 = mock(AbstractExecutablePlugin.class);
    when(plugin3.getPluginType()).thenReturn(PluginType.MEDIA_PROCESS);
    MediaProcessPluginMetadata mediaProcessPluginMetadata = new MediaProcessPluginMetadata();
    mediaProcessPluginMetadata.setRevisionNamePreviousPlugin(plugin1.getPluginType().name());
    mediaProcessPluginMetadata.setRevisionTimestampPreviousPlugin(plugin1.getFinishedDate());
    when(plugin3.getPluginMetadata()).thenReturn(mediaProcessPluginMetadata);
    when(plugin3.getExecutionProgress()).thenReturn(getExecutionProgress(0, 0));
    final ReindexToPreviewPlugin plugin4 = mock(ReindexToPreviewPlugin.class);
    when(plugin4.getPluginType()).thenReturn(PluginType.REINDEX_TO_PUBLISH);
    ReindexToPreviewPluginMetadata reindexToPreviewPluginMetadata = new ReindexToPreviewPluginMetadata();
    reindexToPreviewPluginMetadata.setRevisionNamePreviousPlugin(plugin3.getId());
    reindexToPreviewPluginMetadata.setRevisionTimestampPreviousPlugin(plugin3.getFinishedDate());
    when(plugin4.getPluginMetadata()).thenReturn(new ReindexToPreviewPluginMetadata());
    when(plugin4.getFinishedDate()).thenReturn(new Date(4));

    // Create other objects
    final String datasetId = "dataset ID";
    final WorkflowExecution execution1 = createWorkflowExecution(datasetId, plugin1,
        plugin2);
    execution1.setStartedDate(new Date(12345));
    final WorkflowExecution execution2 = createWorkflowExecution(datasetId, plugin3);
    final WorkflowExecution execution3 = createWorkflowExecution(datasetId, plugin4);

    // Mock the dao and call the method.
    when(workflowExecutionDao.getByTaskExecution(any(), any())).thenReturn(execution1);
    doReturn(new ResultList<>(List.of(execution1, execution2, execution3), false))
        .when(workflowExecutionDao).getAllWorkflowExecutions(any(), any(), any(), anyBoolean(),
            anyInt(), any(), anyBoolean());
    final ExecutionHistory result = orchestratorService.getDatasetExecutionHistory(datasetId);

    // Verify the interactions
    verify(workflowExecutionDao, times(1)).getAllWorkflowExecutions(
        eq(Collections.singleton(datasetId)), isNull(), eq(DaoFieldNames.STARTED_DATE), eq(false),
        eq(0), isNull(), eq(false));
    verify(workflowExecutionDao, times(2)).getById(anyString());
    verify(workflowExecutionDao, times(1)).getByTaskExecution(any(), any());
    verifyNoMoreInteractions(workflowExecutionDao);

    // Verify the result
    assertEquals(1, result.getExecutions().size());
    assertEquals(execution1.getId().toString(),
        result.getExecutions().getFirst().getWorkflowExecutionId());
    assertEquals(execution1.getStartedDate(), result.getExecutions().getFirst().getStartedDate());
  }

  @Test
  void testGetExecutablePluginsWithDataAvailability() throws GenericMetisException {

    // Create plugins
    final AbstractExecutablePlugin<HTTPHarvestPluginMetadata> plugin1 = mock(AbstractExecutablePlugin.class);
    when(plugin1.getPluginType()).thenReturn(PluginType.OAIPMH_HARVEST);
    when(plugin1.getPluginMetadata()).thenReturn(new HTTPHarvestPluginMetadata());
    final ExecutionProgress progress1 = getExecutionProgress(10, 1);
    when(plugin1.getExecutionProgress()).thenReturn(progress1);
    final AbstractExecutablePlugin<TransformationPluginMetadata> plugin2 = mock(AbstractExecutablePlugin.class);
    final ExecutionProgress progress2 = new ExecutionProgress();
    when(plugin2.getPluginType()).thenReturn(PluginType.TRANSFORMATION);
    when(plugin2.getPluginMetadata()).thenReturn(new TransformationPluginMetadata());
    progress2.setProcessedRecords(10);
    progress2.setFailRecords(10);
    when(plugin2.getExecutionProgress()).thenReturn(progress2);
    final AbstractExecutablePlugin<MediaProcessPluginMetadata> plugin3 = mock(AbstractExecutablePlugin.class);
    when(plugin3.getPluginType()).thenReturn(PluginType.MEDIA_PROCESS);
    when(plugin3.getPluginMetadata()).thenReturn(new MediaProcessPluginMetadata());
    when(plugin3.getExecutionProgress()).thenReturn(getExecutionProgress(0, 0));
    final ReindexToPreviewPlugin plugin4 = mock(ReindexToPreviewPlugin.class);
    when(plugin4.getPluginType()).thenReturn(PluginType.REINDEX_TO_PUBLISH);
    when(plugin4.getFinishedDate()).thenReturn(new Date(4));

    // Create other objects
    final String datasetId = "dataset ID";
    final WorkflowExecution execution = createWorkflowExecution(datasetId, plugin1,
        plugin2, plugin3, plugin4);
    final String workflowExecutionId = execution.getId().toString();

    // Test happy flow
    final PluginsWithDataAvailability result = orchestratorService
        .getExecutablePluginsWithDataAvailability(workflowExecutionId);
    assertNotNull(result);
    assertNotNull(result.getPlugins());
    assertEquals(1, result.getPlugins().size());
    assertEquals(plugin1.getPluginType(), result.getPlugins().getFirst().getPluginType());
    assertTrue(result.getPlugins().getFirst().isCanDisplayRawXml());

    // Test when the workflow execution does not exist
    doReturn(null).when(workflowExecutionDao).getById(workflowExecutionId);
    assertThrows(NoWorkflowExecutionFoundException.class,
        () -> orchestratorService.getExecutablePluginsWithDataAvailability(workflowExecutionId));
  }

  @Test
  void testGetRecordEvolutionForVersionExceptions() {

    // Create some objects
    final String workflowExecutionId = "workflow execution ID";
    final PluginType pluginType = PluginType.MEDIA_PROCESS;
    final WorkflowExecution workflowExecution = mock(WorkflowExecution.class);

    // Test when the workflow execution does not exist
    when(workflowExecutionDao.getById(workflowExecutionId)).thenReturn(null);
    assertThrows(NoWorkflowExecutionFoundException.class, () -> orchestratorService
        .getRecordEvolutionForVersion(workflowExecutionId, pluginType));

    // Test when the workflow execution does not have a plugin of the right type
    doReturn(workflowExecution).when(workflowExecutionDao).getById(workflowExecutionId);
    when(workflowExecution.getMetisPlugins()).thenReturn(Collections.emptyList());
    assertThrows(NoWorkflowExecutionFoundException.class,
        () -> orchestratorService.getRecordEvolutionForVersion(workflowExecutionId, pluginType));
  }

  @Test
  void testGetRecordEvolutionForVersionHappyFlow() throws GenericMetisException {

    // Create two workflow executions with three plugins and link them together
    final String datasetId = "dataset ID";
    final AbstractExecutablePlugin<?> plugin1 = createMetisPlugin(ExecutablePluginType.OAIPMH_HARVEST, new Date(1));
    final AbstractExecutablePlugin<?> plugin2 = createMetisPlugin(ExecutablePluginType.TRANSFORMATION, new Date(2));
    final AbstractExecutablePlugin<?> plugin3 = createMetisPlugin(ExecutablePluginType.MEDIA_PROCESS, new Date(3));
    final WorkflowExecution execution1 = createWorkflowExecution(datasetId, plugin1);
    final WorkflowExecution execution2 = createWorkflowExecution(datasetId, plugin2, plugin3);

    // Mock the methods in workflow utils.
    final List<Pair<AbstractExecutablePlugin<?>, WorkflowExecution>> evolutionWithContent = Arrays.asList(
        ImmutablePair.of(plugin1, execution1), ImmutablePair.of(plugin2, execution2));
    doReturn(evolutionWithContent).when(dataEvolutionUtils).compileVersionEvolution(plugin3, execution2);
    doReturn(new ArrayList<>()).when(dataEvolutionUtils).compileVersionEvolution(plugin1, execution1);

    // Execute the call and expect an evolution with content.
    final VersionEvolution resultForThree = orchestratorService.getRecordEvolutionForVersion(execution2.getId().toString(),
        plugin3.getPluginType());
    assertNotNull(resultForThree);
    assertNotNull(resultForThree.getEvolutionSteps());
    assertEquals(2, resultForThree.getEvolutionSteps().size());
    assertEvolutionStepEquals(resultForThree.getEvolutionSteps().get(0), execution1, plugin1);
    assertEvolutionStepEquals(resultForThree.getEvolutionSteps().get(1), execution2, plugin2);

    // Execute the call and expect an evolution without content.
    final VersionEvolution resultForOne = orchestratorService.getRecordEvolutionForVersion(
        execution1.getId().toString(), plugin1.getPluginType());
    assertNotNull(resultForOne);
    assertNotNull(resultForOne.getEvolutionSteps());
    assertTrue(resultForOne.getEvolutionSteps().isEmpty());
  }

  private void assertEvolutionStepEquals(VersionEvolutionStep evolutionStep,
      WorkflowExecution execution, AbstractExecutablePlugin<?> plugin) {
    assertNotNull(evolutionStep);
    assertEquals(plugin.getFinishedDate(), evolutionStep.getFinishedTime());
    assertEquals(plugin.getPluginMetadata().getExecutablePluginType(), evolutionStep.getPluginType());
    assertEquals(execution.getId().toString(), evolutionStep.getWorkflowExecutionId());
  }

  private WorkflowExecution createWorkflowExecution(String datasetId, AbstractMetisPlugin<?>... plugins)
      throws GenericMetisException {
    final WorkflowExecution workflowExecution = new WorkflowExecution();
    workflowExecution.setId(new ObjectId());
    workflowExecution.setDatasetId(datasetId);
    workflowExecution.setMetisPlugins(Arrays.asList(plugins));
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    dataset.setDatasetId(datasetId);
    when(workflowExecutionDao.getById(workflowExecution.getId().toString())).thenReturn(workflowExecution);
    when(datasetDao.getDatasetOrThrow(dataset.getDatasetId())).thenReturn(dataset);
    return workflowExecution;
  }

  private AbstractExecutablePlugin<?> createMetisPlugin(ExecutablePluginType type, Date date) {
    AbstractExecutablePlugin<AbstractExecutablePluginMetadata> result = mock(AbstractExecutablePlugin.class);
    AbstractExecutablePluginMetadata metadata = mock(AbstractExecutablePluginMetadata.class);
    when(metadata.getExecutablePluginType()).thenReturn(type);
    when(result.getPluginType()).thenReturn(type.toPluginType());
    when(result.getPluginMetadata()).thenReturn(metadata);
    when(result.getFinishedDate()).thenReturn(date);
    return result;
  }
}
