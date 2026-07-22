package eu.europeana.metis.core.execution;

import lombok.Builder;
import lombok.Getter;

/**
 * Represents the context required for creating a task to an engine.
 */
@Getter
@Builder
public class EngineTaskCreationContext {

  String datasetId;
  String engineDatasetId;
  String sourceExecutionId;
  String sourceBatchId;
}
