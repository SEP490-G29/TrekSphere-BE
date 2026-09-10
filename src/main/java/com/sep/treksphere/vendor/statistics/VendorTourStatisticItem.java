package com.sep.treksphere.vendor.statistics;

import com.sep.treksphere.tour.TourStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class VendorTourStatisticItem {
    private UUID tourId;
    private String tourName;
    private TourStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;
    private long openScheduleCount;
    private long closedScheduleCount;
    private long cancelledScheduleCount;
    private long matchingGroupCount;
    private double averageFillRate;
    private double fullGroupRate;
}
