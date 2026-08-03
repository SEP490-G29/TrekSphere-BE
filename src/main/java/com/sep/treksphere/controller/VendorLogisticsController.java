package com.sep.treksphere.controller;

import com.sep.treksphere.dto.response.ApiResponse;
import com.sep.treksphere.dto.request.AssignCoordinatorRequest;
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
import com.sep.treksphere.dto.request.AssignPorterRequest;
import com.sep.treksphere.dto.request.AssignEquipmentRequest;
import com.sep.treksphere.dto.response.StaffScheduleResponse;
import com.sep.treksphere.dto.request.CancelScheduleRequest;
import com.sep.treksphere.constant.MessageConstant;

import static com.sep.treksphere.constant.MessageConstant.COORDINATOR_ASSIGNED_SUCCESSFULLY;
import static com.sep.treksphere.constant.MessageConstant.COORDINATOR_REMOVED_SUCCESSFULLY;

@RestController
@RequestMapping("/api/v1/vendor/sessions")
@Tag(name = "Vendor Logistics", description = "Các API quản lý phân công hậu cần cho Vendor (Module 5C)")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('VENDOR_MANAGER', 'VENDOR_STAFF', 'COORDINATOR')")
public class VendorLogisticsController {

    private final LogisticsAllocationService logisticsAllocationService;

