package eu.europeana.metis.core.rest.config;

import com.mongodb.client.MongoClient;
import eu.europeana.metis.common.config.properties.TruststoreConfigurationProperties;
import eu.europeana.metis.common.config.properties.ecloud.EcloudConfigurationProperties;
import eu.europeana.metis.common.config.properties.mongo.MongoConfigurationProperties;
import eu.europeana.metis.core.dao.DatasetDao;
import eu.europeana.metis.core.dao.DatasetXsltDao;
import eu.europeana.metis.core.dao.DepublishRecordIdDao;
import eu.europeana.metis.core.dao.WorkflowDao;
import eu.europeana.metis.core.dao.WorkflowExecutionDao;
import eu.europeana.metis.core.mongo.MorphiaDatastoreProvider;
import eu.europeana.metis.core.mongo.MorphiaDatastoreProviderImpl;
import eu.europeana.metis.core.rest.RequestLimits;
import eu.europeana.metis.core.rest.config.properties.MetisCoreConfigurationProperties;
import eu.europeana.metis.core.service.DatasetService;
import eu.europeana.metis.core.service.DepublishRecordIdService;
import eu.europeana.metis.core.service.OrchestratorService;
import eu.europeana.metis.core.service.UserService;
import eu.europeana.metis.mongo.connection.MongoClientProvider;
import eu.europeana.metis.mongo.connection.MongoProperties;
import eu.europeana.metis.mongo.connection.MongoProperties.ReadPreferenceValue;
import eu.europeana.metis.utils.CustomTruststoreAppender;
import eu.europeana.metis.utils.CustomTruststoreAppender.TrustStoreConfigurationException;
import eu.europeana.metis.utils.apm.ElasticAPMConfiguration;
import jakarta.annotation.PreDestroy;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.DispatcherServlet;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

/**
 * Entry class with configuration fields and beans initialization for the application.
 */
@Configuration
@EnableConfigurationProperties({
    ElasticAPMConfiguration.class, TruststoreConfigurationProperties.class, MongoConfigurationProperties.class,
    MetisCoreConfigurationProperties.class, EcloudConfigurationProperties.class})
@ComponentScan(basePackages = {"eu.europeana.metis.core.rest.controller"})
public class ApplicationConfiguration {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final MongoClient mongoClient;

  /**
   * The default transformation xslt which is expected in the class path provided by a library (e.g. metis-transformation)
   */
  @Value(value = "classpath:default_transformation.xslt")
  private Resource defaultTransformation;

  /**
   * Autowired constructor for Spring Configuration class.
   *
   * @param truststoreConfigurationProperties The properties for configuring the truststore.
   * @param mongoConfigurationProperties The properties for configuring the MongoDB connection.
   * @throws TrustStoreConfigurationException if the configuration of the truststore failed
   */
  @Autowired
  public ApplicationConfiguration(TruststoreConfigurationProperties truststoreConfigurationProperties,
      MongoConfigurationProperties mongoConfigurationProperties)
      throws TrustStoreConfigurationException {
    ApplicationConfiguration.initializeTruststore(truststoreConfigurationProperties);
    this.mongoClient = ApplicationConfiguration.getMongoClient(mongoConfigurationProperties);
  }

  /**
   * This method performs the initializing tasks for the application.
   *
   * @param truststoreConfigurationProperties The properties.
   * @throws CustomTruststoreAppender.TrustStoreConfigurationException In case a problem occurred with the truststore.
   */
  static void initializeTruststore(TruststoreConfigurationProperties truststoreConfigurationProperties)
      throws CustomTruststoreAppender.TrustStoreConfigurationException {
    if (StringUtils.isNotEmpty(truststoreConfigurationProperties.getPath()) && StringUtils
        .isNotEmpty(truststoreConfigurationProperties.getPassword())) {
      CustomTruststoreAppender
          .appendCustomTruststoreToDefault(truststoreConfigurationProperties.getPath(),
              truststoreConfigurationProperties.getPassword());
      LOGGER.info("Custom truststore appended to default truststore");
    }
  }

  /**
   * Gets a {@link MongoClient} instance based on the configuration properties.
   *
   * @param mongoConfigurationProperties The properties for configuring the MongoDB connection.
   * @return The created MongoClient instance.
   */
  public static MongoClient getMongoClient(MongoConfigurationProperties mongoConfigurationProperties) {
    final MongoProperties<IllegalArgumentException> mongoProperties = new MongoProperties<>(
        IllegalArgumentException::new);
    mongoProperties.setAllProperties(
        mongoConfigurationProperties.getHosts(),
        mongoConfigurationProperties.getPorts(),
        mongoConfigurationProperties.getAuthenticationDatabase(),
        mongoConfigurationProperties.getUsername(),
        mongoConfigurationProperties.getPassword(),
        mongoConfigurationProperties.isEnableSsl(),
        ReadPreferenceValue.PRIMARY_PREFERRED,
        mongoConfigurationProperties.getApplicationName());

    return new MongoClientProvider<>(mongoProperties).createMongoClient();
  }



