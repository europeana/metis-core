package eu.europeana.metis.core.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import eu.europeana.metis.core.dao.DatasetDao;
import eu.europeana.metis.core.dao.ScheduledWorkflowDao;
import eu.europeana.metis.core.dao.WorkflowDao;
import eu.europeana.metis.core.dataset.Dataset;
import eu.europeana.metis.core.exceptions.NoDatasetFoundException;
import eu.europeana.metis.core.exceptions.NoScheduledWorkflowFoundException;
import eu.europeana.metis.core.exceptions.NoWorkflowFoundException;
import eu.europeana.metis.core.exceptions.ScheduledWorkflowAlreadyExistsException;
import eu.europeana.metis.core.utils.TestObjectFactory;
import eu.europeana.metis.core.workflow.ScheduleFrequence;
import eu.europeana.metis.core.workflow.ScheduledWorkflow;
import eu.europeana.metis.core.workflow.Workflow;
import eu.europeana.metis.exception.BadContentException;
import java.time.LocalDateTime;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TestScheduleWorkflowService {

  private static ScheduledWorkflowDao scheduledWorkflowDao;
  private static WorkflowDao workflowDao;
  private static DatasetDao datasetDao;
  private static ScheduleWorkflowService scheduleWorkflowService;

  @BeforeAll
  static void prepare() {
    workflowDao = mock(WorkflowDao.class);
    scheduledWorkflowDao = mock(ScheduledWorkflowDao.class);
    datasetDao = mock(DatasetDao.class);

    scheduleWorkflowService = new ScheduleWorkflowService(scheduledWorkflowDao, workflowDao, datasetDao);
  }

  @AfterEach
  void cleanUp() {
    reset(workflowDao);
    reset(scheduledWorkflowDao);
    reset(datasetDao);
  }

  @Test
  void getScheduledWorkflowByDatasetId() throws NoDatasetFoundException {
    final String datasetId = Integer.toString(TestObjectFactory.DATASETID);
    scheduleWorkflowService.getScheduledWorkflowByDatasetId(datasetId);
    verify(scheduledWorkflowDao, times(1)).getScheduledWorkflowByDatasetId(anyString());
    verifyNoMoreInteractions(scheduledWorkflowDao);
  }

  @Test
  void scheduleWorkflow() throws Exception {
    final String datasetId = Integer.toString(TestObjectFactory.DATASETID);
    ScheduledWorkflow scheduledWorkflow = TestObjectFactory.createScheduledWorkflowObject();
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    Workflow workflow = TestObjectFactory.createWorkflowObject();
    when(datasetDao.getDatasetByDatasetId(datasetId)).thenReturn(dataset);
    when(workflowDao.getWorkflow(datasetId)).thenReturn(workflow);
    when(scheduledWorkflowDao.existsForDatasetId(datasetId)).thenReturn(null);
    when(scheduledWorkflowDao.create(scheduledWorkflow)).thenReturn(new ScheduledWorkflow(null, datasetId, null, 0));
    scheduleWorkflowService.scheduleWorkflow(scheduledWorkflow);
    verify(scheduledWorkflowDao, times(1)).create(scheduledWorkflow);
  }

  @Test
  void scheduleWorkflow_NoDatasetFoundException() {
    final String datasetId = Integer.toString(TestObjectFactory.DATASETID);
    ScheduledWorkflow scheduledWorkflow = TestObjectFactory.createScheduledWorkflowObject();
    when(datasetDao.getDatasetByDatasetId(datasetId)).thenReturn(null);
    assertThrows(NoDatasetFoundException.class, () -> scheduleWorkflowService.scheduleWorkflow(scheduledWorkflow));
  }

  @Test
  void scheduleWorkflow_NoWorkflowFoundException() {
    final String datasetId = Integer.toString(TestObjectFactory.DATASETID);
    ScheduledWorkflow scheduledWorkflow = TestObjectFactory.createScheduledWorkflowObject();
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    when(datasetDao.getDatasetByDatasetId(datasetId)).thenReturn(dataset);
    when(workflowDao.getWorkflow(datasetId)).thenReturn(null);
    assertThrows(NoWorkflowFoundException.class, () -> scheduleWorkflowService.scheduleWorkflow(scheduledWorkflow));
  }

  @Test
  void scheduleWorkflow_ScheduledWorkflowAlreadyExistsException() {
    final String datasetId = Integer.toString(TestObjectFactory.DATASETID);
    ScheduledWorkflow scheduledWorkflow = TestObjectFactory.createScheduledWorkflowObject();
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    Workflow workflow = TestObjectFactory.createWorkflowObject();
    when(datasetDao.getDatasetByDatasetId(datasetId)).thenReturn(dataset);
    when(workflowDao.getWorkflow(datasetId)).thenReturn(workflow);
    when(scheduledWorkflowDao.existsForDatasetId(datasetId)).thenReturn(new ObjectId().toString());
    assertThrows(ScheduledWorkflowAlreadyExistsException.class,
        () -> scheduleWorkflowService.scheduleWorkflow(scheduledWorkflow));
  }

  @Test
  void scheduleUserWorkflow_BadContentException_nullPointerDate() {
    final String datasetId = Integer.toString(TestObjectFactory.DATASETID);
    ScheduledWorkflow scheduledWorkflow = TestObjectFactory.createScheduledWorkflowObject();
    scheduledWorkflow.setPointerDate(null);
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    Workflow workflow = TestObjectFactory.createWorkflowObject();
    when(datasetDao.getDatasetByDatasetId(datasetId)).thenReturn(dataset);
    when(workflowDao.getWorkflow(datasetId)).thenReturn(workflow);
    when(scheduledWorkflowDao.existsForDatasetId(datasetId)).thenReturn(null);
    assertThrows(BadContentException.class, () -> scheduleWorkflowService.scheduleWorkflow(scheduledWorkflow));
  }

  @Test
  void scheduleWorkflow_BadContentException_NULLScheduleFrequence() {
    final String datasetId = Integer.toString(TestObjectFactory.DATASETID);
    ScheduledWorkflow scheduledWorkflow = TestObjectFactory.createScheduledWorkflowObject();
    scheduledWorkflow.setScheduleFrequence(ScheduleFrequence.NULL);
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    Workflow workflow = TestObjectFactory.createWorkflowObject();
    when(datasetDao.getDatasetByDatasetId(datasetId)).thenReturn(dataset);
    when(workflowDao.getWorkflow(datasetId)).thenReturn(workflow);
    when(scheduledWorkflowDao.existsForDatasetId(datasetId)).thenReturn(null);
    assertThrows(BadContentException.class, () -> scheduleWorkflowService.scheduleWorkflow(scheduledWorkflow));
  }

  @Test
  void scheduleWorkflow_BadContentException_nullScheduleFrequence() {
    final String datasetId = Integer.toString(TestObjectFactory.DATASETID);
    ScheduledWorkflow scheduledWorkflow = TestObjectFactory.createScheduledWorkflowObject();
    scheduledWorkflow.setScheduleFrequence(null);
    Dataset dataset = TestObjectFactory.createDataset(TestObjectFactory.DATASETNAME);
    Workflow workflow = TestObjectFactory.createWorkflowObject();
    when(datasetDao.getDatasetByDatasetId(datasetId)).thenReturn(dataset);
    when(workflowDao.getWorkflow(datasetId)).thenReturn(workflow);
    when(scheduledWorkflowDao.existsForDatasetId(datasetId)).thenReturn(null);
    assertThrows(BadContentException.class, () -> scheduleWorkflowService.scheduleWorkflow(scheduledWorkflow));
  }

  @Test
  void getAllScheduledWorkflows() {
    scheduleWorkflowService.getAllScheduledWorkflows(ScheduleFrequence.ONCE, 0);
    verify(scheduledWorkflowDao, times(1)).getAllScheduledWorkflows(any(ScheduleFrequence.class), anyInt());
  }

  @Test
  void getAllScheduledUserWorkflowsByDateRangeONCE() {
    scheduleWorkflowService.getAllScheduledWorkflowsByDateRangeONCE(LocalDateTime.now(), LocalDateTime.now(), 0);
    verify(scheduledWorkflowDao, times(1)).getAllScheduledWorkflowsByDateRangeONCE(any(LocalDateTime.class),
        any(LocalDateTime.class), anyInt());
  }

  @Test
  void updateScheduledWorkflow() throws Exception {
    final String datasetId = Integer.toString(TestObjectFactory.DATASETID);
    ScheduledWorkflow scheduledWorkflow = TestObjectFactory.createScheduledWorkflowObject();
    Workflow workflow = TestObjectFactory.createWorkflowObject();

    when(workflowDao.getWorkflow(datasetId)).thenReturn(workflow);
    when(scheduledWorkflowDao.existsForDatasetId(datasetId)).thenReturn(new ObjectId().toString());
    when(scheduledWorkflowDao.update(scheduledWorkflow)).thenReturn(new ObjectId().toString());
    scheduleWorkflowService.updateScheduledWorkflow(scheduledWorkflow);
    verify(scheduledWorkflowDao, times(1)).update(scheduledWorkflow);
  }

  @Test
  void updateScheduledUserWorkflow_NoUserWorkflowFoundException() {
    final String datasetId = Integer.toString(TestObjectFactory.DATASETID);
    ScheduledWorkflow scheduledWorkflow = TestObjectFactory.createScheduledWorkflowObject();
    when(workflowDao.getWorkflow(datasetId)).thenReturn(null);
    assertThrows(NoWorkflowFoundException.class, () -> scheduleWorkflowService.updateScheduledWorkflow(scheduledWorkflow));
  }

  @Test
  void updateScheduledWorkflow_NoScheduledWorkflowFoundException() {
    final String datasetId = Integer.toString(TestObjectFactory.DATASETID);
    ScheduledWorkflow scheduledWorkflow = TestObjectFactory.createScheduledWorkflowObject();
    Workflow workflow = TestObjectFactory.createWorkflowObject();

    when(workflowDao.getWorkflow(datasetId)).thenReturn(workflow);
    when(scheduledWorkflowDao.existsForDatasetId(datasetId)).thenReturn(null);
    assertThrows(NoScheduledWorkflowFoundException.class,
        () -> scheduleWorkflowService.updateScheduledWorkflow(scheduledWorkflow));
  }

  @Test
  void updateScheduledWorkflow_BadContentException_nullPointerDate() {
    final String datasetId = Integer.toString(TestObjectFactory.DATASETID);
    ScheduledWorkflow scheduledWorkflow = TestObjectFactory.createScheduledWorkflowObject();
    scheduledWorkflow.setPointerDate(null);
    Workflow workflow = TestObjectFactory.createWorkflowObject();

    when(workflowDao.getWorkflow(datasetId)).thenReturn(workflow);
    when(scheduledWorkflowDao.existsForDatasetId(datasetId)).thenReturn(new ObjectId().toString());
    assertThrows(BadContentException.class, () -> scheduleWorkflowService.updateScheduledWorkflow(scheduledWorkflow));
  }

  @Test
  void updateScheduledWorkflow_BadContentException_NULLScheduleFrequence() {
    final String datasetId = Integer.toString(TestObjectFactory.DATASETID);
    ScheduledWorkflow scheduledWorkflow = TestObjectFactory.createScheduledWorkflowObject();
    scheduledWorkflow.setScheduleFrequence(ScheduleFrequence.NULL);
    Workflow workflow = TestObjectFactory.createWorkflowObject();

    when(workflowDao.getWorkflow(datasetId)).thenReturn(workflow);
    when(scheduledWorkflowDao.existsForDatasetId(datasetId)).thenReturn(new ObjectId().toString());
    assertThrows(BadContentException.class, () -> scheduleWorkflowService.updateScheduledWorkflow(scheduledWorkflow));
  }

  @Test
  void updateScheduledWorkflow_BadContentException_nullScheduleFrequence() {
    final String datasetId = Integer.toString(TestObjectFactory.DATASETID);
    ScheduledWorkflow scheduledWorkflow = TestObjectFactory
        .createScheduledWorkflowObject();
    scheduledWorkflow.setScheduleFrequence(null);
    Workflow workflow = TestObjectFactory.createWorkflowObject();

    when(workflowDao.getWorkflow(datasetId)).thenReturn(workflow);
    when(scheduledWorkflowDao.existsForDatasetId(datasetId)).thenReturn(new ObjectId().toString());
    assertThrows(BadContentException.class, () -> scheduleWorkflowService.updateScheduledWorkflow(scheduledWorkflow));
  }

  @Test
  void deleteScheduledWorkflow() throws NoDatasetFoundException {
    final String datasetId = Integer.toString(TestObjectFactory.DATASETID);
    scheduleWorkflowService.deleteScheduledWorkflow(datasetId);
    verify(scheduledWorkflowDao, times(1)).deleteScheduledWorkflow(anyString());
  }

  @Test
  void getScheduledWorkflowsPerRequest() {
    scheduleWorkflowService.getScheduledWorkflowsPerRequest();
    verify(scheduledWorkflowDao, times(1)).getScheduledWorkflowPerRequest();
  }

}
