package com.sep.treksphere.vendor.statistics;

public interface VendorStatisticsOverviewProjection {
    Long getTotalTours();
    Long getDraftTours();
    Long getPublishedTours();
    Long getHiddenTours();
    Long getFutureOpenSchedules();
    Long getMatchingGroupCount();
    Double getAverageFillRate();
    Double getFullGroupRate();
}
