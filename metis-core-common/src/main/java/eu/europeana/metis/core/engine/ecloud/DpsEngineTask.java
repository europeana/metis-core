package eu.europeana.metis.core.engine.ecloud;

import static eu.europeana.cloud.service.dps.InputDataType.DATASET_URLS;
import static eu.europeana.cloud.service.dps.InputDataType.REPOSITORY_URLS;

import eu.europeana.cloud.common.model.Revision;
import eu.europeana.cloud.service.dps.DpsTask;
import eu.europeana.cloud.service.dps.InputDataType;
import eu.europeana.cloud.service.dps.OAIPMHHarvestingDetails;
import eu.europeana.metis.core.engine.base.DataRevision;
import eu.europeana.metis.core.engine.base.EngineTask;
import eu.europeana.metis.core.engine.base.EngineTaskKey;
import eu.europeana.metis.core.engine.base.task.input.HarvestInputDataEndpoint;
import eu.europeana.metis.core.engine.base.task.input.InputDataEndpoint;
import eu.europeana.metis.core.engine.base.task.input.InternalInputDataEndpoint;
import eu.europeana.metis.core.engine.base.task.input.OaiHarvestInputDataEndpoint;
import java.util.List;
import java.util.Map;

public class DpsEngineTask implements EngineTask {

  private final DpsTask dpsTask;

  public DpsEngineTask() {
    this.dpsTask = new DpsTask();
  }

  public DpsTask toDpsTask() {
    return dpsTask;
  }

  @Override
  public void setParameters(Map<EngineTaskKey, String> parameters) {
    parameters.forEach((key, value) -> dpsTask.addParameter(key.name(), value));
  }

  @Override
  public <T extends InputDataEndpoint> void setInputDataLocation(T inputDataEndpoint) {
    final InputDataType inputDataType = switch (inputDataEndpoint) {
      case InternalInputDataEndpoint ignored -> DATASET_URLS;
      case HarvestInputDataEndpoint ignored -> REPOSITORY_URLS;
      case OaiHarvestInputDataEndpoint oaiHarvestInputDataParameters -> {
        setOaiHarvestParameters(oaiHarvestInputDataParameters);
        yield REPOSITORY_URLS;
      }
      default -> null;
    };

    if (inputDataType != null) {
      Map<InputDataType, List<String>> inputDataLocation = Map.of(inputDataType, List.of(inputDataEndpoint.url()));
      dpsTask.setInputData(inputDataLocation);
    }
  }

  public void setOaiHarvestParameters(OaiHarvestInputDataEndpoint oaiHarvestInputDataParameters) {
    OAIPMHHarvestingDetails oaipmhHarvestingDetails = new OAIPMHHarvestingDetails();
    oaipmhHarvestingDetails.setSet(oaiHarvestInputDataParameters.set());
    oaipmhHarvestingDetails.setSchema(oaiHarvestInputDataParameters.metadataPrefix());
    oaipmhHarvestingDetails.setDateFrom(oaiHarvestInputDataParameters.from());
    oaipmhHarvestingDetails.setDateUntil(oaiHarvestInputDataParameters.until());
    dpsTask.setHarvestingDetails(oaipmhHarvestingDetails);
  }

  @Override
  public void setOutputRevision(DataRevision dataRevision) {
    final Revision revision = new Revision(dataRevision.name(), dataRevision.providerId(), dataRevision.creationTimeStamp(),
        dataRevision.deleted());
    dpsTask.setOutputRevision(revision);
  }
}
