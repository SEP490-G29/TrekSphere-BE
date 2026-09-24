package com.sep.treksphere.matching.service;

import com.sep.treksphere.matching.dto.request.CreateSosAlertRequest;
import com.sep.treksphere.matching.dto.request.UpdateSosLocationRequest;
import com.sep.treksphere.matching.dto.response.SosAlertResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface SosAlertService {

    SosAlertResponse createAlert(UUID groupId, CreateSosAlertRequest request, UUID currentUserId);

    List<SosAlertResponse> getActiveAlerts(UUID groupId, UUID currentUserId);

    Page<SosAlertResponse> getAlertHistory(UUID groupId, Pageable pageable, UUID currentUserId);

    SosAlertResponse respond(UUID groupId, UUID sosAlertId, UUID currentUserId);

    SosAlertResponse updateLocation(UUID groupId, UUID sosAlertId, UpdateSosLocationRequest request, UUID currentUserId);

    SosAlertResponse resolve(UUID groupId, UUID sosAlertId, UUID currentUserId);
}
