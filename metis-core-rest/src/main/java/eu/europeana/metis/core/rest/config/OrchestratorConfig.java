package eu.europeana.metis.core.rest.config;

import com.rabbitmq.client.Channel;
import eu.europeana.cloud.client.uis.rest.UISClient;
import eu.europeana.cloud.mcs.driver.DataSetServiceClient;
import eu.europeana.cloud.mcs.driver.FileServiceClient;
import eu.europeana.cloud.mcs.driver.RecordServiceClient;
import eu.europeana.metis.common.config.properties.TruststoreConfigurationProperties;
import eu.europeana.metis.common.config.properties.ecloud.EcloudConfigurationProperties;
import eu.europeana.metis.common.config.properties.rabbitmq.RabbitmqConfigurationProperties;
import eu.europeana.metis.common.config.properties.redis.RedisConfigurationProperties;
import eu.europeana.metis.common.config.properties.validation.ValidationConfigurationProperties;
import eu.europeana.metis.core.dao.DataEvolutionUtils;
import eu.europeana.metis.core.dao.DatasetDao;
import eu.europeana.metis.core.dao.DatasetXsltDao;
import eu.europeana.metis.core.dao.DepublishRecordIdDao;
import eu.europeana.metis.core.dao.WorkflowDao;
import eu.europeana.metis.core.dao.WorkflowExecutionDao;
import eu.europeana.metis.core.dao.WorkflowValidationUtils;
import eu.europeana.metis.core.engine.base.ProcessingEngineTaskSettings;
import eu.europeana.metis.core.execution.SemaphoresPerPluginManager;
import eu.europeana.metis.core.execution.WorkflowExecutionMonitor;
import eu.europeana.metis.core.execution.WorkflowExecutorManager;
import eu.europeana.metis.core.execution.WorkflowPostProcessor;
import eu.europeana.metis.core.engine.base.ProcessingEngineTask;
import eu.europeana.metis.core.engine.base.ProcessingEngineTaskClient;
import eu.europeana.metis.core.mongo.MorphiaDatastoreProvider;
import eu.europeana.metis.core.rest.RequestLimits;
import eu.europeana.metis.core.rest.config.properties.MetisCoreConfigurationProperties;
import eu.europeana.metis.core.service.OrchestratorService;
import eu.europeana.metis.core.service.ProxiesService;
import eu.europeana.metis.core.service.RedirectionInferrer;
import eu.europeana.metis.core.service.UserService;
import eu.europeana.metis.core.service.WorkflowExecutionFactory;
import eu.europeana.metis.core.util.ExternalEngineClients;
import eu.europeana.metis.core.workflow.ValidationProperties;
import eu.europeana.metis.core.workflow.plugins.ThrottlingValues;
import java.time.Duration;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Orchestrator configuration class.
 */
@Configuration
@EnableConfigurationProperties({
    TruststoreConfigurationProperties.class, ValidationConfigurationProperties.class,
    RedisConfigurationProperties.class, MetisCoreConfigurationProperties.class,
    EcloudConfigurationProperties.class})
@ComponentScan(basePackages = {"eu.europeana.metis.core.rest.controller"})
public class OrchestratorConfig implements WebMvcConfigurer {

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
   * @param workflowExecutorManager manager for handling workflow execution processes
   * @param depublishRecordIdDao the DAO for managing depublished record IDs
   * @param redissonClient the Redisson client instance for distributed locking and caching
   * @param userService the service for managing user-related operations
   * @param metisCoreConfigurationProperties the core configuration properties for the system
   * @return a configured instance of {@link OrchestratorService}
   */
  @Bean
  public OrchestratorService getOrchestratorService(WorkflowDao workflowDao,
      WorkflowExecutionDao workflowExecutionDao, WorkflowValidationUtils workflowValidationUtils,
      DataEvolutionUtils dataEvolutionUtils, DatasetDao datasetDao,
      WorkflowExecutionFactory workflowExecutionFactory,
      WorkflowExecutorManager workflowExecutorManager,
      DepublishRecordIdDao depublishRecordIdDao,
      RedissonClient redissonClient, UserService userService, MetisCoreConfigurationProperties metisCoreConfigurationProperties) {
    OrchestratorService orchestratorService = new OrchestratorService(workflowExecutionFactory,
        workflowDao, workflowExecutionDao, workflowValidationUtils, dataEvolutionUtils, datasetDao,
        workflowExecutorManager, redissonClient, depublishRecordIdDao, userService);
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
    WorkflowExecutionFactory workflowExecutionFactory = new WorkflowExecutionFactory(datasetXsltDao,
        depublishRecordIdDao, redirectionInferrer);
    workflowExecutionFactory
        .setValidationExternalProperties(validationExternalProperties);
    workflowExecutionFactory
        .setValidationInternalProperties(validationInternalProperties);
    workflowExecutionFactory.setDefaultSamplingSizeForLinkChecking(
        metisCoreConfigurationProperties.linkCheckingDefaultSamplingSize());
    return workflowExecutionFactory;
  }

  @Bean
  public RedirectionInferrer getRedirectionInferrer(WorkflowExecutionDao workflowExecutionDao,
      DataEvolutionUtils dataEvolutionUtils) {
    return new RedirectionInferrer(workflowExecutionDao, dataEvolutionUtils);
  }

