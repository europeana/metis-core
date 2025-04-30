package eu.europeana.metis.core.engine.base;

import eu.europeana.metis.core.engine.base.report.item.content.ContentNodeReport;
import eu.europeana.metis.core.engine.base.report.item.DataItemStatus;
import eu.europeana.metis.core.engine.base.report.item.content.ContentStatisticsReport;
import eu.europeana.metis.core.engine.base.report.task.ProcessingEngineTaskErrors;
import eu.europeana.metis.core.engine.base.report.task.ProcessingEngineTaskProgress;
import eu.europeana.metis.exception.ExternalTaskException;
import eu.europeana.metis.exception.UnrecoverableExternalTaskException;
import java.util.List;
import java.util.Map;

/**
 * Client for submitting, monitoring and cancelling external tasks.
 */
public interface ProcessingEngineTaskClient<T extends ProcessingEngineTask> {

  long submitTask(T externalTask, String topologyName) throws ExternalTaskException;

  ProcessingEngineTaskProgress getTaskProgress(String topologyName, long taskId)
      throws ExternalTaskException, UnrecoverableExternalTaskException;

  long getTotalIndexedRecords(String datasetId, IndexDatabase indexDatabase) throws ExternalTaskException;

  List<String> getPublishedRecords(String datasetId, List<String> recordsIds) throws ExternalTaskException;

  Map<String, Boolean> getRecordStatus(String topologyName, long taskId, int from, int to) throws ExternalTaskException;

  List<DataItemStatus> getExternalRecordStatuses(String topologyName, long taskId, int from, int to) throws ExternalTaskException;

  boolean hasErrorReport(final String topologyName, final long taskId) throws ExternalTaskException;

  ProcessingEngineTaskErrors getTaskErrorReport(final String topologyName, final long taskId, final String error, final int idsCount) throws ExternalTaskException;

  ContentStatisticsReport getTaskStatisticsReport(final String topologyName, final long taskId) throws ExternalTaskException;

  List<ContentNodeReport> getElementReport(final String topologyName, final long taskId, String elementPath) throws ExternalTaskException;

  void cancel(String topologyName, long taskId, String message) throws ExternalTaskException;

  void close();


}
