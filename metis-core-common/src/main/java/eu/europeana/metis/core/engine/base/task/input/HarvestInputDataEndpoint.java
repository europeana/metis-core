package eu.europeana.metis.core.engine.base.task.input;

public sealed interface HarvestInputDataEndpoint extends InputDataEndpoint
    permits OaiHarvestInputDataEndpoint, HttpHarvestInputDataEndpoint {

  Integer stepSize();
}
