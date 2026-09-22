package com.sep.treksphere.vendor.statistics;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VendorStatisticsOverview {
    private long totalTours;
    private long draftTours;
    private long publishedTours;
    private long hiddenTours;
    private long futureOpenSchedules;
    private long matchingGroupCount;
    private double averageFillRate;
    private double fullGroupRate;
}
