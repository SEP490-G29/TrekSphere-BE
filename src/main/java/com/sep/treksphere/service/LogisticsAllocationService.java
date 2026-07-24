package com.sep.treksphere.service;

import com.sep.treksphere.dto.request.logistics.AssignCoordinatorRequest;

import com.sep.treksphere.dto.response.TourSessionAllocationResponse;
import com.sep.treksphere.dto.response.TourSessionSummaryResponse;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.enums.tour.TourSessionStatus;

import java.util.UUID;

public interface LogisticsAllocationService {
    void assignCoordinator(UUID sessionId, AssignCoordinatorRequest request, UUID userId);
    void removeCoordinator(UUID scheduleId, UUID userId);
    
    PaginationResponse<TourSessionSummaryResponse> getVendorSessions(UUID vendorUserId, UUID tourId, TourSessionStatus status, int page, int size);
    TourSessionAllocationResponse getAllocations(UUID sessionId, UUID vendorUserId);
}
