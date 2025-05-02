package eu.europeana.metis.core.engine.base;

import eu.europeana.metis.core.engine.base.report.item.content.ContentNodeReport;
import eu.europeana.metis.core.engine.base.report.item.DataItemStatus;
import eu.europeana.metis.core.engine.base.report.item.content.ContentStatisticsReport;
import eu.europeana.metis.core.engine.base.report.task.EngineTaskErrors;
import eu.europeana.metis.core.engine.base.report.task.EngineTaskProgress;
import eu.europeana.metis.exception.ExternalTaskException;
import eu.europeana.metis.exception.UnrecoverableExternalTaskException;
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

  ContentStatisticsReport getEngineTaskContentStatisticsReport(String topologyName, long taskId) throws ExternalTaskException;

  List<ContentNodeReport> getContentNodeReport(String topologyName, long taskId, String nodePath)
      throws ExternalTaskException;

  void cancelEngineTask(String topologyName, long taskId, String message) throws ExternalTaskException;

  void close();


}