  /**
   * Creates and returns an instance of SecuredProxiesService with the provided dependencies.
   *
   * @param workflowExecutionDao the data access object for workflow execution.
   * @param ecloudDataSetServiceClient the client service for eCloud datasets.
   * @param recordServiceClient the client for interacting with record services.
   * @param fileServiceClient the client for managing file services.
   * @param processingEngineTaskClient the client for Data Processing Services.
   * @param uisClient the client for Unified Information Services.
   * @param datasetDao the data access object for datasets.
   * @param ecloudConfigurationProperties the configuration properties for eCloud integration.
   * @return an initialized instance of SecuredProxiesService.
   */
  @Bean
  public ProxiesService getProxiesService(
      WorkflowExecutionDao workflowExecutionDao, DataSetServiceClient ecloudDataSetServiceClient,
      RecordServiceClient recordServiceClient, FileServiceClient fileServiceClient,
      ProcessingEngineTaskClient<? extends ProcessingEngineTaskSettings, ? extends ProcessingEngineTask> processingEngineTaskClient,
      UISClient uisClient, DatasetDao datasetDao, EcloudConfigurationProperties ecloudConfigurationProperties) {
    ExternalEngineClients<? extends ProcessingEngineTaskSettings, ? extends ProcessingEngineTask> externalEngineClients =
        new ExternalEngineClients<>(ecloudDataSetServiceClient, recordServiceClient,
        fileServiceClient, processingEngineTaskClient, uisClient);

    return new ProxiesService(externalEngineClients, ecloudConfigurationProperties.getProvider(), workflowExecutionDao, datasetDao);
  }

  /**
   * Bean workflow execution post processor.
   *
   * @param depublishRecordIdDao the depublish record id dao
   * @param datasetDao the dataset dao
   * @param workflowExecutionDao the workflow execution dao
   * @param processingEngineTaskClient the dps client
   * @return the workflow post processor
   */
  @Bean
  public WorkflowPostProcessor workflowPostProcessor(DepublishRecordIdDao depublishRecordIdDao,
      DatasetDao datasetDao, WorkflowExecutionDao workflowExecutionDao,
      ProcessingEngineTaskClient<? extends ProcessingEngineTaskSettings, ? extends ProcessingEngineTask> processingEngineTaskClient) {
    return new WorkflowPostProcessor(depublishRecordIdDao, datasetDao, workflowExecutionDao, processingEngineTaskClient);
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

  @Bean
  public WorkflowExecutorManager  getWorkflowExecutorManager(
      SemaphoresPerPluginManager semaphoresPerPluginManager,
      WorkflowExecutionDao workflowExecutionDao, WorkflowPostProcessor workflowPostProcessor,
      @Qualifier("rabbitmqPublisherChannel") Channel rabbitmqPublisherChannel,
      @Qualifier("rabbitmqConsumerChannel") Channel rabbitmqConsumerChannel,
      RedissonClient redissonClient,
      ProcessingEngineTaskClient<? extends ProcessingEngineTaskSettings, ? extends ProcessingEngineTask> processingEngineTaskClient,
      RabbitmqConfigurationProperties rabbitmqConfigurationProperties,
      MetisCoreConfigurationProperties metisCoreConfigurationProperties,
      EcloudConfigurationProperties ecloudConfigurationProperties) {
    WorkflowExecutorManager workflowExecutorManager = new WorkflowExecutorManager(
        semaphoresPerPluginManager, workflowExecutionDao, workflowPostProcessor,
        rabbitmqPublisherChannel, rabbitmqConsumerChannel, redissonClient, processingEngineTaskClient);
    workflowExecutorManager.setRabbitmqQueueName(rabbitmqConfigurationProperties.getQueueName());
    workflowExecutorManager
        .setDpsMonitorCheckIntervalInSecs(metisCoreConfigurationProperties.dpsMonitorCheckIntervalInSeconds());
    workflowExecutorManager.setPeriodOfNoProcessedRecordsChangeInMinutes(
        metisCoreConfigurationProperties.periodOfNoProcessedRecordsChangeInMinutes());
    workflowExecutorManager.setEcloudBaseUrl(ecloudConfigurationProperties.getBaseUrl());
    workflowExecutorManager.setEcloudProvider(ecloudConfigurationProperties.getProvider());
    workflowExecutorManager.setMetisCoreBaseUrl(metisCoreConfigurationProperties.baseUrl());
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
        .setMaxServedExecutionListLength(metisCoreConfigurationProperties.maxServedExecutionListLength());
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
        .plusMillis(metisCoreConfigurationProperties.dpsConnectTimeoutInMilliseconds())
        .plusMillis(metisCoreConfigurationProperties.dpsReadTimeoutInMilliseconds())
        .plusSeconds(metisCoreConfigurationProperties.dpsMonitorCheckIntervalInSeconds())
        .plusSeconds(metisCoreConfigurationProperties.failsafeMarginOfInactivityInSeconds());

    return new WorkflowExecutionMonitor(workflowExecutorManager,
        workflowExecutionDao, redissonClient, failsafeLeniency);
  }

  @Bean
  public ThrottlingValues getThrottlingValues(MetisCoreConfigurationProperties metisCoreConfigurationProperties) {
    return new ThrottlingValues(metisCoreConfigurationProperties.threadLimitThrottlingLevelWeak(),
        metisCoreConfigurationProperties.threadLimitThrottlingLevelMedium(),
        metisCoreConfigurationProperties.threadLimitThrottlingLevelStrong());
  }
}
