package com.sep.treksphere.service;

import com.sep.treksphere.dto.request.TourCheckpointRequest;
import com.sep.treksphere.dto.response.TourCheckpointResponse;

import java.util.List;
import java.util.UUID;

public interface TourCheckpointService {

    List<TourCheckpointResponse> getCheckpointsByTourId(UUID tourId);

    TourCheckpointResponse createCheckpoint(UUID tourId, TourCheckpointRequest request, String userEmail);

    TourCheckpointResponse updateCheckpoint(UUID checkpointId, TourCheckpointRequest request, String userEmail);

    void deleteCheckpoint(UUID checkpointId, String userEmail);
}
