package eu.europeana.metis.core.rest.config;

import com.rabbitmq.client.Channel;
import eu.europeana.cloud.client.dps.rest.DpsClient;
import eu.europeana.cloud.client.uis.rest.UISClient;
import eu.europeana.cloud.mcs.driver.DataSetServiceClient;
import eu.europeana.cloud.mcs.driver.FileServiceClient;
import eu.europeana.cloud.mcs.driver.RecordServiceClient;
import eu.europeana.metis.core.dao.DataEvolutionUtils;
import eu.europeana.metis.core.dao.DatasetDao;
import eu.europeana.metis.core.dao.DatasetXsltDao;
import eu.europeana.metis.core.dao.DepublishRecordIdDao;
import eu.europeana.metis.core.dao.ScheduledWorkflowDao;
import eu.europeana.metis.core.dao.WorkflowDao;
import eu.europeana.metis.core.dao.WorkflowExecutionDao;
import eu.europeana.metis.core.dao.WorkflowValidationUtils;
import eu.europeana.metis.core.execution.SchedulerExecutor;
import eu.europeana.metis.core.execution.SemaphoresPerPluginManager;
import eu.europeana.metis.core.execution.WorkflowExecutionMonitor;
import eu.europeana.metis.core.execution.WorkflowExecutorManager;
import eu.europeana.metis.core.execution.WorkflowPostProcessor;
import eu.europeana.metis.core.mongo.MorphiaDatastoreProvider;
import eu.europeana.metis.core.rest.RequestLimits;
import eu.europeana.metis.core.rest.config.properties.MetisCoreConfigurationProperties;
import eu.europeana.metis.core.service.Authorizer;
import eu.europeana.metis.core.service.OrchestratorService;
import eu.europeana.metis.core.service.ProxiesService;
import eu.europeana.metis.core.service.RedirectionInferrer;
import eu.europeana.metis.core.service.ScheduleWorkflowService;
import eu.europeana.metis.core.service.SecuredOrchestratorService;
import eu.europeana.metis.core.service.SecuredProxiesService;
import eu.europeana.metis.core.service.SecuredScheduleWorkflowService;
import eu.europeana.metis.core.service.WorkflowExecutionFactory;
import eu.europeana.metis.core.workflow.ValidationProperties;
import eu.europeana.metis.core.workflow.plugins.ThrottlingValues;
import jakarta.annotation.PreDestroy;
import java.net.MalformedURLException;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import metis.common.config.properties.TruststoreConfigurationProperties;
import metis.common.config.properties.ecloud.EcloudConfigurationProperties;
import metis.common.config.properties.rabbitmq.RabbitmqConfigurationProperties;
import metis.common.config.properties.redis.RedisConfigurationProperties;
import metis.common.config.properties.redis.RedissonConfigurationProperties;
import metis.common.config.properties.validation.ValidationConfigurationProperties;
import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Orchestrator configuration class.
 *
 * @author Simon Tzanakis (Simon.Tzanakis@europeana.eu)
 * @since 2017-11-22
 */
@Configuration
@EnableConfigurationProperties({
    TruststoreConfigurationProperties.class, ValidationConfigurationProperties.class,
    RedisConfigurationProperties.class, MetisCoreConfigurationProperties.class,
    EcloudConfigurationProperties.class})
@ComponentScan(basePackages = {"eu.europeana.metis.core.rest.controller"})
@EnableScheduling
public class OrchestratorConfig implements WebMvcConfigurer {

  private static final Logger LOGGER = LoggerFactory.getLogger(OrchestratorConfig.class);
  private SchedulerExecutor schedulerExecutor;
  private WorkflowExecutionMonitor workflowExecutionMonitor;
  private RedissonClient redissonClient;

