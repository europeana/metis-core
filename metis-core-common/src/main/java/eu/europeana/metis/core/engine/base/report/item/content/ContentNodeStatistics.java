package eu.europeana.metis.core.engine.base.report.item.content;

import java.util.Set;

public record ContentNodeStatistics(String parentXpath, String xpath, String value, long occurrence,
                                    Set<ContentAttributeStatistics> attributesStatistics) {
}
