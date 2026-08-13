package com.sep.treksphere.service;

import com.sep.treksphere.dto.request.TrackingOfflinePackRequest;
import com.sep.treksphere.dto.request.TrackingSyncRequest;
import com.sep.treksphere.dto.response.TrackingOfflinePackResponse;
import com.sep.treksphere.dto.response.TrackingSyncResponse;
import com.sep.treksphere.dto.response.TrackingSyncStateResponse;

import java.util.UUID;

public interface TrackingOfflineService {
    TrackingOfflinePackResponse createOfflinePack(UUID actorId, UUID sessionId, TrackingOfflinePackRequest request);
    TrackingSyncResponse sync(UUID actorId, UUID sessionId, TrackingSyncRequest request);
    TrackingSyncStateResponse getSyncState(UUID actorId, UUID sessionId, long afterRevision);
}
