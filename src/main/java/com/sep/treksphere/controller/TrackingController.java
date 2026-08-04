package com.sep.treksphere.controller;

import com.sep.treksphere.constant.MessageConstant;
import com.sep.treksphere.dto.request.SessionCheckpointLogRequest;
import com.sep.treksphere.dto.response.ApiResponse;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.dto.response.SessionCheckpointLogResponse;
import com.sep.treksphere.dto.request.TourSessionAttendanceRequest;
import com.sep.treksphere.dto.response.TourSessionAttendanceResponse;
import com.sep.treksphere.dto.request.SessionEquipmentCheckRequest;
import com.sep.treksphere.dto.response.SessionEquipmentCheckResponse;
import com.sep.treksphere.dto.request.CreateSosAlertRequest;
import com.sep.treksphere.dto.response.SosAlertResponse;
import com.sep.treksphere.dto.response.TourSessionEndResponse;
import com.sep.treksphere.dto.response.TourSessionStartResponse;
import com.sep.treksphere.security.CustomUserDetails;
import com.sep.treksphere.service.TrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
    @Operation(summary = "Bắt đầu phiên đi tour thực tế", description = "Lead Coordinator bắt đầu Tour Session sau khi lịch khởi hành hợp lệ, hoàn tất điểm danh START, kiểm tra dụng cụ và cấu hình đầy đủ checkpoint. Hệ thống xác thực GPS tại điểm xuất phát, chuyển session sang IN_PROGRESS và khởi tạo nhật ký checkpoint.")
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

    @PostMapping("/{sessionId}/checkpoint-logs")
    @Operation(summary = "Ghi nhận checkpoint trạm dừng chân tiếp theo", description = "Hướng dẫn viên thực hiện check-in trạm dừng kế tiếp dọc hành trình theo đúng thứ tự. Hệ thống tự động so khớp và kiểm tra toạ độ GPS trong bán kính 200m.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<ApiResponse<SessionCheckpointLogResponse>> checkinCheckpoint(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID sessionId,
            @RequestBody @Valid SessionCheckpointLogRequest request
    ) {
        SessionCheckpointLogResponse data = trackingService.checkinCheckpoint(
                userDetails.getUser().getUserId(),
                sessionId,
                request
        );

        ApiResponse<SessionCheckpointLogResponse> response = ApiResponse.success(
                HttpStatus.OK,
                data,
                MessageConstant.CHECKIN_SUCCESSFUL
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{sessionId}/end")
    @Operation(summary = "Kết thúc phiên đi tour thực tế", description = "Chuyển trạng thái của Tour Session sang COMPLETED, lưu thời gian kết thúc thực tế và tự động check-in trạm đích (trạm cuối cùng).")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<ApiResponse<TourSessionEndResponse>> endSession(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID sessionId,
            @RequestBody @Valid SessionCheckpointLogRequest request
    ) {
        TourSessionEndResponse data = trackingService.endSession(
                userDetails.getUser().getUserId(),
                sessionId,
                request
        );

        ApiResponse<TourSessionEndResponse> response = ApiResponse.success(
                HttpStatus.OK,
                data,
                MessageConstant.SESSION_ENDED_SUCCESSFULLY
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{sessionId}/attendance")
    @Operation(summary = "Điểm danh danh sách Trekkers", description = "Hướng dẫn viên cập nhật điểm danh theo lô cho các Trekkers được gửi lên. START được ghi trước hoặc trong chuyến đi; END chỉ được ghi khi chuyến đi đang diễn ra và Trekker đã được ghi nhận START.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<ApiResponse<TourSessionAttendanceResponse>> recordAttendance(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID sessionId,
            @RequestBody @Valid TourSessionAttendanceRequest request
    ) {
        TourSessionAttendanceResponse data = trackingService.recordAttendance(
                userDetails.getUser().getUserId(),
                sessionId,
                request
        );

        ApiResponse<TourSessionAttendanceResponse> response = ApiResponse.success(
                HttpStatus.OK,
                data,
                MessageConstant.ATTENDANCE_RECORDED_SUCCESSFULLY
        );

        return ResponseEntity.ok(response);
    }

    @PutMapping("/equipments/{id}/check")
    @Operation(summary = "Kiểm tra dụng cụ đi tour", description = "Cho phép Hướng dẫn viên được phân công hoặc Nhân viên của nhà cung cấp sở hữu dụng cụ đánh dấu đã kiểm tra/mang theo dụng cụ đi tour thành công.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('COORDINATOR', 'VENDOR_STAFF')")
    public ResponseEntity<ApiResponse<SessionEquipmentCheckResponse>> checkEquipment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") UUID sessionEquipmentId,
            @RequestBody @Valid SessionEquipmentCheckRequest request
    ) {
        SessionEquipmentCheckResponse data = trackingService.checkEquipment(
                userDetails.getUser().getUserId(),
                sessionEquipmentId,
                request
        );

        ApiResponse<SessionEquipmentCheckResponse> response = ApiResponse.success(
                HttpStatus.OK,
                data,
                MessageConstant.SESSION_EQUIPMENT_CHECKED_SUCCESS
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/sos")
    @Operation(summary = "Phát tín hiệu SOS khẩn cấp", description = "Cho phép Hướng dẫn viên được phân công hoặc Khách du lịch tham gia chuyến đi phát tín hiệu cứu hộ khẩn cấp kèm tọa độ GPS thực tế.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('COORDINATOR', 'TREKKER')")
    public ResponseEntity<ApiResponse<SosAlertResponse>> createSosAlert(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid CreateSosAlertRequest request
    ) {
        SosAlertResponse data = trackingService.createSosAlert(
                userDetails.getUser().getUserId(),
                request
        );

        ApiResponse<SosAlertResponse> response = ApiResponse.success(
                HttpStatus.CREATED,
                data,
                MessageConstant.SOS_ALERT_CREATED_SUCCESS
        );

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/sos/active")
    @Operation(summary = "Xem danh sách các cuộc gọi SOS hoạt động", description = "Cho phép Admin xem toàn bộ hoặc Vendor Manager xem các tín hiệu SOS PENDING thuộc Vendor của mình, có hỗ trợ phân trang.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDOR_MANAGER')")
    public ResponseEntity<ApiResponse<PaginationResponse<SosAlertResponse>>> getActiveSosAlerts(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @ParameterObject
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        PaginationResponse<SosAlertResponse> data = trackingService.getActiveSosAlerts(
                userDetails.getUser().getUserId(),
                pageable
        );

        ApiResponse<PaginationResponse<SosAlertResponse>> response = ApiResponse.success(
                HttpStatus.OK,
                data,
                MessageConstant.ACTIVE_SOS_ALERTS_FETCHED
        );

        return ResponseEntity.ok(response);
    }

    @PutMapping("/sos/{sosId}/resolve")
    @Operation(summary = "Giải quyết yêu cầu cứu hộ SOS", description = "Cho phép Vendor Manager (thuộc Vendor quản lý) hoặc Hướng dẫn viên (được phân công dẫn đoàn) giải quyết yêu cầu cứu hộ SOS.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('VENDOR_MANAGER', 'COORDINATOR')")
    public ResponseEntity<ApiResponse<SosAlertResponse>> resolveSosAlert(
            @PathVariable UUID sosId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        SosAlertResponse data = trackingService.resolveSosAlert(
                userDetails.getUser().getUserId(),
                sosId
        );

        ApiResponse<SosAlertResponse> response = ApiResponse.success(
                HttpStatus.OK,
                data,
                MessageConstant.SOS_ALERT_RESOLVED_SUCCESS
        );

        return ResponseEntity.ok(response);
    }
}
