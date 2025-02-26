package eu.europeana.metis.core.util;

import eu.europeana.cloud.client.dps.rest.DpsClient;
import eu.europeana.cloud.client.uis.rest.UISClient;
import eu.europeana.cloud.mcs.driver.DataSetServiceClient;
import eu.europeana.cloud.mcs.driver.FileServiceClient;
import eu.europeana.cloud.mcs.driver.RecordServiceClient;

/**
 * Represents a collection of eCloud components, including data set, record, file, DPS, and UIS clients,
 * as well as the eCloud provider.
 *
 * @param ecloudDataSetServiceClient the data set service client
 * @param recordServiceClient the record service client
 * @param fileServiceClient the file service client
 * @param dpsClient the DPS client
 * @param uisClient the UIS client
 */
public record EcloudClients(
    DataSetServiceClient ecloudDataSetServiceClient,
    RecordServiceClient recordServiceClient,
    FileServiceClient fileServiceClient,
    DpsClient dpsClient,
    UISClient uisClient
    ) {}
