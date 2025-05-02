package eu.europeana.metis.core.engine.base;

import java.util.Map;

/**
 * Represents a task that can be submitted to an external system.
 */
public interface EngineTask {

  void setParameters(Map<EngineTaskKey, String> parameters);
  void setOutputRevision(DataRevision dataRevision);
  void setInputDataLocation(InputDataType inputDataType,String inputDataLocation);
  void setOaiHarvestParameters(OaiHarvestParameters oaiHarvestParameters);

  enum InputDataType {
    INTERNAL_DATASET,
    EXTERNAL_REPOSITORY;
  }
}
