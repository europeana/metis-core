package eu.europeana.metis.core.engine.sandbox;

import eu.europeana.metis.core.dataset.Dataset;
import eu.europeana.metis.core.engine.base.DataRevision;
import eu.europeana.metis.core.engine.base.EngineTaskClient;
import eu.europeana.metis.core.engine.base.EngineTaskKey;
import eu.europeana.metis.core.engine.base.IndexDatabase;
import eu.europeana.metis.core.engine.base.item.report.DataItemStatus;
import eu.europeana.metis.core.engine.base.task.input.InputDataEndpoint;
import eu.europeana.metis.core.engine.base.task.report.EngineTaskErrors;
import eu.europeana.metis.core.engine.base.task.report.EngineTaskProgress;
import eu.europeana.metis.core.engine.base.task.report.EngineTaskState;
import eu.europeana.metis.core.rest.Record;
import eu.europeana.metis.core.rest.stats.NodePathStatisticsDTO;
import eu.europeana.metis.core.rest.stats.RecordStatisticsDTO;
import eu.europeana.metis.exception.ExternalTaskException;
import eu.europeana.metis.sandbox.common.DatasetMetadataRequest;
import eu.europeana.metis.sandbox.common.batch.FullBatchJobType;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.common.task.input.SandboxTaskProgress;
import eu.europeana.metis.sandbox.common.task.input.SandboxTaskProgress.SandboxTaskState;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClient;

@Slf4j
public class SandboxEngineTaskClient implements EngineTaskClient<SandboxEngineTaskSettings, SandboxEngineTask> {

  private final SandboxEngineTaskSettings engineTaskSettings;
  private final RestClient restClient;

  public SandboxEngineTaskClient(SandboxEngineTaskSettings engineTaskSettings, RestClient restClient) {
    this.engineTaskSettings = engineTaskSettings;
    this.restClient = restClient;
  }

  @Override
  public SandboxEngineTaskSettings getEngineTaskSettings() {
    return engineTaskSettings;
  }

  @Override
  public SandboxEngineTask createEngineTask(
      Map<EngineTaskKey, String> parameters,
      InputDataEndpoint inputDataEndpoint,
      DataRevision outputDataRevision) {
    return new SandboxEngineTask(parameters, inputDataEndpoint, outputDataRevision);
  }

  @Override
  public String createEngineDatasetId(Dataset dataset) throws ExternalTaskException {
    Country country = Country.valueOf(dataset.getCountry().name());
    Language language = Language.valueOf(dataset.getLanguage().name());
    DatasetMetadataRequest datasetMetadataRequest = new DatasetMetadataRequest(dataset.getDatasetName(), country, language);

    return restClient.post()
                     .uri("/task/dataset")
                     .body(datasetMetadataRequest)
                     .retrieve()
                     .body(String.class);
  }

  @Override
  public String submitEngineTask(SandboxEngineTask engineTask, String topologyName) throws ExternalTaskException {
    try {
      return restClient.post()
                       .uri("/task/submit")
                       .body(engineTask.getSandboxTask())
                       .retrieve()
                       .body(String.class);
    } catch (RuntimeException e) {
      throw new ExternalTaskException("Submitting task to DPS failed", e);
    }
  }

  @Override
  public EngineTaskProgress getEngineTaskProgress(String topologyName, String taskId, FullBatchJobType step)
      throws ExternalTaskException {
    try {
      SandboxTaskProgress sandboxTaskProgress =
          restClient.get()
                    .uri(uriBuilder -> uriBuilder
                        .path("/task/progress")
                        .queryParam("executionId", taskId)
                        .queryParam("step", step)
                        .build())
                    .retrieve()
                    .body(SandboxTaskProgress.class);
      return convertToProcessingEngineTaskProgress(Objects.requireNonNull(sandboxTaskProgress));
    } catch (RuntimeException e) {
      throw new ExternalTaskException("Fetching task progress failed", e);
    }
  }

