package eu.europeana.metis.core.execution.task;

import static eu.europeana.metis.core.engine.base.EngineTaskParametersConfigurator.createLinkCheckingParameters;
import static eu.europeana.metis.core.engine.base.EngineTaskParametersConfigurator.createMediaParameters;
import static eu.europeana.metis.core.engine.base.EngineTaskParametersConfigurator.createTransformationExternalParameters;
import static eu.europeana.metis.core.engine.base.EngineTaskParametersConfigurator.createTransformationInternalParameters;
import static eu.europeana.metis.core.engine.base.EngineTaskParametersConfigurator.createValidationExternalParameters;
import static eu.europeana.metis.core.engine.base.EngineTaskParametersConfigurator.createValidationInternalParameters;

import eu.europeana.metis.core.dao.DatasetXsltDao;
import eu.europeana.metis.core.dataset.DatasetXslt;
import eu.europeana.metis.core.engine.base.EngineTask;
import eu.europeana.metis.core.engine.base.EngineTaskClient;
import eu.europeana.metis.core.engine.base.EngineTaskKey;
import eu.europeana.metis.core.engine.base.EngineTaskSettings;
import eu.europeana.metis.core.engine.base.task.input.IntermediateInputDataEndpoint;
import eu.europeana.metis.core.engine.base.task.input.SimpleIntermediateInputDataEndpoint;
import eu.europeana.metis.core.engine.base.task.input.TransformExternalInputDataEndpoint;
import eu.europeana.metis.core.engine.base.task.input.TransformInternalInputDataEndpoint;
import eu.europeana.metis.core.execution.EngineTaskCreationContext;
import eu.europeana.metis.core.workflow.plugins.AbstractExecutablePlugin;
import eu.europeana.metis.core.workflow.plugins.EnrichmentPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.LinkCheckingPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.MediaProcessPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.NormalizationPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.ThrottlingLevel;
import eu.europeana.metis.core.workflow.plugins.ThrottlingValues;
import eu.europeana.metis.core.workflow.plugins.TransformationExternalPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.TransformationPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.ValidationExternalPluginMetadata;
import eu.europeana.metis.core.workflow.plugins.ValidationInternalPluginMetadata;
import eu.europeana.metis.exception.ExternalTaskException;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

/**
 * Factory class for creating curate engine tasks.
 *
 * @param <S> The type of {@link EngineTaskSettings} associated with the harvest task.
 * @param <T> The type of {@link EngineTask} created by this factory.
 */
