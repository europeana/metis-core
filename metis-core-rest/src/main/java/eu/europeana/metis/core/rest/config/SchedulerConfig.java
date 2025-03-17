package eu.europeana.metis.core.rest.config;

import eu.europeana.metis.core.dao.DatasetDao;
import eu.europeana.metis.core.dao.ScheduledWorkflowDao;
import eu.europeana.metis.core.dao.WorkflowDao;
import eu.europeana.metis.core.execution.SchedulerExecutor;
import eu.europeana.metis.core.mongo.MorphiaDatastoreProvider;
import eu.europeana.metis.core.service.OrchestratorService;
import eu.europeana.metis.core.service.ScheduleWorkflowService;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Scheduler configuration class.
 */
@Configuration
public class SchedulerConfig {

  /**
   * Creates and returns a bean of ScheduledWorkflowDao.
   *
   * @param morphiaDatastoreProvider the provider for accessing the Morphia datastore
   * @return an instance of ScheduledWorkflowDao configured with the given datastore provider
   */
  @Bean
  public ScheduledWorkflowDao getScheduledWorkflowDao(MorphiaDatastoreProvider morphiaDatastoreProvider) {
    return new ScheduledWorkflowDao(morphiaDatastoreProvider);
  }

  /**
   * Creates and returns an instance of SecuredScheduleWorkflowService.
   *
   * @param scheduledWorkflowDao the DAO responsible for managing scheduled workflows.
   * @param workflowDao the DAO responsible for managing workflows.
   * @param datasetDao the DAO responsible for managing datasets.
   * @return a new instance of SecuredScheduleWorkflowService configured with the given DAOs.
   */
  @Bean
  public ScheduleWorkflowService getScheduleWorkflowService(ScheduledWorkflowDao scheduledWorkflowDao,
      WorkflowDao workflowDao, DatasetDao datasetDao) {
    return new ScheduleWorkflowService(scheduledWorkflowDao, workflowDao, datasetDao);
  }

  /**
   * Creates and returns a bean of SchedulerExecutor.
   *
   * <p>This instance is responsible for scheduling the workflows. It periodically checks if there are scheduled workflows
   * in a range of dates and if some are found it will send them in the distributed queue.</p>
   *
   * @param orchestratorService the service responsible for executing workflows
   * @param scheduleWorkflowService the service responsible for managing scheduled workflows
   * @param redissonClient the Redisson client instance for distributed locking and caching
   * @return an instance of SchedulerExecutor configured with the given services and Redisson client
   */
  @Bean
  public SchedulerExecutor getSchedulingExecutor(OrchestratorService orchestratorService,
      ScheduleWorkflowService scheduleWorkflowService, RedissonClient redissonClient) {
    return new SchedulerExecutor(orchestratorService, scheduleWorkflowService, redissonClient);
  }
}
