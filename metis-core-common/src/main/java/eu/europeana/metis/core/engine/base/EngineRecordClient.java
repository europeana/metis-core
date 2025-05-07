package eu.europeana.metis.core.engine.base;

import eu.europeana.metis.core.rest.Record;
import eu.europeana.metis.exception.ExternalTaskException;
import java.util.Date;
import java.util.List;

public interface EngineRecordClient {

  List<Record> getRecords(String datasetId, String representationName, String revisionName, Date revisionTimestamp,
      int numberOfRecords) throws ExternalTaskException;

  List<Record> getRecords(List<String> recordIds, String revisionName, Date revisionTimestamp) throws ExternalTaskException;

  Record getRecord(String recordId, String revisionName, Date revisionTimestamp) throws ExternalTaskException;

  List<String> getPublishedRecords(String datasetId, List<String> recordsIds) throws ExternalTaskException;

  long getTotalIndexedRecords(String datasetId, IndexDatabase indexDatabase) throws ExternalTaskException;

  boolean createEngineDatasetId(String datasetId) throws ExternalTaskException;

}
