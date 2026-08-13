package com.sep.treksphere.controller;

import com.sep.treksphere.dto.request.TrackingOfflinePackRequest;
import com.sep.treksphere.dto.request.TrackingSyncRequest;
import com.sep.treksphere.dto.response.*;
import com.sep.treksphere.security.CustomUserDetails;
import com.sep.treksphere.service.TrackingOfflineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tracking/sessions")
@RequiredArgsConstructor
@Tag(name = "Tracking Offline", description = "Offline pack, event sync và reconcile cho Tracking")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('COORDINATOR')")
public class TrackingOfflineController {

    private final TrackingOfflineService offlineService;

    @PostMapping("/{sessionId}/offline-pack")
    @Operation(summary = "Đăng ký thiết bị và tải dữ liệu dùng offline")
    public ResponseEntity<ApiResponse<TrackingOfflinePackResponse>> createOfflinePack(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID sessionId,
            @RequestBody @Valid TrackingOfflinePackRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK,
                offlineService.createOfflinePack(user.getUser().getUserId(), sessionId, request)));
    }

    @PostMapping("/{sessionId}/sync-events")
    @Operation(summary = "Đồng bộ các thao tác Tracking đã lưu khi mất mạng")
    public ResponseEntity<ApiResponse<TrackingSyncResponse>> syncEvents(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID sessionId,
            @RequestBody @Valid TrackingSyncRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK,
                offlineService.sync(user.getUser().getUserId(), sessionId, request)));
    }

    @GetMapping("/{sessionId}/sync-state")
    @Operation(summary = "Lấy revision và snapshot mới nhất để reconcile")
    public ResponseEntity<ApiResponse<TrackingSyncStateResponse>> getSyncState(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID sessionId,
            @RequestParam(defaultValue = "0") long afterRevision
    ) {
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK,
                offlineService.getSyncState(user.getUser().getUserId(), sessionId, afterRevision)));
    }
}
