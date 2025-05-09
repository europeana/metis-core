package eu.europeana.metis.core.execution;

import static eu.europeana.metis.core.engine.base.EngineTaskKey.DEPUBLICATION_REASON;
import static eu.europeana.metis.core.engine.base.EngineTaskKey.MAXIMUM_PARALLELIZATION;
import static eu.europeana.metis.core.workflow.plugins.ExecutablePluginType.DEPUBLISH;
import static eu.europeana.metis.core.workflow.plugins.ExecutablePluginType.HTTP_HARVEST;
import static eu.europeana.metis.core.workflow.plugins.ExecutablePluginType.PREVIEW;
import static eu.europeana.metis.core.workflow.plugins.ExecutablePluginType.VALIDATION_EXTERNAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import eu.europeana.metis.core.engine.base.DataRevision;
import eu.europeana.metis.core.engine.base.EngineTask;
import eu.europeana.metis.core.engine.base.EngineTaskClient;
import eu.europeana.metis.core.engine.base.EngineTaskSettings;
import eu.europeana.metis.core.engine.base.task.input.HttpHarvestInputDataEndpoint;
import eu.europeana.metis.core.engine.base.task.input.InputDataEndpoint;
import eu.europeana.metis.core.engine.base.task.input.InternalInputDataEndpoint;
import eu.europeana.metis.core.engine.base.task.input.OaiHarvestInputDataEndpoint;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TestPluginExecutor {

  @Mock
  private EngineTaskClient<EngineTaskSettings, EngineTask> engineTaskClient;

  @Mock
  private EngineTask engineTask;

  @Mock
  private EngineTaskSettings engineTaskSettings;

  @BeforeEach
  void setUp() {
    lenient().when(engineTaskClient.getEngineTaskSettings()).thenReturn(engineTaskSettings);
    lenient().when(engineTaskSettings.getProvider()).thenReturn("provider");
    lenient().when(engineTaskSettings.getBaseUrl()).thenReturn("http://base.url");
    lenient().when(engineTaskSettings.getThrottlingValues()).thenReturn(new ThrottlingValues(1, 2, 3));
  }

  @Test
  void testSubmit_OaipmhHarvestPlugin() throws ExternalTaskException {
    OaipmhHarvestPlugin oaipmhHarvestPlugin = new OaipmhHarvestPlugin();
    OaipmhHarvestPluginMetadata oaipmhHarvestPluginMetadata = new OaipmhHarvestPluginMetadata();
    oaipmhHarvestPluginMetadata.setUrl("http://harvest.url");
    oaipmhHarvestPlugin.setPluginMetadata(oaipmhHarvestPluginMetadata);
    oaipmhHarvestPlugin.setStartedDate(new Date());
    PluginExecutor<EngineTaskSettings, EngineTask> pluginExecutor = new PluginExecutor<>(oaipmhHarvestPlugin, engineTaskClient);

    ArgumentCaptor<InputDataEndpoint> inputDataEndpointCaptor = ArgumentCaptor.forClass(InputDataEndpoint.class);

    when(engineTaskClient.createEngineTask(anyMap(), inputDataEndpointCaptor.capture(), any(DataRevision.class)))
        .thenReturn(engineTask);

    when(engineTaskClient.submitEngineTask(eq(engineTask), anyString())).thenReturn("taskId");
    pluginExecutor.submit("datasetId", "previousTaskId");

    InputDataEndpoint inputDataEndpoint = inputDataEndpointCaptor.getValue();
    assertInstanceOf(OaiHarvestInputDataEndpoint.class, inputDataEndpoint);
    assertEquals(oaipmhHarvestPluginMetadata.getUrl(), inputDataEndpoint.url());
    assertEquals("taskId", oaipmhHarvestPlugin.getExternalTaskId());
    assertEquals(DataStatus.VALID, oaipmhHarvestPlugin.getDataStatus());
  }

  @Test
  void testSubmit_HttpHarvestPlugin() throws ExternalTaskException {
    HTTPHarvestPlugin httpHarvestPlugin = new HTTPHarvestPlugin();
    HTTPHarvestPluginMetadata httpHarvestPluginMetadata = new HTTPHarvestPluginMetadata();
    httpHarvestPluginMetadata.setUrl("http://harvest.url");
    httpHarvestPlugin.setPluginMetadata(httpHarvestPluginMetadata);
    httpHarvestPlugin.setStartedDate(new Date());
    PluginExecutor<EngineTaskSettings, EngineTask> pluginExecutor = new PluginExecutor<>(httpHarvestPlugin, engineTaskClient);

    ArgumentCaptor<InputDataEndpoint> inputDataEndpointCaptor = ArgumentCaptor.forClass(InputDataEndpoint.class);

    when(engineTaskClient.createEngineTask(anyMap(), inputDataEndpointCaptor.capture(), any(DataRevision.class)))
        .thenReturn(engineTask);

    when(engineTaskClient.submitEngineTask(eq(engineTask), anyString())).thenReturn("taskId");
    pluginExecutor.submit("datasetId", "previousTaskId");

    InputDataEndpoint inputDataEndpoint = inputDataEndpointCaptor.getValue();
    assertInstanceOf(HttpHarvestInputDataEndpoint.class, inputDataEndpoint);
    assertEquals(httpHarvestPluginMetadata.getUrl(), inputDataEndpoint.url());
    assertEquals("taskId", httpHarvestPlugin.getExternalTaskId());
    assertEquals(DataStatus.VALID, httpHarvestPlugin.getDataStatus());
  }

  @Test
  void testSubmit_InvalidHarvestPlugin() {
    ValidationExternalPlugin validationExternalPlugin = new ValidationExternalPlugin();
    ValidationExternalPluginMetadata validationExternalPluginMetadata = spy(ValidationExternalPluginMetadata.class);
    validationExternalPlugin.setPluginMetadata(validationExternalPluginMetadata);
    validationExternalPlugin.setStartedDate(new Date());
    when(validationExternalPluginMetadata.getExecutablePluginType()).thenReturn(HTTP_HARVEST);

    PluginExecutor<EngineTaskSettings, EngineTask> pluginExecutor = new PluginExecutor<>(validationExternalPlugin,
        engineTaskClient);
    assertThrows(IllegalStateException.class, () -> pluginExecutor.submit("datasetId", "previousTaskId"));
  }

  @Test
  void testSubmit_ValidationExternalPlugin() throws ExternalTaskException {
    ValidationExternalPlugin validationExternalPlugin = new ValidationExternalPlugin();
    ValidationExternalPluginMetadata validationExternalPluginMetadata = new ValidationExternalPluginMetadata();
    validationExternalPluginMetadata.setRevisionNamePreviousPlugin(PluginType.OAIPMH_HARVEST.name());
    validationExternalPluginMetadata.setRevisionTimestampPreviousPlugin(new Date());
    validationExternalPlugin.setPluginMetadata(validationExternalPluginMetadata);
    validationExternalPlugin.setStartedDate(new Date());
    PluginExecutor<EngineTaskSettings, EngineTask> pluginExecutor = new PluginExecutor<>(validationExternalPlugin,
        engineTaskClient);

    ArgumentCaptor<InputDataEndpoint> inputDataEndpointCaptor = ArgumentCaptor.forClass(InputDataEndpoint.class);
    ArgumentCaptor<DataRevision> dataRevisionCaptor = ArgumentCaptor.forClass(DataRevision.class);

    when(engineTaskClient.createEngineTask(anyMap(), inputDataEndpointCaptor.capture(), dataRevisionCaptor.capture()))
        .thenReturn(engineTask);

    when(engineTaskClient.submitEngineTask(eq(engineTask), anyString())).thenReturn("taskId");
    pluginExecutor.submit("datasetId", "previousTaskId");

    InputDataEndpoint inputDataEndpoint = inputDataEndpointCaptor.getValue();
    assertInstanceOf(InternalInputDataEndpoint.class, inputDataEndpoint);
    assertEquals(validationExternalPlugin.getPluginType().name(), dataRevisionCaptor.getValue().name());
    assertEquals(engineTaskSettings.getProvider(), dataRevisionCaptor.getValue().providerId());
    assertEquals(validationExternalPlugin.getStartedDate(), dataRevisionCaptor.getValue().creationTimeStamp());
    assertEquals("taskId", validationExternalPlugin.getExternalTaskId());
    assertEquals(DataStatus.VALID, validationExternalPlugin.getDataStatus());
  }

  @Test
  void testSubmit_TransformationPlugin() throws ExternalTaskException {
    TransformationPlugin transformationPlugin = new TransformationPlugin();
    TransformationPluginMetadata transformationPluginMetadata = new TransformationPluginMetadata();
    transformationPluginMetadata.setRevisionNamePreviousPlugin(PluginType.VALIDATION_EXTERNAL.name());
    transformationPluginMetadata.setRevisionTimestampPreviousPlugin(new Date());
    transformationPlugin.setPluginMetadata(transformationPluginMetadata);
    transformationPlugin.setStartedDate(new Date());
    PluginExecutor<EngineTaskSettings, EngineTask> pluginExecutor = new PluginExecutor<>(transformationPlugin, engineTaskClient);

    ArgumentCaptor<InputDataEndpoint> inputDataEndpointCaptor = ArgumentCaptor.forClass(InputDataEndpoint.class);
    ArgumentCaptor<DataRevision> dataRevisionCaptor = ArgumentCaptor.forClass(DataRevision.class);

    when(engineTaskClient.createEngineTask(anyMap(), inputDataEndpointCaptor.capture(), dataRevisionCaptor.capture()))
        .thenReturn(engineTask);

    when(engineTaskClient.submitEngineTask(eq(engineTask), anyString())).thenReturn("taskId");
    pluginExecutor.submit("datasetId", "previousTaskId");

    InputDataEndpoint inputDataEndpoint = inputDataEndpointCaptor.getValue();
    assertInstanceOf(InternalInputDataEndpoint.class, inputDataEndpoint);
    assertEquals(transformationPlugin.getPluginType().name(), dataRevisionCaptor.getValue().name());
    assertEquals(engineTaskSettings.getProvider(), dataRevisionCaptor.getValue().providerId());
    assertEquals(transformationPlugin.getStartedDate(), dataRevisionCaptor.getValue().creationTimeStamp());
    assertEquals("taskId", transformationPlugin.getExternalTaskId());
    assertEquals(DataStatus.VALID, transformationPlugin.getDataStatus());
  }

  @Test
  void testSubmit_ValidationInternalPlugin() throws ExternalTaskException {
    ValidationInternalPlugin validationInternalPlugin = new ValidationInternalPlugin();
    ValidationInternalPluginMetadata validationInternalPluginMetadata = new ValidationInternalPluginMetadata();
    validationInternalPluginMetadata.setRevisionNamePreviousPlugin(PluginType.TRANSFORMATION.name());
    validationInternalPluginMetadata.setRevisionTimestampPreviousPlugin(new Date());
    validationInternalPlugin.setPluginMetadata(validationInternalPluginMetadata);
    validationInternalPlugin.setStartedDate(new Date());
    PluginExecutor<EngineTaskSettings, EngineTask> pluginExecutor = new PluginExecutor<>(validationInternalPlugin,
        engineTaskClient);

    ArgumentCaptor<InputDataEndpoint> inputDataEndpointCaptor = ArgumentCaptor.forClass(InputDataEndpoint.class);
    ArgumentCaptor<DataRevision> dataRevisionCaptor = ArgumentCaptor.forClass(DataRevision.class);

    when(engineTaskClient.createEngineTask(anyMap(), inputDataEndpointCaptor.capture(), dataRevisionCaptor.capture()))
        .thenReturn(engineTask);

    when(engineTaskClient.submitEngineTask(eq(engineTask), anyString())).thenReturn("taskId");
    pluginExecutor.submit("datasetId", "previousTaskId");

    InputDataEndpoint inputDataEndpoint = inputDataEndpointCaptor.getValue();
    assertInstanceOf(InternalInputDataEndpoint.class, inputDataEndpoint);
    assertEquals(validationInternalPlugin.getPluginType().name(), dataRevisionCaptor.getValue().name());
    assertEquals(engineTaskSettings.getProvider(), dataRevisionCaptor.getValue().providerId());
    assertEquals(validationInternalPlugin.getStartedDate(), dataRevisionCaptor.getValue().creationTimeStamp());
    assertEquals("taskId", validationInternalPlugin.getExternalTaskId());
    assertEquals(DataStatus.VALID, validationInternalPlugin.getDataStatus());
  }

  @Test
  void testSubmit_NormalizationPlugin() throws ExternalTaskException {
    NormalizationPlugin normalizationPlugin = new NormalizationPlugin();
    NormalizationPluginMetadata normalizationPluginMetadata = new NormalizationPluginMetadata();
    normalizationPluginMetadata.setRevisionNamePreviousPlugin(PluginType.VALIDATION_INTERNAL.name());
    normalizationPluginMetadata.setRevisionTimestampPreviousPlugin(new Date());
    normalizationPlugin.setPluginMetadata(normalizationPluginMetadata);
    normalizationPlugin.setStartedDate(new Date());
    PluginExecutor<EngineTaskSettings, EngineTask> pluginExecutor = new PluginExecutor<>(normalizationPlugin, engineTaskClient);

    ArgumentCaptor<InputDataEndpoint> inputDataEndpointCaptor = ArgumentCaptor.forClass(InputDataEndpoint.class);
    ArgumentCaptor<DataRevision> dataRevisionCaptor = ArgumentCaptor.forClass(DataRevision.class);

    when(engineTaskClient.createEngineTask(anyMap(), inputDataEndpointCaptor.capture(), dataRevisionCaptor.capture()))
        .thenReturn(engineTask);

    when(engineTaskClient.submitEngineTask(eq(engineTask), anyString())).thenReturn("taskId");
    pluginExecutor.submit("datasetId", "previousTaskId");

    InputDataEndpoint inputDataEndpoint = inputDataEndpointCaptor.getValue();
    assertInstanceOf(InternalInputDataEndpoint.class, inputDataEndpoint);
    assertEquals(normalizationPlugin.getPluginType().name(), dataRevisionCaptor.getValue().name());
    assertEquals(engineTaskSettings.getProvider(), dataRevisionCaptor.getValue().providerId());
    assertEquals(normalizationPlugin.getStartedDate(), dataRevisionCaptor.getValue().creationTimeStamp());
    assertEquals("taskId", normalizationPlugin.getExternalTaskId());
    assertEquals(DataStatus.VALID, normalizationPlugin.getDataStatus());
  }

  @Test
  void testSubmit_EnrichmentPlugin() throws ExternalTaskException {
    EnrichmentPlugin enrichmentPlugin = new EnrichmentPlugin();
    EnrichmentPluginMetadata enrichmentPluginMetadata = new EnrichmentPluginMetadata();
    enrichmentPluginMetadata.setRevisionNamePreviousPlugin(PluginType.NORMALIZATION.name());
    enrichmentPluginMetadata.setRevisionTimestampPreviousPlugin(new Date());
    enrichmentPlugin.setPluginMetadata(enrichmentPluginMetadata);
    enrichmentPlugin.setStartedDate(new Date());
    PluginExecutor<EngineTaskSettings, EngineTask> pluginExecutor = new PluginExecutor<>(enrichmentPlugin, engineTaskClient);

    ArgumentCaptor<InputDataEndpoint> inputDataEndpointCaptor = ArgumentCaptor.forClass(InputDataEndpoint.class);
    ArgumentCaptor<DataRevision> dataRevisionCaptor = ArgumentCaptor.forClass(DataRevision.class);

    when(engineTaskClient.createEngineTask(anyMap(), inputDataEndpointCaptor.capture(), dataRevisionCaptor.capture()))
        .thenReturn(engineTask);

    when(engineTaskClient.submitEngineTask(eq(engineTask), anyString())).thenReturn("taskId");
    pluginExecutor.submit("datasetId", "previousTaskId");

    InputDataEndpoint inputDataEndpoint = inputDataEndpointCaptor.getValue();
    assertInstanceOf(InternalInputDataEndpoint.class, inputDataEndpoint);
    assertEquals(enrichmentPlugin.getPluginType().name(), dataRevisionCaptor.getValue().name());
    assertEquals(engineTaskSettings.getProvider(), dataRevisionCaptor.getValue().providerId());
    assertEquals(enrichmentPlugin.getStartedDate(), dataRevisionCaptor.getValue().creationTimeStamp());
    assertEquals("taskId", enrichmentPlugin.getExternalTaskId());
    assertEquals(DataStatus.VALID, enrichmentPlugin.getDataStatus());
  }

  @Test
  void testSubmit_MediaPlugin() throws ExternalTaskException {
    MediaProcessPlugin mediaProcessPlugin = new MediaProcessPlugin();
    MediaProcessPluginMetadata mediaProcessPluginMetadata = new MediaProcessPluginMetadata();
    mediaProcessPluginMetadata.setRevisionNamePreviousPlugin(PluginType.ENRICHMENT.name());
    mediaProcessPluginMetadata.setRevisionTimestampPreviousPlugin(new Date());
    mediaProcessPlugin.setPluginMetadata(mediaProcessPluginMetadata);
    mediaProcessPlugin.setStartedDate(new Date());
    PluginExecutor<EngineTaskSettings, EngineTask> pluginExecutor = new PluginExecutor<>(mediaProcessPlugin, engineTaskClient);

    ArgumentCaptor<InputDataEndpoint> inputDataEndpointCaptor = ArgumentCaptor.forClass(InputDataEndpoint.class);
    ArgumentCaptor<DataRevision> dataRevisionCaptor = ArgumentCaptor.forClass(DataRevision.class);

    when(engineTaskClient.createEngineTask(anyMap(), inputDataEndpointCaptor.capture(), dataRevisionCaptor.capture()))
        .thenReturn(engineTask);

    when(engineTaskClient.submitEngineTask(eq(engineTask), anyString())).thenReturn("taskId");
    pluginExecutor.submit("datasetId", "previousTaskId");

    InputDataEndpoint inputDataEndpoint = inputDataEndpointCaptor.getValue();
    assertInstanceOf(InternalInputDataEndpoint.class, inputDataEndpoint);
    assertEquals(mediaProcessPlugin.getPluginType().name(), dataRevisionCaptor.getValue().name());
    assertEquals(engineTaskSettings.getProvider(), dataRevisionCaptor.getValue().providerId());
    assertEquals(mediaProcessPlugin.getStartedDate(), dataRevisionCaptor.getValue().creationTimeStamp());
    assertEquals("taskId", mediaProcessPlugin.getExternalTaskId());
    assertEquals(DataStatus.VALID, mediaProcessPlugin.getDataStatus());
  }

  @Test
  void testSubmit_MediaPlugin_WithThrottling() throws ExternalTaskException {
    MediaProcessPlugin mediaProcessPlugin = new MediaProcessPlugin();
    MediaProcessPluginMetadata mediaProcessPluginMetadata = new MediaProcessPluginMetadata();
    mediaProcessPluginMetadata.setRevisionNamePreviousPlugin(PluginType.ENRICHMENT.name());
    mediaProcessPluginMetadata.setRevisionTimestampPreviousPlugin(new Date());
    mediaProcessPluginMetadata.setThrottlingLevel(ThrottlingLevel.STRONG);
    mediaProcessPlugin.setPluginMetadata(mediaProcessPluginMetadata);
    mediaProcessPlugin.setStartedDate(new Date());
    PluginExecutor<EngineTaskSettings, EngineTask> pluginExecutor = new PluginExecutor<>(mediaProcessPlugin, engineTaskClient);

    ArgumentCaptor<Map> properties = ArgumentCaptor.forClass(Map.class);
    ArgumentCaptor<InputDataEndpoint> inputDataEndpointCaptor = ArgumentCaptor.forClass(InputDataEndpoint.class);
    ArgumentCaptor<DataRevision> dataRevisionCaptor = ArgumentCaptor.forClass(DataRevision.class);

    when(engineTaskClient.createEngineTask(properties.capture(), inputDataEndpointCaptor.capture(), dataRevisionCaptor.capture()))
        .thenReturn(engineTask);

    when(engineTaskClient.submitEngineTask(eq(engineTask), anyString())).thenReturn("taskId");
    pluginExecutor.submit("datasetId", "previousTaskId");

    InputDataEndpoint inputDataEndpoint = inputDataEndpointCaptor.getValue();
    assertInstanceOf(InternalInputDataEndpoint.class, inputDataEndpoint);
    assertEquals(mediaProcessPlugin.getPluginType().name(), dataRevisionCaptor.getValue().name());
    assertEquals(engineTaskSettings.getProvider(), dataRevisionCaptor.getValue().providerId());
    assertEquals(mediaProcessPlugin.getStartedDate(), dataRevisionCaptor.getValue().creationTimeStamp());
    String maximumParallelization = String.valueOf(
        engineTaskSettings.getThrottlingValues().getThreadNumberFromThrottlingLevel(ThrottlingLevel.STRONG));
    assertEquals(maximumParallelization, properties.getValue().get(MAXIMUM_PARALLELIZATION));
    assertEquals("taskId", mediaProcessPlugin.getExternalTaskId());
    assertEquals(DataStatus.VALID, mediaProcessPlugin.getDataStatus());
  }

  @Test
  void testSubmit_LinkCheckingPlugin() throws ExternalTaskException {
    LinkCheckingPlugin linkCheckingPlugin = new LinkCheckingPlugin();
    LinkCheckingPluginMetadata linkCheckingPluginMetadata = new LinkCheckingPluginMetadata();
    linkCheckingPluginMetadata.setRevisionNamePreviousPlugin(PluginType.MEDIA_PROCESS.name());
    linkCheckingPluginMetadata.setRevisionTimestampPreviousPlugin(new Date());
    linkCheckingPlugin.setPluginMetadata(linkCheckingPluginMetadata);
    linkCheckingPlugin.setStartedDate(new Date());
    PluginExecutor<EngineTaskSettings, EngineTask> pluginExecutor = new PluginExecutor<>(linkCheckingPlugin, engineTaskClient);

    ArgumentCaptor<InputDataEndpoint> inputDataEndpointCaptor = ArgumentCaptor.forClass(InputDataEndpoint.class);
    ArgumentCaptor<DataRevision> dataRevisionCaptor = ArgumentCaptor.forClass(DataRevision.class);

    when(engineTaskClient.createEngineTask(anyMap(), inputDataEndpointCaptor.capture(), dataRevisionCaptor.capture()))
        .thenReturn(engineTask);

    when(engineTaskClient.submitEngineTask(eq(engineTask), anyString())).thenReturn("taskId");
    pluginExecutor.submit("datasetId", "previousTaskId");

    InputDataEndpoint inputDataEndpoint = inputDataEndpointCaptor.getValue();
    assertInstanceOf(InternalInputDataEndpoint.class, inputDataEndpoint);
    assertEquals(linkCheckingPlugin.getPluginType().name(), dataRevisionCaptor.getValue().name());
    assertEquals(engineTaskSettings.getProvider(), dataRevisionCaptor.getValue().providerId());
    assertEquals(linkCheckingPlugin.getStartedDate(), dataRevisionCaptor.getValue().creationTimeStamp());
    assertEquals("taskId", linkCheckingPlugin.getExternalTaskId());
    assertEquals(DataStatus.VALID, linkCheckingPlugin.getDataStatus());
  }

  @Test
  void testSubmit_InvalidProcessPlugin() {
    IndexToPreviewPlugin indexToPreviewPlugin = new IndexToPreviewPlugin();
    IndexToPreviewPluginMetadata indexToPreviewPluginMetadata = spy(IndexToPreviewPluginMetadata.class);
    indexToPreviewPlugin.setPluginMetadata(indexToPreviewPluginMetadata);
    indexToPreviewPlugin.setStartedDate(new Date());
    when(indexToPreviewPluginMetadata.getExecutablePluginType()).thenReturn(VALIDATION_EXTERNAL);

    PluginExecutor<EngineTaskSettings, EngineTask> pluginExecutor = new PluginExecutor<>(indexToPreviewPlugin, engineTaskClient);
    assertThrows(IllegalStateException.class, () -> pluginExecutor.submit("datasetId", "previousTaskId"));
  }

  @Test
  void testSubmit_IndexToPreviewPlugin() throws ExternalTaskException {
    IndexToPreviewPlugin indexToPreviewPlugin = new IndexToPreviewPlugin();
    IndexToPreviewPluginMetadata indexToPreviewPluginMetadata = new IndexToPreviewPluginMetadata();
    indexToPreviewPluginMetadata.setRevisionNamePreviousPlugin(PluginType.MEDIA_PROCESS.name());
    indexToPreviewPluginMetadata.setRevisionTimestampPreviousPlugin(new Date());
    indexToPreviewPluginMetadata.setHarvestDate(new Date());
    indexToPreviewPlugin.setPluginMetadata(indexToPreviewPluginMetadata);
    indexToPreviewPlugin.setStartedDate(new Date());
    PluginExecutor<EngineTaskSettings, EngineTask> pluginExecutor = new PluginExecutor<>(indexToPreviewPlugin, engineTaskClient);

    ArgumentCaptor<InputDataEndpoint> inputDataEndpointCaptor = ArgumentCaptor.forClass(InputDataEndpoint.class);
    ArgumentCaptor<DataRevision> dataRevisionCaptor = ArgumentCaptor.forClass(DataRevision.class);

    when(engineTaskClient.createEngineTask(anyMap(), inputDataEndpointCaptor.capture(), dataRevisionCaptor.capture()))
        .thenReturn(engineTask);

    when(engineTaskClient.submitEngineTask(eq(engineTask), anyString())).thenReturn("taskId");
    pluginExecutor.submit("datasetId", "previousTaskId");

    InputDataEndpoint inputDataEndpoint = inputDataEndpointCaptor.getValue();
    assertInstanceOf(InternalInputDataEndpoint.class, inputDataEndpoint);
    assertEquals(indexToPreviewPlugin.getPluginType().name(), dataRevisionCaptor.getValue().name());
    assertEquals(engineTaskSettings.getProvider(), dataRevisionCaptor.getValue().providerId());
    assertEquals(indexToPreviewPlugin.getStartedDate(), dataRevisionCaptor.getValue().creationTimeStamp());
    assertEquals("taskId", indexToPreviewPlugin.getExternalTaskId());
    assertEquals(DataStatus.VALID, indexToPreviewPlugin.getDataStatus());
  }

  @Test
  void testSubmit_IndexToPublishPlugin() throws ExternalTaskException {
    IndexToPublishPlugin indexToPublishPlugin = new IndexToPublishPlugin();
    IndexToPublishPluginMetadata indexToPublishPluginMetadata = new IndexToPublishPluginMetadata();
    indexToPublishPluginMetadata.setRevisionNamePreviousPlugin(PluginType.MEDIA_PROCESS.name());
    indexToPublishPluginMetadata.setRevisionTimestampPreviousPlugin(new Date());
    indexToPublishPluginMetadata.setHarvestDate(new Date());
    indexToPublishPlugin.setPluginMetadata(indexToPublishPluginMetadata);
    indexToPublishPlugin.setStartedDate(new Date());
    PluginExecutor<EngineTaskSettings, EngineTask> pluginExecutor = new PluginExecutor<>(indexToPublishPlugin, engineTaskClient);

    ArgumentCaptor<InputDataEndpoint> inputDataEndpointCaptor = ArgumentCaptor.forClass(InputDataEndpoint.class);
    ArgumentCaptor<DataRevision> dataRevisionCaptor = ArgumentCaptor.forClass(DataRevision.class);

    when(engineTaskClient.createEngineTask(anyMap(), inputDataEndpointCaptor.capture(), dataRevisionCaptor.capture()))
        .thenReturn(engineTask);

    when(engineTaskClient.submitEngineTask(eq(engineTask), anyString())).thenReturn("taskId");
    pluginExecutor.submit("datasetId", "previousTaskId");

    InputDataEndpoint inputDataEndpoint = inputDataEndpointCaptor.getValue();
    assertInstanceOf(InternalInputDataEndpoint.class, inputDataEndpoint);
    assertEquals(indexToPublishPlugin.getPluginType().name(), dataRevisionCaptor.getValue().name());
    assertEquals(engineTaskSettings.getProvider(), dataRevisionCaptor.getValue().providerId());
    assertEquals(indexToPublishPlugin.getStartedDate(), dataRevisionCaptor.getValue().creationTimeStamp());
    assertEquals("taskId", indexToPublishPlugin.getExternalTaskId());
    assertEquals(DataStatus.VALID, indexToPublishPlugin.getDataStatus());
  }

  @Test
  void testSubmit_InvalidIndexPlugin() {
    OaipmhHarvestPlugin indexToPreviewPlugin = new OaipmhHarvestPlugin();
    OaipmhHarvestPluginMetadata oaipmhHarvestPluginMetadata = spy(OaipmhHarvestPluginMetadata.class);
    indexToPreviewPlugin.setPluginMetadata(oaipmhHarvestPluginMetadata);
    indexToPreviewPlugin.setStartedDate(new Date());
    when(oaipmhHarvestPluginMetadata.getExecutablePluginType()).thenReturn(PREVIEW);

    PluginExecutor<EngineTaskSettings, EngineTask> pluginExecutor = new PluginExecutor<>(indexToPreviewPlugin, engineTaskClient);
    assertThrows(IllegalStateException.class, () -> pluginExecutor.submit("datasetId", "previousTaskId"));
  }

  @Test
  void testSubmit_DepublishPlugin_Dataset() throws ExternalTaskException {
    DepublishPlugin depublishPlugin = new DepublishPlugin();
    DepublishPluginMetadata depublishPluginMetadata = new DepublishPluginMetadata();
    depublishPluginMetadata.setDatasetDepublish(true);
    depublishPlugin.setPluginMetadata(depublishPluginMetadata);
    depublishPlugin.setStartedDate(new Date());
    PluginExecutor<EngineTaskSettings, EngineTask> pluginExecutor = new PluginExecutor<>(depublishPlugin, engineTaskClient);

    ArgumentCaptor<InputDataEndpoint> inputDataEndpointCaptor = ArgumentCaptor.forClass(InputDataEndpoint.class);
    ArgumentCaptor<DataRevision> dataRevisionCaptor = ArgumentCaptor.forClass(DataRevision.class);

    when(engineTaskClient.createEngineTask(anyMap(), inputDataEndpointCaptor.capture(), dataRevisionCaptor.capture()))
        .thenReturn(engineTask);

    when(engineTaskClient.submitEngineTask(eq(engineTask), anyString())).thenReturn("taskId");
    pluginExecutor.submit("datasetId", "previousTaskId");

    InputDataEndpoint inputDataEndpoint = inputDataEndpointCaptor.getValue();
    assertNull(inputDataEndpoint);
    assertNull(dataRevisionCaptor.getValue());
    assertEquals("taskId", depublishPlugin.getExternalTaskId());
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
    PluginExecutor<EngineTaskSettings, EngineTask> pluginExecutor = new PluginExecutor<>(depublishPlugin, engineTaskClient);

    ArgumentCaptor<Map> properties = ArgumentCaptor.forClass(Map.class);
    ArgumentCaptor<InputDataEndpoint> inputDataEndpointCaptor = ArgumentCaptor.forClass(InputDataEndpoint.class);
    ArgumentCaptor<DataRevision> dataRevisionCaptor = ArgumentCaptor.forClass(DataRevision.class);

    when(engineTaskClient.createEngineTask(properties.capture(), inputDataEndpointCaptor.capture(), dataRevisionCaptor.capture()))
        .thenReturn(engineTask);

    when(engineTaskClient.submitEngineTask(eq(engineTask), anyString())).thenReturn("taskId");
    pluginExecutor.submit("datasetId", "previousTaskId");

    assertEquals(depublishPluginMetadata.getDepublicationReason().name(), properties.getValue().get(DEPUBLICATION_REASON));
    InputDataEndpoint inputDataEndpoint = inputDataEndpointCaptor.getValue();
    assertNull(inputDataEndpoint);
    assertNull(dataRevisionCaptor.getValue());
    assertEquals("taskId", depublishPlugin.getExternalTaskId());
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
    PluginExecutor<EngineTaskSettings, EngineTask> pluginExecutor = new PluginExecutor<>(depublishPlugin, engineTaskClient);

    ArgumentCaptor<InputDataEndpoint> inputDataEndpointCaptor = ArgumentCaptor.forClass(InputDataEndpoint.class);
    ArgumentCaptor<DataRevision> dataRevisionCaptor = ArgumentCaptor.forClass(DataRevision.class);

    when(engineTaskClient.createEngineTask(anyMap(), inputDataEndpointCaptor.capture(), dataRevisionCaptor.capture()))
        .thenReturn(engineTask);

    when(engineTaskClient.submitEngineTask(eq(engineTask), anyString())).thenReturn("taskId");
    pluginExecutor.submit("datasetId", "previousTaskId");

    InputDataEndpoint inputDataEndpoint = inputDataEndpointCaptor.getValue();
    assertNull(inputDataEndpoint);
    assertNull(dataRevisionCaptor.getValue());
    assertEquals("taskId", depublishPlugin.getExternalTaskId());
    assertEquals(DataStatus.VALID, depublishPlugin.getDataStatus());
  }

  @Test
  void testSubmit_DepublishPlugin_Records_Invalid() {
    DepublishPlugin depublishPlugin = new DepublishPlugin();
    DepublishPluginMetadata depublishPluginMetadata = new DepublishPluginMetadata();
    depublishPluginMetadata.setDatasetDepublish(false);
    depublishPlugin.setPluginMetadata(depublishPluginMetadata);
    depublishPlugin.setStartedDate(new Date());
    PluginExecutor<EngineTaskSettings, EngineTask> pluginExecutor = new PluginExecutor<>(depublishPlugin, engineTaskClient);
    assertThrows(IllegalStateException.class, () -> pluginExecutor.submit("datasetId", "previousTaskId"),
        "Requested record depublication but there are no records ids for depublication in the db");
  }

  @Test
  void testSubmit_InvalidDepublishPlugin() {
    OaipmhHarvestPlugin indexToPreviewPlugin = new OaipmhHarvestPlugin();
    OaipmhHarvestPluginMetadata oaipmhHarvestPluginMetadata = spy(OaipmhHarvestPluginMetadata.class);
    indexToPreviewPlugin.setPluginMetadata(oaipmhHarvestPluginMetadata);
    indexToPreviewPlugin.setStartedDate(new Date());
    when(oaipmhHarvestPluginMetadata.getExecutablePluginType()).thenReturn(DEPUBLISH);

    PluginExecutor<EngineTaskSettings, EngineTask> pluginExecutor = new PluginExecutor<>(indexToPreviewPlugin, engineTaskClient);
    assertThrows(IllegalStateException.class, () -> pluginExecutor.submit("datasetId", "previousTaskId"));
  }

  @Test
  void testSubmit_Fail() throws ExternalTaskException {
    OaipmhHarvestPlugin oaipmhHarvestPlugin = new OaipmhHarvestPlugin();
    OaipmhHarvestPluginMetadata oaipmhHarvestPluginMetadata = new OaipmhHarvestPluginMetadata();
    oaipmhHarvestPluginMetadata.setUrl("http://harvest.url");
    oaipmhHarvestPlugin.setPluginMetadata(oaipmhHarvestPluginMetadata);
    oaipmhHarvestPlugin.setStartedDate(new Date());
    PluginExecutor<EngineTaskSettings, EngineTask> pluginExecutor = new PluginExecutor<>(oaipmhHarvestPlugin, engineTaskClient);

    when(engineTaskClient.createEngineTask(anyMap(), any(InputDataEndpoint.class), any(DataRevision.class)))
        .thenReturn(engineTask);

    when(engineTaskClient.submitEngineTask(eq(engineTask), anyString())).thenThrow(new ExternalTaskException("Error"));
    assertThrows(ExternalTaskException.class, () -> pluginExecutor.submit("datasetId", "previousTaskId"));
  }

}