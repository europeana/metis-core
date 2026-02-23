package eu.europeana.metis.core.execution.task;

import static eu.europeana.metis.core.engine.base.EngineTaskParametersConfigurator.createLinkCheckingParameters;
import static eu.europeana.metis.core.engine.base.EngineTaskParametersConfigurator.createMediaParameters;
import static eu.europeana.metis.core.engine.base.EngineTaskParametersConfigurator.createTransformationParameters;
import static eu.europeana.metis.core.engine.base.EngineTaskParametersConfigurator.createValidationExternalParameters;
import static eu.europeana.metis.core.engine.base.EngineTaskParametersConfigurator.createValidationInternalParameters;

import eu.europeana.metis.core.engine.base.EngineTask;
import eu.europeana.metis.core.engine.base.EngineTaskClient;
import eu.europeana.metis.core.engine.base.EngineTaskKey;
import eu.europeana.metis.core.engine.base.EngineTaskSettings;
import eu.europeana.metis.core.engine.base.PluginTypeToBatchJobMapper;
import eu.europeana.metis.core.workflow.plugins.AbstractExecutablePlugin;
import eu.europeana.metis.core.workflow.plugins.EnrichmentPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.LinkCheckingPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.MediaProcessPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.NormalizationPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.ThrottlingLevel;
import eu.europeana.metis.core.workflow.plugins.ThrottlingValues;
import eu.europeana.metis.core.workflow.plugins.TransformationPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.ValidationExternalPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.ValidationInternalPluginMetadata;
import eu.europeana.metis.sandbox.batch.common.FullBatchJobType;
import java.util.EnumMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * Factory class for creating curate engine tasks.
 *
 * @param <S> The type of {@link EngineTaskSettings} associated with the harvest task.
 * @param <T> The type of {@link EngineTask} created by this factory.
 */
public class CurateTaskFactory<S extends EngineTaskSettings, T extends EngineTask> extends
    AbstractInternalEngineTaskFactory<S, T> {

  /**
   * Constructor.
   *
   * @param engineTaskClient the client interface for managing engine tasks, including task creation, monitoring, and operational
   * interactions
   * @param plugin an instance of {@code AbstractExecutablePlugin}, representing the plugin providing configuration and metadata
   * for the associated task
   */
  public CurateTaskFactory(EngineTaskClient<S, T> engineTaskClient, AbstractExecutablePlugin<?> plugin) {
    super(engineTaskClient, plugin);
  }

  @Override
  public T create(String datasetId, String engineDatasetId, String previousTaskId) {
    Map<EngineTaskKey, String> pluginParameters = getProcessPluginParameters();
    FullBatchJobType fullBatchJobType = PluginTypeToBatchJobMapper.map(plugin.getPluginType());
    if (fullBatchJobType != null) {
      pluginParameters.put(EngineTaskKey.JOB_NAME, fullBatchJobType.name());
    }
    return createInternalEngineTask(datasetId, engineDatasetId, previousTaskId, pluginParameters);
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
}
