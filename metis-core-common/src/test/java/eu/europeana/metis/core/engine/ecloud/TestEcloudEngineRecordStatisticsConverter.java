package eu.europeana.metis.core.engine.ecloud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.europeana.cloud.common.model.dps.AttributeStatistics;
import eu.europeana.cloud.common.model.dps.NodeReport;
import eu.europeana.cloud.common.model.dps.NodeStatistics;
import eu.europeana.cloud.common.model.dps.StatisticsReport;
import eu.europeana.metis.core.rest.stats.AttributeStatisticsDTO;
import eu.europeana.metis.core.rest.stats.NodePathStatisticsDTO;
import eu.europeana.metis.core.rest.stats.NodeValueStatisticsDTO;
import eu.europeana.metis.core.rest.stats.RecordStatisticsDTO;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class TestEcloudEngineRecordStatisticsConverter {

  @Test
  void testCompileRecordStatistics() {
    List<NodeStatistics> inputNodeStatistics = List.of(
        createNodeStatistics("parentXpath1", "xPath1", "value1", Set.of(new AttributeStatistics("name1", "value1"))),
        createNodeStatistics("parentXpath2", "xPath2", "value2", Set.of(new AttributeStatistics("name2", "value2"))),
        createNodeStatistics("parentXpath3", "xPath2", "value3",
            Set.of(new AttributeStatistics("name3", "value3"), new AttributeStatistics("name3", "value4")))
    );

    StatisticsReport statisticsReport = new StatisticsReport();
    statisticsReport.setTaskId(123L);
    statisticsReport.setNodeStatistics(inputNodeStatistics);

    RecordStatisticsDTO recordStatisticsDTO = EcloudEngineRecordStatisticsConverter.compileRecordStatistics(statisticsReport);

    assertEquals("123", recordStatisticsDTO.taskId());
    assertEquals(2, recordStatisticsDTO.nodePathStatistics().size());

    Map<String, List<NodeStatistics>> groupedByXpath =
        inputNodeStatistics.stream().collect(Collectors.groupingBy(NodeStatistics::getXpath));

    for (NodePathStatisticsDTO nodePathStatisticsDTO : recordStatisticsDTO.nodePathStatistics()) {
      String xpath = nodePathStatisticsDTO.xPath();
      assertNodeValueStatistics(groupedByXpath.get(xpath), nodePathStatisticsDTO.nodeValueStatistics());
    }
  }

  private static void assertNodeValueStatistics(List<NodeStatistics> inputNodeStatisticsByXpath,
      List<NodeValueStatisticsDTO> nodeValueStatisticsDTOS) {
    assertEquals(inputNodeStatisticsByXpath.size(), nodeValueStatisticsDTOS.size());

    Map<String, NodeStatistics> valueToNodeStatisticsMap =
        inputNodeStatisticsByXpath.stream().collect(Collectors.toMap(NodeStatistics::getValue, ns -> ns));

    for (NodeValueStatisticsDTO nodeValueStatisticsDTO : nodeValueStatisticsDTOS) {
      NodeStatistics nodeStatistics = valueToNodeStatisticsMap.get(nodeValueStatisticsDTO.value());
      assertEquals(nodeStatistics.getOccurrence(), nodeValueStatisticsDTO.occurrences());

      assertAttributeStatistics(nodeStatistics.getAttributesStatistics(), nodeValueStatisticsDTO.attributeStatistics());
    }
  }

  private static void assertAttributeStatistics(Collection<AttributeStatistics> attributeStatistics,
      List<AttributeStatisticsDTO> attributeStatisticsDTOS) {
    assertEquals(attributeStatistics.size(), attributeStatisticsDTOS.size());

    for (AttributeStatisticsDTO attributeStatisticsDTO : attributeStatisticsDTOS) {
      boolean matchFound = attributeStatistics.stream().anyMatch(attr ->
          attr.getName().equals(attributeStatisticsDTO.xPath()) &&
              attr.getValue().equals(attributeStatisticsDTO.value()) &&
              attr.getOccurrence() == attributeStatisticsDTO.occurrences()
      );
      assertTrue(matchFound);
    }
  }

  private NodeStatistics createNodeStatistics(String parentXpath, String xpath, String value,
      Set<AttributeStatistics> attributeStatistics) {
    return new NodeStatistics(parentXpath, xpath, value, 1, attributeStatistics);
  }

  @Test
  void compileNodePathStatisticsFromNodeReports() {
    String xpath = "xPath";

    List<NodeReport> nodeReports = List.of(
        createNodeReport("value1", List.of(new AttributeStatistics("name1", "value1", 1))),
        createNodeReport("value2",
            List.of(new AttributeStatistics("name2", "value2", 1), new AttributeStatistics("name3", "value3", 1)))
    );

    NodePathStatisticsDTO nodePathStatisticsDTO =
        EcloudEngineRecordStatisticsConverter.compileNodePathStatistics(xpath, nodeReports);

    assertEquals(xpath, nodePathStatisticsDTO.xPath());
    assertEquals(2, nodePathStatisticsDTO.nodeValueStatistics().size());

    Map<String, NodeReport> valueToNodeReportMap =
        nodeReports.stream().collect(Collectors.toMap(NodeReport::getNodeValue, Function.identity()));

    for (NodeValueStatisticsDTO nodeValueStatisticsDTO : nodePathStatisticsDTO.nodeValueStatistics()) {
      NodeReport nodeReport = valueToNodeReportMap.get(nodeValueStatisticsDTO.value());
      assertEquals(nodeReport.getOccurrence(), nodeValueStatisticsDTO.occurrences());

      assertAttributeStatistics(nodeReport.getAttributeStatistics(), nodeValueStatisticsDTO.attributeStatistics());
    }
  }

  private NodeReport createNodeReport(String value, List<AttributeStatistics> attributeStatistics) {
    NodeReport nodeReport = new NodeReport();
    nodeReport.setNodeValue(value);
    nodeReport.setOccurrence(1);
    nodeReport.setAttributeStatistics(attributeStatistics);
    return nodeReport;
  }

}