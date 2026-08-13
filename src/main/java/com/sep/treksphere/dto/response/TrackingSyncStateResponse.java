package com.sep.treksphere.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TrackingSyncStateResponse {
    private Long revision;
    private Boolean fullSnapshot;
    private List<TrackingEventResult> changes;
    private TrackingSnapshotResponse snapshot;
}
