package eu.europeana.metis.core.engine.base;

import eu.europeana.metis.core.engine.base.item.report.DataItemStatus;
import eu.europeana.metis.core.engine.base.task.report.EngineTaskErrors;
import eu.europeana.metis.core.engine.base.task.report.EngineTaskProgress;
import eu.europeana.metis.exception.ExternalTaskException;
import eu.europeana.metis.exception.UnrecoverableExternalTaskException;
import java.util.List;

public interface EngineTaskMonitoringClient {

  EngineTaskProgress getEngineTaskProgress(String topologyName, long taskId)
      throws ExternalTaskException, UnrecoverableExternalTaskException;

  List<DataItemStatus> getDataItemStatuses(String topologyName, long taskId, int from, int to) throws ExternalTaskException;

  boolean hasEngineTaskErrorReport(String topologyName, long taskId) throws ExternalTaskException;

  EngineTaskErrors getEngineTaskErrors(String topologyName, long taskId, int maxEntries) throws ExternalTaskException;

}
