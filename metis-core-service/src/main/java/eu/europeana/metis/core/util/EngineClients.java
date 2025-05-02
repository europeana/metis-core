package eu.europeana.metis.core.util;

import eu.europeana.cloud.client.uis.rest.UISClient;
import eu.europeana.cloud.mcs.driver.DataSetServiceClient;
import eu.europeana.cloud.mcs.driver.FileServiceClient;
import eu.europeana.cloud.mcs.driver.RecordServiceClient;
import eu.europeana.metis.core.engine.base.EngineTask;
import eu.europeana.metis.core.engine.base.EngineTaskClient;
import eu.europeana.metis.core.engine.base.EngineTaskSettings;

public record EngineClients<S extends EngineTaskSettings, T extends EngineTask>(
    DataSetServiceClient ecloudDataSetServiceClient,
    RecordServiceClient recordServiceClient,
    FileServiceClient fileServiceClient,
    EngineTaskClient<S, T> engineTaskClient,
    UISClient uisClient
) {

}
