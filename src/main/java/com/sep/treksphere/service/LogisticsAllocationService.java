package com.sep.treksphere.service;

import com.sep.treksphere.dto.request.logistics.AssignCoordinatorRequest;
import com.sep.treksphere.security.CustomUserDetails;

import java.util.UUID;

public interface LogisticsAllocationService {
    void assignCoordinator(UUID sessionId, AssignCoordinatorRequest request, CustomUserDetails user);
    void removeCoordinator(UUID scheduleId, CustomUserDetails user);
}
