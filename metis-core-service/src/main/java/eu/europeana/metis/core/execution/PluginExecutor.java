package eu.europeana.metis.core.execution;

import eu.europeana.cloud.service.dps.PluginParameterKeys;
import eu.europeana.metis.core.common.RecordIdUtils;
import eu.europeana.metis.core.dao.DataEvolutionUtils;
import eu.europeana.metis.core.engine.base.OaiHarvestParameters;
import eu.europeana.metis.core.engine.base.ProcessingEngineTask;
import eu.europeana.metis.core.engine.base.ProcessingEngineTaskClient;
import eu.europeana.metis.core.engine.base.ProcessingEngineTaskPreparator;
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
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

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
    Map<String, String> pluginParameters = Map.of();
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
    final Map<String, String> basicTaskParameters = ProcessingEngineTaskPreparator.createBasicTaskParametersHarvest(
        datasetId,
        pluginHarvestParameters.incrementalHarvest(),
        plugin.getStartedDate(),
        processingEngineTaskClient);
    final Map<String, String> allParameters = new HashMap<>(basicTaskParameters);
    allParameters.putAll(pluginHarvestParameters.pluginParameters());
    processingEngineTask = ProcessingEngineTaskPreparator.createExternalTaskForHarvest(
        pluginHarvestParameters.targetUrl(),
        plugin.getPluginType(),
        plugin.getStartedDate(),
        allParameters,
        processingEngineTaskClient,
        pluginHarvestParameters.oaiHarvestParameters()
    );
    return processingEngineTask;
  }

  private record PluginHarvestParameters(String targetUrl, boolean incrementalHarvest, Map<String, String> pluginParameters,
                                         OaiHarvestParameters oaiHarvestParameters) {

  }

  @NotNull
  private <S extends ProcessingEngineTaskSettings, T extends ProcessingEngineTask> T createProcessingEngineTask(String datasetId,
      String previousTaskId, ProcessingEngineTaskClient<S, T> processingEngineTaskClient, Map<String, String> pluginParameters) {
    T processingEngineTask;
    final Map<String, String> basicTaskParameters = ProcessingEngineTaskPreparator.createBasicTaskParameters(
        datasetId,
        previousTaskId,
        plugin.getPluginMetadata().getRevisionNamePreviousPlugin(),
        plugin.getPluginMetadata().getRevisionTimestampPreviousPlugin(),
        processingEngineTaskClient);
    final Map<String, String> allParameters = new HashMap<>(basicTaskParameters);
    allParameters.putAll(pluginParameters);
    processingEngineTask = ProcessingEngineTaskPreparator.createExternalTaskForPluginWithExistingDataset(
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
      ProcessingEngineTaskClient<S, T> processingEngineTaskClient, Map<String, String> pluginParameters) {
    T processingEngineTask = ProcessingEngineTaskPreparator.createExternalTaskForDepublish(pluginParameters, processingEngineTaskClient);
    return processingEngineTask;
  }

  private @NotNull PluginHarvestParameters getPluginHarvestParameters() {
    boolean incrementalHarvest;
    OaiHarvestParameters oaiHarvestParameters = null;
    String targetUrl;
    final Map<String, String> pluginParameters = switch (plugin.getPluginMetadata()) {
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

  private <S extends ProcessingEngineTaskSettings, T extends ProcessingEngineTask> @NotNull Map<String, String> getProcessPluginParameters(
      String datasetId, ProcessingEngineTaskClient<S, T> processingEngineTaskClient) {
    return switch (plugin.getPluginMetadata()) {
      case ValidationExternalPluginMetadata validationExternalPluginMetadata -> {
        String urlOfSchemasZip = validationExternalPluginMetadata.getUrlOfSchemasZip();
        String schemaRootPath = validationExternalPluginMetadata.getSchemaRootPath();
        String schematronRootPath = validationExternalPluginMetadata.getSchematronRootPath();
        yield ProcessingEngineTaskPreparator.createParametersForValidationExternal(urlOfSchemasZip, schemaRootPath,
            schematronRootPath);
      }
      case TransformationPluginMetadata transformationPluginMetadata -> {
        String metisCoreBaseUrl = processingEngineTaskClient.getProcessingEngineTaskSettings().metisCoreBaseUrl();
        String xsltId = transformationPluginMetadata.getXsltId();
        String datasetName = transformationPluginMetadata.getDatasetName();
        String country = transformationPluginMetadata.getCountry();
        String language = transformationPluginMetadata.getLanguage();
        yield ProcessingEngineTaskPreparator.createParametersForTransformation(metisCoreBaseUrl, xsltId, datasetId, datasetName,
            country, language);
      }
      case ValidationInternalPluginMetadata validationInternalPluginMetadata -> {
        String urlOfSchemasZip = validationInternalPluginMetadata.getUrlOfSchemasZip();
        String schemaRootPath = validationInternalPluginMetadata.getSchemaRootPath();
        String schematronRootPath = validationInternalPluginMetadata.getSchematronRootPath();
        yield ProcessingEngineTaskPreparator.createParametersForValidationInternal(urlOfSchemasZip, schemaRootPath,
            schematronRootPath);
      }
      case NormalizationPluginMetadata normalizationPluginMetadata -> new HashMap<>();
      case EnrichmentPluginMetadata enrichmentPluginMetadata -> new HashMap<>();
      case MediaProcessPluginMetadata mediaProcessPluginMetadata -> {
        ThrottlingValues throttlingValues = processingEngineTaskClient.getProcessingEngineTaskSettings().throttlingValues();
        ThrottlingLevel throttlingLevel = mediaProcessPluginMetadata.getThrottlingLevel() == null ?
            ThrottlingLevel.WEAK : mediaProcessPluginMetadata.getThrottlingLevel();
        String maximumParallelization = String.valueOf(throttlingValues.getThreadNumberFromThrottlingLevel(throttlingLevel));
        yield ProcessingEngineTaskPreparator.createParametersForMedia(maximumParallelization);
      }
      case LinkCheckingPluginMetadata linkCheckingPluginMetadata -> {
        Boolean performSampling = linkCheckingPluginMetadata.getPerformSampling();
        Integer sampleSize = linkCheckingPluginMetadata.getSampleSize();
        yield ProcessingEngineTaskPreparator.createParametersForLinkChecking(performSampling, sampleSize);
      }
      default -> throw new IllegalStateException("Unexpected value: " + plugin);
    };
  }

  private @NotNull Map<String, String> getIndexPluginParameters(String datasetId) {
    final Map<String, String> pluginParameters = switch (plugin.getPluginMetadata()) {
      case AbstractIndexPluginMetadata indexPluginMetadata -> {
        boolean incrementalIndexing = indexPluginMetadata.isIncrementalIndexing();
        Date harvestDate = indexPluginMetadata.getHarvestDate();
        boolean preserveTimestamps = indexPluginMetadata.isPreserveTimestamps();
        List<String> datasetIdsToRedirectFrom = indexPluginMetadata.getDatasetIdsToRedirectFrom();
        boolean performRedirects = indexPluginMetadata.isPerformRedirects();
        String targetIndexingDatabase = ((IndexToPreviewPlugin) plugin).getTargetIndexingDatabase().name();
        yield ProcessingEngineTaskPreparator.createParametersIndex(datasetId, plugin.getStartedDate(), incrementalIndexing,
            harvestDate, preserveTimestamps, datasetIdsToRedirectFrom, performRedirects, targetIndexingDatabase);
      }
      default -> throw new IllegalStateException("Unexpected value: " + plugin);
    };
    return pluginParameters;
  }

  private @NotNull Map<String, String> getDepublishPluginParameters(String datasetId) {
    final Map<String, String> pluginParameters = new HashMap<>();
    pluginParameters.put(PluginParameterKeys.METIS_DATASET_ID, datasetId);

    if (plugin.getPluginMetadata() instanceof DepublishPluginMetadata depublishPluginMetadata) {
      //Do set the records ids parameter only if record ids depublication enabled and there are record ids
      if (!depublishPluginMetadata.isDatasetDepublish()) {
        if (CollectionUtils.isEmpty(depublishPluginMetadata.getRecordIdsToDepublish())) {
          throw new IllegalStateException(
              "Requested record depublication but there are no records ids for depublication in the db");
        } else {
          final String recordIdList = String.join(",", RecordIdUtils
              .composeFullRecordIds(datasetId, depublishPluginMetadata.getRecordIdsToDepublish()));
          pluginParameters.put("RECORD_IDS_TO_DEPUBLISH", recordIdList);
        }
      }
      pluginParameters.put("DEPUBLICATION_REASON", depublishPluginMetadata.getDepublicationReason().name());
    }
    return pluginParameters;
  }

}
