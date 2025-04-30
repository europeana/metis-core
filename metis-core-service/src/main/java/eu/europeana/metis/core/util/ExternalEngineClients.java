package eu.europeana.metis.core.util;

import eu.europeana.cloud.client.uis.rest.UISClient;
import eu.europeana.cloud.mcs.driver.DataSetServiceClient;
import eu.europeana.cloud.mcs.driver.FileServiceClient;
import eu.europeana.cloud.mcs.driver.RecordServiceClient;
import eu.europeana.metis.core.engine.base.ProcessingEngineTask;
import eu.europeana.metis.core.engine.base.ProcessingEngineTaskClient;
import eu.europeana.metis.core.engine.base.ProcessingEngineTaskSettings;

public record ExternalEngineClients<S extends ProcessingEngineTaskSettings, T extends ProcessingEngineTask>(
    DataSetServiceClient ecloudDataSetServiceClient,
    RecordServiceClient recordServiceClient,
    FileServiceClient fileServiceClient,
    ProcessingEngineTaskClient<S, T> processingEngineTaskClient,
    UISClient uisClient
) {

}
