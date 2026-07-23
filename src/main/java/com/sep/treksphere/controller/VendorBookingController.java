package com.sep.treksphere.controller;

import com.sep.treksphere.constant.MessageConstant;
import com.sep.treksphere.dto.request.VendorBookingFilterRequest;
import com.sep.treksphere.dto.response.ApiResponse;
import com.sep.treksphere.dto.response.BookingDetailResponse;
import com.sep.treksphere.dto.response.BookingResponse;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.security.CustomUserDetails;
import com.sep.treksphere.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vendor/bookings")
@RequiredArgsConstructor
@Tag(name = "Vendor Booking Management", description = "Các API dành cho Vendor (Manager & Staff) quản lý Booking và Thanh toán")
@SecurityRequirement(name = "bearerAuth")
public class VendorBookingController {

    private final BookingService bookingService;

    @Operation(summary = "Quản lý danh sách đặt tour thuộc Vendor quản lý", description = "Lấy danh sách các đơn đặt tour với các bộ lọc phân trang, trạng thái, tour, từ khoá")
    @PreAuthorize("hasAnyRole('VENDOR_MANAGER', 'VENDOR_STAFF')")
    @GetMapping
    public ResponseEntity<ApiResponse<PaginationResponse<BookingResponse>>> getVendorBookings(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @ParameterObject @ModelAttribute VendorBookingFilterRequest request
    ) {
        PaginationResponse<BookingResponse> result = bookingService.getVendorBookings(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, result));
    }

    @Operation(summary = "Xác nhận đã nhận tiền thanh toán thành công", description = "Chuyển trạng thái thanh toán của đơn hàng sang PAID")
    @PreAuthorize("hasAnyRole('VENDOR_MANAGER', 'VENDOR_STAFF')")
    @PutMapping("/{id}/confirm-payment")
    public ResponseEntity<ApiResponse<BookingDetailResponse>> confirmPayment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id
    ) {
        BookingDetailResponse result = bookingService.confirmVendorPayment(userDetails.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, result, MessageConstant.PAYMENT_CONFIRMED_SUCCESSFULLY));
    }

    @Operation(summary = "Xác nhận giữ chỗ chính thức cho đơn hàng", description = "Chuyển trạng thái đơn hàng sang CONFIRMED")
    @PreAuthorize("hasAnyRole('VENDOR_MANAGER', 'VENDOR_STAFF')")
    @PutMapping("/{id}/confirm-booking")
    public ResponseEntity<ApiResponse<BookingDetailResponse>> confirmBooking(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id
    ) {
        BookingDetailResponse result = bookingService.confirmVendorBooking(userDetails.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, result, MessageConstant.BOOKING_CONFIRMED_SUCCESSFULLY));
    }

    @Operation(summary = "Xác nhận đã hoàn tiền cho khách (Chỉ áp dụng cho đơn đã huỷ)", description = "Chuyển trạng thái thanh toán sang REFUNDED (Dành riêng cho VendorManager)")
    @PreAuthorize("hasRole('VENDOR_MANAGER')")
    @PutMapping("/{id}/refund")
    public ResponseEntity<ApiResponse<BookingDetailResponse>> confirmRefund(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id
    ) {
        BookingDetailResponse result = bookingService.confirmVendorRefund(userDetails.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, result, MessageConstant.REFUND_CONFIRMED_SUCCESSFULLY));
    }
}
