package com.sep.treksphere.controller;

import com.sep.treksphere.dto.request.VendorDashboardFilterRequest;
import com.sep.treksphere.dto.response.ApiResponse;
import com.sep.treksphere.dto.response.dashboard.*;
import com.sep.treksphere.security.CustomUserDetails;
import com.sep.treksphere.service.VendorDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vendor/dashboard")
@RequiredArgsConstructor
@Tag(name = "Vendor Dashboard & Analytics", description = "Các API thống kê, báo cáo chỉ số lấp đầy tour trekking và quản lý chuyến đi cho Vendor")
@SecurityRequirement(name = "bearerAuth")
public class VendorDashboardController {

    private final VendorDashboardService vendorDashboardService;

    @Operation(summary = "Lấy 4 thẻ chỉ số KPI tổng quan (Doanh thu, Khách đã đặt, Tỷ lệ lấp đầy TB, Tỷ lệ hủy)",
            description = "Bao gồm so sánh % tăng/giảm với kỳ trước đó.")
    @PreAuthorize("hasAnyRole('VENDOR_MANAGER', 'VENDOR_STAFF')")
    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<VendorDashboardOverviewResponse>> getOverview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @ParameterObject VendorDashboardFilterRequest request
    ) {
        VendorDashboardOverviewResponse response = vendorDashboardService.getOverview(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response, "Lấy chỉ số tổng quan dashboard thành công"));
    }

    @Operation(summary = "Lấy dữ liệu biểu đồ doanh thu theo ngày/tháng",
            description = "Trực quan hóa doanh thu theo mốc thời gian.")
    @PreAuthorize("hasAnyRole('VENDOR_MANAGER', 'VENDOR_STAFF')")
    @GetMapping("/revenue-chart")
    public ResponseEntity<ApiResponse<RevenueChartResponse>> getRevenueChart(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @ParameterObject VendorDashboardFilterRequest request
    ) {
        RevenueChartResponse response = vendorDashboardService.getRevenueChart(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response, "Lấy dữ liệu biểu đồ doanh thu thành công"));
    }

    @Operation(summary = "Lấy danh sách Top tour bán chạy nhất",
            description = "Top các tour có lượng khách đăng ký cao nhất.")
    @PreAuthorize("hasAnyRole('VENDOR_MANAGER', 'VENDOR_STAFF')")
    @GetMapping("/top-tours")
    public ResponseEntity<ApiResponse<List<TopSellingTourResponse>>> getTopTours(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @ParameterObject VendorDashboardFilterRequest request,
            @RequestParam(defaultValue = "5") int limit
    ) {
        List<TopSellingTourResponse> response = vendorDashboardService.getTopTours(userDetails.getUsername(), request, limit);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response, "Lấy danh sách top tour bán chạy thành công"));
    }

    @Operation(summary = "Lấy lịch khởi hành sắp tới & Tỷ lệ lấp đầy (Cảnh báo màu Xanh/Vàng/Đỏ)",
            description = "Xem danh sách các chuyến đi sắp tới kèm màu cảnh báo nguy cơ hủy tour vì thiếu người.")
    @PreAuthorize("hasAnyRole('VENDOR_MANAGER', 'VENDOR_STAFF')")
    @GetMapping("/upcoming-schedules")
    public ResponseEntity<ApiResponse<List<UpcomingScheduleResponse>>> getUpcomingSchedules(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) Integer daysAhead
    ) {
        List<UpcomingScheduleResponse> response = vendorDashboardService.getUpcomingSchedules(userDetails.getUsername(), limit, daysAhead);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response, "Lấy danh sách lịch khởi hành sắp tới thành công"));
    }

    @Operation(summary = "Cảnh báo chuyến đi dưới ngưỡng tối thiểu (Alert Banner)",
            description = "Lấy danh sách các chuyến khởi hành trong X ngày tới nhưng chưa đạt đủ minCapacity.")
    @PreAuthorize("hasAnyRole('VENDOR_MANAGER', 'VENDOR_STAFF')")
    @GetMapping("/under-capacity-alerts")
    public ResponseEntity<ApiResponse<List<UnderCapacityAlertResponse>>> getUnderCapacityAlerts(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "7") Integer alertDaysThreshold
    ) {
        List<UnderCapacityAlertResponse> response = vendorDashboardService.getUnderCapacityAlerts(userDetails.getUsername(), alertDaysThreshold);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response, "Lấy danh sách cảnh báo thiếu chỗ thành công"));
    }

    @Operation(summary = "Xem chi tiết 1 chuyến khởi hành (Danh sách khách & Trạng thái đóng tiền/cọc)",
            description = "Xem danh sách thông tin khách hàng tham gia từng chuyến đi cụ thể.")
    @PreAuthorize("hasAnyRole('VENDOR_MANAGER', 'VENDOR_STAFF')")
    @GetMapping("/schedules/{scheduleId}/manifest")
    public ResponseEntity<ApiResponse<ScheduleManifestResponse>> getScheduleManifest(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID scheduleId
    ) {
        ScheduleManifestResponse response = vendorDashboardService.getScheduleManifest(userDetails.getUsername(), scheduleId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response, "Lấy chi tiết danh sách khách của chuyến đi thành công"));
    }
}
