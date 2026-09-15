package com.sep.treksphere.tour.recommendation;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class TourBehaviorEventBatchResponse {
    private int recordedCount;
    private int deduplicatedCount;
    private List<UUID> eventIds;
}
