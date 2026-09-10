package com.sep.treksphere.vendor.statistics;

import java.time.LocalDateTime;
import java.util.UUID;

public interface VendorTourStatisticProjection {
    UUID getTourId();
    String getTourName();
    String getStatus();
    LocalDateTime getCreatedAt();
    LocalDateTime getPublishedAt();
    Long getOpenScheduleCount();
    Long getClosedScheduleCount();
    Long getCancelledScheduleCount();
    Long getMatchingGroupCount();
    Double getAverageFillRate();
    Double getFullGroupRate();
}
