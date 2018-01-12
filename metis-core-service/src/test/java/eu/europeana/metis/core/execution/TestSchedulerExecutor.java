package eu.europeana.metis.core.execution;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisConnectionException;
import eu.europeana.metis.core.exceptions.NoDatasetFoundException;
import eu.europeana.metis.core.service.OrchestratorService;
import eu.europeana.metis.core.test.utils.TestObjectFactory;
import eu.europeana.metis.core.workflow.ScheduleFrequence;
import eu.europeana.metis.core.workflow.ScheduledWorkflow;

/**
 * @author Simon Tzanakis (Simon.Tzanakis@europeana.eu)
 * @since 2017-10-17
 */
public class TestSchedulerExecutor {

  private static int periodicSchedulerCheckInSecs = 1;
  private static OrchestratorService orchestratorService;
  private static RedissonClient redissonClient;
  private static final String SCHEDULER_LOCK = "schedulerLock";

  @BeforeClass
  public static void prepare() {
    orchestratorService = Mockito.mock(OrchestratorService.class);
    redissonClient = Mockito.mock(RedissonClient.class);
  }

  @After
  public void cleanUp() {
    Mockito.reset(orchestratorService);
    Mockito.reset(redissonClient);
  }

  @Test
  public void run() throws Exception {
    RLock rlock = mock(RLock.class);
    when(redissonClient.getFairLock(SCHEDULER_LOCK)).thenReturn(rlock);
    doNothing().when(rlock).lock();
    SchedulerExecutor schedulerExecutor = new SchedulerExecutor(orchestratorService, redissonClient);

    int userWorkflowExecutionsPerRequest = 3;
    int listSize = userWorkflowExecutionsPerRequest - 1; //To not trigger paging
    Date now = new Date();
    now.setTime(now.getTime() + periodicSchedulerCheckInSecs * 1000);
    List<ScheduledWorkflow> listOfScheduledWorkflowsWithDateONCE = TestObjectFactory
        .createListOfScheduledUserWorkflowsWithDateAndFrequence(listSize, now, ScheduleFrequence.ONCE);
    List<ScheduledWorkflow> listOfScheduledWorkflowsWithDateDAILY = TestObjectFactory
        .createListOfScheduledUserWorkflowsWithDateAndFrequence(listSize, now, ScheduleFrequence.DAILY);
    List<ScheduledWorkflow> listOfScheduledWorkflowsWithDateWEEKLY = TestObjectFactory
        .createListOfScheduledUserWorkflowsWithDateAndFrequence(listSize, now, ScheduleFrequence.WEEKLY);
    List<ScheduledWorkflow> listOfScheduledWorkflowsWithDateMONTHLY = TestObjectFactory
        .createListOfScheduledUserWorkflowsWithDateAndFrequence(listSize, now, ScheduleFrequence.MONTHLY);

    when(orchestratorService.getScheduledWorkflowsPerRequest())
        .thenReturn(userWorkflowExecutionsPerRequest);

    when(orchestratorService.getAllScheduledWorkflowsByDateRangeONCE(any(LocalDateTime.class),
        any(LocalDateTime.class), isNull()))
        .thenReturn(listOfScheduledWorkflowsWithDateONCE);
    when(
        orchestratorService.getAllScheduledWorkflows(any(ScheduleFrequence.class), isNull()))
        .thenReturn(listOfScheduledWorkflowsWithDateDAILY).thenReturn(
        listOfScheduledWorkflowsWithDateWEEKLY).thenReturn(
        listOfScheduledWorkflowsWithDateMONTHLY);
    doThrow(new NoDatasetFoundException("Some Error")).doNothing().when(orchestratorService)
        .addWorkflowInQueueOfWorkflowExecutions(anyInt(), anyString(), anyString(),
            anyInt()); //Throw an exception as well, should continue execution after that
    doNothing().when(rlock).unlock();

    schedulerExecutor.performScheduling();

    verify(orchestratorService, times(4)).getScheduledWorkflowsPerRequest();
    verify(orchestratorService, times(1))
        .getAllScheduledWorkflowsByDateRangeONCE(any(LocalDateTime.class),
            any(LocalDateTime.class), isNull());
    verify(orchestratorService, times(3))
        .getAllScheduledWorkflows(any(ScheduleFrequence.class), isNull());
    verify(orchestratorService, atMost(listSize * 4))
        .addWorkflowInQueueOfWorkflowExecutions(anyInt(), anyString(), anyString(),
            anyInt());
  }