  @Bean
  RedissonClient getRedissonClient(
      TruststoreConfigurationProperties truststoreConfigurationProperties,
      RedisConfigurationProperties redisConfigurationProperties)
      throws MalformedURLException {
    Config config = new Config();

    SingleServerConfig singleServerConfig;
    if (redisConfigurationProperties.isEnableSsl()) {
      singleServerConfig = config.useSingleServer().setAddress(String
          .format("rediss://%s:%s", redisConfigurationProperties.getHost(),
              redisConfigurationProperties.getPort()));
      LOGGER.info("Redis enabled SSL");
      if (redisConfigurationProperties.isEnableCustomTruststore()) {
        singleServerConfig
            .setSslTruststore(Paths.get(truststoreConfigurationProperties.getPath()).toUri().toURL());
        singleServerConfig.setSslTruststorePassword(truststoreConfigurationProperties.getPassword());
        LOGGER.info("Redis enabled SSL using custom Truststore");
      }
    } else {
      singleServerConfig = config.useSingleServer().setAddress(String
          .format("redis://%s:%s", redisConfigurationProperties.getHost(),
              redisConfigurationProperties.getPort()));
      LOGGER.info("Redis disabled SSL");
    }
    if (StringUtils.isNotEmpty(redisConfigurationProperties.getUsername())) {
      singleServerConfig.setUsername(redisConfigurationProperties.getUsername());
    }
    if (StringUtils.isNotEmpty(redisConfigurationProperties.getPassword())) {
      singleServerConfig.setPassword(redisConfigurationProperties.getPassword());
    }

    RedissonConfigurationProperties redisson = redisConfigurationProperties.getRedisson();
    singleServerConfig.setConnectionPoolSize(redisson.getConnectionPoolSize())
                      .setConnectionMinimumIdleSize(redisson.getConnectionPoolSize())
                      .setConnectTimeout((int) TimeUnit.SECONDS.toMillis(redisson.getConnectTimeoutInSeconds()))
                      .setDnsMonitoringInterval((int) TimeUnit.SECONDS.toMillis(redisson.getDnsMonitorIntervalInSeconds()))
                      .setIdleConnectionTimeout((int) TimeUnit.SECONDS.toMillis(redisson.getIdleConnectionTimeoutInSeconds()))
                      .setRetryAttempts(redisson.getRetryAttempts());
    //Give some secs to unlock if connection lost, or if too long to unlock
    config.setLockWatchdogTimeout(TimeUnit.SECONDS.toMillis(redisson.getLockWatchdogTimeoutInSeconds()));
    redissonClient = Redisson.create(config);
    return redissonClient;
  }

  @Deprecated(forRemoval = true)
  @Bean
  public OrchestratorService getOrchestratorService(WorkflowDao workflowDao,
      WorkflowExecutionDao workflowExecutionDao, WorkflowValidationUtils workflowValidationUtils,
      DataEvolutionUtils dataEvolutionUtils, DatasetDao datasetDao,
      WorkflowExecutionFactory workflowExecutionFactory,
      WorkflowExecutorManager workflowExecutorManager, Authorizer authorizer,
      DepublishRecordIdDao depublishRecordIdDao,
      MetisCoreConfigurationProperties metisCoreConfigurationProperties) {
    OrchestratorService orchestratorService = new OrchestratorService(workflowExecutionFactory,
        workflowDao, workflowExecutionDao, workflowValidationUtils, dataEvolutionUtils, datasetDao,
        workflowExecutorManager, redissonClient, authorizer, depublishRecordIdDao);
    orchestratorService.setSolrCommitPeriodInMins(metisCoreConfigurationProperties.getSolrCommitPeriodInMinutes());
    return orchestratorService;
  }

