package eu.europeana.metis.core.rest.execution.details;

import eu.europeana.metis.core.workflow.execution.PluginDTO;
import eu.europeana.metis.core.workflow.plugins.AbstractMetisPlugin;
import eu.europeana.metis.core.workflow.plugins.DataStatus;
import eu.europeana.metis.core.workflow.plugins.DepublishPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.EnrichmentPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.ExecutablePluginFactory;
import eu.europeana.metis.core.workflow.plugins.HTTPHarvestPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.IndexToPreviewPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.IndexToPublishPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.LinkCheckingPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.MediaProcessPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.NormalizationPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.OaipmhHarvestPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.PluginStatus;
import eu.europeana.metis.core.workflow.plugins.PluginType;
import eu.europeana.metis.core.workflow.plugins.Topology;
import eu.europeana.metis.core.workflow.plugins.TransformationPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.ValidationExternalPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.ValidationInternalPluginMetadata;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PluginDTOTest {

  private PluginDTO pluginDTO;

  private static Stream<Arguments> providePluginTestData() {
    return Stream.of(
        Arguments.of(ExecutablePluginFactory.createPlugin(new HTTPHarvestPluginMetadata()),
            PluginType.HTTP_HARVEST, Topology.HTTP_HARVEST),
        Arguments.of(ExecutablePluginFactory.createPlugin(new OaipmhHarvestPluginMetadata()),
            PluginType.OAIPMH_HARVEST, Topology.OAIPMH_HARVEST),
        Arguments.of(ExecutablePluginFactory.createPlugin(new ValidationExternalPluginMetadata()),
            PluginType.VALIDATION_EXTERNAL, Topology.VALIDATION),
        Arguments.of(ExecutablePluginFactory.createPlugin(new TransformationPluginMetadata()),
            PluginType.TRANSFORMATION, Topology.TRANSFORMATION),
        Arguments.of(ExecutablePluginFactory.createPlugin(new LinkCheckingPluginMetadata()),
            PluginType.LINK_CHECKING, Topology.LINK_CHECKING),
        Arguments.of(ExecutablePluginFactory.createPlugin(new ValidationInternalPluginMetadata()),
            PluginType.VALIDATION_INTERNAL, Topology.VALIDATION),
        Arguments.of(ExecutablePluginFactory.createPlugin(new NormalizationPluginMetadata()),
            PluginType.NORMALIZATION, Topology.NORMALIZATION),
        Arguments.of(ExecutablePluginFactory.createPlugin(new EnrichmentPluginMetadata()),
            PluginType.ENRICHMENT, Topology.ENRICHMENT),
        Arguments.of(ExecutablePluginFactory.createPlugin(new MediaProcessPluginMetadata()),
            PluginType.MEDIA_PROCESS, Topology.MEDIA_PROCESS),
        Arguments.of(ExecutablePluginFactory.createPlugin(new IndexToPreviewPluginMetadata()),
            PluginType.PREVIEW, Topology.INDEX),
        Arguments.of(ExecutablePluginFactory.createPlugin(new IndexToPublishPluginMetadata()),
            PluginType.PUBLISH, Topology.INDEX),
        Arguments.of(ExecutablePluginFactory.createPlugin(new DepublishPluginMetadata()),
            PluginType.DEPUBLISH, Topology.DEPUBLISH)
    );
  }

  @ParameterizedTest
  @MethodSource("providePluginTestData")
  void getPluginType(AbstractMetisPlugin metisPlugin, PluginType expectedPluginType) {
    pluginDTO = new PluginDTO(metisPlugin, true);
    assertEquals(expectedPluginType, pluginDTO.getPluginType());
  }

  @ParameterizedTest
  @MethodSource("providePluginTestData")
  void getId(AbstractMetisPlugin metisPlugin, PluginType expectedPluginType) {
    pluginDTO = new PluginDTO(metisPlugin, true);
    assertTrue(pluginDTO.getId().contains(expectedPluginType.name()));
  }

  @ParameterizedTest
  @MethodSource("providePluginTestData")
  void getPluginStatus(AbstractMetisPlugin metisPlugin) {
    pluginDTO = new PluginDTO(metisPlugin, true);
    assertEquals(PluginStatus.INQUEUE, pluginDTO.getPluginStatus());
  }

  @ParameterizedTest
  @MethodSource("providePluginTestData")
  void getDataStatus(AbstractMetisPlugin metisPlugin) {
    pluginDTO = new PluginDTO(metisPlugin, true);
    assertEquals(DataStatus.NOT_YET_GENERATED, pluginDTO.getDataStatus());
  }

  @ParameterizedTest
  @MethodSource("providePluginTestData")
  void getFailMessage(AbstractMetisPlugin metisPlugin) {
    pluginDTO = new PluginDTO(metisPlugin, true);
    assertNull(pluginDTO.getFailMessage());
  }

  @ParameterizedTest
  @MethodSource("providePluginTestData")
  void getStartedDate(AbstractMetisPlugin metisPlugin) {
    pluginDTO = new PluginDTO(metisPlugin, true);
    assertNull(pluginDTO.getStartedDate());
  }

  @ParameterizedTest
  @MethodSource("providePluginTestData")
  void getUpdatedDate(AbstractMetisPlugin metisPlugin) {
    pluginDTO = new PluginDTO(metisPlugin, true);
    assertNull(pluginDTO.getUpdatedDate());
  }

  @ParameterizedTest
  @MethodSource("providePluginTestData")
  void getFinishedDate(AbstractMetisPlugin metisPlugin) {
    pluginDTO = new PluginDTO(metisPlugin, true);
    assertNull(pluginDTO.getFinishedDate());
  }

  @ParameterizedTest
  @MethodSource("providePluginTestData")
  void getExternalTaskId(AbstractMetisPlugin metisPlugin) {
    pluginDTO = new PluginDTO(metisPlugin, true);
    assertNull(pluginDTO.getExternalTaskId());
  }

  @ParameterizedTest
  @MethodSource("providePluginTestData")
  void getExecutionProgress(AbstractMetisPlugin metisPlugin) {
    pluginDTO = new PluginDTO(metisPlugin, true);
    assertNotNull(pluginDTO.getExecutionProgress());
  }

  @ParameterizedTest
  @MethodSource("providePluginTestData")
  void getTopologyName(AbstractMetisPlugin metisPlugin, PluginType pluginType, Topology topology) {
    pluginDTO = new PluginDTO(metisPlugin, true);
    assertEquals(topology.getTopologyName(), pluginDTO.getTopologyName());
  }

  @ParameterizedTest
  @MethodSource("providePluginTestData")
  void isCanDisplayRawXml(AbstractMetisPlugin metisPlugin) {
    pluginDTO = new PluginDTO(metisPlugin, true);
    assertTrue(pluginDTO.isCanDisplayRawXml());
  }

  @ParameterizedTest
  @MethodSource("providePluginTestData")
  void getPluginMetadata(AbstractMetisPlugin metisPlugin) {
    pluginDTO = new PluginDTO(metisPlugin, true);
    assertNotNull(pluginDTO.getPluginMetadata());
  }
}
