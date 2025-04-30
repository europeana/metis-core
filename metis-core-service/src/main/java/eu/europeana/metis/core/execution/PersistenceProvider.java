package eu.europeana.metis.core.execution;

import com.rabbitmq.client.Channel;
import eu.europeana.metis.core.dao.WorkflowExecutionDao;
import eu.europeana.metis.core.engine.base.ProcessingEngineTask;
import eu.europeana.metis.core.engine.base.ProcessingEngineTaskClient;
import eu.europeana.metis.core.engine.base.ProcessingEngineTaskSettings;
import org.redisson.api.RedissonClient;

class PersistenceProvider<S extends ProcessingEngineTaskSettings, T extends ProcessingEngineTask> {

  private final Channel rabbitmqPublisherChannel;
  private final Channel rabbitmqConsumerChannel;
  private final SemaphoresPerPluginManager semaphoresPerPluginManager;
  private final WorkflowExecutionDao workflowExecutionDao;
  private final WorkflowPostProcessor workflowPostProcessor;
  private final RedissonClient redissonClient;
  private final ProcessingEngineTaskClient<S, T> processingEngineTaskClient;

  PersistenceProvider(Channel rabbitmqPublisherChannel, Channel rabbitmqConsumerChannel,
      SemaphoresPerPluginManager semaphoresPerPluginManager,
      WorkflowExecutionDao workflowExecutionDao, WorkflowPostProcessor workflowPostProcessor,
      RedissonClient redissonClient, ProcessingEngineTaskClient<S, T> processingEngineTaskClient) {
    this.rabbitmqPublisherChannel = rabbitmqPublisherChannel;
    this.rabbitmqConsumerChannel = rabbitmqConsumerChannel;
    this.semaphoresPerPluginManager = semaphoresPerPluginManager;
    this.workflowExecutionDao = workflowExecutionDao;
    this.workflowPostProcessor = workflowPostProcessor;
    this.redissonClient = redissonClient;
    this.processingEngineTaskClient = processingEngineTaskClient;
  }

  public SemaphoresPerPluginManager getSemaphoresPerPluginManager() {
    return semaphoresPerPluginManager;
  }

  WorkflowExecutionDao getWorkflowExecutionDao() {
    return workflowExecutionDao;
  }

  public WorkflowPostProcessor getWorkflowPostProcessor() {
    return workflowPostProcessor;
  }

  ProcessingEngineTaskClient<S, T> getExternalTaskClient() {
    return processingEngineTaskClient;
  }

  RedissonClient getRedissonClient() {
    return redissonClient;
  }

  public Channel getRabbitmqPublisherChannel() {
    return rabbitmqPublisherChannel;
  }

  public Channel getRabbitmqConsumerChannel() {
    return rabbitmqConsumerChannel;
  }
}
