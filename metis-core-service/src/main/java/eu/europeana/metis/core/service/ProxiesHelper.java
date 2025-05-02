package eu.europeana.metis.core.service;

import eu.europeana.cloud.common.model.dps.NodeReport;
import eu.europeana.cloud.common.model.dps.NodeStatistics;
import eu.europeana.cloud.common.model.dps.StatisticsReport;
import eu.europeana.metis.core.engine.base.item.content.report.ContentAttributeStatistics;
import eu.europeana.metis.core.engine.base.item.content.report.ContentNodeReport;
import eu.europeana.metis.core.engine.base.item.content.report.ContentNodeStatistics;
import eu.europeana.metis.core.engine.base.item.content.report.ContentStatisticsReport;
import eu.europeana.metis.core.rest.stats.AttributeStatistics;
import eu.europeana.metis.core.rest.stats.NodePathStatistics;
import eu.europeana.metis.core.rest.stats.NodeValueStatistics;
import eu.europeana.metis.core.rest.stats.RecordStatistics;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

class ProxiesHelper {

  ProxiesHelper() {
  }

  RecordStatistics compileRecordStatistics(StatisticsReport report) {

    // Group the node statistics by their respective xpath.
    final Map<String, List<NodeStatistics>> nodesByXPath = report.getNodeStatistics().stream()
                                                                 .collect(Collectors.groupingBy(NodeStatistics::getXpath));
    final List<NodePathStatistics> nodePathStatisticsList =
        nodesByXPath.entrySet().stream().map(ProxiesHelper::compileNodePathStatistics)
                    .sorted(Comparator.comparing(NodePathStatistics::xPath)).toList();
    return new RecordStatistics(report.getTaskId(), nodePathStatisticsList);
  }

  RecordStatistics compileRecordStatisticsExternal(ContentStatisticsReport report) {

    // Group the node statistics by their respective xpath.
    final Map<String, List<ContentNodeStatistics>> nodesByXPath = report.nodeStatistics().stream()
                                                                        .collect(Collectors.groupingBy(ContentNodeStatistics::xpath));
    final List<NodePathStatistics> nodePathStatisticsList =
        nodesByXPath.entrySet().stream().map(ProxiesHelper::compileNodePathStatisticsExternal)
                    .sorted(Comparator.comparing(NodePathStatistics::xPath)).toList();
    return new RecordStatistics(report.taskId(), nodePathStatisticsList);
  }

  private static NodePathStatistics compileNodePathStatistics(
      Entry<String, List<NodeStatistics>> nodeWithXPath) {
    return compileNodePathStatistics(nodeWithXPath.getKey(), nodeWithXPath.getValue(),
        ProxiesHelper::compileNodeValueStatistics);
  }

  private static NodePathStatistics compileNodePathStatisticsExternal(
      Entry<String, List<ContentNodeStatistics>> nodeWithXPath) {
    return compileNodePathStatistics(nodeWithXPath.getKey(), nodeWithXPath.getValue(),
        ProxiesHelper::compileNodeValueStatisticsExternal);
  }

  NodePathStatistics compileNodePathStatistics(String nodePath, List<NodeReport> nodeReports) {
    return compileNodePathStatistics(nodePath, nodeReports,
        ProxiesHelper::compileNodeValueStatistics);
  }

  NodePathStatistics compileNodePathStatisticsExternal(String nodePath, List<ContentNodeReport> nodeReports) {
    return compileNodePathStatistics(nodePath, nodeReports,
        ProxiesHelper::compileNodeValueStatisticsExternal);
  }

  private static <I> NodePathStatistics compileNodePathStatistics(String nodePath,
      List<I> nodes, Function<I, NodeValueStatistics> nodeValueConverter) {
    final List<NodeValueStatistics> nodeValueStatisticsList =
        nodes.stream().map(nodeValueConverter).sorted(Comparator.comparing(NodeValueStatistics::value)).toList();
    return new NodePathStatistics(nodePath, nodeValueStatisticsList);
  }

  private static <I> NodePathStatistics compileNodePathStatisticsExternal(String nodePath,
      List<I> nodes, Function<I, NodeValueStatistics> nodeValueConverter) {
    final List<NodeValueStatistics> nodeValueStatisticsList =
        nodes.stream().map(nodeValueConverter).sorted(Comparator.comparing(NodeValueStatistics::value)).toList();
    return new NodePathStatistics(nodePath, nodeValueStatisticsList);
  }

  private static NodeValueStatistics compileNodeValueStatistics(NodeStatistics nodeStatistics) {
    return compileNodeValueStatistics(nodeStatistics.getValue(), nodeStatistics.getOccurrence(),
        nodeStatistics.getAttributesStatistics());
  }

  private static NodeValueStatistics compileNodeValueStatisticsExternal(ContentNodeStatistics nodeStatistics) {
    return compileNodeValueStatisticsExternal(nodeStatistics.value(), nodeStatistics.occurrence(),
        nodeStatistics.attributesStatistics());
  }

  private static NodeValueStatistics compileNodeValueStatistics(NodeReport nodeReport) {
    return compileNodeValueStatistics(nodeReport.getNodeValue(), nodeReport.getOccurrence(),
        nodeReport.getAttributeStatistics());
  }

  private static NodeValueStatistics compileNodeValueStatisticsExternal(ContentNodeReport nodeReport) {
    return compileNodeValueStatisticsExternal(nodeReport.nodeValue(), nodeReport.occurrence(), nodeReport.attributeStatistics());
  }

  private static NodeValueStatistics compileNodeValueStatistics(String nodeValue,
      long occurrence,
      Collection<eu.europeana.cloud.common.model.dps.AttributeStatistics> attributes) {
    final List<AttributeStatistics> attributeStatistics =
        attributes.stream().map(ProxiesHelper::compileAttributeStatistics)
                  .sorted(Comparator.comparing(AttributeStatistics::xPath).thenComparing(AttributeStatistics::value)).toList();
    return new NodeValueStatistics(nodeValue, occurrence, attributeStatistics);
  }

  private static NodeValueStatistics compileNodeValueStatisticsExternal(String nodeValue,
      long occurrence, Collection<ContentAttributeStatistics> attributes) {
    final List<AttributeStatistics> attributeStatistics =
        attributes.stream().map(ProxiesHelper::compileAttributeStatisticsExternal)
                  .sorted(Comparator.comparing(AttributeStatistics::xPath).thenComparing(AttributeStatistics::value)).toList();
    return new NodeValueStatistics(nodeValue, occurrence, attributeStatistics);
  }


  private static AttributeStatistics compileAttributeStatistics(
      eu.europeana.cloud.common.model.dps.AttributeStatistics input) {
    return new AttributeStatistics(input.getName(), input.getValue(), input.getOccurrence());
  }

  private static AttributeStatistics compileAttributeStatisticsExternal(ContentAttributeStatistics input) {
    return new AttributeStatistics(input.name(), input.value(), input.occurrence());
  }
}
