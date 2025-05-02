package eu.europeana.metis.core.engine.base.item.content.report;

import java.util.List;

public record ContentStatisticsReport(long taskId, List<ContentNodeStatistics> nodeStatistics) {
}
