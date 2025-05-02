package eu.europeana.metis.core.execution;

import eu.europeana.metis.core.dao.DataEvolutionUtils;
import eu.europeana.metis.core.engine.base.OaiHarvestParameters;
import eu.europeana.metis.core.engine.base.EngineTask;
import eu.europeana.metis.core.engine.base.EngineTaskClient;
import eu.europeana.metis.core.engine.base.EngineTaskConfigurator;
import eu.europeana.metis.core.engine.base.EngineTaskKey;
import eu.europeana.metis.core.engine.base.EngineTaskSettings;
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

  public <S extends EngineTaskSettings, T extends EngineTask>
  void execute(String datasetId, String previousTaskId, EngineTaskClient<S, T> engineTaskClient)
      throws ExternalTaskException {
    //Prepare parameters
    Map<EngineTaskKey, String> pluginParameters = Map.of();
    PluginHarvestParameters pluginHarvestParameters = null;
    if (DataEvolutionUtils.getHarvestPluginGroup().contains(plugin.getPluginMetadata().getExecutablePluginType())) {
      pluginHarvestParameters = getPluginHarvestParameters();
    } else if (DataEvolutionUtils.getProcessPluginGroup().contains(plugin.getPluginMetadata().getExecutablePluginType())) {
      pluginParameters = getProcessPluginParameters(datasetId, engineTaskClient);
    } else if (DataEvolutionUtils.getIndexPluginGroup().contains(plugin.getPluginMetadata().getExecutablePluginType())) {
      pluginParameters = getIndexPluginParameters(datasetId);
    } else if (plugin.getPluginMetadata().getExecutablePluginType().equals(ExecutablePluginType.DEPUBLISH)) {
      pluginParameters = getDepublishPluginParameters(datasetId);
    }

    //Prepare task
    T engineTask = null;
    if (DataEvolutionUtils.getHarvestPluginGroup().contains(plugin.getPluginMetadata().getExecutablePluginType())) {
      engineTask = createEngineTask(datasetId, engineTaskClient, pluginHarvestParameters);
    } else if (DataEvolutionUtils.getProcessPluginGroup().contains(plugin.getPluginMetadata().getExecutablePluginType())
        || DataEvolutionUtils.getIndexPluginGroup().contains(plugin.getPluginMetadata().getExecutablePluginType())) {
      engineTask = createEngineTask(datasetId, previousTaskId, engineTaskClient, pluginParameters);
    } else if (plugin.getPluginMetadata().getExecutablePluginType().equals(ExecutablePluginType.DEPUBLISH)) {
      engineTask = createEngineTask(engineTaskClient, pluginParameters);
    }

    LOGGER.info("Starting execution of {} plugin for externalDatasetId {}", plugin.getPluginType(), datasetId);
    try {
      long taskId = engineTaskClient.submitEngineTask(engineTask, plugin.getTopologyName());
      plugin.setExternalTaskId(String.valueOf(taskId));
      plugin.setDataStatus(DataStatus.VALID);
    } catch (ExternalTaskException | RuntimeException e) {
      throw new ExternalTaskException("Submitting task failed", e);
    }
    LOGGER.info("Submitted task with externalTaskId: {}", plugin.getExternalTaskId());
  }

  private <S extends EngineTaskSettings, T extends EngineTask> @NotNull T createEngineTask(
      String datasetId,
      EngineTaskClient<S, T> engineTaskClient, PluginHarvestParameters pluginHarvestParameters) {
    T engineTask;
    final Map<EngineTaskKey, String> basicTaskParameters = EngineTaskConfigurator.createDefaultTaskParametersHarvest(
        datasetId,
        pluginHarvestParameters.incrementalHarvest(),
        plugin.getStartedDate(),
        engineTaskClient);
    final Map<EngineTaskKey, String> allParameters = new HashMap<>(basicTaskParameters);
    allParameters.putAll(pluginHarvestParameters.pluginParameters());
    engineTask = EngineTaskConfigurator.createHarvestEngineTask(
        pluginHarvestParameters.targetUrl(),
        plugin.getPluginType(),
        plugin.getStartedDate(),
        allParameters,
        engineTaskClient,
        pluginHarvestParameters.oaiHarvestParameters()
    );
    return engineTask;
  }

  private record PluginHarvestParameters(String targetUrl, boolean incrementalHarvest,
                                         Map<EngineTaskKey, String> pluginParameters,
                                         OaiHarvestParameters oaiHarvestParameters) {

  }

  @NotNull
  private <S extends EngineTaskSettings, T extends EngineTask> T createEngineTask(String datasetId,
      String previousTaskId, EngineTaskClient<S, T> engineTaskClient,
      Map<EngineTaskKey, String> pluginParameters) {
    T engineTask;
    final Map<EngineTaskKey, String> basicTaskParameters = EngineTaskConfigurator.createDefaultTaskParameters(
        datasetId,
        previousTaskId,
        plugin.getPluginMetadata().getRevisionNamePreviousPlugin(),
        plugin.getPluginMetadata().getRevisionTimestampPreviousPlugin(),
        engineTaskClient);
    final Map<EngineTaskKey, String> allParameters = new HashMap<>(basicTaskParameters);
    allParameters.putAll(pluginParameters);
    engineTask = EngineTaskConfigurator.createEngineTask(
        datasetId,
        plugin.getPluginType(),
        plugin.getStartedDate(),
        allParameters,
        engineTaskClient
    );
    return engineTask;
  }

  @NotNull
  private <S extends EngineTaskSettings, T extends EngineTask> T createEngineTask(
      EngineTaskClient<S, T> engineTaskClient, Map<EngineTaskKey, String> pluginParameters) {
    T engineTask = EngineTaskConfigurator.createDepublishEngineTask(pluginParameters,
        engineTaskClient);
    return engineTask;
  }

  private @NotNull PluginHarvestParameters getPluginHarvestParameters() {
    boolean incrementalHarvest;
    OaiHarvestParameters oaiHarvestParameters = null;
    String targetUrl;
    final Map<EngineTaskKey, String> pluginParameters = switch (plugin.getPluginMetadata()) {
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

  private <S extends EngineTaskSettings, T extends EngineTask> @NotNull Map<EngineTaskKey, String> getProcessPluginParameters(
      String datasetId, EngineTaskClient<S, T> engineTaskClient) {
    return switch (plugin.getPluginMetadata()) {
      case ValidationExternalPluginMetadata validationExternalPluginMetadata -> {
        String urlOfSchemasZip = validationExternalPluginMetadata.getUrlOfSchemasZip();
        String schemaRootPath = validationExternalPluginMetadata.getSchemaRootPath();
        String schematronRootPath = validationExternalPluginMetadata.getSchematronRootPath();
        yield EngineTaskConfigurator.createValidationExternalParameters(urlOfSchemasZip, schemaRootPath,
            schematronRootPath);
      }
      case TransformationPluginMetadata transformationPluginMetadata -> {
        String metisCoreBaseUrl = engineTaskClient.getEngineTaskSettings().metisCoreBaseUrl();
        String xsltId = transformationPluginMetadata.getXsltId();
        String datasetName = transformationPluginMetadata.getDatasetName();
        String country = transformationPluginMetadata.getCountry();
        String language = transformationPluginMetadata.getLanguage();
        yield EngineTaskConfigurator.createTransformationParameters(metisCoreBaseUrl, xsltId, datasetId, datasetName,
            country, language);
      }
      case ValidationInternalPluginMetadata validationInternalPluginMetadata -> {
        String urlOfSchemasZip = validationInternalPluginMetadata.getUrlOfSchemasZip();
        String schemaRootPath = validationInternalPluginMetadata.getSchemaRootPath();
        String schematronRootPath = validationInternalPluginMetadata.getSchematronRootPath();
        yield EngineTaskConfigurator.createValidationInternalParameters(urlOfSchemasZip, schemaRootPath,
            schematronRootPath);
      }
      case NormalizationPluginMetadata normalizationPluginMetadata -> new HashMap<>();
      case EnrichmentPluginMetadata enrichmentPluginMetadata -> new HashMap<>();
      case MediaProcessPluginMetadata mediaProcessPluginMetadata -> {
        ThrottlingValues throttlingValues = engineTaskClient.getEngineTaskSettings().throttlingValues();
        ThrottlingLevel throttlingLevel = mediaProcessPluginMetadata.getThrottlingLevel() == null ?
            ThrottlingLevel.WEAK : mediaProcessPluginMetadata.getThrottlingLevel();
        String maximumParallelization = String.valueOf(throttlingValues.getThreadNumberFromThrottlingLevel(throttlingLevel));
        yield EngineTaskConfigurator.createMediaParameters(maximumParallelization);
      }
      case LinkCheckingPluginMetadata linkCheckingPluginMetadata -> {
        Boolean performSampling = linkCheckingPluginMetadata.getPerformSampling();
        Integer sampleSize = linkCheckingPluginMetadata.getSampleSize();
        yield EngineTaskConfigurator.createLinkCheckingParameters(performSampling, sampleSize);
      }
      default -> throw new IllegalStateException("Unexpected value: " + plugin);
    };
  }

  private @NotNull Map<EngineTaskKey, String> getIndexPluginParameters(String datasetId) {
    final Map<EngineTaskKey, String> pluginParameters = switch (plugin.getPluginMetadata()) {
      case AbstractIndexPluginMetadata indexPluginMetadata -> {
        boolean incrementalIndexing = indexPluginMetadata.isIncrementalIndexing();
        Date harvestDate = indexPluginMetadata.getHarvestDate();
        boolean preserveTimestamps = indexPluginMetadata.isPreserveTimestamps();
        List<String> datasetIdsToRedirectFrom = indexPluginMetadata.getDatasetIdsToRedirectFrom();
        boolean performRedirects = indexPluginMetadata.isPerformRedirects();
        String targetIndexingDatabase = ((IndexToPreviewPlugin) plugin).getTargetIndexingDatabase().name();
        yield EngineTaskConfigurator.createIndexParameters(datasetId, plugin.getStartedDate(), incrementalIndexing,
            harvestDate, preserveTimestamps, datasetIdsToRedirectFrom, performRedirects, targetIndexingDatabase);
      }
      default -> throw new IllegalStateException("Unexpected value: " + plugin);
    };
    return pluginParameters;
  }

  private @NotNull Map<EngineTaskKey, String> getDepublishPluginParameters(String datasetId) {
    final Map<EngineTaskKey, String> pluginParameters = switch (plugin.getPluginMetadata()) {
      case DepublishPluginMetadata depublishPluginMetadata -> {
        boolean datasetDepublish = depublishPluginMetadata.isDatasetDepublish();
        Set<String> recordIdsToDepublish = depublishPluginMetadata.getRecordIdsToDepublish();
        String depublicationReason = depublishPluginMetadata.getDepublicationReason().name();
        yield EngineTaskConfigurator.createDepublishParameters(datasetId, datasetDepublish, recordIdsToDepublish,
            depublicationReason);
      }
      default -> throw new IllegalStateException("Unexpected value: " + plugin);
    };
    return pluginParameters;
  }
}
