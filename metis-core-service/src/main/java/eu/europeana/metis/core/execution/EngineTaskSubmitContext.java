package eu.europeana.metis.core.execution;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EngineTaskSubmitContext {
  String datasetId;
  String engineDatasetId;
  String sourceExecutionId;
  String sourceBatchId;
}
