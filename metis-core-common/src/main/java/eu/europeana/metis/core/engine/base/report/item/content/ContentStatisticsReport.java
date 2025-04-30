package eu.europeana.metis.core.engine.base.report.item.content;

import java.util.List;

public record ContentStatisticsReport(long taskId, List<ContentNodeStatistics> nodeStatistics) {
}
