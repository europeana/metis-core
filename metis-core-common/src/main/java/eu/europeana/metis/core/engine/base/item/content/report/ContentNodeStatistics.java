package eu.europeana.metis.core.engine.base.item.content.report;

import java.util.Set;

public record ContentNodeStatistics(String parentXpath, String xpath, String value, long occurrence,
                                    Set<ContentAttributeStatistics> attributesStatistics) {
}
