package eu.europeana.metis.core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import eu.europeana.metis.core.dao.DatasetDao;
import eu.europeana.metis.core.dao.DepublishRecordIdDao;
import eu.europeana.metis.core.dataset.DatasetExecutionInformation;
import eu.europeana.metis.core.dataset.DatasetExecutionInformation.PublicationStatus;
import eu.europeana.metis.core.dataset.DepublishRecordId;
import eu.europeana.metis.core.rest.DepublishRecordIdView;
import eu.europeana.metis.core.util.DepublishRecordIdSortField;
import eu.europeana.metis.core.util.SortDirection;
import eu.europeana.metis.core.utils.TestObjectFactory;
import eu.europeana.metis.core.workflow.Workflow;
import eu.europeana.metis.core.workflow.WorkflowExecution;
import eu.europeana.metis.exception.GenericMetisException;
import eu.europeana.metis.utils.DepublicationReason;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class TestDepublishRecordIdService {

  private static OrchestratorService orchestratorService;
  private static DatasetDao datasetDao;
  private static DepublishRecordIdDao depublishRecordIdDao;
  private static DepublishRecordIdService depublishRecordIdService;
  private static String datasetId;

  @BeforeAll
  static void setUp() {
    orchestratorService = mock(OrchestratorService.class);
    datasetDao = mock(DatasetDao.class);
    depublishRecordIdDao = mock(DepublishRecordIdDao.class);
    datasetId = Integer.toString(TestObjectFactory.DATASETID);
    depublishRecordIdService = spy(new DepublishRecordIdService(orchestratorService, depublishRecordIdDao, datasetDao));
  }

  @BeforeEach
  void cleanUp() {
    reset(orchestratorService);
    reset(datasetDao);
    reset(depublishRecordIdDao);
    reset(depublishRecordIdService);
  }

  @Test
  void addRecordIdsToBeDepublishedTest() throws GenericMetisException {
    depublishRecordIdService.addRecordIdsToBeDepublished(datasetId, "1002");

    verify(depublishRecordIdService, times(1)).checkAndNormalizeRecordIds(any(), any());
    verify(depublishRecordIdDao, times(1)).createRecordIdsToBeDepublished(any(), any());
    verifyNoMoreInteractions(orchestratorService);


  }

  @Test
  void deletePendingRecordIdsTest() throws GenericMetisException {
    depublishRecordIdService.deletePendingRecordIds(datasetId, "1002");

    verify(depublishRecordIdService, times(1)).checkAndNormalizeRecordIds(any(), any());
    verify(depublishRecordIdDao, times(1)).deletePendingRecordIds(any(), any());
    verifyNoMoreInteractions(orchestratorService);

  }

  @Test
  void getDepublishRecordIdsTest() throws GenericMetisException {

    // Mock the DAO
    final DepublishRecordId depublishRecordId = new DepublishRecordId();
    depublishRecordId.setRecordId("RECORD_ID");
    doReturn(List.of(new DepublishRecordIdView(depublishRecordId))).when(depublishRecordIdDao)
        .getDepublishRecordIds(eq(datasetId), anyInt(), any(), any(), anyString());

    final var result = depublishRecordIdService.getDepublishRecordIds(datasetId, 1, DepublishRecordIdSortField.RECORD_ID, SortDirection.ASCENDING, "search");

    verify(depublishRecordIdDao, times(1)).getDepublishRecordIds(datasetId,
            1, DepublishRecordIdSortField.RECORD_ID, SortDirection.ASCENDING, "search");
    verify(depublishRecordIdDao, times(1)).getDepublishRecordIds(anyString(),
            anyInt(), any(), any(), anyString());
    verifyNoMoreInteractions(orchestratorService);

    // verify the result
    assertEquals(1, result.getListSize());
    assertEquals(1, result.getResults().size());
    assertEquals(depublishRecordId.getRecordId(), result.getResults().getFirst().getRecordId());
  }

  @Test
  void createAndAddInQueueDepublishWorkflowExecutionTest() throws GenericMetisException {
    //Mock Workflow and Set<String>
    String mockRecordIdsSeparateLines = "RECORD_ID";
    final Set<String> mockNormalizedRecordIds = Set.of(mockRecordIdsSeparateLines);
    doReturn(mockNormalizedRecordIds).when(depublishRecordIdService).checkAndNormalizeRecordIds(datasetId,
        mockRecordIdsSeparateLines);

    //Do the actual call
    depublishRecordIdService.createAndAddInQueueDepublishWorkflowExecution(datasetId, true, mockRecordIdsSeparateLines,
        DepublicationReason.GENERIC, TestObjectFactory.USER_ID);

    verify(orchestratorService, times(1))
        .addWorkflowInQueueOfWorkflowExecutions(anyString(), any(), any(), anyString());

    //Verify values
    ArgumentCaptor<Workflow> workflowArgumentCaptor = ArgumentCaptor.forClass(Workflow.class);
    verify(orchestratorService, times(1))
        .addWorkflowInQueueOfWorkflowExecutions(anyString(), workflowArgumentCaptor.capture(), any(), anyString());
    Workflow sentWorkflow = workflowArgumentCaptor.getValue();
    assertEquals(datasetId, sentWorkflow.getDatasetId());
  }

  @Test
  void canTriggerDepublicationResultTrueTest() throws GenericMetisException {
    final DatasetExecutionInformation mockExecutionInformation = mock(
        DatasetExecutionInformation.class);

    doReturn(mockExecutionInformation).when(orchestratorService)
        .getDatasetExecutionInformation(datasetId);
    doReturn(PublicationStatus.PUBLISHED).when(mockExecutionInformation).getPublicationStatus();
    doReturn(true).when(mockExecutionInformation).isLastPreviewRecordsReadyForViewing();
    doReturn(true).when(mockExecutionInformation).isLastPublishedRecordsReadyForViewing();
    boolean result = depublishRecordIdService.canTriggerDepublication(datasetId);

    verify(orchestratorService, times(1)).getRunningOrInQueueExecution(datasetId);
    verify(orchestratorService, times(1)).getDatasetExecutionInformation(datasetId);
    verify(mockExecutionInformation, times(1)).getPublicationStatus();
    verify(mockExecutionInformation, times(1)).isLastPublishedRecordsReadyForViewing();
    assertTrue(result);
  }

  @Test
  void canTriggerDepublicationResultFalseTest() throws GenericMetisException {
    final WorkflowExecution mockWorkflow = mock(WorkflowExecution.class);

    doReturn(mockWorkflow).when(orchestratorService).getRunningOrInQueueExecution(datasetId);
    boolean result = depublishRecordIdService.canTriggerDepublication(datasetId);

    verify(orchestratorService, times(1)).getRunningOrInQueueExecution(datasetId);
    assertFalse(result);
  }

}