  /**
   * Creates and configures a {@link SecuredOrchestratorService} bean.
   * <p>
   * This service orchestrates secured workflows and handles the execution, validation, and evolution of workflows across
   * datasets. The method initializes the {@link SecuredOrchestratorService} with various dependencies required for its operation,
   * including DAOs, utility classes, and configuration properties.
   *
   * @param workflowDao the DAO for managing workflows
   * @param workflowExecutionDao the DAO for tracking workflow executions
   * @param workflowValidationUtils utility class for workflow validation
   * @param dataEvolutionUtils utility class for handling data evolution
   * @param datasetDao the DAO for accessing dataset information
   * @param workflowExecutionFactory factory for creating workflow execution instances
   * @param workflowExecutorManager manager for handling workflow execution processes
   * @param depublishRecordIdDao the DAO for managing depublished record IDs
   * @param metisCoreConfigurationProperties the core configuration properties for the system
   * @return a configured instance of {@link SecuredOrchestratorService}
   */
  @Bean
  public SecuredOrchestratorService getSecuredOrchestratorService(WorkflowDao workflowDao,
      WorkflowExecutionDao workflowExecutionDao, WorkflowValidationUtils workflowValidationUtils,
      DataEvolutionUtils dataEvolutionUtils, DatasetDao datasetDao,
      WorkflowExecutionFactory workflowExecutionFactory,
      WorkflowExecutorManager workflowExecutorManager,
      DepublishRecordIdDao depublishRecordIdDao,
      MetisCoreConfigurationProperties metisCoreConfigurationProperties) {
    SecuredOrchestratorService orchestratorService = new SecuredOrchestratorService(workflowExecutionFactory,
        workflowDao, workflowExecutionDao, workflowValidationUtils, dataEvolutionUtils, datasetDao,
        workflowExecutorManager, redissonClient, depublishRecordIdDao);
    orchestratorService.setSolrCommitPeriodInMinutes(metisCoreConfigurationProperties.getSolrCommitPeriodInMinutes());
    return orchestratorService;
  }

  /**
   * Creates and configures a {@link ValidationProperties} bean for external validation properties.
   *
   * @param validationConfigurationProperties the configuration properties that provide details needed to initialize external
   * validation properties, including the schema zip URL, schema root path, and schematron root path.
   * @return an instance of {@link ValidationProperties} initialized with the provided external validation configuration
   * properties.
   */
  @Bean(name = "validationExternalProperties")
  public ValidationProperties getValidationExternalProperties(
      ValidationConfigurationProperties validationConfigurationProperties) {
    return new ValidationProperties(
        validationConfigurationProperties.getValidationExternalSchemaZip(),
        validationConfigurationProperties.getValidationExternalSchemaRoot(),
        validationConfigurationProperties.getValidationExternalSchematronRoot());
  }

  /**
   * Creates and configures a {@link ValidationProperties} bean for internal validation properties.
   *
   * @param validationConfigurationProperties the configuration properties that provide details needed to initialize internal
   * validation properties, including the schema zip URL, schema root path, and schematron root path.
   * @return an instance of {@link ValidationProperties} initialized with the provided internal validation configuration
   * properties.
   */
  @Bean(name = "validationInternalProperties")
  public ValidationProperties getValidationInternalProperties(
      ValidationConfigurationProperties validationConfigurationProperties) {
    return new ValidationProperties(
        validationConfigurationProperties.getValidationInternalSchemaZip(),
        validationConfigurationProperties.getValidationInternalSchemaRoot(),
        validationConfigurationProperties.getValidationInternalSchematronRoot());
  }