  @Override
  public void cancelEngineTask(String topologyName, String taskId, String message, FullBatchJobType step)
      throws ExternalTaskException {
    try {
      restClient.post()
                .uri(uriBuilder -> uriBuilder
                    .path("/task/cancel")
                    .queryParam("executionId", taskId)
                    .queryParam("step", step)
                    .build())
                .retrieve()
                .toBodilessEntity();
    } catch (RuntimeException e) {
      throw new ExternalTaskException("Failed to cancel engine task", e);
    }
  }

  @Override
  public long getTotalIndexedRecords(String datasetId, IndexDatabase indexDatabase) throws ExternalTaskException {
    try {
      Long response = restClient.get()
                                .uri(uriBuilder -> uriBuilder
                                    .path("/task/indexedRecordsCount")
                                    .queryParam("metisDatasetId", datasetId)
                                    .build())
                                .retrieve()
                                .body(Long.class);
      return response != null ? response : 0L;
    } catch (RuntimeException e) {
      throw new ExternalTaskException("Failed to fetch indexed records count", e);
    }
  }

  @Override
  public List<Record> getRecords(String engineDatasetId, String representationName, String revisionName, Date revisionTimestamp,
      int numberOfRecords) throws ExternalTaskException {
    return List.of();
  }

  @Override
  public List<Record> getRecords(List<String> recordIds, String revisionName, Date revisionTimestamp)
      throws ExternalTaskException {
    return List.of();
  }

  @Override
  public Record getRecord(String recordId, String revisionName, Date revisionTimestamp) throws ExternalTaskException {
    return null;
  }

  @Override
  public List<String> getPublishedRecords(String datasetId, List<String> recordsIds) throws ExternalTaskException {
    return List.of();
  }

  @Override
  public List<DataItemStatus> getDataItemStatuses(String topologyName, String taskId, int from, int to)
      throws ExternalTaskException {
    return List.of();
  }

  @Override
  public boolean hasEngineTaskErrorReport(String topologyName, String taskId) throws ExternalTaskException {
    return false;
  }

  @Override
  public EngineTaskErrors getEngineTaskErrors(String topologyName, String taskId, int maxEntries) throws ExternalTaskException {
    return null;
  }

  @Override
  public RecordStatisticsDTO getEngineTaskContentRecordStatistics(String topologyName, String taskId)
      throws ExternalTaskException {
    return null;
  }

  @Override
  public NodePathStatisticsDTO getEngineTaskContentNodePathStatistics(String topologyName, String taskId, String nodePath)
      throws ExternalTaskException {
    return null;
  }

  @Override
  public void close() throws Exception {

  }

  private static EngineTaskProgress convertToProcessingEngineTaskProgress(SandboxTaskProgress sandboxTaskProgress) {
    EngineTaskProgress engineTaskProgress = new EngineTaskProgress();
    engineTaskProgress.setExpectedRecords(Math.toIntExact(sandboxTaskProgress.expectedRecords()));
    engineTaskProgress.setProcessedRecords(Math.toIntExact(sandboxTaskProgress.processedRecords()));
    engineTaskProgress.setDeletedRecords(Math.toIntExact(sandboxTaskProgress.deletedRecords()));
    engineTaskProgress.setIgnoredRecords(0);
    engineTaskProgress.setProcessedErrors(Math.toIntExact(sandboxTaskProgress.failedRecords()));
    engineTaskProgress.setPostProcessedRecordsCount(0);
    engineTaskProgress.setDeletedErrors(0);
    EngineTaskState engineTaskState = convertToEngineTaskState(sandboxTaskProgress.sandboxTaskState());
    engineTaskProgress.setEngineTaskState(engineTaskState);
    engineTaskProgress.setEngineTaskStateInfo("");
    return engineTaskProgress;
  }

  private static EngineTaskState convertToEngineTaskState(SandboxTaskState sandboxTaskState) {

    return switch (sandboxTaskState) {
      case RUNNING -> EngineTaskState.CURRENTLY_PROCESSING;
      case FINISHED -> EngineTaskState.PROCESSED;
      case CANCELLED, FAILED -> EngineTaskState.DROPPED;
    };
  }
}
