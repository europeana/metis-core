package eu.europeana.metis.core.engine.ecloud;

import eu.europeana.cloud.common.model.dps.NodeReport;
import eu.europeana.cloud.common.model.dps.NodeStatistics;
import eu.europeana.cloud.common.model.dps.StatisticsReport;
import eu.europeana.metis.core.rest.stats.AttributeStatisticsDTO;
import eu.europeana.metis.core.rest.stats.NodePathStatisticsDTO;
import eu.europeana.metis.core.rest.stats.NodeValueStatisticsDTO;
import eu.europeana.metis.core.rest.stats.RecordStatisticsDTO;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Utility class for converting statistical data from ECloud to Metis classes.
 */
public final class EcloudEngineRecordStatisticsConverter {

  private EcloudEngineRecordStatisticsConverter() {
  }

  /**
   * Compiles record-level statistics from the given StatisticsReport.
   *
   * @param report The report containing record node statistics data.
   * @return A RecordStatisticsDTO object containing compiled statistical data for the provided report.
   */
  public static RecordStatisticsDTO compileRecordStatistics(StatisticsReport report) {

    // Group the node statistics by their respective xpath.
    final Map<String, List<NodeStatistics>> nodesByXPath = report.getNodeStatistics().stream()
                                                                 .collect(Collectors.groupingBy(NodeStatistics::getXpath));
    final List<NodePathStatisticsDTO> nodePathStatisticsDTOList =
        nodesByXPath.entrySet().stream().map(EcloudEngineRecordStatisticsConverter::compileNodePathStatistics)
                    .sorted(Comparator.comparing(NodePathStatisticsDTO::xPath)).toList();
    return new RecordStatisticsDTO(Long.toString(report.getTaskId()), nodePathStatisticsDTOList);
  }

  /**
   * Compiles statistics for a specific node path based on the provided node reports.
   *
   * @param nodePath The XPath expression representing the node.
   * @param nodeReports The list of node reports containing statistical data.
   * @return A NodePath
   */
  public static NodePathStatisticsDTO compileNodePathStatistics(String nodePath, List<NodeReport> nodeReports) {
    return compileNodePathStatistics(nodePath, nodeReports,
        EcloudEngineRecordStatisticsConverter::compileNodeValueStatistics);
  }

  private static NodePathStatisticsDTO compileNodePathStatistics(
      Entry<String, List<NodeStatistics>> nodeWithXPath) {
    return compileNodePathStatistics(nodeWithXPath.getKey(), nodeWithXPath.getValue(),
        EcloudEngineRecordStatisticsConverter::compileNodeValueStatistics);
  }

  private static <I> NodePathStatisticsDTO compileNodePathStatistics(String nodePath,
      List<I> nodes, Function<I, NodeValueStatisticsDTO> nodeValueConverter) {
    final List<NodeValueStatisticsDTO> nodeValueStatisticsDTOList =
        nodes.stream().map(nodeValueConverter).sorted(Comparator.comparing(NodeValueStatisticsDTO::value)).toList();
    return new NodePathStatisticsDTO(nodePath, nodeValueStatisticsDTOList);
  }

  private static NodeValueStatisticsDTO compileNodeValueStatistics(NodeStatistics nodeStatistics) {
    return compileNodeValueStatistics(nodeStatistics.getValue(), nodeStatistics.getOccurrence(),
        nodeStatistics.getAttributesStatistics());
  }

  private static NodeValueStatisticsDTO compileNodeValueStatistics(NodeReport nodeReport) {
    return compileNodeValueStatistics(nodeReport.getNodeValue(), nodeReport.getOccurrence(),
        nodeReport.getAttributeStatistics());
  }

  private static NodeValueStatisticsDTO compileNodeValueStatistics(String nodeValue,
      long occurrence,
      Collection<eu.europeana.cloud.common.model.dps.AttributeStatistics> attributes) {
    final List<AttributeStatisticsDTO> attributeStatisticDTOS =
        attributes.stream().map(EcloudEngineRecordStatisticsConverter::compileAttributeStatistics)
                  .sorted(Comparator.comparing(AttributeStatisticsDTO::xPath).thenComparing(
                      AttributeStatisticsDTO::value)).toList();
    return new NodeValueStatisticsDTO(nodeValue, occurrence, attributeStatisticDTOS);
  }

  private static AttributeStatisticsDTO compileAttributeStatistics(
      eu.europeana.cloud.common.model.dps.AttributeStatistics input) {
    return new AttributeStatisticsDTO(input.getName(), input.getValue(), input.getOccurrence());
  }
}