  /**
   * Creates and configures a {@link WorkflowExecutionFactory} bean. This factory is used for the execution of workflows and
   * supports validation, redirection inference, and other dataset-specific functionalities.
   *
   * @param validationExternalProperties the external validation properties used for workflow validation
   * @param validationInternalProperties the internal validation properties used for workflow validation
   * @param redirectionInferrer the component responsible for inferring redirection behavior for workflows
   * @param datasetXsltDao the DAO for managing XSLT transformations for workflows
   * @param depublishRecordIdDao the DAO for handling depublish record IDs
   */
  @Bean
  public WorkflowExecutionFactory getWorkflowExecutionFactory(
      @Qualifier("validationExternalProperties") ValidationProperties validationExternalProperties,
      @Qualifier("validationInternalProperties") ValidationProperties validationInternalProperties,
      RedirectionInferrer redirectionInferrer,
      DatasetXsltDao datasetXsltDao, DepublishRecordIdDao depublishRecordIdDao,
      MetisCoreConfigurationProperties metisCoreConfigurationProperties) {
    WorkflowExecutionFactory workflowExecutionFactory = new WorkflowExecutionFactory(datasetXsltDao,
        depublishRecordIdDao, redirectionInferrer);
    workflowExecutionFactory
        .setValidationExternalProperties(validationExternalProperties);
    workflowExecutionFactory
        .setValidationInternalProperties(validationInternalProperties);
    workflowExecutionFactory.setDefaultSamplingSizeForLinkChecking(
        metisCoreConfigurationProperties.getLinkCheckingDefaultSamplingSize());
    return workflowExecutionFactory;
  }

  @Bean
  public RedirectionInferrer getRedirectionInferrer(WorkflowExecutionDao workflowExecutionDao,
      DataEvolutionUtils dataEvolutionUtils) {
    return new RedirectionInferrer(workflowExecutionDao, dataEvolutionUtils);
  }

