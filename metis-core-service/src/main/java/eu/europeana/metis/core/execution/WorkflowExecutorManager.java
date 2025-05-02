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
public class WorkflowExecutorManager
    extends PersistenceProvider implements WorkflowExecutionSettings {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final int DEFAULT_MONITOR_CHECK_INTERVAL_IN_SECS = 5;
  private static final int DEFAULT_PERIOD_OF_NO_PROCESSED_RECORDS_CHANGE_IN_MINUTES = 30;

  private int dpsMonitorCheckIntervalInSecs = DEFAULT_MONITOR_CHECK_INTERVAL_IN_SECS; //Use setter otherwise default
  private int periodOfNoProcessedRecordsChangeInMinutes = DEFAULT_PERIOD_OF_NO_PROCESSED_RECORDS_CHANGE_IN_MINUTES; //Use setter otherwise default

  private String rabbitmqQueueName; //Initialize with setter

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
      SemaphoresPerPluginManager semaphoresPerPluginManager,
      WorkflowExecutionDao workflowExecutionDao, WorkflowPostProcessor workflowPostProcessor,
      Channel rabbitmqPublisherChannel, Channel rabbitmqConsumerChannel,
      RedissonClient redissonClient, EngineTaskClient<? extends EngineTaskSettings, ? extends EngineTask> engineTaskClient) {
    super(rabbitmqPublisherChannel, rabbitmqConsumerChannel, semaphoresPerPluginManager,
        workflowExecutionDao, workflowPostProcessor, redissonClient, engineTaskClient);
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
        getRabbitmqPublisherChannel().basicPublish("", rabbitmqQueueName, basicProperties,
            userWorkflowExecutionObjectId.getBytes(StandardCharsets.UTF_8));
      } catch (IOException e) {
        LOGGER.error("WorkflowExecution with objectId: {} not added in queue..",
            userWorkflowExecutionObjectId, e);
      }
    }
  }

  public void setRabbitmqQueueName(String rabbitmqQueueName) {
    this.rabbitmqQueueName = rabbitmqQueueName;
  }

  public void setDpsMonitorCheckIntervalInSecs(int dpsMonitorCheckIntervalInSecs) {
    this.dpsMonitorCheckIntervalInSecs = dpsMonitorCheckIntervalInSecs;
  }

  public void setPeriodOfNoProcessedRecordsChangeInMinutes(
      int periodOfNoProcessedRecordsChangeInMinutes) {
    this.periodOfNoProcessedRecordsChangeInMinutes = periodOfNoProcessedRecordsChangeInMinutes;
  }

  @Override
  public int getDpsMonitorCheckIntervalInSecs() {
    return dpsMonitorCheckIntervalInSecs;
  }

  @Override
  public int getPeriodOfNoProcessedRecordsChangeInMinutes() {
    return periodOfNoProcessedRecordsChangeInMinutes;
  }
}
