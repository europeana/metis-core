package eu.europeana.metis.core.workflow.plugins;

/**
 * Record that contains basic parameters required for each {@link eu.europeana.cloud.service.dps.DpsTask} that is sent to ECloud.
 */
public record DpsTaskSettings(String ecloudBaseUrl, String ecloudProvider, String ecloudDatasetId, String previousExternalTaskId,
                              String metisCoreBaseUrl, ThrottlingValues throttlingValues) {}
