package eu.europeana.metis.core.execution;

import com.rabbitmq.client.Channel;
import eu.europeana.metis.core.dao.WorkflowExecutionDao;
import eu.europeana.metis.core.engine.base.EngineTask;
import eu.europeana.metis.core.engine.base.EngineTaskClient;
import eu.europeana.metis.core.engine.base.EngineTaskSettings;
import org.redisson.api.RedissonClient;

class PersistenceProvider<S extends EngineTaskSettings, T extends EngineTask> {

  private final Channel rabbitmqPublisherChannel;
  private final Channel rabbitmqConsumerChannel;
  private final SemaphoresPerPluginManager semaphoresPerPluginManager;
  private final WorkflowExecutionDao workflowExecutionDao;
  private final WorkflowPostProcessor workflowPostProcessor;
  private final RedissonClient redissonClient;
  private final EngineTaskClient<S, T> engineTaskClient;

  PersistenceProvider(Channel rabbitmqPublisherChannel, Channel rabbitmqConsumerChannel,
      SemaphoresPerPluginManager semaphoresPerPluginManager,
      WorkflowExecutionDao workflowExecutionDao, WorkflowPostProcessor workflowPostProcessor,
      RedissonClient redissonClient, EngineTaskClient<S, T> engineTaskClient) {
    this.rabbitmqPublisherChannel = rabbitmqPublisherChannel;
    this.rabbitmqConsumerChannel = rabbitmqConsumerChannel;
    this.semaphoresPerPluginManager = semaphoresPerPluginManager;
    this.workflowExecutionDao = workflowExecutionDao;
    this.workflowPostProcessor = workflowPostProcessor;
    this.redissonClient = redissonClient;
    this.engineTaskClient = engineTaskClient;
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

  EngineTaskClient<S, T> getExternalTaskClient() {
    return engineTaskClient;
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
