package eu.europeana.metis.core.engine.base.task.input;

public sealed interface InputDataEndpoint
    permits HarvestInputDataEndpoint, OaiHarvestInputDataEndpoint, InternalInputDataEndpoint {

  String url();
}
