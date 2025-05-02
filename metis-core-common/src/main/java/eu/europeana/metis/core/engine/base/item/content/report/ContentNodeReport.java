package eu.europeana.metis.core.engine.base.item.content.report;

import java.util.List;

public record ContentNodeReport(String nodeValue, long occurrence, List<ContentAttributeStatistics> attributeStatistics) {

}