public class CurateTaskFactory<S extends EngineTaskSettings, T extends EngineTask> extends
    AbstractIntermediateEngineTaskFactory<S, T> {

  private static final ThrottlingLevel DEFAULT_MEDIA_THROTTLING_LEVEL = ThrottlingLevel.WEAK;
  private final DatasetXsltDao datasetXsltDao;

  /**
   * Constructor.
   *
   * @param engineTaskClient the client interface for managing engine tasks, including task creation, monitoring, and operational
   * interactions
   * @param plugin an instance of {@code AbstractExecutablePlugin}, representing the plugin providing configuration and metadata
   * for the associated task
   * @param datasetXsltDao the DAO for retrieving dataset XSLT information
   */
  public CurateTaskFactory(EngineTaskClient<S, T> engineTaskClient, AbstractExecutablePlugin<?> plugin,
      DatasetXsltDao datasetXsltDao) {
    super(engineTaskClient, plugin);
    this.datasetXsltDao = datasetXsltDao;
  }

  @Override
  public T create(EngineTaskCreationContext engineTaskCreationContext) throws ExternalTaskException {
    CurateTaskContext curateTaskContext = getIntermediatePluginParameters(
        engineTaskCreationContext.getSourceExecutionId(),
        engineTaskCreationContext.getSourceBatchId());
    addJobNameParameter(curateTaskContext.pluginParameters());
    Map<EngineTaskKey, String> allParameters = createAllParameters(
        engineTaskCreationContext.getEngineDatasetId(),
        engineTaskCreationContext.getDatasetId(),
        engineTaskCreationContext.getSourceExecutionId(),
        curateTaskContext.pluginParameters()
    );

    return createIntermediateEngineTask(allParameters, curateTaskContext.inputDataEndpoint());
  }

  private @NotNull CurateTaskContext getIntermediatePluginParameters(String sourceExecutionId, String sourceBatchId) {
    SimpleIntermediateInputDataEndpoint simpleInput =
        createSimpleIntermediateInputDataEndpoint(sourceExecutionId, sourceBatchId);
    return switch (plugin.getPluginMetadata()) {
      case TransformationExternalPluginMetadata transformationExternalPluginMetadata -> {
        String xsltId = transformationExternalPluginMetadata.getXsltId();
        String xslt = Optional.ofNullable(datasetXsltDao.getById(xsltId)).map(DatasetXslt::getXslt).orElse(null);
        yield new CurateTaskContext(
            createTransformationExternalParameters(
                engineTaskClient.getEngineTaskSettings().getMetisCoreBaseUrl(),
                transformationExternalPluginMetadata.getXsltId()),
            new TransformExternalInputDataEndpoint(xslt, sourceExecutionId, sourceBatchId)
        );
      }
      case ValidationExternalPluginMetadata validationExternalPluginMetadata -> new CurateTaskContext(
          createValidationExternalParameters(
              validationExternalPluginMetadata.getUrlOfSchemasZip(),
              validationExternalPluginMetadata.getSchemaRootPath(),
              validationExternalPluginMetadata.getSchematronRootPath()
          ),
          simpleInput
      );
      case TransformationPluginMetadata transformationPluginMetadata -> {
        String xsltId = transformationPluginMetadata.getXsltId();
        String xslt = Optional.ofNullable(datasetXsltDao.getById(xsltId)).map(DatasetXslt::getXslt).orElse(null);
        yield new CurateTaskContext(
            createTransformationInternalParameters(
                engineTaskClient.getEngineTaskSettings().getMetisCoreBaseUrl(),
                transformationPluginMetadata.getXsltId(),
                transformationPluginMetadata.getDatasetName(),
                transformationPluginMetadata.getCountry(),
                transformationPluginMetadata.getLanguage()
            ),
            new TransformInternalInputDataEndpoint(xslt, sourceExecutionId, sourceBatchId)
        );
      }
      case ValidationInternalPluginMetadata validationInternalPluginMetadata -> new CurateTaskContext(
          createValidationInternalParameters(
              validationInternalPluginMetadata.getUrlOfSchemasZip(),
              validationInternalPluginMetadata.getSchemaRootPath(),
              validationInternalPluginMetadata.getSchematronRootPath()
          ),
          simpleInput
      );
      case NormalizationPluginMetadata ignored -> new CurateTaskContext(
          new EnumMap<>(EngineTaskKey.class),
          simpleInput
      );
      case EnrichmentPluginMetadata ignored -> new CurateTaskContext(
          new EnumMap<>(EngineTaskKey.class),
          simpleInput
      );
      case MediaProcessPluginMetadata mediaProcessPluginMetadata -> {
        ThrottlingValues throttlingValues = engineTaskClient.getEngineTaskSettings().getThrottlingValues();
        ThrottlingLevel throttlingLevel = mediaProcessPluginMetadata.getThrottlingLevel() == null ? DEFAULT_MEDIA_THROTTLING_LEVEL
            : mediaProcessPluginMetadata.getThrottlingLevel();

        yield new CurateTaskContext(
            createMediaParameters(String.valueOf(throttlingValues.getThreadNumberFromThrottlingLevel(throttlingLevel))),
            simpleInput
        );
      }
      case LinkCheckingPluginMetadata linkCheckingPluginMetadata -> new CurateTaskContext(
          createLinkCheckingParameters(
              linkCheckingPluginMetadata.isPerformSampling(),
              linkCheckingPluginMetadata.getSampleSize()),
          simpleInput
      );
      default -> throw new IllegalStateException("Unexpected value: " + plugin);
    };
  }

  private record CurateTaskContext(
      Map<EngineTaskKey, String> pluginParameters,
      IntermediateInputDataEndpoint inputDataEndpoint
  ) {

  }
}