  @Bean(name = DispatcherServlet.MULTIPART_RESOLVER_BEAN_NAME)
  public StandardServletMultipartResolver getMultipartResolver() {
    return new StandardServletMultipartResolver();
  }

  @Bean
  MorphiaDatastoreProvider getMorphiaDatastoreProvider(MongoConfigurationProperties mongoConfigurationProperties)
      throws IOException {
    return new MorphiaDatastoreProviderImpl(mongoClient, mongoConfigurationProperties.getDatabase(),
        defaultTransformation::getInputStream);
  }

  /**
   * Get the DAO for datasets.
   *
   * @param morphiaDatastoreProvider {@link MorphiaDatastoreProvider}
   * @return {@link DatasetDao} used to access the database for datasets
   */
  @Bean
  public DatasetDao getDatasetDao(MorphiaDatastoreProvider morphiaDatastoreProvider) {
    DatasetDao datasetDao = new DatasetDao(morphiaDatastoreProvider);
    datasetDao.setDatasetsPerRequest(RequestLimits.DATASETS_PER_REQUEST.getLimit());
    return datasetDao;
  }

  /**
   * Get the DAO for xslts.
   *
   * @param morphiaDatastoreProvider {@link MorphiaDatastoreProvider}
   * @return {@link DatasetXsltDao} used to access the database for datasets
   */
  @Bean
  public DatasetXsltDao getXsltDao(MorphiaDatastoreProvider morphiaDatastoreProvider) {
    return new DatasetXsltDao(morphiaDatastoreProvider);
  }

  /**
   * Get the DAO for depublished records.
   *
   * @param morphiaDatastoreProvider {@link MorphiaDatastoreProvider}
   * @param metisCoreConfigurationProperties the properties configuration for Metis Core
   * @return DAO used to access the database for depublished records.
   */
  @Bean
  public DepublishRecordIdDao getDepublishedRecordDao(
      MorphiaDatastoreProvider morphiaDatastoreProvider,
      MetisCoreConfigurationProperties metisCoreConfigurationProperties) {
    return new DepublishRecordIdDao(morphiaDatastoreProvider,
        metisCoreConfigurationProperties.maxDepublishRecordIdsPerDataset());
  }

  /**
   * Get the Service for datasets.
   * <p>It encapsulates several DAOs and combines their functionality into methods</p>
   *
   * @param datasetDao the Dao instance to access the Dataset database
   * @param datasetXsltDao the Dao instance to access the DatasetXslt database
   * @param workflowDao the Dao instance to access the Workflow database
   * @param workflowExecutionDao the Dao instance to access the WorkflowExecution database
   * @param redissonClient {@link RedissonClient}
   * @param userService the user service
   * @param metisCoreConfigurationProperties the metis configuration properties
   * @return the dataset service instance instantiated
   */
  @Bean
  public DatasetService getDatasetService(
      DatasetDao datasetDao, DatasetXsltDao datasetXsltDao,
      WorkflowDao workflowDao, WorkflowExecutionDao workflowExecutionDao,
      RedissonClient redissonClient, UserService userService,
      MetisCoreConfigurationProperties metisCoreConfigurationProperties) {
    DatasetService datasetService = new DatasetService(datasetDao, datasetXsltDao, workflowDao,
        workflowExecutionDao, redissonClient, userService);
    datasetService.setMetisCoreUrl(metisCoreConfigurationProperties.baseUrl());
    return datasetService;
  }

  /**
   * Creates and configures a {@link DepublishRecordIdService} bean.
   *
   * @param depublishRecordIdDao the DAO used for managing depublished record IDs
   * @param orchestratorService the orchestrator service
   * @param datasetDao the DAO for accessing dataset information
   * @return a configured instance of {@link DepublishRecordIdService}
   */
  @Bean
  public DepublishRecordIdService getDepublishedRecordService(
      DepublishRecordIdDao depublishRecordIdDao, OrchestratorService orchestratorService,
      DatasetDao datasetDao) {
    return new DepublishRecordIdService(orchestratorService, depublishRecordIdDao, datasetDao);
  }

  /**
   * Closes connections to databases when the application closes.
   */
  @PreDestroy
  public void close() {
    if (mongoClient != null) {
      mongoClient.close();
    }
  }
}
