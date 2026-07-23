package com.sep.treksphere.controller;

import com.sep.treksphere.dto.response.ApiResponse;
import com.sep.treksphere.dto.request.logistics.AssignCoordinatorRequest;
import com.sep.treksphere.security.CustomUserDetails;
import com.sep.treksphere.service.LogisticsAllocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static com.sep.treksphere.constant.MessageConstant.COORDINATOR_ASSIGNED_SUCCESSFULLY;
import static com.sep.treksphere.constant.MessageConstant.COORDINATOR_REMOVED_SUCCESSFULLY;

@RestController
@RequestMapping("/api/v1/vendor/sessions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('VENDOR_MANAGER', 'VENDOR_STAFF')")
public class VendorLogisticsController {

    private final LogisticsAllocationService logisticsAllocationService;

    @PostMapping("/{sessionId}/coordinators")
    public ResponseEntity<ApiResponse<Void>> assignCoordinator(
            @PathVariable UUID sessionId,
            @Valid @RequestBody AssignCoordinatorRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        logisticsAllocationService.assignCoordinator(sessionId, request, user.getUser().getUserId());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, null, COORDINATOR_ASSIGNED_SUCCESSFULLY));
    }

    @DeleteMapping("/coordinators/{scheduleId}")
    public ResponseEntity<ApiResponse<Void>> removeCoordinator(
            @PathVariable UUID scheduleId,
            @AuthenticationPrincipal CustomUserDetails user) {
        logisticsAllocationService.removeCoordinator(scheduleId, user.getUser().getUserId());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, COORDINATOR_REMOVED_SUCCESSFULLY));
    }
}
