package eu.europeana.metis.core.common;

import lombok.Getter;

/**
 * Enumeration that contains field names for dao queries.
 */
@Getter
public enum DaoFieldNames {
  ID("_id"),
  USER_ID("userId"),
  DATASET_ID("datasetId"),
  DATASET_NAME("datasetName"),
  PROVIDER("provider"),
  DATA_PROVIDER("dataProvider"),
  WORKFLOW_NAME("workflowName"),
  WORKFLOW_STATUS("workflowStatus"),
  PLUGIN_STATUS("pluginStatus"),
  PLUGIN_TYPE("pluginType"),
  NEXT_EXECUTABLE_PLUGIN_TYPE("nextExecutablePluginType"),
  METIS_PLUGINS("metisPlugins"),
  CREATED_DATE("createdDate"),
  STARTED_DATE("startedDate"),
  UPDATED_DATE("updatedDate"),
  FINISHED_DATE("finishedDate"),
  PLUGIN_METADATA("pluginMetadata"),
  EXTERNAL_TASK_ID("externalTaskId"),
  XSLT_ID("xsltId"),
  CLAIMED_BY_INSTANCE("claimedByInstance");

  private final String fieldName;

  DaoFieldNames(String fieldName) {
    this.fieldName = fieldName;
  }

  @Override
  public String toString() {
    return fieldName;
  }
}