  @Deprecated(forRemoval = true)
  @Bean
  public ScheduleWorkflowService getScheduleWorkflowService(
      ScheduledWorkflowDao scheduledWorkflowDao, WorkflowDao workflowDao, DatasetDao datasetDao,
      Authorizer authorizer) {
    return new ScheduleWorkflowService(scheduledWorkflowDao, workflowDao, datasetDao, authorizer);
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
  public SecuredScheduleWorkflowService getSecuredScheduleWorkflowService(ScheduledWorkflowDao scheduledWorkflowDao,
      WorkflowDao workflowDao, DatasetDao datasetDao) {
    return new SecuredScheduleWorkflowService(scheduledWorkflowDao, workflowDao, datasetDao);
  }

  @Deprecated(forRemoval = true)
  @Bean
  public ProxiesService getProxiesService(
      WorkflowExecutionDao workflowExecutionDao, DataSetServiceClient ecloudDataSetServiceClient,
      RecordServiceClient recordServiceClient, FileServiceClient fileServiceClient,
      DpsClient dpsClient, UISClient uisClient, Authorizer authorizer,
      EcloudConfigurationProperties ecloudConfigurationProperties) {
    return new ProxiesService(workflowExecutionDao, ecloudDataSetServiceClient, recordServiceClient,
        fileServiceClient, dpsClient, uisClient, ecloudConfigurationProperties.getProvider(), authorizer);
  }

  /**
   * Creates and returns an instance of SecuredProxiesService with the provided dependencies.
   *
   * @param workflowExecutionDao the data access object for workflow execution.
   * @param ecloudDataSetServiceClient the client service for eCloud datasets.
   * @param recordServiceClient the client for interacting with record services.
   * @param fileServiceClient the client for managing file services.
   * @param dpsClient the client for Data Processing Services.
   * @param uisClient the client for Unified Information Services.
   * @param datasetDao the data access object for datasets.
   * @param ecloudConfigurationProperties the configuration properties for eCloud integration.
   * @return an initialized instance of SecuredProxiesService.
   */
  @Bean
  public SecuredProxiesService getSecuredProxiesService(
      WorkflowExecutionDao workflowExecutionDao, DataSetServiceClient ecloudDataSetServiceClient,
      RecordServiceClient recordServiceClient, FileServiceClient fileServiceClient,
      DpsClient dpsClient, UISClient uisClient, DatasetDao datasetDao,
      EcloudConfigurationProperties ecloudConfigurationProperties) {
    return new SecuredProxiesService(workflowExecutionDao, ecloudDataSetServiceClient, recordServiceClient,
        fileServiceClient, dpsClient, uisClient, ecloudConfigurationProperties.getProvider(), datasetDao);
  }

  /**
   * Bean workflow execution post processor.
   *
   * @param depublishRecordIdDao the depublish record id dao
   * @param datasetDao the dataset dao
   * @param workflowExecutionDao the workflow execution dao
   * @param dpsClient the dps client
   * @return the workflow post processor
   */
  @Bean
  public WorkflowPostProcessor workflowPostProcessor(DepublishRecordIdDao depublishRecordIdDao,
      DatasetDao datasetDao, WorkflowExecutionDao workflowExecutionDao, DpsClient dpsClient) {
    return new WorkflowPostProcessor(depublishRecordIdDao, datasetDao, workflowExecutionDao,
        dpsClient);
  }

  /**
   * Bean semaphore plugin manager.
   *
   * @return the semaphore plugin manager
   */
  @Bean
  public SemaphoresPerPluginManager semaphoresPerPluginManager(
      MetisCoreConfigurationProperties metisCoreConfigurationProperties) {
    return new SemaphoresPerPluginManager(metisCoreConfigurationProperties.getMaxConcurrentThreads());
  }

  @Bean
  public WorkflowExecutorManager getWorkflowExecutorManager(
      SemaphoresPerPluginManager semaphoresPerPluginManager,
      WorkflowExecutionDao workflowExecutionDao, WorkflowPostProcessor workflowPostProcessor,
      @Qualifier("rabbitmqPublisherChannel") Channel rabbitmqPublisherChannel,
      @Qualifier("rabbitmqConsumerChannel") Channel rabbitmqConsumerChannel,
      RedissonClient redissonClient, DpsClient dpsClient,
      RabbitmqConfigurationProperties rabbitmqConfigurationProperties,
      MetisCoreConfigurationProperties metisCoreConfigurationProperties,
      EcloudConfigurationProperties ecloudConfigurationProperties) {
    WorkflowExecutorManager workflowExecutorManager = new WorkflowExecutorManager(
        semaphoresPerPluginManager, workflowExecutionDao, workflowPostProcessor,
        rabbitmqPublisherChannel, rabbitmqConsumerChannel, redissonClient, dpsClient);
    workflowExecutorManager.setRabbitmqQueueName(rabbitmqConfigurationProperties.getQueueName());
    workflowExecutorManager
        .setDpsMonitorCheckIntervalInSecs(metisCoreConfigurationProperties.getDpsMonitorCheckIntervalInSeconds());
    workflowExecutorManager.setPeriodOfNoProcessedRecordsChangeInMinutes(
        metisCoreConfigurationProperties.getPeriodOfNoProcessedRecordsChangeInMinutes());
    workflowExecutorManager.setEcloudBaseUrl(ecloudConfigurationProperties.getBaseUrl());
    workflowExecutorManager.setEcloudProvider(ecloudConfigurationProperties.getProvider());
    workflowExecutorManager.setMetisCoreBaseUrl(metisCoreConfigurationProperties.getBaseUrl());
    workflowExecutorManager.setThrottlingValues(getThrottlingValues(metisCoreConfigurationProperties));
    return workflowExecutorManager;
  }

  @Bean
  public WorkflowExecutionDao getWorkflowExecutionDao(
      MorphiaDatastoreProvider morphiaDatastoreProvider,
      MetisCoreConfigurationProperties metisCoreConfigurationProperties) {
    WorkflowExecutionDao workflowExecutionDao = new WorkflowExecutionDao(morphiaDatastoreProvider);
    workflowExecutionDao
        .setWorkflowExecutionsPerRequest(RequestLimits.WORKFLOW_EXECUTIONS_PER_REQUEST.getLimit());
    workflowExecutionDao
        .setMaxServedExecutionListLength(metisCoreConfigurationProperties.getMaxServedExecutionListLength());
    return workflowExecutionDao;
  }

  @Bean
  DataEvolutionUtils getDataEvolutionUtils(WorkflowExecutionDao workflowExecutionDao) {
    return new DataEvolutionUtils(workflowExecutionDao);
  }

  @Bean
  WorkflowValidationUtils getWorkflowValidationUtils(DataEvolutionUtils dataEvolutionUtils,
      DepublishRecordIdDao depublishRecordIdDao) {
    return new WorkflowValidationUtils(depublishRecordIdDao, dataEvolutionUtils);
  }

  @Bean
  public ScheduledWorkflowDao getScheduledWorkflowDao(
      MorphiaDatastoreProvider morphiaDatastoreProvider) {
    return new ScheduledWorkflowDao(morphiaDatastoreProvider);
  }

  @Bean
  public WorkflowDao getWorkflowDao(MorphiaDatastoreProvider morphiaDatastoreProvider) {
    return new WorkflowDao(morphiaDatastoreProvider);
  }

  @Bean
  public WorkflowExecutionMonitor getWorkflowExecutionMonitor(
      WorkflowExecutorManager workflowExecutorManager, WorkflowExecutionDao workflowExecutionDao,
      RedissonClient redissonClient, MetisCoreConfigurationProperties metisCoreConfigurationProperties) {

    // Computes the leniency for the failsafe action: how long ago (worst case) can the last update
    // time have been set before we assume the execution hangs.
    final Duration failsafeLeniency = Duration.ZERO
        .plusMillis(metisCoreConfigurationProperties.getDpsConnectTimeoutInMilliseconds())
        .plusMillis(metisCoreConfigurationProperties.getDpsReadTimeoutInMilliseconds())
        .plusSeconds(metisCoreConfigurationProperties.getDpsMonitorCheckIntervalInSeconds())
        .plusSeconds(metisCoreConfigurationProperties.getFailsafeMarginOfInactivityInSeconds());

    // Create and return the workflow execution monitor.
    workflowExecutionMonitor = new WorkflowExecutionMonitor(workflowExecutorManager,
        workflowExecutionDao, redissonClient, failsafeLeniency);
    return workflowExecutionMonitor;
  }

  @Bean
  public SchedulerExecutor getSchedulingExecutor(OrchestratorService orchestratorService,
      ScheduleWorkflowService scheduleWorkflowService, RedissonClient redissonClient) {
    schedulerExecutor = new SchedulerExecutor(orchestratorService, scheduleWorkflowService,
        redissonClient);
    return schedulerExecutor;
  }

  @Bean
  public ThrottlingValues getThrottlingValues(MetisCoreConfigurationProperties metisCoreConfigurationProperties) {
    return new ThrottlingValues(metisCoreConfigurationProperties.getThreadLimitThrottlingLevelWeak(),
        metisCoreConfigurationProperties.getThreadLimitThrottlingLevelMedium(),
        metisCoreConfigurationProperties.getThreadLimitThrottlingLevelStrong());
  }

  /**
   * Failsafe periodic thread.
   * <p>It will find stale executions and will re-submit them in the distributed queue.</p>
   */
  // TODO: 24/08/2023 Is there a better way to load the configuration here?
  @Scheduled(fixedDelayString = "${metis-core.periodicFailsafeCheckInMilliseconds}")
  public void runFailsafeExecutor() {
    this.workflowExecutionMonitor.performFailsafe();
    LOGGER.info("Failsafe task finished.");
  }

  /**
   * Scheduling periodic thread.
   * <p>Checks if scheduled workflows are valid for starting and sends them to the distributed
   * queue.</p>
   */
  // TODO: 24/08/2023 Is there a better way to load the configuration here?
  @Scheduled(
      fixedDelayString = "${metis-core.periodicSchedulerCheckInMilliseconds}",
      initialDelayString = "${metis-core.periodicSchedulerCheckInMilliseconds}")
  public void runSchedulingExecutor() {
    this.schedulerExecutor.performScheduling();
    LOGGER.info("Scheduler task finished.");
  }

  /**
   * Close resources
   */
  @PreDestroy
  public void close() {
    // Shut down Redisson
    if (redissonClient != null && !redissonClient.isShuttingDown()) {
      redissonClient.shutdown();
    }
  }
}
