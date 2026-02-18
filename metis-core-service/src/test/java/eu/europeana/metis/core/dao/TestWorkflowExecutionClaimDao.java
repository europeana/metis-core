package eu.europeana.metis.core.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import dev.morphia.Datastore;
import dev.morphia.DeleteOptions;
import dev.morphia.query.filters.Filters;
import eu.europeana.metis.core.mongo.MorphiaDatastoreProviderImpl;
import eu.europeana.metis.core.utils.TestObjectFactory;
import eu.europeana.metis.core.workflow.WorkflowExecution;
import eu.europeana.metis.core.workflow.WorkflowStatus;
import eu.europeana.metis.mongo.embedded.EmbeddedLocalhostMongo;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@Slf4j
class TestWorkflowExecutionClaimDao {

  private static EmbeddedLocalhostMongo embeddedLocalhostMongo;
  private static MorphiaDatastoreProviderImpl provider;
  private static MongoClient mongoClient;

  private WorkflowExecutionClaimDao workflowExecutionClaimDao;

  @BeforeAll
  static void setupMongo() {
    embeddedLocalhostMongo = new EmbeddedLocalhostMongo();
    embeddedLocalhostMongo.start();

    String mongoHost = embeddedLocalhostMongo.getMongoHost();
    int mongoPort = embeddedLocalhostMongo.getMongoPort();
    mongoClient = MongoClients.create(String.format("mongodb://%s:%s", mongoHost, mongoPort));
    provider = new MorphiaDatastoreProviderImpl(mongoClient, "test");
  }

  @BeforeEach
  void setup() {
    workflowExecutionClaimDao = new WorkflowExecutionClaimDao(provider);
  }

  @AfterAll
  static void destroy() {
    embeddedLocalhostMongo.stop();
  }

  @AfterEach
  void cleanUp() {
    Datastore datastore = provider.getDatastore();
    datastore.find(WorkflowExecution.class).delete(new DeleteOptions().multi(true));
  }

  @Test
  void claimNextExecution_shouldClaimNewInqueue() {
    WorkflowExecution workflowExecution = TestObjectFactory.createWorkflowExecutionObject();

    workflowExecution.setWorkflowStatus(WorkflowStatus.INQUEUE);
    workflowExecution.setClaimedByInstance(null);
    workflowExecution.setStartedDate(null);

    provider.getDatastore().save(workflowExecution);

    WorkflowExecution claimedWorkflowExecution = workflowExecutionClaimDao.claimNextExecution(Duration.ofMinutes(10));

    assertNotNull(claimedWorkflowExecution);
    assertEquals(WorkflowStatus.RUNNING, claimedWorkflowExecution.getWorkflowStatus());
    assertNotNull(claimedWorkflowExecution.getStartedDate());
    assertNotNull(claimedWorkflowExecution.getUpdatedDate());
    assertEquals(provider.getInstanceId(), claimedWorkflowExecution.getClaimedByInstance());
  }

  @Test
  void claimNextExecution_shouldClaimRequeuedExecution() {
    WorkflowExecution workflowExecution = TestObjectFactory.createWorkflowExecutionObject();

    workflowExecution.setWorkflowStatus(WorkflowStatus.INQUEUE);
    workflowExecution.setClaimedByInstance(null);
    workflowExecution.setStartedDate(Date.from(Instant.now().minusSeconds(60)));

    provider.getDatastore().save(workflowExecution);

    WorkflowExecution claimedWorkflowExecution = workflowExecutionClaimDao.claimNextExecution(Duration.ofMinutes(10));

    assertNotNull(claimedWorkflowExecution);
    assertEquals(WorkflowStatus.RUNNING, claimedWorkflowExecution.getWorkflowStatus());
    assertEquals(provider.getInstanceId(), claimedWorkflowExecution.getClaimedByInstance());
    assertEquals(workflowExecution.getStartedDate(), claimedWorkflowExecution.getStartedDate());
  }

