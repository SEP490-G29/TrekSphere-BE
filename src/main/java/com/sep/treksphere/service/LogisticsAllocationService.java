package com.sep.treksphere.service;

import com.sep.treksphere.dto.request.AssignCoordinatorRequest;

import com.sep.treksphere.dto.response.StaffScheduleResponse;
import com.sep.treksphere.dto.response.TourSessionAllocationResponse;
import com.sep.treksphere.dto.response.TourSessionSummaryResponse;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.enums.tour.TourSessionStatus;

import java.util.UUID;
import com.sep.treksphere.dto.request.CancelScheduleRequest;
import com.sep.treksphere.dto.request.AssignPorterRequest;
import com.sep.treksphere.dto.request.AssignEquipmentRequest;
import com.sep.treksphere.dto.request.ReturnEquipmentRequest;

public interface LogisticsAllocationService {
    void assignEquipment(UUID sessionId, AssignEquipmentRequest request, UUID vendorUserId);
    void removeEquipment(UUID sessionEquipmentId, UUID vendorUserId);
    void returnEquipment(UUID sessionEquipmentId, ReturnEquipmentRequest request, UUID vendorUserId);
    void assignPorter(UUID sessionId, AssignPorterRequest request, UUID vendorUserId);
    void removePorter(UUID porterScheduleId, UUID vendorUserId);
    void assignCoordinator(UUID sessionId, AssignCoordinatorRequest request, UUID userId);
    void removeCoordinator(UUID scheduleId, UUID userId);
    void emergencyCancelSchedule(UUID scheduleId, CancelScheduleRequest request, UUID vendorUserId, boolean isManager);

    PaginationResponse<TourSessionSummaryResponse> getVendorSessions(UUID vendorUserId, UUID tourId, TourSessionStatus status, int page, int size);
    TourSessionAllocationResponse getAllocations(UUID sessionId, UUID vendorUserId);

    PaginationResponse<StaffScheduleResponse> getCoordinatorSchedules(UUID coordinatorId, UUID vendorUserId, TourSessionStatus status, int page, int size);
}
