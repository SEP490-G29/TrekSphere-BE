package com.sep.treksphere.service;

import com.sep.treksphere.dto.request.TourCheckpointRequest;
import com.sep.treksphere.dto.response.TourCheckpointResponse;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface TourCheckpointService {

    List<TourCheckpointResponse> getCheckpointsByTourId(UUID tourId);

    TourCheckpointResponse createCheckpoint(UUID tourId, TourCheckpointRequest request, List<MultipartFile> images, String userEmail);

    TourCheckpointResponse updateCheckpoint(UUID checkpointId, TourCheckpointRequest request, List<MultipartFile> images, String userEmail);

    void deleteCheckpoint(UUID checkpointId, String userEmail);
}
