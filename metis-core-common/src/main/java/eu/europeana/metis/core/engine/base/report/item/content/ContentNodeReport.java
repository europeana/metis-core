package eu.europeana.metis.core.engine.base.report.item.content;

import java.util.List;

public record ContentNodeReport(String nodeValue, long occurrence, List<ContentAttributeStatistics> attributeStatistics) {

}
