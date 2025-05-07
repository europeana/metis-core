package eu.europeana.metis.core.engine.base;

import eu.europeana.metis.core.engine.base.item.report.DataItemStatus;
import eu.europeana.metis.core.engine.base.task.report.EngineTaskErrors;
import eu.europeana.metis.core.engine.base.task.report.EngineTaskProgress;
import eu.europeana.metis.core.rest.Record;
import eu.europeana.metis.core.rest.stats.NodePathStatistics;
import eu.europeana.metis.core.rest.stats.RecordStatistics;
import eu.europeana.metis.exception.ExternalTaskException;
import eu.europeana.metis.exception.UnrecoverableExternalTaskException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Client for submitting, monitoring and cancelling external tasks.
 */
public interface EngineTaskClient<S extends EngineTaskSettings, T extends EngineTask> {

  S getEngineTaskSettings();

  Supplier<T> getEngineTaskCreator();

  long submitEngineTask(T engineTask, String topologyName) throws ExternalTaskException;

  EngineTaskProgress getEngineTaskProgress(String topologyName, long taskId)
      throws ExternalTaskException, UnrecoverableExternalTaskException;

  long getTotalIndexedRecords(String datasetId, IndexDatabase indexDatabase) throws ExternalTaskException;

  List<String> getPublishedRecords(String datasetId, List<String> recordsIds) throws ExternalTaskException;

  Map<String, Boolean> getRecordStatus(String topologyName, long taskId, int from, int to) throws ExternalTaskException;

  List<DataItemStatus> getDataItemStatuses(String topologyName, long taskId, int from, int to) throws ExternalTaskException;

  boolean hasErrorReport(String topologyName, long taskId) throws ExternalTaskException;

  EngineTaskErrors getEngineTaskErrors(String topologyName, long taskId, String error, int idsCount) throws ExternalTaskException;

  RecordStatistics getEngineTaskContentStatisticsReport(String topologyName, long taskId) throws ExternalTaskException;

  NodePathStatistics getContentNodeReport(String topologyName, long taskId, String nodePath)
      throws ExternalTaskException;

  void cancelEngineTask(String topologyName, long taskId, String message) throws ExternalTaskException;

  void close();

  boolean createEngineDatasetId(String datasetId) throws ExternalTaskException;

  List<Record> getRecords(String datasetId, String representationName, String revisionName, Date revisionTimestamp,
      int numberOfRecords) throws ExternalTaskException;

  List<Record> getRecords(List<String> recordIds, String revisionName, Date revisionTimestamp) throws ExternalTaskException;

  Record getRecord(String recordId, String revisionName, Date revisionTimestamp) throws ExternalTaskException;

}