  @Test
  public void runAllSchedulesOutOfRange() throws Exception {
    RLock rlock = mock(RLock.class);
    when(redissonClient.getFairLock(SCHEDULER_LOCK)).thenReturn(rlock);
    doNothing().when(rlock).lock();
    SchedulerExecutor schedulerExecutor = new SchedulerExecutor(orchestratorService, redissonClient);

    int userWorkflowExecutionsPerRequest = 3;
    int listSize = userWorkflowExecutionsPerRequest - 1; //To not trigger paging
    Date past = new Date();
    past.setTime(past.getTime() - periodicSchedulerCheckInSecs * 1000);
    List<ScheduledWorkflow> listOfScheduledWorkflowsWithDateDAILY = TestObjectFactory
        .createListOfScheduledUserWorkflowsWithDateAndFrequence(listSize, past, ScheduleFrequence.DAILY);
    List<ScheduledWorkflow> listOfScheduledWorkflowsWithDateWEEKLY = TestObjectFactory
        .createListOfScheduledUserWorkflowsWithDateAndFrequence(listSize, past, ScheduleFrequence.WEEKLY);
    List<ScheduledWorkflow> listOfScheduledWorkflowsWithDateMONTHLY = TestObjectFactory
        .createListOfScheduledUserWorkflowsWithDateAndFrequence(listSize, past, ScheduleFrequence.MONTHLY);

    when(orchestratorService.getScheduledWorkflowsPerRequest())
        .thenReturn(userWorkflowExecutionsPerRequest);

    when(orchestratorService.getAllScheduledWorkflowsByDateRangeONCE(any(LocalDateTime.class),
        any(LocalDateTime.class), isNull()))
        .thenReturn(new ArrayList<>());
    when(
        orchestratorService.getAllScheduledWorkflows(any(ScheduleFrequence.class), isNull()))
        .thenReturn(listOfScheduledWorkflowsWithDateDAILY).thenReturn(
        listOfScheduledWorkflowsWithDateWEEKLY).thenReturn(
        listOfScheduledWorkflowsWithDateMONTHLY);
    doThrow(new NoDatasetFoundException("Some Error")).doNothing().when(orchestratorService)
        .addWorkflowInQueueOfWorkflowExecutions(anyInt(), anyString(), anyString(),
            anyInt()); //Throw an exception as well, should continue execution after that
    doNothing().when(rlock).unlock();

    schedulerExecutor.performScheduling();

    verify(orchestratorService, times(4)).getScheduledWorkflowsPerRequest();
    verify(orchestratorService, times(1))
        .getAllScheduledWorkflowsByDateRangeONCE(any(LocalDateTime.class),
            any(LocalDateTime.class), isNull());
    verify(orchestratorService, times(3))
        .getAllScheduledWorkflows(any(ScheduleFrequence.class), isNull());
    verify(orchestratorService, times(0))
        .addWorkflowInQueueOfWorkflowExecutions(anyInt(), anyString(), anyString(),
            anyInt());
  }

  @Test
  public void runThatThrowsExceptionDuringLockAndContinues() {
    RLock rlock = mock(RLock.class);
    when(redissonClient.getFairLock(SCHEDULER_LOCK)).thenReturn(rlock);
    SchedulerExecutor schedulerExecutor = new SchedulerExecutor(orchestratorService, redissonClient);
    doThrow(new RedisConnectionException("Connection error")).when(rlock).lock();
    schedulerExecutor.performScheduling();
    verifyNoMoreInteractions(orchestratorService);
  }

  @Test
  public void runThatThrowsExceptionDuringUnlockAndContinues() {
    RLock rlock = mock(RLock.class);
    when(redissonClient.getFairLock(SCHEDULER_LOCK)).thenReturn(rlock);
    SchedulerExecutor schedulerExecutor = new SchedulerExecutor(orchestratorService, redissonClient);
    doThrow(new RedisConnectionException("Connection error")).when(rlock).lock();
    doThrow(new RedisConnectionException("Connection error")).when(rlock).unlock();
    schedulerExecutor.performScheduling();
    verifyNoMoreInteractions(orchestratorService);
  }

  @Test
  public void close() {
    SchedulerExecutor schedulerExecutor = new SchedulerExecutor(orchestratorService, redissonClient);
    when(redissonClient.isShutdown()).thenReturn(false);
    schedulerExecutor.close();
    verify(redissonClient, times(1)).shutdown();
  }
}