    @Operation(summary = "Phân công Hướng dẫn viên", description = "Gán một Hướng dẫn viên (Coordinator) vào một Phiên Tour (Tour Session)")
    @PostMapping("/{sessionId}/coordinators")
    @PreAuthorize("hasAnyRole('VENDOR_MANAGER', 'VENDOR_STAFF')")
    public ResponseEntity<ApiResponse<Void>> assignCoordinator(
            @PathVariable UUID sessionId,
            @Valid @RequestBody AssignCoordinatorRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        logisticsAllocationService.assignCoordinator(sessionId, request, user.getUser().getUserId());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, null, COORDINATOR_ASSIGNED_SUCCESSFULLY));
    }

    @Operation(summary = "Phân công Porter", description = "Gán một Porter vào một Phiên Tour")
    @PostMapping("/{sessionId}/porters")
    @PreAuthorize("hasAnyRole('VENDOR_MANAGER', 'VENDOR_STAFF')")
    public ResponseEntity<ApiResponse<Void>> assignPorter(
            @PathVariable UUID sessionId,
            @Valid @RequestBody AssignPorterRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        logisticsAllocationService.assignPorter(sessionId, request, user.getUser().getUserId());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, null, MessageConstant.PORTER_ASSIGNED_SUCCESSFULLY));
    }

    @Operation(summary = "Phân bổ Trang bị", description = "Phân bổ trang bị từ kho vào một Phiên Tour")
    @PostMapping("/{sessionId}/equipments")
    @PreAuthorize("hasAnyRole('VENDOR_MANAGER', 'VENDOR_STAFF')")
    public ResponseEntity<ApiResponse<Void>> assignEquipment(
            @PathVariable UUID sessionId,
            @Valid @RequestBody AssignEquipmentRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        String message = logisticsAllocationService.assignEquipment(sessionId, request, user.getUser().getUserId());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, null, message));
    }

    @Operation(summary = "Hủy phân bổ Trang bị", description = "Xóa một trang bị đã được phân bổ khỏi Phiên Tour và hoàn trả lại kho")
    @DeleteMapping("/equipments/{sessionEquipmentId}")
    @PreAuthorize("hasAnyRole('VENDOR_MANAGER', 'VENDOR_STAFF')")
    public ResponseEntity<ApiResponse<Void>> removeEquipment(
            @PathVariable UUID sessionEquipmentId,
            @AuthenticationPrincipal CustomUserDetails user) {
        logisticsAllocationService.removeEquipment(sessionEquipmentId, user.getUser().getUserId());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, null, MessageConstant.EQUIPMENT_REMOVED_SUCCESSFULLY));
    }

    @Operation(summary = "Gỡ phân công Porter", description = "Xóa một Porter đã được gán khỏi Phiên Tour")
    @DeleteMapping("/porters/{porterScheduleId}")
    @PreAuthorize("hasAnyRole('VENDOR_MANAGER', 'VENDOR_STAFF')")
    public ResponseEntity<ApiResponse<Void>> removePorter(
            @PathVariable UUID porterScheduleId,
            @AuthenticationPrincipal CustomUserDetails user) {
        logisticsAllocationService.removePorter(porterScheduleId, user.getUser().getUserId());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, MessageConstant.PORTER_REMOVED_SUCCESSFULLY));
    }

    @Operation(summary = "Gỡ phân công Hướng dẫn viên", description = "Xóa một Hướng dẫn viên đã được gán khỏi Phiên Tour")
    @DeleteMapping("/coordinators/{scheduleId}")
    @PreAuthorize("hasAnyRole('VENDOR_MANAGER', 'VENDOR_STAFF')")
    public ResponseEntity<ApiResponse<Void>> removeCoordinator(
            @PathVariable UUID scheduleId,
            @AuthenticationPrincipal CustomUserDetails user) {
        logisticsAllocationService.removeCoordinator(scheduleId, user.getUser().getUserId());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, COORDINATOR_REMOVED_SUCCESSFULLY));
    }

    @Operation(summary = "Lấy danh sách Phiên Tour", description = "Lấy danh sách các Phiên Tour do Vendor quản lý (Hỗ trợ phân trang, lọc theo tourId và trạng thái)")
    @GetMapping
    @PreAuthorize("hasAnyRole('VENDOR_MANAGER', 'VENDOR_STAFF')")
    public ResponseEntity<ApiResponse<PaginationResponse<TourSessionSummaryResponse>>> getVendorSessions(
            @RequestParam(required = false) UUID tourId,
            @RequestParam(required = false) TourSessionStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails user) {
        PaginationResponse<TourSessionSummaryResponse> response = logisticsAllocationService.getVendorSessions(user.getUser().getUserId(), tourId, status, page, size);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response, "Lấy danh sách Tour Session thành công"));
    }

    @Operation(summary = "Lấy chi tiết phân bổ", description = "Lấy thông tin chi tiết phân bổ nhân sự của một Phiên Tour cụ thể (Vendor/Coordinator)")
    @GetMapping("/{sessionId}/allocations")
    @PreAuthorize("hasAnyRole('VENDOR_MANAGER', 'VENDOR_STAFF', 'COORDINATOR')")
    public ResponseEntity<ApiResponse<TourSessionAllocationResponse>> getAllocations(
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal CustomUserDetails user) {
        boolean isCoordinator = user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_COORDINATOR"));
        TourSessionAllocationResponse response = logisticsAllocationService.getAllocations(sessionId, user.getUser().getUserId(), isCoordinator);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response, "Lấy thông tin phân bổ thành công"));
    }

    @Operation(summary = "Lấy lịch trình làm việc của Hướng dẫn viên", description = "Xem danh sách các Tour mà một Hướng dẫn viên (Coordinator) đã được phân công. Quản lý có thể xem tất cả.")
    @GetMapping("/coordinators/schedules")
    public ResponseEntity<ApiResponse<PaginationResponse<StaffScheduleResponse>>> getCoordinatorSchedules(
            @RequestParam(required = false) UUID coordinatorId,
            @RequestParam(required = false) TourSessionStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails user) {
            
        boolean isManagerOrStaff = user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_VENDOR_MANAGER") || a.getAuthority().equals("ROLE_VENDOR_STAFF"));
                
        UUID targetCoordinatorId = coordinatorId;
        
        if (!isManagerOrStaff) {
            // Coordinator can only view their own schedule
            targetCoordinatorId = user.getUser().getUserId();
        }
        
        PaginationResponse<StaffScheduleResponse> response = logisticsAllocationService.getCoordinatorSchedules(targetCoordinatorId, user.getUser().getUserId(), status, page, size);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response, "Lấy lịch trình làm việc thành công"));
    }

    @Operation(summary = "Hủy phân công khẩn cấp", description = "Hủy lịch trình làm việc do sự cố khẩn cấp (Phải báo trước 1 ngày)")
    @PostMapping("/coordinators/schedules/{scheduleId}/cancel")
    public ResponseEntity<ApiResponse<Void>> emergencyCancelSchedule(
            @PathVariable UUID scheduleId,
            @Valid @RequestBody CancelScheduleRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
            
        boolean isManager = user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_VENDOR_MANAGER"));
                
        logisticsAllocationService.emergencyCancelSchedule(scheduleId, request, user.getUser().getUserId(), isManager);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, MessageConstant.SCHEDULE_CANCELLED_SUCCESSFULLY));
    }
}
