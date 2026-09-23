package com.sep.treksphere.vendor.repository;

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
