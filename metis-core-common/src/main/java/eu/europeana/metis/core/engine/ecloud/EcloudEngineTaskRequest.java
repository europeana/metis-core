package eu.europeana.metis.core.engine.ecloud;

import eu.europeana.cloud.service.dps.BatchInfo;
import eu.europeana.cloud.service.dps.CreateDpsTaskRequest;
import eu.europeana.cloud.service.dps.DepublicationInfo;
import eu.europeana.cloud.service.dps.HttpHarvestingDetails;
import eu.europeana.cloud.service.dps.OAIPMHHarvestingDetails;
import eu.europeana.cloud.service.dps.TaskSource;
import eu.europeana.metis.core.engine.base.EngineTaskKey;
import eu.europeana.metis.core.engine.base.EngineTaskRequest;
import eu.europeana.metis.core.engine.base.task.input.DepublishInputDataEndpoint;
import eu.europeana.metis.core.engine.base.task.input.HttpHarvestInputDataEndpoint;
import eu.europeana.metis.core.engine.base.task.input.InputDataEndpoint;
import eu.europeana.metis.core.engine.base.task.input.IntermediateInputDataEndpoint;
import eu.europeana.metis.core.engine.base.task.input.OaiHarvestInputDataEndpoint;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.Getter;

/**
 * Represents a task for the Ecloud processing engine that wraps and transforms task parameters for use in a DPS task.
 */
@Getter
public class EcloudEngineTaskRequest extends EngineTaskRequest {

  private final CreateDpsTaskRequest createDpsTaskRequest = new CreateDpsTaskRequest();

  /**
   * Constructs an EcloudEngineTask with specified task parameters and input data endpoint. Initializes
   * internal configurations for the task.
   *
   * @param parameters Map of EngineTaskKey and String values used to configure the task. Must not be null.
   * @param inputDataEndpoint InputDataEndpoint representing the input data source for the task. Must not be null.
   */
  public EcloudEngineTaskRequest(Map<EngineTaskKey, String> parameters, InputDataEndpoint inputDataEndpoint) {
    super(parameters, inputDataEndpoint);
    setParameters();
    prepareTaskRequest();
  }

  private void setParameters() {
    parameters.forEach((key, value) -> createDpsTaskRequest.addParameter(key.name(), value));
  }

  private void prepareTaskRequest() {
    String providerId = parameters.get(EngineTaskKey.PROVIDER_ID);
    TaskSource taskSource = switch (this.inputDataEndpoint) {
      case OaiHarvestInputDataEndpoint(String url, String set, String metadataPrefix, Instant from, Instant until, Integer stepSize) ->
          new OAIPMHHarvestingDetails(url, metadataPrefix, set,
              Optional.ofNullable(from).map(Date::from).orElse(null),
              Optional.ofNullable(until).map(Date::from).orElse(null), null);
      case HttpHarvestInputDataEndpoint(String url, Integer stepSize) -> new HttpHarvestingDetails(url);
      case IntermediateInputDataEndpoint endpoint -> new BatchInfo(providerId, endpoint.sourceBatchId());
      case DepublishInputDataEndpoint(boolean datasetDepublish, Set<String> idsToDepublish) ->
          new DepublicationInfo(datasetDepublish, idsToDepublish);
    };
    createDpsTaskRequest.setSource(taskSource);
    createDpsTaskRequest.setResultsProvider(providerId);
  }
}
