package eu.europeana.metis.core.execution;

import static eu.europeana.metis.core.engine.base.EngineTaskKey.DEPUBLICATION_REASON;
import static eu.europeana.metis.core.engine.base.EngineTaskKey.MAXIMUM_PARALLELIZATION;
import static eu.europeana.metis.core.workflow.plugins.ExecutablePluginType.DEPUBLISH;
import static eu.europeana.metis.core.workflow.plugins.ExecutablePluginType.HTTP_HARVEST;
import static eu.europeana.metis.core.workflow.plugins.ExecutablePluginType.PREVIEW;
import static eu.europeana.metis.core.workflow.plugins.ExecutablePluginType.VALIDATION_EXTERNAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.of;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import eu.europeana.metis.core.dao.DatasetXsltDao;
import eu.europeana.metis.core.engine.base.DataRevision;
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
import java.util.Date;
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

  @Mock
  private EngineTaskClient<EngineTaskSettings, EngineTask> engineTaskClient;
  @Mock
  private DatasetXsltDao datasetXsltDao;

  @Mock
  private EngineTask engineTask;

  @Mock
  private EngineTaskSettings engineTaskSettings;
  private static final String TASK_ID = "taskId";
  private static final String DATASET_ID = "datasetId";
  private static final String ENGINE_DATASET_ID = "engineDatasetId";
  private static final String PREVIOUS_TASK_ID = "previousTaskId";
  private static final String HARVEST_URL = "http://harvest.url";

  @BeforeEach
  void setUp() {
    lenient().when(engineTaskClient.getEngineTaskSettings()).thenReturn(engineTaskSettings);
    lenient().when(engineTaskSettings.getProvider()).thenReturn("provider");
    lenient().when(engineTaskSettings.getBaseUrl()).thenReturn("http://base.url");
    lenient().when(engineTaskSettings.getThrottlingValues()).thenReturn(new ThrottlingValues(1, 2, 3));
  }

  @ParameterizedTest
  @MethodSource("pluginArguments")
  void testSubmit_InternalPlugins(T plugin, M metadata, PluginType previousPlugin, ThrottlingLevel throttlingLevel)
      throws ExternalTaskException {
    setupPlugin(plugin, metadata, previousPlugin, throttlingLevel);

    EngineTaskSubmitter<EngineTaskSettings, EngineTask> engineTaskSubmitter =
        new EngineTaskSubmitter<>(plugin, engineTaskClient, datasetXsltDao);

    ArgumentCaptor<Map<EngineTaskKey, String>> propertiesCaptor = ArgumentCaptor.forClass(Map.class);
    ArgumentCaptor<InputDataEndpoint> inputDataCaptor = ArgumentCaptor.forClass(InputDataEndpoint.class);
    ArgumentCaptor<DataRevision> dataRevisionCaptor = ArgumentCaptor.forClass(DataRevision.class);

    when(engineTaskClient.createEngineTask(propertiesCaptor.capture(), inputDataCaptor.capture(),
        dataRevisionCaptor.capture())).thenReturn(engineTask);
    when(engineTaskClient.submitEngineTask(eq(engineTask), anyString())).thenReturn(TASK_ID);

    engineTaskSubmitter.submit(DATASET_ID, ENGINE_DATASET_ID, PREVIOUS_TASK_ID);

    assertTaskSubmission(plugin, propertiesCaptor, inputDataCaptor, dataRevisionCaptor, throttlingLevel);
  }

  @Test
  void testSubmit_InvalidHarvestPlugin() {
    ValidationExternalPlugin validationExternalPlugin = new ValidationExternalPlugin();
    ValidationExternalPluginMetadata validationExternalPluginMetadata = spy(ValidationExternalPluginMetadata.class);
    validationExternalPlugin.setPluginMetadata(validationExternalPluginMetadata);
    validationExternalPlugin.setStartedDate(new Date());
    when(validationExternalPluginMetadata.getExecutablePluginType()).thenReturn(HTTP_HARVEST);

    EngineTaskSubmitter<EngineTaskSettings, EngineTask> engineTaskSubmitter = new EngineTaskSubmitter<>(validationExternalPlugin,
        engineTaskClient, datasetXsltDao);
    assertThrows(IllegalStateException.class, () -> engineTaskSubmitter.submit(DATASET_ID, ENGINE_DATASET_ID, PREVIOUS_TASK_ID));
  }

  @Test
  void testSubmit_InvalidProcessPlugin() {
    IndexToPreviewPlugin indexToPreviewPlugin = new IndexToPreviewPlugin();
    IndexToPreviewPluginMetadata indexToPreviewPluginMetadata = spy(IndexToPreviewPluginMetadata.class);
    indexToPreviewPlugin.setPluginMetadata(indexToPreviewPluginMetadata);
    indexToPreviewPlugin.setStartedDate(new Date());
    when(indexToPreviewPluginMetadata.getExecutablePluginType()).thenReturn(VALIDATION_EXTERNAL);
    when(indexToPreviewPluginMetadata.getRevisionNamePreviousPlugin()).thenReturn(VALIDATION_EXTERNAL.name());

    EngineTaskSubmitter<EngineTaskSettings, EngineTask> engineTaskSubmitter =
        new EngineTaskSubmitter<>(indexToPreviewPlugin, engineTaskClient, datasetXsltDao);
    assertThrows(IllegalStateException.class, () -> engineTaskSubmitter.submit(DATASET_ID, ENGINE_DATASET_ID, PREVIOUS_TASK_ID));
  }

  @Test
  void testSubmit_InvalidIndexPlugin() {
    OaipmhHarvestPlugin indexToPreviewPlugin = new OaipmhHarvestPlugin();
    OaipmhHarvestPluginMetadata oaipmhHarvestPluginMetadata = spy(OaipmhHarvestPluginMetadata.class);
    indexToPreviewPlugin.setPluginMetadata(oaipmhHarvestPluginMetadata);
    indexToPreviewPlugin.setStartedDate(new Date());
    when(oaipmhHarvestPluginMetadata.getExecutablePluginType()).thenReturn(PREVIEW);
    when(oaipmhHarvestPluginMetadata.getRevisionNamePreviousPlugin()).thenReturn(PREVIEW.name());

    EngineTaskSubmitter<EngineTaskSettings, EngineTask> engineTaskSubmitter =
        new EngineTaskSubmitter<>(indexToPreviewPlugin, engineTaskClient, datasetXsltDao);
    assertThrows(IllegalStateException.class, () -> engineTaskSubmitter.submit(DATASET_ID, ENGINE_DATASET_ID, PREVIOUS_TASK_ID));
  }

  private static Stream<Arguments> pluginArguments() {
    return Stream.of(
        arguments(new OaipmhHarvestPlugin(), new OaipmhHarvestPluginMetadata(), null),
        arguments(new HTTPHarvestPlugin(), new HTTPHarvestPluginMetadata(), null),
        arguments(new TransformationExternalPlugin(), new TransformationExternalPluginMetadata(), PluginType.OAIPMH_HARVEST),
        arguments(new ValidationExternalPlugin(), new ValidationExternalPluginMetadata(), PluginType.OAIPMH_HARVEST),
        arguments(new TransformationPlugin(), new TransformationPluginMetadata(), PluginType.VALIDATION_EXTERNAL),
        arguments(new ValidationInternalPlugin(), new ValidationInternalPluginMetadata(), PluginType.TRANSFORMATION),
        arguments(new NormalizationPlugin(), new NormalizationPluginMetadata(), PluginType.VALIDATION_INTERNAL),
        arguments(new EnrichmentPlugin(), new EnrichmentPluginMetadata(), PluginType.NORMALIZATION),
        arguments(new MediaProcessPlugin(), new MediaProcessPluginMetadata(), PluginType.ENRICHMENT),
        of(new MediaProcessPlugin(), new MediaProcessPluginMetadata(), PluginType.ENRICHMENT, ThrottlingLevel.STRONG),
        arguments(new LinkCheckingPlugin(), new LinkCheckingPluginMetadata(), PluginType.MEDIA_PROCESS),
        arguments(new IndexToPreviewPlugin(), new IndexToPreviewPluginMetadata(), PluginType.MEDIA_PROCESS),
        arguments(new IndexToPublishPlugin(), new IndexToPublishPluginMetadata(), PluginType.MEDIA_PROCESS)
    );
  }

  private T setupPlugin(T plugin, M metadata, PluginType previousType, ThrottlingLevel throttlingLevel) {
    if (metadata instanceof OaipmhHarvestPluginMetadata pluginMetadata) {
      pluginMetadata.setUrl(HARVEST_URL);
    } else if (metadata instanceof HTTPHarvestPluginMetadata pluginMetadata) {
      pluginMetadata.setUrl(HARVEST_URL);
    } else if (metadata instanceof MediaProcessPluginMetadata mediaProcessPluginMetadata) {
      mediaProcessPluginMetadata.setThrottlingLevel(throttlingLevel);
    } else if (metadata instanceof IndexToPreviewPluginMetadata pluginMetadata) {
      pluginMetadata.setHarvestDate(new Date());
    } else if (metadata instanceof IndexToPublishPluginMetadata pluginMetadata) {
      pluginMetadata.setHarvestDate(new Date());
    }

    if (!(metadata instanceof AbstractHarvestPluginMetadata)) {
      metadata.setRevisionNamePreviousPlugin(previousType.name());
      metadata.setRevisionTimestampPreviousPlugin(new Date());
    }
    plugin.setPluginMetadata(metadata);
    plugin.setStartedDate(new Date());
    return plugin;
  }

  private void assertTaskSubmission(T plugin, ArgumentCaptor<Map<EngineTaskKey, String>> propertiesCaptor,
      ArgumentCaptor<InputDataEndpoint> inputDataCaptor,
      ArgumentCaptor<DataRevision> dataRevisionCaptor, ThrottlingLevel throttlingLevel) {
    assertEquals(plugin.getPluginType().name(), dataRevisionCaptor.getValue().name());
    assertEquals(engineTaskSettings.getProvider(), dataRevisionCaptor.getValue().providerId());
    assertEquals(plugin.getStartedDate(), dataRevisionCaptor.getValue().creationTimeStamp());

    if (inputDataCaptor.getValue() instanceof HttpHarvestInputDataEndpoint ||
        inputDataCaptor.getValue() instanceof OaiHarvestInputDataEndpoint) {
      assertEquals(HARVEST_URL, inputDataCaptor.getValue().url());
    }
    assertEquals(TASK_ID, plugin.getExternalTaskId());
    assertEquals(DataStatus.VALID, plugin.getDataStatus());
    if (throttlingLevel != null) {
      String expectedParallelization = String.valueOf(
          engineTaskSettings.getThrottlingValues().getThreadNumberFromThrottlingLevel(throttlingLevel));
      assertEquals(expectedParallelization, propertiesCaptor.getValue().get(MAXIMUM_PARALLELIZATION));
    }
  }

  private static Arguments arguments(Object plugin, Object metadata, PluginType type) {
    return of(plugin, metadata, type, null);
  }

  @Test
  void testSubmit_DepublishPlugin_Dataset() throws ExternalTaskException {
    DepublishPlugin depublishPlugin = new DepublishPlugin();
    DepublishPluginMetadata depublishPluginMetadata = new DepublishPluginMetadata();
    depublishPluginMetadata.setDatasetDepublish(true);
    depublishPlugin.setPluginMetadata(depublishPluginMetadata);
    depublishPlugin.setStartedDate(new Date());
    EngineTaskSubmitter<EngineTaskSettings, EngineTask> engineTaskSubmitter =
        new EngineTaskSubmitter<>(depublishPlugin, engineTaskClient, datasetXsltDao);

    ArgumentCaptor<InputDataEndpoint> inputDataEndpointCaptor = ArgumentCaptor.forClass(InputDataEndpoint.class);
    ArgumentCaptor<DataRevision> dataRevisionCaptor = ArgumentCaptor.forClass(DataRevision.class);

    when(engineTaskClient.createEngineTask(anyMap(), inputDataEndpointCaptor.capture(), dataRevisionCaptor.capture()))
        .thenReturn(engineTask);

    when(engineTaskClient.submitEngineTask(eq(engineTask), anyString())).thenReturn(TASK_ID);
    engineTaskSubmitter.submit(DATASET_ID, ENGINE_DATASET_ID, PREVIOUS_TASK_ID);

    InputDataEndpoint inputDataEndpoint = inputDataEndpointCaptor.getValue();
    assertInstanceOf(DepublishInputDataEndpoint.class, inputDataEndpoint);
    assertEquals(depublishPlugin.getPluginType().name(), dataRevisionCaptor.getValue().name());
    assertEquals(engineTaskSettings.getProvider(), dataRevisionCaptor.getValue().providerId());
    assertEquals(depublishPlugin.getStartedDate(), dataRevisionCaptor.getValue().creationTimeStamp());
    assertEquals(TASK_ID, depublishPlugin.getExternalTaskId());
    assertEquals(DataStatus.VALID, depublishPlugin.getDataStatus());
  }

  @Test
  void testSubmit_DepublishPlugin_Dataset_WithReason() throws ExternalTaskException {
    DepublishPlugin depublishPlugin = new DepublishPlugin();
    DepublishPluginMetadata depublishPluginMetadata = new DepublishPluginMetadata();
    depublishPluginMetadata.setDatasetDepublish(true);
    depublishPluginMetadata.setDepublicationReason(DepublicationReason.GDPR);
    depublishPlugin.setPluginMetadata(depublishPluginMetadata);
    depublishPlugin.setStartedDate(new Date());
    EngineTaskSubmitter<EngineTaskSettings, EngineTask> engineTaskSubmitter =
        new EngineTaskSubmitter<>(depublishPlugin, engineTaskClient, datasetXsltDao);

    ArgumentCaptor<Map<EngineTaskKey, String>> properties = ArgumentCaptor.forClass(Map.class);
    ArgumentCaptor<InputDataEndpoint> inputDataEndpointCaptor = ArgumentCaptor.forClass(InputDataEndpoint.class);
    ArgumentCaptor<DataRevision> dataRevisionCaptor = ArgumentCaptor.forClass(DataRevision.class);

    when(engineTaskClient.createEngineTask(properties.capture(), inputDataEndpointCaptor.capture(), dataRevisionCaptor.capture()))
        .thenReturn(engineTask);

    when(engineTaskClient.submitEngineTask(eq(engineTask), anyString())).thenReturn(TASK_ID);
    engineTaskSubmitter.submit(DATASET_ID, ENGINE_DATASET_ID, PREVIOUS_TASK_ID);

    assertEquals(depublishPluginMetadata.getDepublicationReason().name(), properties.getValue().get(DEPUBLICATION_REASON));
    InputDataEndpoint inputDataEndpoint = inputDataEndpointCaptor.getValue();
    assertInstanceOf(DepublishInputDataEndpoint.class, inputDataEndpoint);
    assertEquals(depublishPlugin.getPluginType().name(), dataRevisionCaptor.getValue().name());
    assertEquals(engineTaskSettings.getProvider(), dataRevisionCaptor.getValue().providerId());
    assertEquals(depublishPlugin.getStartedDate(), dataRevisionCaptor.getValue().creationTimeStamp());
    assertEquals(TASK_ID, depublishPlugin.getExternalTaskId());
    assertEquals(DataStatus.VALID, depublishPlugin.getDataStatus());
  }

  @Test
  void testSubmit_DepublishPlugin_Records() throws ExternalTaskException {
    DepublishPlugin depublishPlugin = new DepublishPlugin();
    DepublishPluginMetadata depublishPluginMetadata = new DepublishPluginMetadata();
    depublishPluginMetadata.setDatasetDepublish(false);
    depublishPluginMetadata.setRecordIdsToDepublish(Set.of("RecordId1", "RecordId2"));
    depublishPlugin.setPluginMetadata(depublishPluginMetadata);
    depublishPlugin.setStartedDate(new Date());
    EngineTaskSubmitter<EngineTaskSettings, EngineTask> engineTaskSubmitter =
        new EngineTaskSubmitter<>(depublishPlugin, engineTaskClient, datasetXsltDao);

    ArgumentCaptor<InputDataEndpoint> inputDataEndpointCaptor = ArgumentCaptor.forClass(InputDataEndpoint.class);
    ArgumentCaptor<DataRevision> dataRevisionCaptor = ArgumentCaptor.forClass(DataRevision.class);

    when(engineTaskClient.createEngineTask(anyMap(), inputDataEndpointCaptor.capture(), dataRevisionCaptor.capture()))
        .thenReturn(engineTask);

    when(engineTaskClient.submitEngineTask(eq(engineTask), anyString())).thenReturn(TASK_ID);
    engineTaskSubmitter.submit(DATASET_ID, ENGINE_DATASET_ID, PREVIOUS_TASK_ID);

    InputDataEndpoint inputDataEndpoint = inputDataEndpointCaptor.getValue();
    assertInstanceOf(DepublishInputDataEndpoint.class, inputDataEndpoint);
    assertEquals(depublishPlugin.getPluginType().name(), dataRevisionCaptor.getValue().name());
    assertEquals(engineTaskSettings.getProvider(), dataRevisionCaptor.getValue().providerId());
    assertEquals(depublishPlugin.getStartedDate(), dataRevisionCaptor.getValue().creationTimeStamp());
    assertEquals(TASK_ID, depublishPlugin.getExternalTaskId());
    assertEquals(DataStatus.VALID, depublishPlugin.getDataStatus());
  }

  @Test
  void testSubmit_DepublishPlugin_Records_Invalid() {
    DepublishPlugin depublishPlugin = new DepublishPlugin();
    DepublishPluginMetadata depublishPluginMetadata = new DepublishPluginMetadata();
    depublishPluginMetadata.setDatasetDepublish(false);
    depublishPlugin.setPluginMetadata(depublishPluginMetadata);
    depublishPlugin.setStartedDate(new Date());
    EngineTaskSubmitter<EngineTaskSettings, EngineTask> engineTaskSubmitter =
        new EngineTaskSubmitter<>(depublishPlugin, engineTaskClient, datasetXsltDao);
    assertThrows(IllegalStateException.class, () -> engineTaskSubmitter.submit(DATASET_ID, ENGINE_DATASET_ID, PREVIOUS_TASK_ID),
        "Requested record depublication but there are no records ids for depublication in the db");
  }

  @Test
  void testSubmit_InvalidDepublishPlugin() {
    OaipmhHarvestPlugin indexToPreviewPlugin = new OaipmhHarvestPlugin();
    OaipmhHarvestPluginMetadata oaipmhHarvestPluginMetadata = spy(OaipmhHarvestPluginMetadata.class);
    indexToPreviewPlugin.setPluginMetadata(oaipmhHarvestPluginMetadata);
    indexToPreviewPlugin.setStartedDate(new Date());
    when(oaipmhHarvestPluginMetadata.getExecutablePluginType()).thenReturn(DEPUBLISH);

    EngineTaskSubmitter<EngineTaskSettings, EngineTask> engineTaskSubmitter =
        new EngineTaskSubmitter<>(indexToPreviewPlugin, engineTaskClient, datasetXsltDao);
    assertThrows(IllegalStateException.class, () -> engineTaskSubmitter.submit(DATASET_ID, ENGINE_DATASET_ID, PREVIOUS_TASK_ID));
  }

  @Test
  void testSubmit_Fail() throws ExternalTaskException {
    OaipmhHarvestPlugin oaipmhHarvestPlugin = new OaipmhHarvestPlugin();
    OaipmhHarvestPluginMetadata oaipmhHarvestPluginMetadata = new OaipmhHarvestPluginMetadata();
    oaipmhHarvestPluginMetadata.setUrl(HARVEST_URL);
    oaipmhHarvestPlugin.setPluginMetadata(oaipmhHarvestPluginMetadata);
    oaipmhHarvestPlugin.setStartedDate(new Date());
    EngineTaskSubmitter<EngineTaskSettings, EngineTask> engineTaskSubmitter =
        new EngineTaskSubmitter<>(oaipmhHarvestPlugin, engineTaskClient, datasetXsltDao);

    when(engineTaskClient.createEngineTask(anyMap(), any(InputDataEndpoint.class), any(DataRevision.class)))
        .thenReturn(engineTask);

    when(engineTaskClient.submitEngineTask(eq(engineTask), anyString())).thenThrow(new ExternalTaskException(""));
    assertThrows(ExternalTaskException.class, () -> engineTaskSubmitter.submit(DATASET_ID, ENGINE_DATASET_ID, PREVIOUS_TASK_ID));
  }

}