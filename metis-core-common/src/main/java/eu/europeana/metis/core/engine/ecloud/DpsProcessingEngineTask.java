package eu.europeana.metis.core.engine.ecloud;

import static eu.europeana.cloud.service.dps.InputDataType.DATASET_URLS;

import eu.europeana.cloud.common.model.Revision;
import eu.europeana.cloud.service.dps.DpsTask;
import eu.europeana.cloud.service.dps.OAIPMHHarvestingDetails;
import eu.europeana.metis.core.engine.base.DataRevision;
import eu.europeana.metis.core.engine.base.ProcessingEngineTask;
import eu.europeana.metis.core.engine.base.OaiHarvestParameters;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DpsProcessingEngineTask implements ProcessingEngineTask {

  private final DpsTask dpsTask;

  public DpsProcessingEngineTask() {
    this.dpsTask = new DpsTask();
  }

  public DpsTask toDpsTask() {
    return dpsTask;
  }

  @Override
  public void setParameters(Map<String, String> parameters) {
    dpsTask.setParameters(parameters);
  }

  @Override
  public void setOutputRevision(DataRevision dataRevision) {
    final Revision revision = new Revision(dataRevision.name(), dataRevision.providerId(), dataRevision.creationTimeStamp(),
        dataRevision.deleted());
    dpsTask.setOutputRevision(revision);
  }

  @Override
  public void setInputDataLocation(InputDataType inputDataType, String inputDataLocation) {
    Map<eu.europeana.cloud.service.dps.InputDataType, List<String>> dataEntries;
    if (inputDataType.equals(InputDataType.INTERNAL_DATASET)) {
      dataEntries = Map.of(DATASET_URLS, Collections.singletonList(inputDataLocation));
    } else {
      dataEntries = Map.of(eu.europeana.cloud.service.dps.InputDataType.REPOSITORY_URLS, Collections.singletonList(inputDataLocation));
    }
    dpsTask.setInputData(dataEntries);
  }

  @Override
  public void setOaiHarvestParameters(OaiHarvestParameters oaiHarvestParameters) {
    OAIPMHHarvestingDetails oaipmhHarvestingDetails = new OAIPMHHarvestingDetails();
    oaipmhHarvestingDetails.setSet(oaiHarvestParameters.set());
    oaipmhHarvestingDetails.setSchema(oaiHarvestParameters.metadataPrefix());
    oaipmhHarvestingDetails.setDateFrom(oaiHarvestParameters.from());
    oaipmhHarvestingDetails.setDateUntil(oaiHarvestParameters.until());
    dpsTask.setHarvestingDetails(oaipmhHarvestingDetails);
  }
}
