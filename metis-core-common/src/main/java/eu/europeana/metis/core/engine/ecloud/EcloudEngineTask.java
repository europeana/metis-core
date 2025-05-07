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

public class EcloudEngineTask extends EngineTask {

  private final DpsTask dpsTask = new DpsTask();

  public EcloudEngineTask(Map<EngineTaskKey, String> parameters, InputDataEndpoint inputDataEndpoint, DataRevision outputDataRevision) {
    super(parameters, inputDataEndpoint, outputDataRevision);
    setParameters();
    setInputDataLocation();
    setOutputRevision();
  }

  public DpsTask toDpsTask() {
    return dpsTask;
  }

  private void setParameters() {
    parameters.forEach((key, value) -> dpsTask.addParameter(key.name(), value));
  }

  private <T extends InputDataEndpoint> void setInputDataLocation() {
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

  private void setOaiHarvestParameters(OaiHarvestInputDataEndpoint oaiHarvestInputDataParameters) {
    OAIPMHHarvestingDetails oaipmhHarvestingDetails = new OAIPMHHarvestingDetails();
    oaipmhHarvestingDetails.setSet(oaiHarvestInputDataParameters.set());
    oaipmhHarvestingDetails.setSchema(oaiHarvestInputDataParameters.metadataPrefix());
    oaipmhHarvestingDetails.setDateFrom(oaiHarvestInputDataParameters.from());
    oaipmhHarvestingDetails.setDateUntil(oaiHarvestInputDataParameters.until());
    dpsTask.setHarvestingDetails(oaipmhHarvestingDetails);
  }

  private void setOutputRevision() {
    final Revision revision = new Revision(outputDataRevision.name(), outputDataRevision.providerId(), outputDataRevision.creationTimeStamp(),
        outputDataRevision.deleted());
    dpsTask.setOutputRevision(revision);
  }
}
