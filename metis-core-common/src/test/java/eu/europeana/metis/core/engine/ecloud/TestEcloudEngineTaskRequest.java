package eu.europeana.metis.core.engine.ecloud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import eu.europeana.cloud.service.dps.BatchInfo;
import eu.europeana.cloud.service.dps.CreateDpsTaskRequest;
import eu.europeana.cloud.service.dps.DepublicationInfo;
import eu.europeana.metis.core.engine.base.EngineTaskKey;
import eu.europeana.metis.core.engine.base.task.input.DepublishInputDataEndpoint;
import eu.europeana.metis.core.engine.base.task.input.SimpleIntermediateInputDataEndpoint;
import eu.europeana.metis.core.workflow.plugins.Topology;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class TestEcloudEngineTaskRequest {

  @Test
  void linkCheckingKeepsInputProviderWithoutOutputProvider() {
    CreateDpsTaskRequest createDpsTaskRequest = new EcloudEngineTaskRequest(Map.of(EngineTaskKey.PROVIDER_ID, "provider"),
        new SimpleIntermediateInputDataEndpoint("sourceTaskId", "sourceBatchId"),
        Topology.LINK_CHECKING.getTopologyName()).getCreateDpsTaskRequest();
    BatchInfo source = assertInstanceOf(BatchInfo.class, createDpsTaskRequest.getSource());
    assertEquals("provider", source.getProviderId());
    assertEquals("sourceBatchId", source.getBatchId());
    assertNull(createDpsTaskRequest.getResultsProvider());
  }

  @Test
  void depublishingHasNoOutputEvenWithProvider() {
    CreateDpsTaskRequest createDpsTaskRequest = new EcloudEngineTaskRequest(Map.of(EngineTaskKey.PROVIDER_ID, "provider"),
        new DepublishInputDataEndpoint(true, Set.of()),
        Topology.DEPUBLISH.getTopologyName()).getCreateDpsTaskRequest();
    assertInstanceOf(DepublicationInfo.class, createDpsTaskRequest.getSource());
    assertNull(createDpsTaskRequest.getResultsProvider());
  }

  @ParameterizedTest
  @EnumSource(value = Topology.class, names = {"LINK_CHECKING", "DEPUBLISH"}, mode = EnumSource.Mode.EXCLUDE)
  void recordProducingTopologiesKeepOutputProvider(Topology topology) {
    CreateDpsTaskRequest createDpsTaskRequest = new EcloudEngineTaskRequest(Map.of(EngineTaskKey.PROVIDER_ID, "provider"),
        new SimpleIntermediateInputDataEndpoint("sourceTaskId", "sourceBatchId"),
        topology.getTopologyName()).getCreateDpsTaskRequest();
    assertEquals("provider", createDpsTaskRequest.getResultsProvider());
  }
}
