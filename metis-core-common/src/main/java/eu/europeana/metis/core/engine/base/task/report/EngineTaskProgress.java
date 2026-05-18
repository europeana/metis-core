package eu.europeana.metis.core.engine.base.task.report;

import lombok.Getter;
import lombok.Setter;

/**
 * Contains execution progress information of a task.
 */
@Setter
@Getter
public class EngineTaskProgress {

  private long expectedRecords;
  private long processedRecords;
  private long successRecords;
  private long failRecords;
  private long warningRecords;
  private long duplicateRecords;
  private long unchangedRecords;
  private long expectedDepublishRecords;
  private long successDepublishRecords;
  private long failDepublishRecords;
  private long processedDepublishRecords;

  private EngineTaskState engineTaskState;
  private String engineTaskStateInfo;
}
