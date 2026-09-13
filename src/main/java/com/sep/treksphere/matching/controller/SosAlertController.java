package com.sep.treksphere.matching.controller;

import com.sep.treksphere.common.constant.MessageConstant;
import com.sep.treksphere.common.dto.ApiResponse;
import com.sep.treksphere.common.dto.PaginationResponse;
import com.sep.treksphere.common.security.CustomUserDetails;
import com.sep.treksphere.matching.dto.request.CreateSosAlertRequest;
import com.sep.treksphere.matching.dto.response.SosAlertResponse;
import com.sep.treksphere.matching.service.SosAlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/matching-groups/{groupId}/sos-alerts")
@RequiredArgsConstructor
@Tag(name = "SOS Alert", description = "Phát và đóng tín hiệu SOS khẩn cấp trong Group Trip")
public class SosAlertController {

    private final SosAlertService sosAlertService;

    @PostMapping
    @PreAuthorize("hasAuthority('GROUP_TRIP_SOS')")
    @Operation(summary = "Phát tín hiệu SOS khẩn cấp trong Group Trip đang diễn ra")
    public ResponseEntity<ApiResponse<SosAlertResponse>> create(
            @PathVariable UUID groupId,
            @Valid @RequestBody CreateSosAlertRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID currentUserId = userDetails.getUser().getUserId();
        SosAlertResponse response = sosAlertService.createAlert(groupId, request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, response, MessageConstant.SOS_ALERT_CREATED_SUCCESS));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAuthority('GROUP_TRIP_SOS')")
    @Operation(summary = "Lấy danh sách tín hiệu SOS đang mở (OPEN) của Group Trip")
    public ResponseEntity<ApiResponse<List<SosAlertResponse>>> getActive(
            @PathVariable UUID groupId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID currentUserId = userDetails.getUser().getUserId();
        List<SosAlertResponse> response = sosAlertService.getActiveAlerts(groupId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response, MessageConstant.ACTIVE_SOS_ALERTS_FETCHED));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('GROUP_TRIP_SOS')")
    @Operation(summary = "Lấy lịch sử tín hiệu SOS của Group Trip (phân trang)")
    public ResponseEntity<ApiResponse<PaginationResponse<SosAlertResponse>>> getHistory(
            @PathVariable UUID groupId,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID currentUserId = userDetails.getUser().getUserId();
        Page<SosAlertResponse> page = sosAlertService.getAlertHistory(groupId, pageable, currentUserId);
        PaginationResponse<SosAlertResponse> response = PaginationResponse.<SosAlertResponse>builder()
                .content(page.getContent())
                .pageNumber(page.getNumber() + 1)
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response, MessageConstant.SOS_ALERT_HISTORY_FETCHED));
    }

    @PatchMapping("/{sosAlertId}/resolve")
    @PreAuthorize("hasAuthority('GROUP_TRIP_SOS')")
    @Operation(summary = "Đóng tín hiệu SOS (Sender của chính alert đó, hoặc Trưởng nhóm)")
    public ResponseEntity<ApiResponse<SosAlertResponse>> resolve(
            @PathVariable UUID groupId,
            @PathVariable UUID sosAlertId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID currentUserId = userDetails.getUser().getUserId();
        SosAlertResponse response = sosAlertService.resolve(groupId, sosAlertId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response, MessageConstant.SOS_ALERT_RESOLVED_SUCCESS));
    }
}
