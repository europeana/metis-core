package eu.europeana.metis.core.execution;

import static eu.europeana.metis.core.engine.base.EngineTaskKey.DEPUBLICATION_REASON;
import static eu.europeana.metis.core.engine.base.EngineTaskKey.MAXIMUM_PARALLELIZATION;
import static eu.europeana.metis.core.workflow.plugins.ExecutablePluginType.DEPUBLISH;
import static eu.europeana.metis.core.workflow.plugins.ExecutablePluginType.HTTP_HARVEST;
import static eu.europeana.metis.core.workflow.plugins.ExecutablePluginType.PREVIEW;
import static eu.europeana.metis.core.workflow.plugins.ExecutablePluginType.VALIDATION_EXTERNAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.of;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europeana.metis.core.dao.DatasetXsltDao;
import eu.europeana.metis.core.engine.base.EngineTask;
import eu.europeana.metis.core.engine.base.EngineTaskClient;
import eu.europeana.metis.core.engine.base.EngineTaskKey;
import eu.europeana.metis.core.engine.base.EngineTaskSettings;
import eu.europeana.metis.core.engine.base.task.input.DepublishInputDataEndpoint;
import eu.europeana.metis.core.engine.base.task.input.HttpHarvestInputDataEndpoint;
import eu.europeana.metis.core.engine.base.task.input.InputDataEndpoint;
import eu.europeana.metis.core.engine.base.task.input.OaiHarvestInputDataEndpoint;
import eu.europeana.metis.core.workflow.plugins.AbstractExecutablePlugin;
import eu.europeana.metis.core.workflow.plugins.AbstractExecutablePluginMetadata;
import eu.europeana.metis.core.workflow.plugins.AbstractHarvestPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.DataStatus;
import eu.europeana.metis.core.workflow.plugins.DepublishPlugin;
import eu.europeana.metis.core.workflow.plugins.DepublishPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.EnrichmentPlugin;
import eu.europeana.metis.core.workflow.plugins.EnrichmentPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.HTTPHarvestPlugin;
import eu.europeana.metis.core.workflow.plugins.HTTPHarvestPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.IndexToPreviewPlugin;
import eu.europeana.metis.core.workflow.plugins.IndexToPreviewPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.IndexToPublishPlugin;
import eu.europeana.metis.core.workflow.plugins.IndexToPublishPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.LinkCheckingPlugin;
import eu.europeana.metis.core.workflow.plugins.LinkCheckingPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.MediaProcessPlugin;
import eu.europeana.metis.core.workflow.plugins.MediaProcessPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.NormalizationPlugin;
import eu.europeana.metis.core.workflow.plugins.NormalizationPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.OaipmhHarvestPlugin;
import eu.europeana.metis.core.workflow.plugins.OaipmhHarvestPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.PluginType;
import eu.europeana.metis.core.workflow.plugins.ThrottlingLevel;
import eu.europeana.metis.core.workflow.plugins.ThrottlingValues;
import eu.europeana.metis.core.workflow.plugins.TransformationExternalPlugin;
import eu.europeana.metis.core.workflow.plugins.TransformationExternalPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.TransformationPlugin;
import eu.europeana.metis.core.workflow.plugins.TransformationPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.ValidationExternalPlugin;
import eu.europeana.metis.core.workflow.plugins.ValidationExternalPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.ValidationInternalPlugin;
import eu.europeana.metis.core.workflow.plugins.ValidationInternalPluginMetadata;
import eu.europeana.metis.exception.ExternalTaskException;
import eu.europeana.metis.utils.DepublicationReason;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TestEngineTaskSubmitter<T extends AbstractExecutablePlugin<M>, M extends AbstractExecutablePluginMetadata> {

  private static final String BATCH_ID = "batchId";
  private static final String CREATED_TASK_ID = "createdTaskId";
  private static final String SUBMITTED_TASK_ID = "submittedTaskId";
  private static final String DATASET_ID = "datasetId";
  private static final String ENGINE_DATASET_ID = "engineDatasetId";
  private static final String PREVIOUS_TASK_ID = "previousTaskId";
  private static final String HARVEST_URL = "http://harvest.url";

  @Mock
  private EngineTaskClient<EngineTaskSettings, EngineTask> engineTaskClient;

  @Mock
  private DatasetXsltDao datasetXsltDao;

  @Mock
  private EngineTask engineTaskRequest;

  @Mock
  private EngineTaskSettings engineTaskSettings;

  @BeforeEach
  void setUp() {
    lenient().when(engineTaskClient.getEngineTaskSettings()).thenReturn(engineTaskSettings);
    lenient().when(engineTaskSettings.getProvider()).thenReturn("provider");
    lenient().when(engineTaskSettings.getBaseUrl()).thenReturn("http://base.url");
    lenient().when(engineTaskSettings.getThrottlingValues()).thenReturn(new ThrottlingValues(1, 2, 3));
    lenient().when(engineTaskRequest.getEngineTaskId()).thenReturn(CREATED_TASK_ID);
    lenient().when(engineTaskRequest.getEngineBatchId()).thenReturn(BATCH_ID);
  }

  @ParameterizedTest
  @MethodSource("pluginArguments")
  void testCreateTask_InternalPlugins(T plugin, M metadata, PluginType previousPlugin, ThrottlingLevel throttlingLevel)
      throws ExternalTaskException {

    setupPlugin(plugin, metadata, previousPlugin, throttlingLevel);

    EngineTaskSubmitter<EngineTaskSettings, EngineTask> engineTaskSubmitter =
        new EngineTaskSubmitter<>(plugin, engineTaskClient, datasetXsltDao);

    ArgumentCaptor<Map<EngineTaskKey, String>> propertiesCaptor =
        ArgumentCaptor.forClass(Map.class);
    ArgumentCaptor<InputDataEndpoint> inputDataCaptor =
        ArgumentCaptor.forClass(InputDataEndpoint.class);

    when(engineTaskClient.createEngineTask(
        propertiesCaptor.capture(),
        inputDataCaptor.capture(),
        anyString()))
        .thenReturn(engineTaskRequest);

    EngineTaskCreationContext engineTaskCreationContext =
        EngineTaskCreationContext.builder()
                                 .datasetId(DATASET_ID)
                                 .engineDatasetId(ENGINE_DATASET_ID)
                                 .sourceExecutionId(PREVIOUS_TASK_ID)
                                 .sourceBatchId(BATCH_ID)
                                 .build();

    EngineTask engineTask = engineTaskSubmitter.createTask(engineTaskCreationContext);

    assertSame(engineTaskRequest, engineTask);
    assertTaskCreation(plugin, propertiesCaptor, inputDataCaptor, throttlingLevel);

    verify(engineTaskClient, never())
        .submitEngineTask(any(EngineTask.class), anyString());
  }

  @Test
  void testCreateTask_InvalidHarvestPlugin() {
    ValidationExternalPlugin validationExternalPlugin =
        new ValidationExternalPlugin();
    ValidationExternalPluginMetadata validationExternalPluginMetadata =
        spy(ValidationExternalPluginMetadata.class);

    validationExternalPlugin.setPluginMetadata(
        validationExternalPluginMetadata);
    validationExternalPlugin.setStartedDate(Instant.now());

    when(validationExternalPluginMetadata.getExecutablePluginType())
        .thenReturn(HTTP_HARVEST);

    EngineTaskSubmitter<EngineTaskSettings, EngineTask> engineTaskSubmitter =
        new EngineTaskSubmitter<>(
            validationExternalPlugin,
            engineTaskClient,
            datasetXsltDao);

    EngineTaskCreationContext engineTaskCreationContext =
        EngineTaskCreationContext.builder()
                                 .datasetId(DATASET_ID)
                                 .engineDatasetId(ENGINE_DATASET_ID)
                                 .sourceExecutionId(PREVIOUS_TASK_ID)
                                 .sourceBatchId(BATCH_ID)
                                 .build();
    assertThrows(IllegalStateException.class, () -> engineTaskSubmitter.createTask(engineTaskCreationContext));
  }

  @Test
  void testCreateTask_InvalidProcessPlugin() {
    IndexToPreviewPlugin indexToPreviewPlugin = new IndexToPreviewPlugin();
    IndexToPreviewPluginMetadata indexToPreviewPluginMetadata =
        spy(IndexToPreviewPluginMetadata.class);

    indexToPreviewPlugin.setPluginMetadata(indexToPreviewPluginMetadata);
    indexToPreviewPlugin.setStartedDate(Instant.now());

    when(indexToPreviewPluginMetadata.getExecutablePluginType())
        .thenReturn(VALIDATION_EXTERNAL);

    EngineTaskSubmitter<EngineTaskSettings, EngineTask> engineTaskSubmitter =
        new EngineTaskSubmitter<>(
            indexToPreviewPlugin,
            engineTaskClient,
            datasetXsltDao);

    EngineTaskCreationContext engineTaskCreationContext =
        EngineTaskCreationContext.builder()
                                 .datasetId(DATASET_ID)
                                 .engineDatasetId(ENGINE_DATASET_ID)
                                 .sourceExecutionId(PREVIOUS_TASK_ID)
                                 .sourceBatchId(BATCH_ID)
                                 .build();

    assertThrows(IllegalStateException.class, () -> engineTaskSubmitter.createTask(engineTaskCreationContext));
  }

  @Test
  void testCreateTask_InvalidIndexPlugin() {
    OaipmhHarvestPlugin oaipmhHarvestPlugin = new OaipmhHarvestPlugin();
    OaipmhHarvestPluginMetadata oaipmhHarvestPluginMetadata =
        spy(OaipmhHarvestPluginMetadata.class);

    oaipmhHarvestPlugin.setPluginMetadata(oaipmhHarvestPluginMetadata);
    oaipmhHarvestPlugin.setStartedDate(Instant.now());

    when(oaipmhHarvestPluginMetadata.getExecutablePluginType())
        .thenReturn(PREVIEW);

    EngineTaskSubmitter<EngineTaskSettings, EngineTask> engineTaskSubmitter =
        new EngineTaskSubmitter<>(
            oaipmhHarvestPlugin,
            engineTaskClient,
            datasetXsltDao);

    EngineTaskCreationContext engineTaskCreationContext =
        EngineTaskCreationContext.builder()
                                 .datasetId(DATASET_ID)
                                 .engineDatasetId(ENGINE_DATASET_ID)
                                 .sourceExecutionId(PREVIOUS_TASK_ID)
                                 .sourceBatchId(BATCH_ID)
                                 .build();

    assertThrows(IllegalStateException.class, () -> engineTaskSubmitter.createTask(engineTaskCreationContext));
  }

  @Test
  void testCreateTask_DepublishPlugin_Dataset()
      throws ExternalTaskException {

    DepublishPlugin depublishPlugin = new DepublishPlugin();
    DepublishPluginMetadata depublishPluginMetadata =
        new DepublishPluginMetadata();

    depublishPluginMetadata.setDatasetDepublish(true);
    depublishPlugin.setPluginMetadata(depublishPluginMetadata);
    depublishPlugin.setStartedDate(Instant.now());

    EngineTaskSubmitter<EngineTaskSettings, EngineTask> engineTaskSubmitter =
        new EngineTaskSubmitter<>(
            depublishPlugin,
            engineTaskClient,
            datasetXsltDao);

    ArgumentCaptor<InputDataEndpoint> inputDataEndpointCaptor =
        ArgumentCaptor.forClass(InputDataEndpoint.class);

    when(engineTaskClient.createEngineTask(
        anyMap(),
        inputDataEndpointCaptor.capture(),
        anyString()))
        .thenReturn(engineTaskRequest);

    EngineTask createdTask = engineTaskSubmitter.createTask(EngineTaskCreationContext.builder()
                                                                                     .datasetId(DATASET_ID)
                                                                                     .engineDatasetId(ENGINE_DATASET_ID)
                                                                                     .sourceExecutionId(PREVIOUS_TASK_ID)
                                                                                     .sourceBatchId(BATCH_ID)
                                                                                     .build());

    assertSame(engineTaskRequest, createdTask);
    assertInstanceOf(
        DepublishInputDataEndpoint.class,
        inputDataEndpointCaptor.getValue());
    assertEquals(CREATED_TASK_ID, depublishPlugin.getEngineTaskId());
    assertEquals(BATCH_ID, depublishPlugin.getEngineBatchId());
    assertEquals(DataStatus.VALID, depublishPlugin.getDataStatus());

    verify(engineTaskClient, never())
        .submitEngineTask(any(EngineTask.class), anyString());
  }

  @Test
  void testCreateTask_DepublishPlugin_Dataset_WithReason()
      throws ExternalTaskException {

    DepublishPlugin depublishPlugin = new DepublishPlugin();
    DepublishPluginMetadata depublishPluginMetadata =
        new DepublishPluginMetadata();

    depublishPluginMetadata.setDatasetDepublish(true);
    depublishPluginMetadata.setDepublicationReason(
        DepublicationReason.GDPR);

    depublishPlugin.setPluginMetadata(depublishPluginMetadata);
    depublishPlugin.setStartedDate(Instant.now());

    EngineTaskSubmitter<EngineTaskSettings, EngineTask> engineTaskSubmitter =
        new EngineTaskSubmitter<>(
            depublishPlugin,
            engineTaskClient,
            datasetXsltDao);

    ArgumentCaptor<Map<EngineTaskKey, String>> propertiesCaptor =
        ArgumentCaptor.forClass(Map.class);
    ArgumentCaptor<InputDataEndpoint> inputDataEndpointCaptor =
        ArgumentCaptor.forClass(InputDataEndpoint.class);

    when(engineTaskClient.createEngineTask(
        propertiesCaptor.capture(),
        inputDataEndpointCaptor.capture(),
        anyString()))
        .thenReturn(engineTaskRequest);

    EngineTask createdTask = engineTaskSubmitter.createTask(EngineTaskCreationContext.builder()
                                                                                     .datasetId(DATASET_ID)
                                                                                     .engineDatasetId(ENGINE_DATASET_ID)
                                                                                     .sourceExecutionId(PREVIOUS_TASK_ID)
                                                                                     .sourceBatchId(BATCH_ID)
                                                                                     .build());

    assertSame(engineTaskRequest, createdTask);
    assertEquals(
        depublishPluginMetadata.getDepublicationReason().name(),
        propertiesCaptor.getValue().get(DEPUBLICATION_REASON));
    assertInstanceOf(
        DepublishInputDataEndpoint.class,
        inputDataEndpointCaptor.getValue());
    assertEquals(CREATED_TASK_ID, depublishPlugin.getEngineTaskId());
    assertEquals(BATCH_ID, depublishPlugin.getEngineBatchId());
    assertEquals(DataStatus.VALID, depublishPlugin.getDataStatus());

    verify(engineTaskClient, never())
        .submitEngineTask(any(EngineTask.class), anyString());
  }

  @Test
  void testCreateTask_DepublishPlugin_Records()
      throws ExternalTaskException {

    DepublishPlugin depublishPlugin = new DepublishPlugin();
    DepublishPluginMetadata depublishPluginMetadata =
        new DepublishPluginMetadata();

    depublishPluginMetadata.setDatasetDepublish(false);
    depublishPluginMetadata.setRecordIdsToDepublish(
        Set.of("RecordId1", "RecordId2"));

    depublishPlugin.setPluginMetadata(depublishPluginMetadata);
    depublishPlugin.setStartedDate(Instant.now());

    EngineTaskSubmitter<EngineTaskSettings, EngineTask> engineTaskSubmitter =
        new EngineTaskSubmitter<>(
            depublishPlugin,
            engineTaskClient,
            datasetXsltDao);

    ArgumentCaptor<InputDataEndpoint> inputDataEndpointCaptor =
        ArgumentCaptor.forClass(InputDataEndpoint.class);

    when(engineTaskClient.createEngineTask(
        anyMap(),
        inputDataEndpointCaptor.capture(),
        anyString()))
        .thenReturn(engineTaskRequest);

    EngineTask createdTask = engineTaskSubmitter.createTask(EngineTaskCreationContext.builder()
                                                                                     .datasetId(DATASET_ID)
                                                                                     .engineDatasetId(ENGINE_DATASET_ID)
                                                                                     .sourceExecutionId(PREVIOUS_TASK_ID)
                                                                                     .sourceBatchId(BATCH_ID)
                                                                                     .build());

    assertSame(engineTaskRequest, createdTask);
    assertInstanceOf(
        DepublishInputDataEndpoint.class,
        inputDataEndpointCaptor.getValue());
    assertEquals(CREATED_TASK_ID, depublishPlugin.getEngineTaskId());
    assertEquals(BATCH_ID, depublishPlugin.getEngineBatchId());
    assertEquals(DataStatus.VALID, depublishPlugin.getDataStatus());

    verify(engineTaskClient, never())
        .submitEngineTask(any(EngineTask.class), anyString());
  }

  @Test
  void testCreateTask_DepublishPlugin_Records_Invalid() {
    DepublishPlugin depublishPlugin = new DepublishPlugin();
    DepublishPluginMetadata depublishPluginMetadata =
        new DepublishPluginMetadata();

    depublishPluginMetadata.setDatasetDepublish(false);
    depublishPlugin.setPluginMetadata(depublishPluginMetadata);
    depublishPlugin.setStartedDate(Instant.now());

    EngineTaskSubmitter<EngineTaskSettings, EngineTask> engineTaskSubmitter =
        new EngineTaskSubmitter<>(depublishPlugin,
            engineTaskClient,
            datasetXsltDao);

    EngineTaskCreationContext engineTaskCreationContext =
        EngineTaskCreationContext.builder()
                                 .datasetId(DATASET_ID)
                                 .engineDatasetId(ENGINE_DATASET_ID)
                                 .sourceExecutionId(PREVIOUS_TASK_ID)
                                 .sourceBatchId(BATCH_ID)
                                 .build();

    assertThrows(IllegalStateException.class, () -> engineTaskSubmitter.createTask(engineTaskCreationContext));
  }

  @Test
  void testCreateTask_InvalidDepublishPlugin() {
    OaipmhHarvestPlugin oaipmhHarvestPlugin =
        new OaipmhHarvestPlugin();
    OaipmhHarvestPluginMetadata oaipmhHarvestPluginMetadata =
        spy(OaipmhHarvestPluginMetadata.class);

    oaipmhHarvestPlugin.setPluginMetadata(oaipmhHarvestPluginMetadata);
    oaipmhHarvestPlugin.setStartedDate(Instant.now());

    when(oaipmhHarvestPluginMetadata.getExecutablePluginType())
        .thenReturn(DEPUBLISH);

    EngineTaskSubmitter<EngineTaskSettings, EngineTask> engineTaskSubmitter =
        new EngineTaskSubmitter<>(
            oaipmhHarvestPlugin,
            engineTaskClient,
            datasetXsltDao);

    EngineTaskCreationContext engineTaskCreationContext =
        EngineTaskCreationContext.builder()
                                 .datasetId(DATASET_ID)
                                 .engineDatasetId(ENGINE_DATASET_ID)
                                 .sourceExecutionId(PREVIOUS_TASK_ID)
                                 .sourceBatchId(BATCH_ID)
                                 .build();

    assertThrows(IllegalStateException.class, () -> engineTaskSubmitter.createTask(engineTaskCreationContext));
  }

  @Test
  void testCreateTask_RuntimeExceptionIsWrapped()
      throws ExternalTaskException {

    OaipmhHarvestPlugin oaipmhHarvestPlugin =
        createOaipmhHarvestPlugin();

    EngineTaskSubmitter<EngineTaskSettings, EngineTask> engineTaskSubmitter =
        new EngineTaskSubmitter<>(
            oaipmhHarvestPlugin,
            engineTaskClient,
            datasetXsltDao);

    RuntimeException runtimeException =
        new RuntimeException("Network error");

    when(engineTaskClient.createEngineTask(
        anyMap(),
        any(InputDataEndpoint.class),
        anyString()))
        .thenThrow(runtimeException);

    ExternalTaskException exception = assertThrows(
        ExternalTaskException.class,
        () -> engineTaskSubmitter.createTask(EngineTaskCreationContext.builder()
                                                                      .datasetId(DATASET_ID)
                                                                      .engineDatasetId(ENGINE_DATASET_ID)
                                                                      .sourceExecutionId(PREVIOUS_TASK_ID)
                                                                      .sourceBatchId(BATCH_ID)
                                                                      .build()));

    assertSame(runtimeException, exception.getCause());
    assertEquals(
        "Create task for plugin type OAIPMH_HARVEST and dataset datasetId failed",
        exception.getMessage());
  }

  @Test
  void testCreateTask_ExternalTaskExceptionIsPropagated()
      throws ExternalTaskException {

    OaipmhHarvestPlugin oaipmhHarvestPlugin =
        createOaipmhHarvestPlugin();

    EngineTaskSubmitter<EngineTaskSettings, EngineTask> engineTaskSubmitter =
        new EngineTaskSubmitter<>(
            oaipmhHarvestPlugin,
            engineTaskClient,
            datasetXsltDao);

    ExternalTaskException externalTaskException =
        new ExternalTaskException("Creation failed");

    when(engineTaskClient.createEngineTask(
        anyMap(),
        any(InputDataEndpoint.class),
        anyString()))
        .thenThrow(externalTaskException);

    ExternalTaskException thrownException = assertThrows(
        ExternalTaskException.class,
        () -> engineTaskSubmitter.createTask(EngineTaskCreationContext.builder()
                                                                      .datasetId(DATASET_ID)
                                                                      .engineDatasetId(ENGINE_DATASET_ID)
                                                                      .sourceExecutionId(PREVIOUS_TASK_ID)
                                                                      .sourceBatchId(BATCH_ID)
                                                                      .build()));

    assertSame(externalTaskException, thrownException);
  }

  @Test
  void testSubmitTask_UpdatesPluginEngineTaskId()
      throws ExternalTaskException {

    OaipmhHarvestPlugin oaipmhHarvestPlugin =
        createOaipmhHarvestPlugin();

    oaipmhHarvestPlugin.setEngineTaskId(CREATED_TASK_ID);

    EngineTaskSubmitter<EngineTaskSettings, EngineTask> engineTaskSubmitter =
        new EngineTaskSubmitter<>(
            oaipmhHarvestPlugin,
            engineTaskClient,
            datasetXsltDao);

    when(engineTaskClient.submitEngineTask(
        eq(engineTaskRequest),
        anyString()))
        .thenReturn(SUBMITTED_TASK_ID);

    engineTaskSubmitter.submitTask(engineTaskRequest);

    assertEquals(
        SUBMITTED_TASK_ID,
        oaipmhHarvestPlugin.getEngineTaskId());

    verify(engineTaskClient).submitEngineTask(
        engineTaskRequest,
        oaipmhHarvestPlugin.getTopologyName());
  }

  @Test
  void testSubmitTask_ExternalTaskExceptionIsPropagated()
      throws ExternalTaskException {

    OaipmhHarvestPlugin oaipmhHarvestPlugin =
        createOaipmhHarvestPlugin();

    EngineTaskSubmitter<EngineTaskSettings, EngineTask> engineTaskSubmitter =
        new EngineTaskSubmitter<>(
            oaipmhHarvestPlugin,
            engineTaskClient,
            datasetXsltDao);

    ExternalTaskException externalTaskException =
        new ExternalTaskException("Submission failed");

    when(engineTaskClient.submitEngineTask(
        eq(engineTaskRequest),
        anyString()))
        .thenThrow(externalTaskException);

    ExternalTaskException thrownException = assertThrows(
        ExternalTaskException.class,
        () -> engineTaskSubmitter.submitTask(engineTaskRequest));

    assertSame(externalTaskException, thrownException);
  }

  @Test
  void testSubmitTask_RuntimeExceptionIsPropagated()
      throws ExternalTaskException {

    OaipmhHarvestPlugin oaipmhHarvestPlugin =
        createOaipmhHarvestPlugin();

    EngineTaskSubmitter<EngineTaskSettings, EngineTask> engineTaskSubmitter =
        new EngineTaskSubmitter<>(
            oaipmhHarvestPlugin,
            engineTaskClient,
            datasetXsltDao);

    RuntimeException runtimeException =
        new RuntimeException("Network error");

    when(engineTaskClient.submitEngineTask(
        eq(engineTaskRequest),
        anyString()))
        .thenThrow(runtimeException);

    RuntimeException thrownException = assertThrows(
        RuntimeException.class,
        () -> engineTaskSubmitter.submitTask(engineTaskRequest));

    assertSame(runtimeException, thrownException);
  }

  private static Stream<Arguments> pluginArguments() {
    return Stream.of(
        arguments(
            new OaipmhHarvestPlugin(),
            new OaipmhHarvestPluginMetadata(),
            null),
        arguments(
            new HTTPHarvestPlugin(),
            new HTTPHarvestPluginMetadata(),
            null),
        arguments(
            new TransformationExternalPlugin(),
            new TransformationExternalPluginMetadata(),
            PluginType.OAIPMH_HARVEST),
        arguments(
            new ValidationExternalPlugin(),
            new ValidationExternalPluginMetadata(),
            PluginType.OAIPMH_HARVEST),
        arguments(
            new TransformationPlugin(),
            new TransformationPluginMetadata(),
            PluginType.VALIDATION_EXTERNAL),
        arguments(
            new ValidationInternalPlugin(),
            new ValidationInternalPluginMetadata(),
            PluginType.TRANSFORMATION),
        arguments(
            new NormalizationPlugin(),
            new NormalizationPluginMetadata(),
            PluginType.VALIDATION_INTERNAL),
        arguments(
            new EnrichmentPlugin(),
            new EnrichmentPluginMetadata(),
            PluginType.NORMALIZATION),
        arguments(
            new MediaProcessPlugin(),
            new MediaProcessPluginMetadata(),
            PluginType.ENRICHMENT),
        of(
            new MediaProcessPlugin(),
            new MediaProcessPluginMetadata(),
            PluginType.ENRICHMENT,
            ThrottlingLevel.STRONG),
        arguments(
            new LinkCheckingPlugin(),
            new LinkCheckingPluginMetadata(),
            PluginType.MEDIA_PROCESS),
        arguments(
            new IndexToPreviewPlugin(),
            new IndexToPreviewPluginMetadata(),
            PluginType.MEDIA_PROCESS),
        arguments(
            new IndexToPublishPlugin(),
            new IndexToPublishPluginMetadata(),
            PluginType.MEDIA_PROCESS)
    );
  }

  private T setupPlugin(
      T plugin,
      M metadata,
      PluginType previousType,
      ThrottlingLevel throttlingLevel) {

    if (metadata instanceof OaipmhHarvestPluginMetadata pluginMetadata) {
      pluginMetadata.setUrl(HARVEST_URL);
    } else if (metadata
        instanceof HTTPHarvestPluginMetadata pluginMetadata) {
      pluginMetadata.setUrl(HARVEST_URL);
    } else if (metadata
        instanceof MediaProcessPluginMetadata pluginMetadata) {
      pluginMetadata.setThrottlingLevel(throttlingLevel);
    } else if (metadata
        instanceof IndexToPreviewPluginMetadata pluginMetadata) {
      pluginMetadata.setHarvestDate(Instant.now());
    } else if (metadata
        instanceof IndexToPublishPluginMetadata pluginMetadata) {
      pluginMetadata.setHarvestDate(Instant.now());
    }

    if (!(metadata instanceof AbstractHarvestPluginMetadata)) {
      metadata.setRevisionNamePreviousPlugin(previousType.name());
      metadata.setRevisionTimestampPreviousPlugin(Instant.now());
    }

    plugin.setPluginMetadata(metadata);
    plugin.setStartedDate(Instant.now());

    return plugin;
  }

  private void assertTaskCreation(
      T plugin,
      ArgumentCaptor<Map<EngineTaskKey, String>> propertiesCaptor,
      ArgumentCaptor<InputDataEndpoint> inputDataCaptor,
      ThrottlingLevel throttlingLevel) {

    assertNotNull(inputDataCaptor.getValue());

    if (inputDataCaptor.getValue()
        instanceof HttpHarvestInputDataEndpoint
        || inputDataCaptor.getValue()
        instanceof OaiHarvestInputDataEndpoint) {
      assertEquals(HARVEST_URL, inputDataCaptor.getValue().url());
    }

    assertEquals(CREATED_TASK_ID, plugin.getEngineTaskId());
    assertEquals(BATCH_ID, plugin.getEngineBatchId());
    assertEquals(DataStatus.VALID, plugin.getDataStatus());

    if (throttlingLevel != null) {
      String expectedParallelization = String.valueOf(
          engineTaskSettings
              .getThrottlingValues()
              .getThreadNumberFromThrottlingLevel(throttlingLevel));

      assertEquals(
          expectedParallelization,
          propertiesCaptor
              .getValue()
              .get(MAXIMUM_PARALLELIZATION));
    }
  }

  private OaipmhHarvestPlugin createOaipmhHarvestPlugin() {
    OaipmhHarvestPlugin plugin = new OaipmhHarvestPlugin();
    OaipmhHarvestPluginMetadata metadata =
        new OaipmhHarvestPluginMetadata();

    metadata.setUrl(HARVEST_URL);
    plugin.setPluginMetadata(metadata);
    plugin.setStartedDate(Instant.now());

    return plugin;
  }

  private static Arguments arguments(
      Object plugin,
      Object metadata,
      PluginType type) {
    return of(plugin, metadata, type, null);
  }
}