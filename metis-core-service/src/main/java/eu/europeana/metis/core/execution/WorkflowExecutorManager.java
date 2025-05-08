package eu.europeana.metis.core.execution;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.MessageProperties;
import eu.europeana.metis.core.dao.WorkflowExecutionDao;
import eu.europeana.metis.core.engine.base.AbstractEngineTask;
import eu.europeana.metis.core.engine.base.EngineTaskClient;
import eu.europeana.metis.core.engine.base.AbstractEngineTaskSettings;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manager class for adding executions in the distributed queue.
 *
 * @param <S> The type representing the task settings required for the engine tasks.
 * @param <T> The type representing the tasks to be managed by the engine.
 */
public class WorkflowExecutorManager<S extends AbstractEngineTaskSettings, T extends AbstractEngineTask> {

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
   * Constructor for WorkflowExecutorManager that initializes various dependencies for managing
   * workflow execution and processing.
   *
   * @param workflowExecutorManagerSettings Settings related to workflow execution.
   * @param semaphoresPerPluginManager Manager handling semaphores for different plugin types.
   * @param workflowExecutionDao Data access object for workflow execution data.
   * @param workflowPostProcessor Processor for post-workflow execution tasks.
   * @param rabbitmqPublisherChannel RabbitMQ channel used for publishing messages.
   * @param rabbitmqConsumerChannel RabbitMQ channel used for consuming messages.
   * @param redissonClient Redisson client used for distributed operations.
   * @param engineTaskClient Client for executing engine tasks.
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
