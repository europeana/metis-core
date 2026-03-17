package eu.europeana.metis.core.service;

import static eu.europeana.metis.core.service.TestRedirectionBase.getIndexToPublishPluginMetadata;
import static eu.europeana.metis.core.service.TestRedirectionBase.getTestDataset;
import static eu.europeana.metis.core.service.TestRedirectionBase.getValidationExternalProperties;
import static eu.europeana.metis.core.service.TestRedirectionBase.getValidationInternalProperties;
import static eu.europeana.metis.core.service.TestRedirectionBase.getWorkflow;
import static eu.europeana.metis.core.service.TestRedirectionBase.getWorkflowFromNormalization;
import static eu.europeana.metis.core.service.TestRedirectionBase.getWorkflowPostReindex;
import static eu.europeana.metis.core.service.TestRedirectionBase.getWorkflowPreReindex;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import eu.europeana.metis.core.dao.DataEvolutionUtils;
import eu.europeana.metis.core.dao.DatasetXsltDao;
import eu.europeana.metis.core.dao.DepublishRecordIdDao;
import eu.europeana.metis.core.dao.PluginWithExecutionId;
import eu.europeana.metis.core.dao.WorkflowExecutionDao;
import eu.europeana.metis.core.dataset.Dataset;
import eu.europeana.metis.core.workflow.Workflow;
import eu.europeana.metis.core.workflow.WorkflowExecution;
import eu.europeana.metis.core.workflow.WorkflowExecutionHelper;
import eu.europeana.metis.core.workflow.plugins.AbstractMetisPlugin;
import eu.europeana.metis.core.workflow.plugins.ExecutablePlugin;
import eu.europeana.metis.core.workflow.plugins.IndexToPublishPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.PluginType;
import eu.europeana.metis.exception.BadContentException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TestRedirection {

  private WorkflowExecutionDao workflowExecutionDao;
  private DataEvolutionUtils dataEvolutionUtils;
  private WorkflowExecutionFactory workflowExecutionFactory;
  private final WorkflowExecutionHelper workflowExecutionHelper = new WorkflowExecutionHelper();

  @BeforeEach
  void setup() {
    workflowExecutionDao = mock(WorkflowExecutionDao.class);
    dataEvolutionUtils = mock(DataEvolutionUtils.class);
    DatasetXsltDao datasetXsltDao = mock(DatasetXsltDao.class);
    DepublishRecordIdDao depublishRecordIdDao = mock(DepublishRecordIdDao.class);
    RedirectionInferrer redirectionInferrer = new RedirectionInferrer(workflowExecutionDao, dataEvolutionUtils);

    workflowExecutionFactory = new WorkflowExecutionFactory(datasetXsltDao, depublishRecordIdDao, redirectionInferrer);
  }

  @Test
  void redirectionReviewWithPerformRedirectsWhenAncestorRootIsDifferent() throws BadContentException {
    final Dataset dataset = getTestDataset();
    final WorkflowExecution workflowExecutionPre = getWorkflowPreReindex(dataset);
    final WorkflowExecution workflowExecutionPost = getWorkflowPostReindex(dataset);

    final PluginWithExecutionId<ExecutablePlugin> httpHarvestPluginWithExecutionId =
        new PluginWithExecutionId<>("executionIdH1",
            (ExecutablePlugin) workflowExecutionHelper.getMetisPluginWithType(workflowExecutionPre, PluginType.HTTP_HARVEST)
                                                      .orElseThrow());

    final PluginWithExecutionId<ExecutablePlugin> httpHarvestPluginWithExecutionId2 =
        new PluginWithExecutionId<>("executionIdH2",
            (ExecutablePlugin) workflowExecutionHelper.getMetisPluginWithType(workflowExecutionPost, PluginType.HTTP_HARVEST)
                                                      .orElseThrow());

    final PluginWithExecutionId<ExecutablePlugin> indexToPublishPluginWithExecutionId =
        new PluginWithExecutionId<>("executionIdV1",
            (ExecutablePlugin) workflowExecutionHelper.getMetisPluginWithType(workflowExecutionPre, PluginType.PUBLISH)
                                                      .orElseThrow());
    when(workflowExecutionDao.getLatestSuccessfulExecutablePlugin(anyString(), any(), anyBoolean()))
        .thenReturn(indexToPublishPluginWithExecutionId);
    final PluginWithExecutionId<ExecutablePlugin> predecessor = new PluginWithExecutionId<>("executionIdV2",
        (ExecutablePlugin) workflowExecutionHelper.getMetisPluginWithType(workflowExecutionPost, PluginType.VALIDATION_INTERNAL)
                                                  .orElseThrow());

    when(dataEvolutionUtils.getRootAncestor(indexToPublishPluginWithExecutionId))
        .thenReturn(httpHarvestPluginWithExecutionId);
    when(dataEvolutionUtils.getRootAncestor(predecessor))
        .thenReturn(httpHarvestPluginWithExecutionId2);

    final Workflow workflow = getWorkflowFromNormalization(dataset);

    workflowExecutionFactory.setValidationInternalProperties(getValidationInternalProperties());
    workflowExecutionFactory.setValidationExternalProperties(getValidationExternalProperties());
    final WorkflowExecution workflowExecution =
        workflowExecutionFactory.createWorkflowExecution(workflow, dataset, predecessor);

    final AbstractMetisPlugin<?> abstractMetisPlugin = workflowExecutionHelper.getMetisPluginWithType(
        workflowExecution, PluginType.PUBLISH).orElseThrow();
    IndexToPublishPluginMetadata indexToPublishPluginMetadata =
        assertInstanceOf(IndexToPublishPluginMetadata.class, abstractMetisPlugin.getPluginMetadata());
    assertTrue(indexToPublishPluginMetadata.isPerformRedirects());
  }

  @Test
  void redirectionReviewWithPerformRedirectsWhenRedirectIdsPresent() throws BadContentException {
    final Dataset dataset = getTestDataset();
    final WorkflowExecution workflowExecutionPre = getWorkflowPreReindex(dataset);

    final PluginWithExecutionId<ExecutablePlugin> indexToPublishPluginPluginWithExecutionId =
        new PluginWithExecutionId<>("executionId",
            (ExecutablePlugin) workflowExecutionHelper.getMetisPluginWithType(workflowExecutionPre, PluginType.PUBLISH)
                                                      .orElseThrow());

    when(workflowExecutionDao.getLatestSuccessfulExecutablePlugin(anyString(), any(), anyBoolean()))
        .thenReturn(indexToPublishPluginPluginWithExecutionId);

    final ObjectId objectId = new ObjectId();
    final Workflow workflow = getWorkflow(objectId, getIndexToPublishPluginMetadata(Date.from(Instant.now()),
        ((ExecutablePlugin) workflowExecutionHelper.getMetisPluginWithType(workflowExecutionPre, PluginType.PREVIEW)
                                                   .orElseThrow()).getPluginMetadata()
                                                                  .getRevisionNamePreviousPlugin(),
        ((ExecutablePlugin) workflowExecutionHelper.getMetisPluginWithType(workflowExecutionPre, PluginType.PREVIEW)
                                                   .orElseThrow()).getPluginMetadata()
                                                                  .getRevisionTimestampPreviousPlugin()));

    dataset.setDatasetIdsToRedirectFrom(List.of("253"));
    final PluginWithExecutionId<ExecutablePlugin> predecessor = new PluginWithExecutionId<>("executionId",
        ((ExecutablePlugin) workflowExecutionHelper.getMetisPluginWithType(workflowExecutionPre, PluginType.PREVIEW)
                                                   .orElseThrow()));

    final WorkflowExecution workflowExecution =
        workflowExecutionFactory.createWorkflowExecution(workflow, dataset, predecessor);

    final AbstractMetisPlugin<?> abstractMetisPlugin = workflowExecutionHelper.getMetisPluginWithType(
        workflowExecution, PluginType.PUBLISH).orElseThrow();
    IndexToPublishPluginMetadata indexToPublishPluginMetadata =
        assertInstanceOf(IndexToPublishPluginMetadata.class, abstractMetisPlugin.getPluginMetadata());
    assertTrue(indexToPublishPluginMetadata.isPerformRedirects());
  }
}
