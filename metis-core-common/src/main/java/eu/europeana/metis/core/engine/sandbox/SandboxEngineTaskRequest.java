package eu.europeana.metis.core.engine.sandbox;

import eu.europeana.metis.core.engine.base.EngineTaskRequest;
import eu.europeana.metis.core.engine.base.EngineTaskKey;
import eu.europeana.metis.core.engine.base.task.input.DepublishInputDataEndpoint;
import eu.europeana.metis.core.engine.base.task.input.HttpHarvestInputDataEndpoint;
import eu.europeana.metis.core.engine.base.task.input.InputDataEndpoint;
import eu.europeana.metis.core.engine.base.task.input.OaiHarvestInputDataEndpoint;
import eu.europeana.metis.core.engine.base.task.input.SimpleIntermediateInputDataEndpoint;
import eu.europeana.metis.core.engine.base.task.input.TransformExternalInputDataEndpoint;
import eu.europeana.metis.core.engine.base.task.input.TransformInternalInputDataEndpoint;
import eu.europeana.metis.sandbox.common.task.input.HttpHarvestInputMetadataRequest;
import eu.europeana.metis.sandbox.common.task.input.InputMetadataRequest;
import eu.europeana.metis.sandbox.common.task.input.OaiHarvestInputMetadataRequest;
import eu.europeana.metis.sandbox.common.task.input.SandboxTask;
import eu.europeana.metis.sandbox.common.task.input.SandboxTaskKey;
import eu.europeana.metis.sandbox.common.task.input.SimpleIntermediateInputMetadataRequest;
import eu.europeana.metis.sandbox.common.task.input.TransformExternalInputMetadataRequest;
import eu.europeana.metis.sandbox.common.task.input.TransformInternalInputMetadataRequest;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;

/**
 * Represents a task for the metis-sandbox processing engine that wraps and transforms task parameters for use in a metis-sandbox
 * task.
 */
@Getter
public class SandboxEngineTaskRequest extends EngineTaskRequest {

  SandboxTask sandboxTask = new SandboxTask();

  /**
   * Constructs an EngineTask with specified parameters and input data endpoint.
   *
   * @param parameters Map of EngineTaskKey and String values used to configure the task. Must not be null.
   * @param inputDataEndpoint InputDataEndpoint representing the input data source for the task. Must not be null.
   */
  protected SandboxEngineTaskRequest(Map<EngineTaskKey, String> parameters, InputDataEndpoint inputDataEndpoint) {
    super(parameters, inputDataEndpoint);
    setParameters();
    prepareTaskRequest();
  }

  private void setParameters() {
    Set<String> validKeys = Arrays.stream(SandboxTaskKey.values())
                                  .map(Enum::name)
                                  .collect(Collectors.toSet());

    Map<SandboxTaskKey, String> sandboxParameters =
        parameters.entrySet().stream()
                  .filter(entry -> validKeys.contains(entry.getKey().name()))
                  .collect(Collectors.toMap(
                      entry ->
                          SandboxTaskKey.valueOf(entry.getKey().name()),
                      Entry::getValue
                  ));
    sandboxTask.setParameters(sandboxParameters);
  }

  private void prepareTaskRequest() {
    InputMetadataRequest inputMetadataRequest = switch (this.inputDataEndpoint) {
      case OaiHarvestInputDataEndpoint(String url, String set, String metadataPrefix, Instant from, Instant until, Integer stepSize) ->
          new OaiHarvestInputMetadataRequest(url, set, metadataPrefix, from, until, stepSize);
      case HttpHarvestInputDataEndpoint(String url, Integer stepSize) -> new HttpHarvestInputMetadataRequest(url, stepSize);
      case TransformExternalInputDataEndpoint(String xslt, String url, String sourceExecutionId, String sourceBatchId) ->
          new TransformExternalInputMetadataRequest(xslt, sourceExecutionId);
      case TransformInternalInputDataEndpoint(String xslt, String url, String sourceExecutionId, String sourceBatchId) ->
          new TransformInternalInputMetadataRequest(xslt, sourceExecutionId);
      case SimpleIntermediateInputDataEndpoint(String url, String sourceExecutionId, String sourceBatchId) ->
          new SimpleIntermediateInputMetadataRequest(sourceExecutionId);
      case DepublishInputDataEndpoint ignored ->
          throw new IllegalArgumentException("DepublishInputDataEndpoint is not supported for sandbox tasks");
    };
    sandboxTask.setInputMetadataRequest(inputMetadataRequest);
  }
}
