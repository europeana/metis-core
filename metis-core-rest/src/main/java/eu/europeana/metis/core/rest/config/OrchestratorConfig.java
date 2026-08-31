package eu.europeana.metis.core.rest.config;

import eu.europeana.metis.common.config.properties.TruststoreConfigurationProperties;
import eu.europeana.metis.common.config.properties.ecloud.EcloudConfigurationProperties;
import eu.europeana.metis.common.config.properties.redis.RedisConfigurationProperties;
import eu.europeana.metis.common.config.properties.validation.ValidationConfigurationProperties;
import eu.europeana.metis.core.dao.DataEvolutionUtils;
import eu.europeana.metis.core.dao.DatasetDao;
import eu.europeana.metis.core.dao.DatasetXsltDao;
import eu.europeana.metis.core.dao.DepublishRecordIdDao;
import eu.europeana.metis.core.dao.WorkflowDao;
import eu.europeana.metis.core.dao.WorkflowExecutionClaimDao;
import eu.europeana.metis.core.dao.WorkflowExecutionDao;
import eu.europeana.metis.core.dao.WorkflowValidationUtils;
import eu.europeana.metis.core.engine.base.EngineTask;
import eu.europeana.metis.core.engine.base.EngineTaskClient;
import eu.europeana.metis.core.engine.base.EngineTaskSettings;
import eu.europeana.metis.core.execution.SemaphoresPerPluginManager;
import eu.europeana.metis.core.execution.WorkflowExecutionDispatcher;
import eu.europeana.metis.core.execution.WorkflowExecutorSettings;
import eu.europeana.metis.core.execution.WorkflowPostProcessor;
import eu.europeana.metis.core.mongo.MorphiaDatastoreProvider;
import eu.europeana.metis.core.rest.RequestLimits;
import eu.europeana.metis.core.rest.config.properties.MetisCoreConfigurationProperties;
import eu.europeana.metis.core.service.OrchestratorService;
import eu.europeana.metis.core.service.ProxiesService;
import eu.europeana.metis.core.service.RedirectionInferrer;
import eu.europeana.metis.core.service.UserService;
import eu.europeana.metis.core.service.WorkflowExecutionFactory;
import eu.europeana.metis.core.workflow.ValidationProperties;
import eu.europeana.metis.core.workflow.plugins.ThrottlingValues;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Configuration class for setting up beans and managing the dependencies required by the orchestrator services in the
 * application.
 * <p>
 * This class initializes and wires components such as services, DAOs, utilities, and configuration properties to enable workflow
 * orchestration and execution.
 *
 * @param <S> The type representing the settings required for the engine tasks.
 * @param <T> The type representing the tasks to be managed by the engine.
 */
@Configuration
@EnableConfigurationProperties({
    TruststoreConfigurationProperties.class, ValidationConfigurationProperties.class,
    RedisConfigurationProperties.class, MetisCoreConfigurationProperties.class,
    EcloudConfigurationProperties.class})
@ComponentScan(basePackages = {"eu.europeana.metis.core.rest.controller"})
public class OrchestratorConfig<S extends EngineTaskSettings, T extends EngineTask> {

  private static final int WORKFLOW_CORE_POOL_SIZE = 20;
  private ThreadPoolTaskExecutor threadPoolTaskExecutor;

