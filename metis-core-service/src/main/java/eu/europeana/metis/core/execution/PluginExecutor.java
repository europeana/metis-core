package eu.europeana.metis.core.execution;

import static eu.europeana.metis.core.engine.base.EngineTaskParametersConfigurator.createDataRevision;
import static eu.europeana.metis.core.engine.base.EngineTaskParametersConfigurator.createDefaultTaskParameters;
import static eu.europeana.metis.core.engine.base.EngineTaskParametersConfigurator.createDefaultTaskParametersHarvest;
import static eu.europeana.metis.core.engine.base.EngineTaskParametersConfigurator.createDepublishParameters;
import static eu.europeana.metis.core.engine.base.EngineTaskParametersConfigurator.createIndexParameters;
import static eu.europeana.metis.core.engine.base.EngineTaskParametersConfigurator.createLinkCheckingParameters;
import static eu.europeana.metis.core.engine.base.EngineTaskParametersConfigurator.createMediaParameters;
import static eu.europeana.metis.core.engine.base.EngineTaskParametersConfigurator.createTransformationParameters;
import static eu.europeana.metis.core.engine.base.EngineTaskParametersConfigurator.createValidationExternalParameters;
import static eu.europeana.metis.core.engine.base.EngineTaskParametersConfigurator.createValidationInternalParameters;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElseGet;

import eu.europeana.metis.core.engine.base.DataRevision;
import eu.europeana.metis.core.engine.base.EngineTask;
import eu.europeana.metis.core.engine.base.EngineTaskClient;
import eu.europeana.metis.core.engine.base.EngineTaskKey;
import eu.europeana.metis.core.engine.base.EngineTaskSettings;
import eu.europeana.metis.core.engine.base.task.input.DepublishInputDataEndpoint;
import eu.europeana.metis.core.engine.base.task.input.HttpHarvestInputDataEndpoint;
import eu.europeana.metis.core.engine.base.task.input.InputDataEndpoint;
import eu.europeana.metis.core.engine.base.task.input.InternalInputDataEndpoint;
import eu.europeana.metis.core.engine.base.task.input.OaiHarvestInputDataEndpoint;
import eu.europeana.metis.core.workflow.plugins.AbstractExecutablePlugin;
import eu.europeana.metis.core.workflow.plugins.AbstractIndexPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.DataStatus;
import eu.europeana.metis.core.workflow.plugins.DepublishPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.EnrichmentPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.ExecutablePluginType.ExecutablePluginTypeGroup;
import eu.europeana.metis.core.workflow.plugins.HTTPHarvestPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.IndexToPreviewPlugin;
import eu.europeana.metis.core.workflow.plugins.IndexToPublishPlugin;
import eu.europeana.metis.core.workflow.plugins.LinkCheckingPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.MediaProcessPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.NormalizationPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.OaipmhHarvestPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.PluginType;
import eu.europeana.metis.core.workflow.plugins.ThrottlingLevel;
import eu.europeana.metis.core.workflow.plugins.ThrottlingValues;
import eu.europeana.metis.core.workflow.plugins.TransformationPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.ValidationExternalPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.ValidationInternalPluginMetadata;
import eu.europeana.metis.exception.ExternalTaskException;
import eu.europeana.metis.utils.CommonStringValues;
import eu.europeana.metis.utils.DepublicationReason;
import java.lang.invoke.MethodHandles;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class responsible for executing plugins by creating and submitting corresponding engine tasks.
 *
 * @param <S> Generic type parameter extending AbstractEngineTaskSettings.
 * @param <T> Generic type parameter extending AbstractEngineTask.
 */
