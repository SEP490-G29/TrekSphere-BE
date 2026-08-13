package com.sep.treksphere.controller;

import com.sep.treksphere.dto.request.TrackingLocationBatchRequest;
import com.sep.treksphere.dto.response.*;
import com.sep.treksphere.security.CustomUserDetails;
import com.sep.treksphere.service.TrackingLocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/v1/tracking/sessions")
@RequiredArgsConstructor
@Tag(name = "Tracking Locations", description = "Gửi và truy vấn lịch sử GPS")
@SecurityRequirement(name = "bearerAuth")
public class TrackingLocationController {

    private final TrackingLocationService locationService;

    @PostMapping("/{sessionId}/locations:batch")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Gửi batch GPS online/offline")
    public ResponseEntity<ApiResponse<TrackingLocationBatchResponse>> ingest(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID sessionId,
            @RequestBody @Valid TrackingLocationBatchRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK,
                locationService.ingest(user.getUser().getUserId(), sessionId, request)));
    }

    @GetMapping("/{sessionId}/locations/latest")
    @PreAuthorize("hasAnyRole('COORDINATOR', 'VENDOR_MANAGER', 'VENDOR_STAFF')")
    @Operation(summary = "Lấy vị trí mới nhất của các actor trong session")
    public ResponseEntity<ApiResponse<List<TrackingLocationResponse>>> getLatest(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID sessionId
    ) {
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK,
                locationService.getLatest(user.getUser().getUserId(), sessionId)));
    }

    @GetMapping("/{sessionId}/locations")
    @PreAuthorize("hasAnyRole('COORDINATOR', 'VENDOR_MANAGER', 'VENDOR_STAFF')")
    @Operation(summary = "Lấy lịch sử GPS theo khoảng thời gian")
    public ResponseEntity<ApiResponse<List<TrackingLocationResponse>>> getHistory(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID sessionId,
            @RequestParam(required = false) UUID actorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "2000") int limit
    ) {
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK,
                locationService.getHistory(user.getUser().getUserId(), sessionId, actorId, from, to, limit)));
    }
}