  /**
   * Creates and configures a {@link OrchestratorService} bean.
   * <p>
   * This service orchestrates workflows and handles the execution, validation, and evolution of workflows across datasets. The
   * method initializes the {@link OrchestratorService} with various dependencies required for its operation, including DAOs,
   * utility classes, and configuration properties.
   *
   * @param workflowDao the DAO for managing workflows
   * @param workflowExecutionDao the DAO for tracking workflow executions
   * @param workflowValidationUtils utility class for workflow validation
   * @param dataEvolutionUtils utility class for handling data evolution
   * @param datasetDao the DAO for accessing dataset information
   * @param workflowExecutionFactory factory for creating workflow execution instances
   * @param workflowExecutorSettings settings for handling workflow execution processes
   * @param depublishRecordIdDao the DAO for managing depublished record IDs
   * @param redissonClient the Redisson client instance for distributed locking and caching
   * @param userService the service for managing user-related operations
   * @param metisCoreConfigurationProperties the core configuration properties for the system
   * @return a configured instance of {@link OrchestratorService}
   */
  @Bean
  public OrchestratorService<S, T> getOrchestratorService(WorkflowDao workflowDao,
      WorkflowExecutionDao workflowExecutionDao, WorkflowValidationUtils workflowValidationUtils,
      DataEvolutionUtils dataEvolutionUtils, DatasetDao datasetDao,
      WorkflowExecutionFactory workflowExecutionFactory,
      WorkflowExecutorSettings<S, T> workflowExecutorSettings,
      DepublishRecordIdDao depublishRecordIdDao,
      RedissonClient redissonClient, UserService userService, MetisCoreConfigurationProperties metisCoreConfigurationProperties) {
    OrchestratorService<S, T> orchestratorService = new OrchestratorService<>(workflowExecutionFactory,
        workflowDao, workflowExecutionDao, workflowValidationUtils, dataEvolutionUtils, datasetDao,
        workflowExecutorSettings, redissonClient, depublishRecordIdDao, userService);
    orchestratorService.setSolrCommitPeriodInMinutes(metisCoreConfigurationProperties.solrCommitPeriodInMinutes());
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
   * @param metisCoreConfigurationProperties the Metis core configuration properties
   * @return a configured instance of {@link WorkflowExecutionFactory}
   */
  @Bean
  public WorkflowExecutionFactory getWorkflowExecutionFactory(
      @Qualifier("validationExternalProperties") ValidationProperties validationExternalProperties,
      @Qualifier("validationInternalProperties") ValidationProperties validationInternalProperties,
      RedirectionInferrer redirectionInferrer,
      DatasetXsltDao datasetXsltDao, DepublishRecordIdDao depublishRecordIdDao,
      MetisCoreConfigurationProperties metisCoreConfigurationProperties) {
    WorkflowExecutionFactory workflowExecutionFactory =
        new WorkflowExecutionFactory(datasetXsltDao, depublishRecordIdDao, redirectionInferrer);
    workflowExecutionFactory.setValidationExternalProperties(validationExternalProperties);
    workflowExecutionFactory.setValidationInternalProperties(validationInternalProperties);
    workflowExecutionFactory.setDefaultSamplingSizeForLinkChecking(
        metisCoreConfigurationProperties.linkCheckingDefaultSamplingSize());
    return workflowExecutionFactory;
  }

  /**
   * Provides an instance of RedirectionInferrer configured with the required dependencies.
   *
   * @param workflowExecutionDao WorkflowExecutionDao instance to manage workflow execution data.
   * @param dataEvolutionUtils DataEvolutionUtils instance to help with data transformations.
   * @return A configured RedirectionInferrer object.
   */
  @Bean
  public RedirectionInferrer getRedirectionInferrer(WorkflowExecutionDao workflowExecutionDao,
      DataEvolutionUtils dataEvolutionUtils) {
    return new RedirectionInferrer(workflowExecutionDao, dataEvolutionUtils);
  }

  /**
   * Creates and returns an instance of SecuredProxiesService with the provided dependencies.
   *
   * @param workflowExecutionDao the data access object for workflow execution.
   * @param engineTaskClient the client for Data Processing Services.
   * @param datasetDao the data access object for datasets.
   * @return an initialized instance of SecuredProxiesService.
   */
  @Bean
  public ProxiesService<S, T> getProxiesService(
      WorkflowExecutionDao workflowExecutionDao, EngineTaskClient<S, T> engineTaskClient, DatasetDao datasetDao) {
    return new ProxiesService<>(engineTaskClient, workflowExecutionDao, datasetDao);
  }

  /**
   * Bean workflow execution post-processor.
   *
   * @param depublishRecordIdDao the depublish record id dao
   * @param datasetDao the dataset dao
   * @param workflowExecutionDao the workflow execution dao
   * @param engineTaskClient the dps client
   * @return the workflow post-processor
   */
  @Bean
  public WorkflowPostProcessor workflowPostProcessor(
      DepublishRecordIdDao depublishRecordIdDao,
      DatasetDao datasetDao, WorkflowExecutionDao workflowExecutionDao,
      EngineTaskClient<S, T> engineTaskClient) {
    return new WorkflowPostProcessor(depublishRecordIdDao, datasetDao, workflowExecutionDao, engineTaskClient);
  }

  /**
   * Bean semaphore plugin manager.
   *
   * @param metisCoreConfigurationProperties the Metis core configuration properties
   * @return the semaphore plugin manager
   */
  @Bean
  public SemaphoresPerPluginManager semaphoresPerPluginManager(
      MetisCoreConfigurationProperties metisCoreConfigurationProperties) {
    return new SemaphoresPerPluginManager(metisCoreConfigurationProperties.maxConcurrentThreads());
  }

  /**
   * Configures and returns a ThreadPoolTaskExecutor bean named "pipelineTaskExecutor". The executor is used for concurrent task
   * execution with a defined core pool size, maximum pool size, and queue capacity. It also sets a custom thread name prefix for
   * better identification of threads.
   *
   * @return the configured ThreadPoolTaskExecutor instance for task execution.
   */
  @Bean(name = "workflowExecutorPool")
  ThreadPoolTaskExecutor workflowExecutorPool() {
    threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
    threadPoolTaskExecutor.setCorePoolSize(WORKFLOW_CORE_POOL_SIZE);
    threadPoolTaskExecutor.setMaxPoolSize(WORKFLOW_CORE_POOL_SIZE);
    threadPoolTaskExecutor.setQueueCapacity(WORKFLOW_CORE_POOL_SIZE * 2);
    threadPoolTaskExecutor.setThreadNamePrefix("workflowExecutorPool-");
    threadPoolTaskExecutor.initialize();
    return threadPoolTaskExecutor;
  }

  /**
   * Creates and configures a {@link WorkflowExecutionDispatcher} instance to manage and dispatch workflow executions.
   *
   * @param workflowExecutorSettings the settings for handling workflow execution logic.
   * @param workflowExecutionClaimDao the DAO for managing workflow execution claim persistence operations.
   * @param metisCoreConfigurationProperties the configuration properties for setting up core components.
   * @param threadPoolTaskExecutor the thread pool task executor used for managing concurrency and task execution.
   * @return an instance of {@link WorkflowExecutionDispatcher}.
   */
  @Bean
  public WorkflowExecutionDispatcher<S, T> workflowExecutionDispatcher(
      WorkflowExecutorSettings<S, T> workflowExecutorSettings,
      WorkflowExecutionClaimDao workflowExecutionClaimDao, MetisCoreConfigurationProperties metisCoreConfigurationProperties,
      @Qualifier("workflowExecutorPool") ThreadPoolTaskExecutor threadPoolTaskExecutor) {
    return new WorkflowExecutionDispatcher<>(
        workflowExecutorSettings, threadPoolTaskExecutor.getThreadPoolExecutor(), workflowExecutionClaimDao,
        getFailsafeLeniencyDuration(metisCoreConfigurationProperties)
    );
  }

  /**
   * Creates and configures a {@link WorkflowExecutorSettings} bean.
   *
   * @param semaphoresPerPluginManager Manages semaphores for controlling access to plugins.
   * @param workflowExecutionDao Data access object for managing workflow executions.
   * @param workflowPostProcessor Post-processor for workflow execution-related actions.
   * @param datasetXsltDao Data access object for managing dataset XSLTs.
   * @param engineTaskClient Client for interactions with data processing services.
   * @param metisCoreConfigurationProperties Core configuration properties for the system.
   * @return A configured instance of WorkflowExecutorManager.
   */
  @Bean
  public WorkflowExecutorSettings<S, T> getWorkflowExecutorSettings(
      SemaphoresPerPluginManager semaphoresPerPluginManager,
      WorkflowExecutionDao workflowExecutionDao,
      WorkflowPostProcessor workflowPostProcessor,
      DatasetXsltDao datasetXsltDao,
      EngineTaskClient<S, T> engineTaskClient,
      MetisCoreConfigurationProperties metisCoreConfigurationProperties) {
    return new WorkflowExecutorSettings<>(
        Duration.ofSeconds(metisCoreConfigurationProperties.dpsMonitorCheckIntervalInSeconds()),
        Duration.ofMinutes(metisCoreConfigurationProperties.periodOfNoProcessedRecordsChangeInMinutes()),
        semaphoresPerPluginManager, workflowExecutionDao, workflowPostProcessor, datasetXsltDao, engineTaskClient);
  }

  /**
   * Provides an instance of WorkflowExecutionDao configured with datastore provider and properties.
   *
   * @param morphiaDatastoreProvider MorphiaDatastoreProvider instance to interact with the datastore.
   * @param metisCoreConfigurationProperties Configuration properties for Metis Core settings.
   * @return Configured instance of WorkflowExecutionDao.
   */
  @Bean
  public WorkflowExecutionDao getWorkflowExecutionDao(
      MorphiaDatastoreProvider morphiaDatastoreProvider,
      MetisCoreConfigurationProperties metisCoreConfigurationProperties) {
    WorkflowExecutionDao workflowExecutionDao = new WorkflowExecutionDao(morphiaDatastoreProvider);
    workflowExecutionDao
        .setWorkflowExecutionsPerRequest(RequestLimits.WORKFLOW_EXECUTIONS_PER_REQUEST.getLimit());
    workflowExecutionDao
        .setMaxServedExecutionListLength(metisCoreConfigurationProperties.maxServedExecutionListLength());
    return workflowExecutionDao;
  }

  /**
   * Provides an instance of WorkflowExecutionClaimDao configured with datastore provider and properties.
   *
   * @param morphiaDatastoreProvider MorphiaDatastoreProvider instance to interact with the datastore.
   * @return Configured instance of WorkflowExecutionDao.
   */
  @Bean
  public WorkflowExecutionClaimDao getWorkflowExecutionClaimDao(MorphiaDatastoreProvider morphiaDatastoreProvider) {
    return new WorkflowExecutionClaimDao(morphiaDatastoreProvider);
  }

  @Bean
  DataEvolutionUtils getDataEvolutionUtils(WorkflowExecutionDao workflowExecutionDao) {
    return new DataEvolutionUtils(workflowExecutionDao);
  }

  @Bean
  WorkflowValidationUtils getWorkflowValidationUtils(
      DepublishRecordIdDao depublishRecordIdDao, DatasetXsltDao datasetXsltDao,
      DataEvolutionUtils dataEvolutionUtils, MetisCoreConfigurationProperties metisCoreConfigurationProperties) {
    return new WorkflowValidationUtils(
        metisCoreConfigurationProperties.engineType(), depublishRecordIdDao, datasetXsltDao, dataEvolutionUtils);
  }

  /**
   * Provides an instance of WorkflowDao initialized with the provided MorphiaDatastoreProvider.
   *
   * @param morphiaDatastoreProvider MorphiaDatastoreProvider used to initialize the WorkflowDao.
   * @return An instance of WorkflowDao.
   */
  @Bean
  public WorkflowDao getWorkflowDao(MorphiaDatastoreProvider morphiaDatastoreProvider) {
    return new WorkflowDao(morphiaDatastoreProvider);
  }

  private static Duration getFailsafeLeniencyDuration(MetisCoreConfigurationProperties metisCoreConfigurationProperties) {
    /*Computes the leniency for the failsafe action: how long ago (the worst case)
     did the last update action take place before we assume the execution hangs.*/
    return Duration.ZERO
        .plusMillis(metisCoreConfigurationProperties.dpsConnectTimeoutInMilliseconds())
        .plusMillis(metisCoreConfigurationProperties.dpsReadTimeoutInMilliseconds())
        .plusSeconds(metisCoreConfigurationProperties.dpsMonitorCheckIntervalInSeconds())
        .plusSeconds(metisCoreConfigurationProperties.failsafeMarginOfInactivityInSeconds());
  }

  /**
   * Retrieves the throttling values based on the provided configuration properties.
   *
   * @param metisCoreConfigurationProperties Configuration properties containing the throttling level settings.
   * @return An instance of ThrottlingValues initialized with the provided configuration properties.
   */
  @Bean
  public ThrottlingValues getThrottlingValues(MetisCoreConfigurationProperties metisCoreConfigurationProperties) {
    return new ThrottlingValues(metisCoreConfigurationProperties.threadLimitThrottlingLevelWeak(),
        metisCoreConfigurationProperties.threadLimitThrottlingLevelMedium(),
        metisCoreConfigurationProperties.threadLimitThrottlingLevelStrong());
  }

  /**
   * Closes connections to databases when the application closes.
   */
  @PreDestroy
  public void close() {
    if (threadPoolTaskExecutor != null) {
      threadPoolTaskExecutor.shutdown();
    }
  }
}
