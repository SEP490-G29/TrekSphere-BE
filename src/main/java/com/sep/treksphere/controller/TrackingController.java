package com.sep.treksphere.controller;

import com.sep.treksphere.constant.MessageConstant;
import com.sep.treksphere.dto.request.SessionCheckpointLogRequest;
import com.sep.treksphere.dto.response.ApiResponse;
import com.sep.treksphere.dto.response.TourSessionStartResponse;
import com.sep.treksphere.security.CustomUserDetails;
import com.sep.treksphere.service.TrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tracking/sessions")
@RequiredArgsConstructor
@Tag(name = "Tracking Management", description = "Các API liên quan đến giám sát đi tour thực tế và GPS Tracking")
public class TrackingController {

    private final TrackingService trackingService;

    @PostMapping("/{sessionId}/start")
    @Operation(summary = "Bắt đầu phiên đi tour thực tế", description = "Chuyển trạng thái của Tour Session sang IN_PROGRESS và tự động khởi tạo nhật ký checkpoint log bằng tọa độ GPS thực tế.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<ApiResponse<TourSessionStartResponse>> startSession(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID sessionId,
            @RequestBody @Valid SessionCheckpointLogRequest request
    ) {
        TourSessionStartResponse data = trackingService.startSession(userDetails.getUser().getUserId(), sessionId, request);

        ApiResponse<TourSessionStartResponse> response = ApiResponse.success(HttpStatus.OK, data, MessageConstant.SESSION_STARTED_SUCCESSFULLY);

        return ResponseEntity.ok(response);
    }
}
