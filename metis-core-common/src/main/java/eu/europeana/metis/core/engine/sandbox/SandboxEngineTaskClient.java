package eu.europeana.metis.core.engine.sandbox;

import static java.util.Objects.requireNonNull;

import eu.europeana.metis.core.dataset.Dataset;
import eu.europeana.metis.core.engine.base.EngineTaskClient;
import eu.europeana.metis.core.engine.base.EngineTaskKey;
import eu.europeana.metis.core.engine.base.IndexDatabase;
import eu.europeana.metis.core.engine.base.PluginTypeToBatchJobMapper;
import eu.europeana.metis.core.engine.base.item.report.DataItemStatus;
import eu.europeana.metis.core.engine.base.task.input.InputDataEndpoint;
import eu.europeana.metis.core.engine.base.task.report.EngineTaskErrors;
import eu.europeana.metis.core.engine.base.task.report.EngineTaskProgress;
import eu.europeana.metis.core.engine.base.task.report.EngineTaskState;
import eu.europeana.metis.core.rest.Record;
import eu.europeana.metis.core.rest.stats.NodePathStatisticsDTO;
import eu.europeana.metis.core.rest.stats.RecordStatisticsDTO;
import eu.europeana.metis.core.workflow.plugins.ExecutablePluginType;
import eu.europeana.metis.exception.ExternalTaskException;
import eu.europeana.metis.sandbox.common.DatasetMetadataRequest;
import eu.europeana.metis.sandbox.common.batch.FullBatchJobType;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.common.task.input.SandboxTaskProgress;
import eu.europeana.metis.sandbox.common.task.input.SandboxTaskProgress.SandboxTaskState;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClient;

/**
 * Client for managing and interacting with tasks in the Metis Sandbox processing engine. Handles task creation, submission,
 * monitoring, error reporting, and record operations.
 */
@Slf4j
@RequiredArgsConstructor
public class SandboxEngineTaskClient implements EngineTaskClient<SandboxEngineTaskSettings, SandboxEngineTask> {

  private static final String FULL_BATCH_JOB_TYPE_PARAM = "fullBatchJobType";
  private static final String EXECUTION_ID_PARAM = "executionId";
  private static final String RECORD_ID_PARAM = "recordId";
  private static final String METIS_DATASET_ID_PARAM = "metisDatasetId";
  private final SandboxEngineTaskSettings engineTaskSettings;
  /**
   * REST client used for communication with external services.
   */
  private final RestClient restClient;

  @Override
  public SandboxEngineTaskSettings getEngineTaskSettings() {
    return engineTaskSettings;
  }

  @Override
  public SandboxEngineTask createEngineTask(
      Map<EngineTaskKey, String> parameters,
      InputDataEndpoint inputDataEndpoint,
      String topologyName) {
    SandboxEngineTaskRequest sandboxEngineTaskRequest = new SandboxEngineTaskRequest(parameters, inputDataEndpoint);
    return new SandboxEngineTask(sandboxEngineTaskRequest.getSandboxTask());
  }

