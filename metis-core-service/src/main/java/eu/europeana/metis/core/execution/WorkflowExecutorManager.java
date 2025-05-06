package eu.europeana.metis.core.execution;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.MessageProperties;
import eu.europeana.metis.core.dao.WorkflowExecutionDao;
import eu.europeana.metis.core.engine.base.EngineTask;
import eu.europeana.metis.core.engine.base.EngineTaskClient;
import eu.europeana.metis.core.engine.base.EngineTaskSettings;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manager class for adding executions in the distributed queue.
 */
public class WorkflowExecutorManager<S extends EngineTaskSettings, T extends EngineTask> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final WorkflowExecutorManagerSettings workflowExecutorManagerSettings;
  private final Channel rabbitmqPublisherChannel;
  private final Channel rabbitmqConsumerChannel;
  private final SemaphoresPerPluginManager semaphoresPerPluginManager;
  private final WorkflowExecutionDao workflowExecutionDao;
  private final WorkflowPostProcessor workflowPostProcessor;
  private final RedissonClient redissonClient;
  private final EngineTaskClient<S, T> engineTaskClient;

  /**
   * Autowired constructor.
   *
   * @param semaphoresPerPluginManager the semaphores per plugin manager
   * @param workflowExecutionDao the DAO for accessing WorkflowExecutions
   * @param workflowPostProcessor the workflow post processor
   * @param rabbitmqPublisherChannel the channel for publishing to RabbitMQ
   * @param rabbitmqConsumerChannel the channel for consuming from RabbitMQ
   * @param redissonClient the redisson client for distributed locks
   * @param engineTaskClient the Data Processing Service client from ECloud
   */
  public WorkflowExecutorManager(
      WorkflowExecutorManagerSettings workflowExecutorManagerSettings, SemaphoresPerPluginManager semaphoresPerPluginManager,
      WorkflowExecutionDao workflowExecutionDao, WorkflowPostProcessor workflowPostProcessor,
      Channel rabbitmqPublisherChannel, Channel rabbitmqConsumerChannel,
      RedissonClient redissonClient, EngineTaskClient<S, T> engineTaskClient) {
    this.workflowExecutorManagerSettings = workflowExecutorManagerSettings;
    this.rabbitmqPublisherChannel = rabbitmqPublisherChannel;
    this.rabbitmqConsumerChannel = rabbitmqConsumerChannel;
    this.semaphoresPerPluginManager = semaphoresPerPluginManager;
    this.workflowExecutionDao = workflowExecutionDao;
    this.workflowPostProcessor = workflowPostProcessor;
    this.redissonClient = redissonClient;
    this.engineTaskClient = engineTaskClient;
  }

  /**
   * Adds a WorkflowExecution identifier in the distributed queue.
   *
   * @param userWorkflowExecutionObjectId the WorkflowExecution identifier
   */
  public void addWorkflowExecutionToQueue(String userWorkflowExecutionObjectId) {
    //Based on Rabbitmq the basicPublish between threads should be controlled(synchronized)
    synchronized (getRabbitmqPublisherChannel()) {
      BasicProperties basicProperties = MessageProperties.PERSISTENT_TEXT_PLAIN.builder().build();
      try {
        //First parameter is the ExchangeName which is not used
        getRabbitmqPublisherChannel().basicPublish("", workflowExecutorManagerSettings.getRabbitmqQueueName(), basicProperties,
            userWorkflowExecutionObjectId.getBytes(StandardCharsets.UTF_8));
      } catch (IOException e) {
        LOGGER.error("WorkflowExecution with objectId: {} not added in queue..",
            userWorkflowExecutionObjectId, e);
      }
    }
  }

  public WorkflowExecutorManagerSettings getWorkflowExecutionSettings() {
    return workflowExecutorManagerSettings;
  }

  public Channel getRabbitmqPublisherChannel() {
    return rabbitmqPublisherChannel;
  }

  public Channel getRabbitmqConsumerChannel() {
    return rabbitmqConsumerChannel;
  }

  public SemaphoresPerPluginManager getSemaphoresPerPluginManager() {
    return semaphoresPerPluginManager;
  }

  public WorkflowExecutionDao getWorkflowExecutionDao() {
    return workflowExecutionDao;
  }

  public WorkflowPostProcessor getWorkflowPostProcessor() {
    return workflowPostProcessor;
  }

  public RedissonClient getRedissonClient() {
    return redissonClient;
  }

  public EngineTaskClient<S, T> getEngineTaskClient() {
    return engineTaskClient;
  }
}
