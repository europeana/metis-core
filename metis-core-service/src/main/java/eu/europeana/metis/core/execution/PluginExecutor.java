package eu.europeana.metis.core.execution;

import eu.europeana.metis.core.dao.DataEvolutionUtils;
import eu.europeana.metis.core.engine.base.OaiHarvestParameters;
import eu.europeana.metis.core.engine.base.ProcessingEngineTask;
import eu.europeana.metis.core.engine.base.ProcessingEngineTaskClient;
import eu.europeana.metis.core.engine.base.ProcessingEngineTaskConfigurator;
import eu.europeana.metis.core.engine.base.ProcessingEngineTaskKeys;
import eu.europeana.metis.core.engine.base.ProcessingEngineTaskSettings;
import eu.europeana.metis.core.workflow.plugins.AbstractExecutablePlugin;
import eu.europeana.metis.core.workflow.plugins.AbstractIndexPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.DataStatus;
import eu.europeana.metis.core.workflow.plugins.DepublishPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.EnrichmentPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.ExecutablePluginType;
import eu.europeana.metis.core.workflow.plugins.HTTPHarvestPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.IndexToPreviewPlugin;
import eu.europeana.metis.core.workflow.plugins.LinkCheckingPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.MediaProcessPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.NormalizationPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.OaipmhHarvestPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.ThrottlingLevel;
import eu.europeana.metis.core.workflow.plugins.ThrottlingValues;
import eu.europeana.metis.core.workflow.plugins.TransformationPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.ValidationExternalPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.ValidationInternalPluginMetadata;
import eu.europeana.metis.exception.ExternalTaskException;
import java.lang.invoke.MethodHandles;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PluginExecutor {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final AbstractExecutablePlugin<?> plugin;

  public PluginExecutor(AbstractExecutablePlugin<?> plugin) {
    this.plugin = plugin;
  }

  public <S extends ProcessingEngineTaskSettings, T extends ProcessingEngineTask>
  void execute(String datasetId, String previousTaskId, ProcessingEngineTaskClient<S, T> processingEngineTaskClient)
      throws ExternalTaskException {
    //Prepare parameters
    Map<ProcessingEngineTaskKeys, String> pluginParameters = Map.of();
    PluginHarvestParameters pluginHarvestParameters = null;
    if (DataEvolutionUtils.getHarvestPluginGroup().contains(plugin.getPluginMetadata().getExecutablePluginType())) {
      pluginHarvestParameters = getPluginHarvestParameters();
    } else if (DataEvolutionUtils.getProcessPluginGroup().contains(plugin.getPluginMetadata().getExecutablePluginType())) {
      pluginParameters = getProcessPluginParameters(datasetId, processingEngineTaskClient);
    } else if (DataEvolutionUtils.getIndexPluginGroup().contains(plugin.getPluginMetadata().getExecutablePluginType())) {
      pluginParameters = getIndexPluginParameters(datasetId);
    } else if (plugin.getPluginMetadata().getExecutablePluginType().equals(ExecutablePluginType.DEPUBLISH)) {
      pluginParameters = getDepublishPluginParameters(datasetId);
    }

    //Prepare task
    T processingEngineTask = null;
    if (DataEvolutionUtils.getHarvestPluginGroup().contains(plugin.getPluginMetadata().getExecutablePluginType())) {
      processingEngineTask = createProcessingEngineTask(datasetId, processingEngineTaskClient, pluginHarvestParameters);
    } else if (DataEvolutionUtils.getProcessPluginGroup().contains(plugin.getPluginMetadata().getExecutablePluginType())
        || DataEvolutionUtils.getIndexPluginGroup().contains(plugin.getPluginMetadata().getExecutablePluginType())) {
      processingEngineTask = createProcessingEngineTask(datasetId, previousTaskId, processingEngineTaskClient, pluginParameters);
    } else if (plugin.getPluginMetadata().getExecutablePluginType().equals(ExecutablePluginType.DEPUBLISH)) {
      processingEngineTask = createProcessingEngineTask(processingEngineTaskClient, pluginParameters);
    }

    LOGGER.info("Starting execution of {} plugin for externalDatasetId {}", plugin.getPluginType(), datasetId);
    try {
      long taskId = processingEngineTaskClient.submitTask(processingEngineTask, plugin.getTopologyName());
      plugin.setExternalTaskId(String.valueOf(taskId));
      plugin.setDataStatus(DataStatus.VALID);
    } catch (ExternalTaskException | RuntimeException e) {
      throw new ExternalTaskException("Submitting task failed", e);
    }
    LOGGER.info("Submitted task with externalTaskId: {}", plugin.getExternalTaskId());
  }

  private <S extends ProcessingEngineTaskSettings, T extends ProcessingEngineTask> @NotNull T createProcessingEngineTask(
      String datasetId,
      ProcessingEngineTaskClient<S, T> processingEngineTaskClient, PluginHarvestParameters pluginHarvestParameters) {
    T processingEngineTask;
    final Map<ProcessingEngineTaskKeys, String> basicTaskParameters = ProcessingEngineTaskConfigurator.createDefaultTaskParametersHarvest(
        datasetId,
        pluginHarvestParameters.incrementalHarvest(),
        plugin.getStartedDate(),
        processingEngineTaskClient);
    final Map<ProcessingEngineTaskKeys, String> allParameters = new HashMap<>(basicTaskParameters);
    allParameters.putAll(pluginHarvestParameters.pluginParameters());
    processingEngineTask = ProcessingEngineTaskConfigurator.createProcessingEngineTaskForHarvest(
        pluginHarvestParameters.targetUrl(),
        plugin.getPluginType(),
        plugin.getStartedDate(),
        allParameters,
        processingEngineTaskClient,
        pluginHarvestParameters.oaiHarvestParameters()
    );
    return processingEngineTask;
  }

  private record PluginHarvestParameters(String targetUrl, boolean incrementalHarvest,
                                         Map<ProcessingEngineTaskKeys, String> pluginParameters,
                                         OaiHarvestParameters oaiHarvestParameters) {

  }

  @NotNull
  private <S extends ProcessingEngineTaskSettings, T extends ProcessingEngineTask> T createProcessingEngineTask(String datasetId,
      String previousTaskId, ProcessingEngineTaskClient<S, T> processingEngineTaskClient,
      Map<ProcessingEngineTaskKeys, String> pluginParameters) {
    T processingEngineTask;
    final Map<ProcessingEngineTaskKeys, String> basicTaskParameters = ProcessingEngineTaskConfigurator.createDefaultTaskParameters(
        datasetId,
        previousTaskId,
        plugin.getPluginMetadata().getRevisionNamePreviousPlugin(),
        plugin.getPluginMetadata().getRevisionTimestampPreviousPlugin(),
        processingEngineTaskClient);
    final Map<ProcessingEngineTaskKeys, String> allParameters = new HashMap<>(basicTaskParameters);
    allParameters.putAll(pluginParameters);
    processingEngineTask = ProcessingEngineTaskConfigurator.createProcessingEngineTask(
        datasetId,
        plugin.getPluginType(),
        plugin.getStartedDate(),
        allParameters,
        processingEngineTaskClient
    );
    return processingEngineTask;
  }

  @NotNull
  private <S extends ProcessingEngineTaskSettings, T extends ProcessingEngineTask> T createProcessingEngineTask(
      ProcessingEngineTaskClient<S, T> processingEngineTaskClient, Map<ProcessingEngineTaskKeys, String> pluginParameters) {
    T processingEngineTask = ProcessingEngineTaskConfigurator.createProcessingEngineTaskForDepublish(pluginParameters,
        processingEngineTaskClient);
    return processingEngineTask;
  }

  private @NotNull PluginHarvestParameters getPluginHarvestParameters() {
    boolean incrementalHarvest;
    OaiHarvestParameters oaiHarvestParameters = null;
    String targetUrl;
    final Map<ProcessingEngineTaskKeys, String> pluginParameters = switch (plugin.getPluginMetadata()) {
      case OaipmhHarvestPluginMetadata oaipmhHarvestPluginMetadata -> {
        incrementalHarvest = oaipmhHarvestPluginMetadata.isIncrementalHarvest();
        targetUrl = oaipmhHarvestPluginMetadata.getUrl();
        oaiHarvestParameters = new OaiHarvestParameters(
            oaipmhHarvestPluginMetadata.getSetSpec(),
            oaipmhHarvestPluginMetadata.getMetadataFormat(),
            oaipmhHarvestPluginMetadata.getFromDate(),
            oaipmhHarvestPluginMetadata.getUntilDate());
        yield new HashMap<>();
      }
      case HTTPHarvestPluginMetadata httpHarvestPluginMetadata -> {
        incrementalHarvest = httpHarvestPluginMetadata.isIncrementalHarvest();
        targetUrl = httpHarvestPluginMetadata.getUrl();
        yield new HashMap<>();
      }
      default -> throw new IllegalStateException("Unexpected value: " + plugin);
    };
    return new PluginHarvestParameters(targetUrl, incrementalHarvest, pluginParameters, oaiHarvestParameters);
  }

  private <S extends ProcessingEngineTaskSettings, T extends ProcessingEngineTask> @NotNull Map<ProcessingEngineTaskKeys, String> getProcessPluginParameters(
      String datasetId, ProcessingEngineTaskClient<S, T> processingEngineTaskClient) {
    return switch (plugin.getPluginMetadata()) {
      case ValidationExternalPluginMetadata validationExternalPluginMetadata -> {
        String urlOfSchemasZip = validationExternalPluginMetadata.getUrlOfSchemasZip();
        String schemaRootPath = validationExternalPluginMetadata.getSchemaRootPath();
        String schematronRootPath = validationExternalPluginMetadata.getSchematronRootPath();
        yield ProcessingEngineTaskConfigurator.createParametersForValidationExternal(urlOfSchemasZip, schemaRootPath,
            schematronRootPath);
      }
      case TransformationPluginMetadata transformationPluginMetadata -> {
        String metisCoreBaseUrl = processingEngineTaskClient.getProcessingEngineTaskSettings().metisCoreBaseUrl();
        String xsltId = transformationPluginMetadata.getXsltId();
        String datasetName = transformationPluginMetadata.getDatasetName();
        String country = transformationPluginMetadata.getCountry();
        String language = transformationPluginMetadata.getLanguage();
        yield ProcessingEngineTaskConfigurator.createParametersForTransformation(metisCoreBaseUrl, xsltId, datasetId, datasetName,
            country, language);
      }
      case ValidationInternalPluginMetadata validationInternalPluginMetadata -> {
        String urlOfSchemasZip = validationInternalPluginMetadata.getUrlOfSchemasZip();
        String schemaRootPath = validationInternalPluginMetadata.getSchemaRootPath();
        String schematronRootPath = validationInternalPluginMetadata.getSchematronRootPath();
        yield ProcessingEngineTaskConfigurator.createParametersForValidationInternal(urlOfSchemasZip, schemaRootPath,
            schematronRootPath);
      }
      case NormalizationPluginMetadata normalizationPluginMetadata -> new HashMap<>();
      case EnrichmentPluginMetadata enrichmentPluginMetadata -> new HashMap<>();
      case MediaProcessPluginMetadata mediaProcessPluginMetadata -> {
        ThrottlingValues throttlingValues = processingEngineTaskClient.getProcessingEngineTaskSettings().throttlingValues();
        ThrottlingLevel throttlingLevel = mediaProcessPluginMetadata.getThrottlingLevel() == null ?
            ThrottlingLevel.WEAK : mediaProcessPluginMetadata.getThrottlingLevel();
        String maximumParallelization = String.valueOf(throttlingValues.getThreadNumberFromThrottlingLevel(throttlingLevel));
        yield ProcessingEngineTaskConfigurator.createParametersForMedia(maximumParallelization);
      }
      case LinkCheckingPluginMetadata linkCheckingPluginMetadata -> {
        Boolean performSampling = linkCheckingPluginMetadata.getPerformSampling();
        Integer sampleSize = linkCheckingPluginMetadata.getSampleSize();
        yield ProcessingEngineTaskConfigurator.createParametersForLinkChecking(performSampling, sampleSize);
      }
      default -> throw new IllegalStateException("Unexpected value: " + plugin);
    };
  }

  private @NotNull Map<ProcessingEngineTaskKeys, String> getIndexPluginParameters(String datasetId) {
    final Map<ProcessingEngineTaskKeys, String> pluginParameters = switch (plugin.getPluginMetadata()) {
      case AbstractIndexPluginMetadata indexPluginMetadata -> {
        boolean incrementalIndexing = indexPluginMetadata.isIncrementalIndexing();
        Date harvestDate = indexPluginMetadata.getHarvestDate();
        boolean preserveTimestamps = indexPluginMetadata.isPreserveTimestamps();
        List<String> datasetIdsToRedirectFrom = indexPluginMetadata.getDatasetIdsToRedirectFrom();
        boolean performRedirects = indexPluginMetadata.isPerformRedirects();
        String targetIndexingDatabase = ((IndexToPreviewPlugin) plugin).getTargetIndexingDatabase().name();
        yield ProcessingEngineTaskConfigurator.createParametersIndex(datasetId, plugin.getStartedDate(), incrementalIndexing,
            harvestDate, preserveTimestamps, datasetIdsToRedirectFrom, performRedirects, targetIndexingDatabase);
      }
      default -> throw new IllegalStateException("Unexpected value: " + plugin);
    };
    return pluginParameters;
  }

  private @NotNull Map<ProcessingEngineTaskKeys, String> getDepublishPluginParameters(String datasetId) {
    final Map<ProcessingEngineTaskKeys, String> pluginParameters = switch (plugin.getPluginMetadata()) {
      case DepublishPluginMetadata depublishPluginMetadata -> {
        boolean datasetDepublish = depublishPluginMetadata.isDatasetDepublish();
        Set<String> recordIdsToDepublish = depublishPluginMetadata.getRecordIdsToDepublish();
        String depublicationReason = depublishPluginMetadata.getDepublicationReason().name();
        yield ProcessingEngineTaskConfigurator.createParametersDepublish(datasetId, datasetDepublish, recordIdsToDepublish,
            depublicationReason);
      }
      default -> throw new IllegalStateException("Unexpected value: " + plugin);
    };
    return pluginParameters;
  }
}
