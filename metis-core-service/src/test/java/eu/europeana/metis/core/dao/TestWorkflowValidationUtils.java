package eu.europeana.metis.core.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import eu.europeana.metis.core.dataset.DepublishRecordId.DepublicationStatus;
import eu.europeana.metis.core.engine.base.EngineType;
import eu.europeana.metis.core.exceptions.PluginExecutionNotAllowed;
import eu.europeana.metis.core.util.DepublishRecordIdSortField;
import eu.europeana.metis.core.util.SortDirection;
import eu.europeana.metis.core.utils.TestObjectFactory;
import eu.europeana.metis.core.workflow.Workflow;
import eu.europeana.metis.core.workflow.plugins.AbstractExecutablePlugin;
import eu.europeana.metis.core.workflow.plugins.AbstractExecutablePluginMetadata;
import eu.europeana.metis.core.workflow.plugins.DepublishPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.ExecutablePluginFactory;
import eu.europeana.metis.core.workflow.plugins.ExecutablePluginType;
import eu.europeana.metis.core.workflow.plugins.ExecutionProgress;
import eu.europeana.metis.core.workflow.plugins.HTTPHarvestPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.OaipmhHarvestPluginMetadata;
import eu.europeana.metis.exception.BadContentException;
import eu.europeana.metis.exception.GenericMetisException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TestWorkflowValidationUtils {

  private static final String DATASET_ID = Integer.toString(TestObjectFactory.DATASETID);
  private static WorkflowValidationUtils validationUtils;
  private static DepublishRecordIdDao depublishRecordIdDao;
  private static DatasetXsltDao datasetXsltDao;
  private static DataEvolutionUtils dataEvolutionUtils;
  private static final ExecutablePluginType predecessorType = ExecutablePluginType.OAIPMH_HARVEST;

  @BeforeAll
  static void prepare() {
    depublishRecordIdDao = mock(DepublishRecordIdDao.class);
    datasetXsltDao = mock(DatasetXsltDao.class);
    dataEvolutionUtils = mock(DataEvolutionUtils.class);
    validationUtils = spy(new WorkflowValidationUtils(EngineType.ECLOUD, depublishRecordIdDao, datasetXsltDao, dataEvolutionUtils));
  }

  @AfterEach
  void cleanUp() {
    reset(validationUtils, depublishRecordIdDao, dataEvolutionUtils);
  }

  @Test
  void testValidateWorkflowPlugins_SuccessfulPredecessor() throws GenericMetisException {
    final AbstractExecutablePlugin<?> predecessor =
        ExecutablePluginFactory.createPlugin(new OaipmhHarvestPluginMetadata());
    predecessor.setExecutionProgress(new ExecutionProgress());
    predecessor.getExecutionProgress().setProcessedRecords(1);
    predecessor.getExecutionProgress().setFailRecords(0);
    doReturn(new PluginWithExecutionId<>("", predecessor)).when(dataEvolutionUtils)
                                                          .computePredecessorPlugin(any(), eq(predecessorType), eq(DATASET_ID));

    assertSame(predecessor, validationUtils.validateWorkflowPlugins(createWorkflow(
        ExecutablePluginType.OAIPMH_HARVEST), predecessorType).getPlugin());
  }

  @Test
  void testValidateWorkflowPlugins_EmptyWorkflow() {
    assertThrows(BadContentException.class, () -> validationUtils
        .validateWorkflowPlugins(createWorkflow(), predecessorType));
  }

  @Test
  void testValidateWorkflowPlugins_NullWorkflowList() {
    final Workflow workflowWithNullList = new Workflow();
    workflowWithNullList.setMetisPluginsMetadata(null);

    assertThrows(BadContentException.class, () -> validationUtils
        .validateWorkflowPlugins(workflowWithNullList, predecessorType));
  }

  @Test
  void testValidateWorkflowPlugins_InvalidPluginType() {
    assertThrows(BadContentException.class, () -> validationUtils.validateWorkflowPlugins(
        createWorkflow(ExecutablePluginType.NORMALIZATION, null,
            ExecutablePluginType.LINK_CHECKING), predecessorType));
  }

  @Test
  void testValidateWorkflowPlugins_DepublishAndOaiPlugins() {
    Workflow workflowDepublishAndOai = new Workflow();
    workflowDepublishAndOai.setDatasetId(Integer.toString(TestObjectFactory.DATASETID));
    OaipmhHarvestPluginMetadata oaipmhHarvestPluginMetadata = new OaipmhHarvestPluginMetadata();
    oaipmhHarvestPluginMetadata.setEnabled(true);
    DepublishPluginMetadata depublishPluginMetadata = new DepublishPluginMetadata();
    depublishPluginMetadata.setEnabled(true);
    depublishPluginMetadata.setDatasetDepublish(true);
    List<AbstractExecutablePluginMetadata> abstractMetisPluginMetadata = new ArrayList<>(2);
    abstractMetisPluginMetadata.add(oaipmhHarvestPluginMetadata);
    abstractMetisPluginMetadata.add(depublishPluginMetadata);
    workflowDepublishAndOai.setMetisPluginsMetadata(abstractMetisPluginMetadata);

    assertThrows(BadContentException.class,
        () -> validationUtils.validateWorkflowPlugins(workflowDepublishAndOai, null));
  }

  @Test
  void testValidateWorkflowPlugins_RecordDepublishWithoutIds() throws BadContentException {
    Workflow workflowDepublish = new Workflow();
    workflowDepublish.setDatasetId(Integer.toString(TestObjectFactory.DATASETID));
    DepublishPluginMetadata depublishPluginMetadata = new DepublishPluginMetadata();
    depublishPluginMetadata.setDatasetDepublish(false);
    List<AbstractExecutablePluginMetadata> abstractMetisPluginMetadata = new ArrayList<>(1);
    abstractMetisPluginMetadata.add(depublishPluginMetadata);
    workflowDepublish.setMetisPluginsMetadata(abstractMetisPluginMetadata);
    when(depublishRecordIdDao
        .getAllDepublishRecordIdsWithStatus(workflowDepublish.getDatasetId(),
            DepublishRecordIdSortField.DEPUBLICATION_STATE, SortDirection.ASCENDING,
            DepublicationStatus.PENDING_DEPUBLICATION)).thenReturn(Collections.emptySet());

    assertThrows(BadContentException.class, () -> validationUtils
        .validateWorkflowPlugins(workflowDepublish, null));
  }

  @Test
  void testValidateWorkflowPlugins_LinkCheckingAndOthers() throws GenericMetisException {
    final AbstractExecutablePlugin<?> predecessor =
        ExecutablePluginFactory.createPlugin(new OaipmhHarvestPluginMetadata());
    predecessor.setExecutionProgress(new ExecutionProgress());
    predecessor.getExecutionProgress().setProcessedRecords(1);
    predecessor.getExecutionProgress().setFailRecords(0);
    doReturn(new PluginWithExecutionId<>("", predecessor)).when(dataEvolutionUtils)
                                                          .computePredecessorPlugin(any(), eq(predecessorType), eq(DATASET_ID));

    assertSame(predecessor, validationUtils.validateWorkflowPlugins(
        createWorkflow(ExecutablePluginType.LINK_CHECKING), predecessorType).getPlugin());
    assertThrows(PluginExecutionNotAllowed.class, () -> validationUtils.validateWorkflowPlugins(
        createWorkflow(ExecutablePluginType.LINK_CHECKING, ExecutablePluginType.TRANSFORMATION),
        predecessorType));
  }

  @Test
  void testValidateWorkflowPlugins_GapsInWorkflow() {
    assertThrows(PluginExecutionNotAllowed.class, () -> validationUtils.validateWorkflowPlugins(
        createWorkflow(ExecutablePluginType.TRANSFORMATION, ExecutablePluginType.ENRICHMENT),
        predecessorType));
  }

  @Test
  void testValidateWorkflowPlugins_DuplicatePluginTypes() {
    assertThrows(PluginExecutionNotAllowed.class, () -> validationUtils.validateWorkflowPlugins(
        createWorkflow(ExecutablePluginType.TRANSFORMATION, ExecutablePluginType.ENRICHMENT,
            ExecutablePluginType.ENRICHMENT), predecessorType));
    assertThrows(PluginExecutionNotAllowed.class, () -> validationUtils.validateWorkflowPlugins(
        createWorkflow(ExecutablePluginType.TRANSFORMATION, ExecutablePluginType.LINK_CHECKING,
            ExecutablePluginType.LINK_CHECKING), predecessorType));
  }

  @Test
  void testValidateWorkflowPlugins_DisabledPlugins() throws GenericMetisException {
    final Workflow workflowWithDisabledPlugins = createWorkflow(
        ExecutablePluginType.NORMALIZATION,
        ExecutablePluginType.ENRICHMENT, ExecutablePluginType.MEDIA_PROCESS);
    final AbstractExecutablePlugin<?> predecessor =
        ExecutablePluginFactory.createPlugin(new OaipmhHarvestPluginMetadata());
    predecessor.setExecutionProgress(new ExecutionProgress());
    predecessor.getExecutionProgress().setProcessedRecords(1);
    predecessor.getExecutionProgress().setFailRecords(0);
    doReturn(new PluginWithExecutionId<>("", predecessor)).when(dataEvolutionUtils)
                                                          .computePredecessorPlugin(any(), eq(predecessorType), eq(DATASET_ID));

    assertSame(predecessor, validationUtils.validateWorkflowPlugins(workflowWithDisabledPlugins,
        predecessorType).getPlugin());
    when(workflowWithDisabledPlugins.getMetisPluginsMetadata().get(1).isEnabled())
        .thenReturn(false);
    assertThrows(PluginExecutionNotAllowed.class, () -> validationUtils.validateWorkflowPlugins(
        workflowWithDisabledPlugins, predecessorType));
  }

  @Test
  void testValidateWorkflowPlugins_BadPredecessor() throws PluginExecutionNotAllowed {
    doThrow(PluginExecutionNotAllowed.class).when(dataEvolutionUtils)
                                            .computePredecessorPlugin(any(), eq(predecessorType), eq(DATASET_ID));

    assertThrows(PluginExecutionNotAllowed.class, () -> validationUtils.validateWorkflowPlugins(
        createWorkflow(ExecutablePluginType.ENRICHMENT, ExecutablePluginType.OAIPMH_HARVEST),
        predecessorType));
  }

  private Workflow createWorkflow(ExecutablePluginType... pluginTypes) {
    final Workflow workflow = new Workflow();
    workflow.setDatasetId(DATASET_ID);
    workflow.setMetisPluginsMetadata(Arrays.stream(pluginTypes).map(type -> {
      final AbstractExecutablePluginMetadata plugin = mock(
          AbstractExecutablePluginMetadata.class);
      when(plugin.isEnabled()).thenReturn(true);
      doReturn(type).when(plugin).getExecutablePluginType();
      return plugin;
    }).toList());
    return workflow;
  }

  @Test
  void testValidateWorkflowPlugins_testHarvestingParameters() throws GenericMetisException {

    // Prepare correct url variables
    final String simpleUrl = "http://test.com/path";
    final String urlWithFragmentAndQuery = simpleUrl + "#fragment?query=1";
    final String metadataFormat = "metadataFormatParameter";
    final String setSpec = "setSpecParameter";

    // Create oai harvesting with all parameters
    final OaipmhHarvestPluginMetadata oai = new OaipmhHarvestPluginMetadata();
    oai.setEnabled(true);
    oai.setUrl(" " + urlWithFragmentAndQuery + " ");
    oai.setMetadataFormat(" " + metadataFormat + " ");
    oai.setSetSpec(" " + setSpec + " ");

    // Create http harvesting
    final HTTPHarvestPluginMetadata http = new HTTPHarvestPluginMetadata();
    http.setEnabled(true);
    http.setUrl(" " + urlWithFragmentAndQuery + " ");

    // Create the workflow and execute the method
    final Workflow workflow = new Workflow();
    workflow.setDatasetId(DATASET_ID);
    workflow.setMetisPluginsMetadata(Arrays.asList(oai, http));
    validationUtils.validateWorkflowPlugins(workflow, null);

    // Test output
    assertEquals(simpleUrl, oai.getUrl());
    assertEquals(metadataFormat, oai.getMetadataFormat());
    assertEquals(setSpec, oai.getSetSpec());
    assertEquals(urlWithFragmentAndQuery, http.getUrl());

    // Create oai harvesting with only url
    oai.setUrl(urlWithFragmentAndQuery);
    oai.setMetadataFormat(null);
    oai.setSetSpec(null);

    // Create the workflow and execute the method
    workflow.setMetisPluginsMetadata(Collections.singletonList(oai));
    validationUtils.validateWorkflowPlugins(workflow, null);

    // Test output
    assertEquals(simpleUrl, oai.getUrl());
    assertNull(oai.getMetadataFormat());
    assertNull(oai.getSetSpec());

    // Test OAI with invalid URL
    oai.setUrl("invalid URL");
    workflow.setMetisPluginsMetadata(Collections.singletonList(oai));
    assertThrows(BadContentException.class,
        () -> validationUtils.validateWorkflowPlugins(workflow, null));

    // Test HTTP with missing URL
    http.setUrl(null);
    workflow.setMetisPluginsMetadata(Collections.singletonList(http));
    assertThrows(BadContentException.class,
        () -> validationUtils.validateWorkflowPlugins(workflow, null));

    // Test incremental OAI
    oai.setUrl(urlWithFragmentAndQuery);
    oai.setIncrementalHarvest(true);
    workflow.setMetisPluginsMetadata(Collections.singletonList(oai));
    doReturn(true).when(validationUtils).isIncrementalHarvestingAllowed(DATASET_ID);
    validationUtils.validateWorkflowPlugins(workflow, null);
    doReturn(false).when(validationUtils).isIncrementalHarvestingAllowed(DATASET_ID);
    assertThrows(BadContentException.class,
        () -> validationUtils.validateWorkflowPlugins(workflow, null));

    // Test incremental HTTP
    http.setUrl(urlWithFragmentAndQuery);
    http.setIncrementalHarvest(true);
    workflow.setMetisPluginsMetadata(Collections.singletonList(http));
    doReturn(true).when(validationUtils).isIncrementalHarvestingAllowed(DATASET_ID);
    validationUtils.validateWorkflowPlugins(workflow, null);
    doReturn(false).when(validationUtils).isIncrementalHarvestingAllowed(DATASET_ID);
    assertThrows(BadContentException.class,
        () -> validationUtils.validateWorkflowPlugins(workflow, null));
  }

  @Test
  void testIsIncrementalHarvestingAllowed() {
    doReturn(List.of(new PluginWithExecutionId<>((String) null, null)))
        .when(dataEvolutionUtils).getPublishedHarvestIncrements(DATASET_ID);
    assertTrue(validationUtils.isIncrementalHarvestingAllowed(DATASET_ID));
    doReturn(Collections.emptyList()).when(dataEvolutionUtils)
                                     .getPublishedHarvestIncrements(DATASET_ID);
    assertFalse(validationUtils.isIncrementalHarvestingAllowed(DATASET_ID));
    doReturn(null).when(dataEvolutionUtils).getPublishedHarvestIncrements(DATASET_ID);
    assertFalse(validationUtils.isIncrementalHarvestingAllowed(DATASET_ID));
  }
}
