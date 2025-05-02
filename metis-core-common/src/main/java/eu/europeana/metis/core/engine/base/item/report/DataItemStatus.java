package eu.europeana.metis.core.engine.base.item.report;

public record DataItemStatus(int resourceNum,
                             String resource,
                             DataItemState dataItemState,
                             String info,
                             String europeanaId,
                             long processingTime,
                             String resultResource) {

}
