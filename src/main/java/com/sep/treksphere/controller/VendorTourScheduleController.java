package com.sep.treksphere.controller;

import com.sep.treksphere.constant.MessageConstant;
import com.sep.treksphere.dto.request.CreateScheduleRequest;
import com.sep.treksphere.dto.request.UpdateScheduleRequest;
import com.sep.treksphere.dto.response.ApiResponse;
import com.sep.treksphere.dto.response.TourScheduleResponse;
import com.sep.treksphere.security.CustomUserDetails;
import com.sep.treksphere.service.TourScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vendor/tours")
@RequiredArgsConstructor
@Tag(name = "Vendor Tour Schedule Management", description = "Các API quản lý lịch khởi hành dành cho Vendor Manager và Vendor Staff")
public class VendorTourScheduleController {

    private final TourScheduleService tourScheduleService;

    @Operation(summary = "Tạo lịch khởi hành mới", description = "VendorStaff/Manager tạo lịch khởi hành mới cho Tour.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('VENDOR_MANAGER', 'VENDOR_STAFF')")
    @PostMapping("/{tourId}/schedules")
    public ResponseEntity<ApiResponse<TourScheduleResponse>> createSchedule(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID tourId,
            @Valid @RequestBody CreateScheduleRequest request) {

        TourScheduleResponse response = tourScheduleService.createSchedule(userDetails.getUsername(), tourId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, response, MessageConstant.SCHEDULE_CREATED_SUCCESSFULLY));
    }

    @Operation(summary = "Điều chỉnh thông tin lịch khởi hành", description = "VendorStaff/Manager điều chỉnh thông tin lịch khởi hành (bao gồm đổi status OPEN -> CLOSED).")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('VENDOR_MANAGER', 'VENDOR_STAFF')")
    @PutMapping("/schedules/{scheduleId}")
    public ResponseEntity<ApiResponse<TourScheduleResponse>> updateSchedule(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID scheduleId,
            @Valid @RequestBody UpdateScheduleRequest request) {

        TourScheduleResponse response = tourScheduleService.updateSchedule(userDetails.getUsername(), scheduleId, request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response, MessageConstant.SCHEDULE_UPDATED_SUCCESSFULLY));
    }

    @Operation(summary = "Huỷ/Xóa lịch khởi hành", description = "Chỉ VendorManager được phép xóa lịch khởi hành khi chưa có khách đặt.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('VENDOR_MANAGER')")
    @DeleteMapping("/schedules/{scheduleId}")
    public ResponseEntity<ApiResponse<Void>> deleteSchedule(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID scheduleId) {

        tourScheduleService.deleteSchedule(userDetails.getUsername(), scheduleId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, null, MessageConstant.SCHEDULE_DELETED_SUCCESSFULLY));
    }
}
