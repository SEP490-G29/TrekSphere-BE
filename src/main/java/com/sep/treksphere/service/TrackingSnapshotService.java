package com.sep.treksphere.service;

import com.sep.treksphere.dto.response.TrackingSnapshotResponse;

import java.util.UUID;

public interface TrackingSnapshotService {
    TrackingSnapshotResponse getSnapshot(UUID sessionId);
    long getRevision(UUID sessionId);
}