  @Override
  public String createEngineDatasetId(Dataset dataset) {
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
                       .body(engineTask)
                       .retrieve()
                       .body(String.class);
    } catch (RuntimeException e) {
      throw new ExternalTaskException("Submitting task to DPS failed", e);
    }
  }

  @Override
  public EngineTaskProgress getEngineTaskProgress(String topologyName, String taskId, ExecutablePluginType executablePluginType)
      throws ExternalTaskException {
    FullBatchJobType fullBatchJobType = PluginTypeToBatchJobMapper.map(executablePluginType).orElseThrow();
    try {
      SandboxTaskProgress sandboxTaskProgress =
          restClient.get()
                    .uri(uriBuilder -> uriBuilder
                        .path("/task/progress")
                        .queryParam(EXECUTION_ID_PARAM, taskId)
                        .queryParam(FULL_BATCH_JOB_TYPE_PARAM, fullBatchJobType)
                        .build())
                    .retrieve()
                    .body(SandboxTaskProgress.class);
      return convertToProcessingEngineTaskProgress(requireNonNull(sandboxTaskProgress));
    } catch (RuntimeException e) {
      throw new ExternalTaskException("Fetching task progress failed", e);
    }
  }

  @Override
  public void cancelEngineTask(String topologyName, String taskId, String message, ExecutablePluginType executablePluginType)
      throws ExternalTaskException {
    FullBatchJobType fullBatchJobType = PluginTypeToBatchJobMapper.map(executablePluginType).orElseThrow();
    try {
      restClient.post()
                .uri(uriBuilder -> uriBuilder
                    .path("/task/cancel")
                    .queryParam(EXECUTION_ID_PARAM, taskId)
                    .queryParam(FULL_BATCH_JOB_TYPE_PARAM, fullBatchJobType)
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
                                    .queryParam(METIS_DATASET_ID_PARAM, datasetId)
                                    .build())
                                .retrieve()
                                .body(Long.class);
      return response != null ? response : 0L;
    } catch (RuntimeException e) {
      throw new ExternalTaskException("Failed to fetch indexed records count", e);
    }
  }

  @Override
  public List<Record> getRecords(String engineDatasetId, String engineBatchId, int numberOfRecords) throws ExternalTaskException {
    return List.of();
  }

  @Override
  public List<Record> getRecords(List<String> recordIds, String engineBatchId) throws ExternalTaskException {
    return List.of();
  }

  @Override
  public Record getRecord(String engineDatasetId, String recordId, String engineBatchId, ExecutablePluginType executablePluginType)
      throws ExternalTaskException {
    FullBatchJobType fullBatchJobType = PluginTypeToBatchJobMapper.map(executablePluginType).orElseThrow();
    try {
      String recordXml =
          restClient.get()
                    .uri(uriBuilder -> uriBuilder
                        .path("/dataset/{datasetId}/record")
                        .queryParam(RECORD_ID_PARAM, recordId)
                        .queryParam(FULL_BATCH_JOB_TYPE_PARAM, fullBatchJobType)
                        .build(engineDatasetId))
                    .retrieve()
                    .body(String.class);
      return recordXml == null ? null : new Record(recordId, recordXml);
    } catch (RuntimeException e) {
      throw new ExternalTaskException("Failed to fetch record from Sandbox", e);
    }
  }

  @Override
  public List<String> getPublishedRecords(String datasetId, List<String> recordsIds) {
    return List.of();
  }

  @Override
  public List<DataItemStatus> getDataItemStatuses(String topologyName, String taskId, int from, int to)
      throws ExternalTaskException {
    return List.of();
  }

  @Override
  public boolean hasEngineTaskErrorReport(String topologyName, String taskId) {
    return false;
  }

  @Override
  public EngineTaskErrors getEngineTaskErrors(String topologyName, String taskId, int maxEntries) {
    return null;
  }

  @Override
  public RecordStatisticsDTO getEngineTaskContentRecordStatistics(String topologyName, String taskId) {
    return null;
  }

  @Override
  public NodePathStatisticsDTO getEngineTaskContentNodePathStatistics(String topologyName, String taskId, String nodePath) {
    return null;
  }

  @Override
  public void close() {
    //Not required
  }

  private static EngineTaskProgress convertToProcessingEngineTaskProgress(SandboxTaskProgress sandboxTaskProgress) {
    EngineTaskProgress engineTaskProgress = new EngineTaskProgress();
    engineTaskProgress.setExpectedRecords(sandboxTaskProgress.expectedRecords());
    engineTaskProgress.setProcessedRecords(sandboxTaskProgress.processedRecords());
    engineTaskProgress.setSuccessRecords(sandboxTaskProgress.successRecords());
    engineTaskProgress.setFailRecords(sandboxTaskProgress.failRecords());
    engineTaskProgress.setWarningRecords(sandboxTaskProgress.warningRecords());
    engineTaskProgress.setDuplicateRecords(sandboxTaskProgress.duplicateRecords());
    engineTaskProgress.setUnchangedRecords(sandboxTaskProgress.unchangedRecords());

    engineTaskProgress.setExpectedDepublishRecords(sandboxTaskProgress.expectedDepublishRecords());
    engineTaskProgress.setProcessedDepublishRecords(sandboxTaskProgress.processedDepublishRecords());
    engineTaskProgress.setSuccessDepublishRecords(sandboxTaskProgress.successDepublishRecords());
    engineTaskProgress.setFailDepublishRecords(sandboxTaskProgress.failDepublishRecords());

    EngineTaskState engineTaskState = convertToEngineTaskState(sandboxTaskProgress.sandboxTaskState());
    engineTaskProgress.setEngineTaskState(engineTaskState);
    engineTaskProgress.setEngineTaskStateInfo("");
    return engineTaskProgress;
  }

  private static EngineTaskState convertToEngineTaskState(SandboxTaskState sandboxTaskState) {

    return switch (sandboxTaskState) {
      case RUNNING -> EngineTaskState.QUEUED;
      case FINISHED -> EngineTaskState.PROCESSED;
      case CANCELLED, FAILED -> EngineTaskState.DROPPED;
    };
  }
}