  @Test
  void claimNextExecution_shouldClaimStaleRunning() {
    WorkflowExecution workflowExecution = TestObjectFactory.createWorkflowExecutionObject();

    workflowExecution.setWorkflowStatus(WorkflowStatus.RUNNING);
    workflowExecution.setClaimedByInstance("old-instance");
    workflowExecution.setUpdatedDate(Date.from(Instant.now().minusSeconds(120)));

    provider.getDatastore().save(workflowExecution);

    WorkflowExecution claimedWorkflowExecution = workflowExecutionClaimDao.claimNextExecution(Duration.ofSeconds(60));

    assertNotNull(claimedWorkflowExecution);
    assertEquals(WorkflowStatus.RUNNING, claimedWorkflowExecution.getWorkflowStatus());
    assertEquals(provider.getInstanceId(), claimedWorkflowExecution.getClaimedByInstance());
    assertTrue(claimedWorkflowExecution.getUpdatedDate().after(workflowExecution.getUpdatedDate()));
  }

  @Test
  void claimNextExecution_shouldPreferStaleOverOthers() {
    WorkflowExecution newInqueue1 = TestObjectFactory.createWorkflowExecutionObject();
    newInqueue1.setWorkflowStatus(WorkflowStatus.INQUEUE);
    newInqueue1.setStartedDate(null);
    WorkflowExecution newInqueue2 = TestObjectFactory.createWorkflowExecutionObject();
    newInqueue2.setWorkflowStatus(WorkflowStatus.INQUEUE);
    newInqueue2.setStartedDate(null);
    WorkflowExecution requeued1 = TestObjectFactory.createWorkflowExecutionObject();
    requeued1.setWorkflowStatus(WorkflowStatus.INQUEUE);
    requeued1.setStartedDate(Date.from(Instant.now().minusSeconds(30)));

    WorkflowExecution staleWorkflowExecution = TestObjectFactory.createWorkflowExecutionObject();
    staleWorkflowExecution.setWorkflowStatus(WorkflowStatus.RUNNING);
    staleWorkflowExecution.setUpdatedDate(Date.from(Instant.now().minusSeconds(300)));
    staleWorkflowExecution.setClaimedByInstance("old-instance");

    provider.getDatastore().save(newInqueue1);
    provider.getDatastore().save(requeued1);
    provider.getDatastore().save(newInqueue2);
    provider.getDatastore().save(staleWorkflowExecution);

    WorkflowExecution workflowExecution = workflowExecutionClaimDao.claimNextExecution(Duration.ofSeconds(60));
    assertEquals(staleWorkflowExecution.getId(), workflowExecution.getId());
    workflowExecution = workflowExecutionClaimDao.claimNextExecution(Duration.ofSeconds(60));
    assertEquals(requeued1.getId(), workflowExecution.getId());
    workflowExecution = workflowExecutionClaimDao.claimNextExecution(Duration.ofSeconds(60));
    assertEquals(newInqueue1.getId(), workflowExecution.getId());
    workflowExecution = workflowExecutionClaimDao.claimNextExecution(Duration.ofSeconds(60));
    assertEquals(newInqueue2.getId(), workflowExecution.getId());
  }

  @Test
  void claimNextExecution_shouldReturnNullWhenNothingEligible() {
    WorkflowExecution workflowExecution = TestObjectFactory.createWorkflowExecutionObject();

    workflowExecution.setWorkflowStatus(WorkflowStatus.RUNNING);
    workflowExecution.setClaimedByInstance("instance-a");
    workflowExecution.setUpdatedDate(new Date());

    provider.getDatastore().save(workflowExecution);

    WorkflowExecution claimedWorkflowExecution = workflowExecutionClaimDao.claimNextExecution(Duration.ofMinutes(10));

    assertNull(claimedWorkflowExecution);
  }

  @Test
  void claimNextExecution_shouldClaimOldestFirst() {
    WorkflowExecution olderWorkflowExecution = TestObjectFactory.createWorkflowExecutionObject();
    olderWorkflowExecution.setWorkflowStatus(WorkflowStatus.INQUEUE);
    olderWorkflowExecution.setCreatedDate(new Date(1000));

    WorkflowExecution newerWorkflowExecution = TestObjectFactory.createWorkflowExecutionObject();
    newerWorkflowExecution.setWorkflowStatus(WorkflowStatus.INQUEUE);
    newerWorkflowExecution.setCreatedDate(new Date(2000));

    provider.getDatastore().save(newerWorkflowExecution);
    provider.getDatastore().save(olderWorkflowExecution);

    WorkflowExecution claimedWorkflowExecution = workflowExecutionClaimDao.claimNextExecution(Duration.ofMinutes(1));

    assertEquals(olderWorkflowExecution.getId(), claimedWorkflowExecution.getId());
  }

