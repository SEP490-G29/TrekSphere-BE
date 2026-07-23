package com.sep.treksphere.controller;

import com.sep.treksphere.dto.response.ApiResponse;
import com.sep.treksphere.dto.request.logistics.AssignCoordinatorRequest;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.dto.response.TourSessionSummaryResponse;
import com.sep.treksphere.dto.response.TourSessionAllocationResponse;
import com.sep.treksphere.enums.tour.TourSessionStatus;
import com.sep.treksphere.security.CustomUserDetails;
import com.sep.treksphere.service.LogisticsAllocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.UUID;

import static com.sep.treksphere.constant.MessageConstant.COORDINATOR_ASSIGNED_SUCCESSFULLY;
import static com.sep.treksphere.constant.MessageConstant.COORDINATOR_REMOVED_SUCCESSFULLY;

@RestController
@RequestMapping("/api/v1/vendor/sessions")
@Tag(name = "Vendor Logistics", description = "Các API quản lý phân công hậu cần cho Vendor (Module 5C)")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('VENDOR_MANAGER', 'VENDOR_STAFF')")
public class VendorLogisticsController {

    private final LogisticsAllocationService logisticsAllocationService;

    @Operation(summary = "Phân công Hướng dẫn viên", description = "Gán một Hướng dẫn viên (Coordinator) vào một Phiên Tour (Tour Session)")
    @PostMapping("/{sessionId}/coordinators")
    public ResponseEntity<ApiResponse<Void>> assignCoordinator(
            @PathVariable UUID sessionId,
            @Valid @RequestBody AssignCoordinatorRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        logisticsAllocationService.assignCoordinator(sessionId, request, user.getUser().getUserId());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, null, COORDINATOR_ASSIGNED_SUCCESSFULLY));
    }

    @Operation(summary = "Gỡ phân công Hướng dẫn viên", description = "Xóa một Hướng dẫn viên đã được gán khỏi Phiên Tour")
    @DeleteMapping("/coordinators/{scheduleId}")
    public ResponseEntity<ApiResponse<Void>> removeCoordinator(
            @PathVariable UUID scheduleId,
            @AuthenticationPrincipal CustomUserDetails user) {
        logisticsAllocationService.removeCoordinator(scheduleId, user.getUser().getUserId());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, COORDINATOR_REMOVED_SUCCESSFULLY));
    }

    @Operation(summary = "Lấy danh sách Phiên Tour", description = "Lấy danh sách các Phiên Tour do Vendor quản lý (Hỗ trợ phân trang, lọc theo tourId và trạng thái)")
    @GetMapping
    public ResponseEntity<ApiResponse<PaginationResponse<TourSessionSummaryResponse>>> getVendorSessions(
            @RequestParam(required = false) UUID tourId,
            @RequestParam(required = false) TourSessionStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails user) {
        PaginationResponse<TourSessionSummaryResponse> response = logisticsAllocationService.getVendorSessions(user.getUser().getUserId(), tourId, status, page, size);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response, "Lấy danh sách Tour Session thành công"));
    }

    @Operation(summary = "Lấy chi tiết phân bổ", description = "Lấy thông tin chi tiết phân bổ nhân sự (Coordinator) của một Phiên Tour cụ thể")
    @GetMapping("/{sessionId}/allocations")
    public ResponseEntity<ApiResponse<TourSessionAllocationResponse>> getAllocations(
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal CustomUserDetails user) {
        TourSessionAllocationResponse response = logisticsAllocationService.getAllocations(sessionId, user.getUser().getUserId());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response, "Lấy thông tin phân bổ thành công"));
    }
}