public class PluginExecutor<S extends EngineTaskSettings, T extends EngineTask> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final AbstractExecutablePlugin<?> plugin;
  private final EngineTaskClient<S, T> engineTaskClient;

  /**
   * Constructs a PluginExecutor with the specified plugin and engine task client.
   *
   * @param plugin AbstractExecutablePlugin instance used to execute the plugin logic.
   * @param engineTaskClient EngineTaskClient instance used to manage and interact with engine tasks.
   */
  public PluginExecutor(AbstractExecutablePlugin<?> plugin, EngineTaskClient<S, T> engineTaskClient) {
    this.plugin = plugin;
    this.engineTaskClient = engineTaskClient;
  }

  /**
   * Submits a task to the processing engine for the specified dataset and previous task if any.
   *
   * @param datasetId Identifier of the dataset for which the task is submitted.
   * @param engineDatasetId
   * @param previousTaskId Identifier of the previous task to maintain task dependencies. Can be null or empty.
   * @throws ExternalTaskException If an error occurs while submitting the task.
   */
  public void submit(String datasetId, String engineDatasetId, String previousTaskId) throws ExternalTaskException {
    T engineTask = createEngineTask(datasetId, engineDatasetId, previousTaskId);

    LOGGER.info("Starting execution of {} plugin for externalDatasetId {}", plugin.getPluginType(), datasetId);
    try {
      String taskId = engineTaskClient.submitEngineTask(engineTask, plugin.getTopologyName());
      plugin.setExternalTaskId(taskId);
      plugin.setDataStatus(DataStatus.VALID);
    } catch (ExternalTaskException | RuntimeException e) {
      throw new ExternalTaskException(
          format("Submitting task for plugin type %s and dataset %s failed", plugin.getPluginType(), datasetId), e);
    }
    LOGGER.info("Submitted task with externalTaskId: {}", plugin.getExternalTaskId());
  }

  private T createEngineTask(String datasetId, String engineDatasetId, String previousTaskId) {
    Map<EngineTaskKey, String> pluginParameters;
    PluginHarvestParameters pluginHarvestParameters;
    ExecutablePluginTypeGroup executablePluginTypeGroup = plugin.getPluginMetadata().getExecutablePluginType()
                                                                .getExecutablePluginTypeGroup();
    return switch (executablePluginTypeGroup) {
      case HARVEST -> {
        pluginHarvestParameters = getPluginHarvestParameters();
        yield createHarvestEngineTask(datasetId, engineDatasetId, pluginHarvestParameters);
      }
      case PROCESS -> {
        pluginParameters = getProcessPluginParameters();
        yield createProcessEngineTask(datasetId, engineDatasetId, previousTaskId, pluginParameters);
      }
      case INDEX -> {
        pluginParameters = getIndexPluginParameters();
        yield createProcessEngineTask(datasetId, engineDatasetId, previousTaskId, pluginParameters);
      }
      case DEPUBLISH -> {
        pluginParameters = getDepublishPluginParameters(datasetId);
        yield createDepublishEngineTask(datasetId, engineDatasetId, pluginParameters);
      }
    };
  }

  private @NotNull T createHarvestEngineTask(String datasetId, String engineDatasetId,
      PluginHarvestParameters pluginHarvestParameters) {
    final String dataLocation = getDataLocation(engineDatasetId);
    final Map<EngineTaskKey, String> basicTaskParameters =
        createDefaultTaskParametersHarvest(
            datasetId, pluginHarvestParameters.incrementalHarvest(), plugin.getStartedDate(), dataLocation,
            engineTaskClient.getEngineTaskSettings().getProvider());
    final Map<EngineTaskKey, String> allParameters = new EnumMap<>(EngineTaskKey.class);
    allParameters.putAll(basicTaskParameters);

    final DataRevision outputDataRevision = createDataRevision(
        plugin.getPluginType(), plugin.getStartedDate(), engineTaskClient.getEngineTaskSettings().getProvider());

    final InputDataEndpoint inputDataEndpoint =
        requireNonNullElseGet(pluginHarvestParameters.oaiHarvestInputDataParameters(),
            () -> new HttpHarvestInputDataEndpoint(pluginHarvestParameters.targetUrl()));
    return engineTaskClient.createEngineTask(allParameters, inputDataEndpoint, outputDataRevision);
  }

  private record PluginHarvestParameters(String targetUrl, boolean incrementalHarvest,
                                         OaiHarvestInputDataEndpoint oaiHarvestInputDataParameters) {

  }

  @NotNull
  private T createProcessEngineTask(String datasetId, String engineDatasetId, String previousTaskId,
      Map<EngineTaskKey, String> pluginParameters) {
    final DataRevision inputDataRevision = createDataRevision(
        requireNonNull(PluginType.getPluginTypeFromEnumName(plugin.getPluginMetadata().getRevisionNamePreviousPlugin())),
        plugin.getPluginMetadata().getRevisionTimestampPreviousPlugin(),
        engineTaskClient.getEngineTaskSettings().getProvider());

    final String dataLocation = getDataLocation(engineDatasetId);
    final Map<EngineTaskKey, String> basicTaskParameters =
        createDefaultTaskParameters(datasetId, previousTaskId, inputDataRevision, dataLocation);
    final Map<EngineTaskKey, String> allParameters = new EnumMap<>(EngineTaskKey.class);
    allParameters.putAll(basicTaskParameters);
    allParameters.putAll(pluginParameters);

    final DataRevision outputDataRevision = createDataRevision(
        plugin.getPluginType(), plugin.getStartedDate(), engineTaskClient.getEngineTaskSettings().getProvider());

    final InternalInputDataEndpoint internalInputDataEndpoint = new InternalInputDataEndpoint(dataLocation,
        inputDataRevision);
    return engineTaskClient.createEngineTask(allParameters, internalInputDataEndpoint, outputDataRevision);
  }

  private @NotNull String getDataLocation(String datasetId) {
    return format(CommonStringValues.S_DATA_PROVIDERS_S_DATA_SETS_S_TEMPLATE,
        engineTaskClient.getEngineTaskSettings().getBaseUrl(),
        engineTaskClient.getEngineTaskSettings().getProvider(),
        datasetId);
  }

  @NotNull
  private T createDepublishEngineTask(String datasetId, String engineDatasetId, Map<EngineTaskKey, String> pluginParameters) {
    final String dataLocation = getDataLocation(engineDatasetId);
    final DepublishInputDataEndpoint internalInputDataEndpoint = new DepublishInputDataEndpoint(dataLocation);
    final DataRevision outputDataRevision = createDataRevision(
        plugin.getPluginType(), plugin.getStartedDate(), engineTaskClient.getEngineTaskSettings().getProvider());
    return engineTaskClient.createEngineTask(pluginParameters, internalInputDataEndpoint, outputDataRevision);
  }

  private @NotNull PluginHarvestParameters getPluginHarvestParameters() {
    boolean incrementalHarvest;
    OaiHarvestInputDataEndpoint oaiHarvestInputDataParameters = null;
    String targetUrl;
    switch (plugin.getPluginMetadata()) {
      case OaipmhHarvestPluginMetadata oaipmhHarvestPluginMetadata -> {
        incrementalHarvest = oaipmhHarvestPluginMetadata.isIncrementalHarvest();
        targetUrl = oaipmhHarvestPluginMetadata.getUrl();
        oaiHarvestInputDataParameters = new OaiHarvestInputDataEndpoint(
            oaipmhHarvestPluginMetadata.getUrl(),
            oaipmhHarvestPluginMetadata.getSetSpec(),
            oaipmhHarvestPluginMetadata.getMetadataFormat(),
            oaipmhHarvestPluginMetadata.getFromDate(),
            oaipmhHarvestPluginMetadata.getUntilDate());
      }
      case HTTPHarvestPluginMetadata httpHarvestPluginMetadata -> {
        incrementalHarvest = httpHarvestPluginMetadata.isIncrementalHarvest();
        targetUrl = httpHarvestPluginMetadata.getUrl();
      }
      default -> throw new IllegalStateException("Unexpected value: " + plugin);
    }
    return new PluginHarvestParameters(targetUrl, incrementalHarvest, oaiHarvestInputDataParameters);
  }

  private @NotNull Map<EngineTaskKey, String> getProcessPluginParameters() {
    return switch (plugin.getPluginMetadata()) {
      case ValidationExternalPluginMetadata validationExternalPluginMetadata -> {
        String urlOfSchemasZip = validationExternalPluginMetadata.getUrlOfSchemasZip();
        String schemaRootPath = validationExternalPluginMetadata.getSchemaRootPath();
        String schematronRootPath = validationExternalPluginMetadata.getSchematronRootPath();
        yield createValidationExternalParameters(urlOfSchemasZip, schemaRootPath,
            schematronRootPath);
      }
      case TransformationPluginMetadata transformationPluginMetadata -> {
        String metisCoreBaseUrl = engineTaskClient.getEngineTaskSettings().getMetisCoreBaseUrl();
        String xsltId = transformationPluginMetadata.getXsltId();
        String datasetName = transformationPluginMetadata.getDatasetName();
        String country = transformationPluginMetadata.getCountry();
        String language = transformationPluginMetadata.getLanguage();
        yield createTransformationParameters(metisCoreBaseUrl, xsltId, datasetName, country, language);
      }
      case ValidationInternalPluginMetadata validationInternalPluginMetadata -> {
        String urlOfSchemasZip = validationInternalPluginMetadata.getUrlOfSchemasZip();
        String schemaRootPath = validationInternalPluginMetadata.getSchemaRootPath();
        String schematronRootPath = validationInternalPluginMetadata.getSchematronRootPath();
        yield createValidationInternalParameters(urlOfSchemasZip, schemaRootPath,
            schematronRootPath);
      }
      case NormalizationPluginMetadata ignored -> new EnumMap<>(EngineTaskKey.class);
      case EnrichmentPluginMetadata ignored -> new EnumMap<>(EngineTaskKey.class);
      case MediaProcessPluginMetadata mediaProcessPluginMetadata -> {
        ThrottlingValues throttlingValues = engineTaskClient.getEngineTaskSettings().getThrottlingValues();
        ThrottlingLevel throttlingLevel = mediaProcessPluginMetadata.getThrottlingLevel() == null ?
            ThrottlingLevel.WEAK : mediaProcessPluginMetadata.getThrottlingLevel();
        String maximumParallelization = String.valueOf(throttlingValues.getThreadNumberFromThrottlingLevel(throttlingLevel));
        yield createMediaParameters(maximumParallelization);
      }
      case LinkCheckingPluginMetadata linkCheckingPluginMetadata -> {
        boolean performSampling = linkCheckingPluginMetadata.getPerformSampling();
        Integer sampleSize = linkCheckingPluginMetadata.getSampleSize();
        yield createLinkCheckingParameters(performSampling, sampleSize);
      }
      default -> throw new IllegalStateException("Unexpected value: " + plugin);
    };
  }

  private @NotNull Map<EngineTaskKey, String> getIndexPluginParameters() {
    if (plugin.getPluginMetadata() instanceof AbstractIndexPluginMetadata indexPluginMetadata) {
      boolean incrementalIndexing = indexPluginMetadata.isIncrementalIndexing();
      Date harvestDate = indexPluginMetadata.getHarvestDate();
      boolean preserveTimestamps = indexPluginMetadata.isPreserveTimestamps();
      List<String> datasetIdsToRedirectFrom = indexPluginMetadata.getDatasetIdsToRedirectFrom();
      boolean performRedirects = indexPluginMetadata.isPerformRedirects();
      final String targetIndexingDatabase;
      if (plugin instanceof IndexToPreviewPlugin indexToPreviewPlugin) {
        targetIndexingDatabase = indexToPreviewPlugin.getTargetIndexingDatabase().name();
      } else {
        targetIndexingDatabase = ((IndexToPublishPlugin) plugin).getTargetIndexingDatabase().name();
      }
      return createIndexParameters(plugin.getStartedDate(), incrementalIndexing,
          harvestDate, preserveTimestamps, datasetIdsToRedirectFrom, performRedirects, targetIndexingDatabase);
    } else {
      throw new IllegalStateException("Unexpected value: " + plugin);
    }
  }

  private @NotNull Map<EngineTaskKey, String> getDepublishPluginParameters(String datasetId) {
    if (plugin.getPluginMetadata() instanceof DepublishPluginMetadata depublishPluginMetadata) {
      boolean datasetDepublish = depublishPluginMetadata.isDatasetDepublish();
      Set<String> recordIdsToDepublish = depublishPluginMetadata.getRecordIdsToDepublish();
      String depublicationReason = depublishPluginMetadata.getDepublicationReason() == null ? DepublicationReason.GENERIC.name()
          : depublishPluginMetadata.getDepublicationReason().name();
      return createDepublishParameters(datasetId, datasetDepublish, recordIdsToDepublish,
          depublicationReason);
    } else {
      throw new IllegalStateException("Unexpected value: " + plugin);
    }
  }
}