  @Test
  void requeue_shouldReturnTrueWhenUpdated() {
    WorkflowExecution workflowExecution = TestObjectFactory.createWorkflowExecutionObject();

    workflowExecution.setWorkflowStatus(WorkflowStatus.RUNNING);
    workflowExecution.setClaimedByInstance(provider.getInstanceId());

    provider.getDatastore().save(workflowExecution);

    boolean result = workflowExecutionClaimDao.requeue(workflowExecution);

    assertTrue(result);

    WorkflowExecution updatedWorkflowExecution =
        provider.getDatastore()
                .find(WorkflowExecution.class)
                .filter(Filters.eq("_id", workflowExecution.getId()))
                .first();

    assertNotNull(updatedWorkflowExecution);
    assertEquals(WorkflowStatus.INQUEUE, updatedWorkflowExecution.getWorkflowStatus());
    assertNull(updatedWorkflowExecution.getClaimedByInstance());
  }

  @Test
  void requeue_shouldReturnFalseWhenClaimDoesNotMatch() {
    WorkflowExecution workflowExecution = TestObjectFactory.createWorkflowExecutionObject();

    workflowExecution.setWorkflowStatus(WorkflowStatus.RUNNING);
    workflowExecution.setClaimedByInstance("other-instance");

    provider.getDatastore().save(workflowExecution);
    workflowExecution.setClaimedByInstance("wrong");

    boolean result = workflowExecutionClaimDao.requeue(workflowExecution);

    assertFalse(result);
  }

  @Test
  void requeue_shouldReturnFalseWhenNotRunning() {
    WorkflowExecution workflowExecution = TestObjectFactory.createWorkflowExecutionObject();

    workflowExecution.setWorkflowStatus(WorkflowStatus.FINISHED);
    workflowExecution.setClaimedByInstance(provider.getInstanceId());

    provider.getDatastore().save(workflowExecution);

    boolean result = workflowExecutionClaimDao.requeue(workflowExecution);

    assertFalse(result);
  }

  @Test
  void concurrentBulkClaim_shouldDistributeWork() throws Exception {
    int totalExecutions = 1000;
    int workers = 16;

    // Insert executions
    for (int i = 0; i < totalExecutions; i++) {
      WorkflowExecution workflowExecution = TestObjectFactory.createWorkflowExecutionObject();
      workflowExecution.setWorkflowStatus(WorkflowStatus.INQUEUE);
      workflowExecution.setClaimedByInstance(null);
      workflowExecution.setStartedDate(null);

      provider.getDatastore().save(workflowExecution);
    }

    Set<ObjectId> claimedIds = ConcurrentHashMap.newKeySet();
    try(ExecutorService executorService = Executors.newFixedThreadPool(workers)) {

      CountDownLatch startGate = new CountDownLatch(1);
      CountDownLatch finishGate = new CountDownLatch(workers);

      for (int i = 0; i < workers; i++) {

        MorphiaDatastoreProviderImpl morphiaDatastoreProvider = new MorphiaDatastoreProviderImpl(mongoClient, "test");
        WorkflowExecutionClaimDao executionClaimDao = new WorkflowExecutionClaimDao(morphiaDatastoreProvider);

        executorService.submit(() -> {

          startGate.await();

          while (true) {
            WorkflowExecution workflowExecution = executionClaimDao.claimNextExecution(Duration.ofMinutes(5));
            if (workflowExecution == null) {
              break;
            }
            claimedIds.add(workflowExecution.getId());
          }
          finishGate.countDown();
          return null;
        });
      }

      startGate.countDown();
      finishGate.await();
      executorService.shutdown();
    }

    assertEquals(totalExecutions, claimedIds.size());

    Set<String> instances = provider.getDatastore().getDatabase()
                                    .getCollection(WorkflowExecution.class.getSimpleName())
                                    .distinct("claimedByInstance", String.class)
                                    .into(new HashSet<>());

    assertEquals(1, instances.size());
    log.info("Instances participating: {}", instances.size());
  }
}
