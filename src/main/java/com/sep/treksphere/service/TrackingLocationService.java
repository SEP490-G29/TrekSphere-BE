package com.sep.treksphere.service;

import com.sep.treksphere.dto.request.TrackingLocationBatchRequest;
import com.sep.treksphere.dto.response.TrackingLocationBatchResponse;
import com.sep.treksphere.dto.response.TrackingLocationResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TrackingLocationService {
    TrackingLocationBatchResponse ingest(UUID actorId, UUID sessionId, TrackingLocationBatchRequest request);
    List<TrackingLocationResponse> getLatest(UUID viewerId, UUID sessionId);
    List<TrackingLocationResponse> getHistory(UUID viewerId, UUID sessionId, UUID actorId,
                                               Instant from, Instant to, int limit);
}
