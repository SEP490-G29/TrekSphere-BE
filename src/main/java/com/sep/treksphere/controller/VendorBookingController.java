package com.sep.treksphere.controller;

import com.sep.treksphere.constant.MessageConstant;
import com.sep.treksphere.dto.request.BookingCancelRequest;
import com.sep.treksphere.dto.request.VendorBookingFilterRequest;
import com.sep.treksphere.dto.request.VendorBookingCancelRequest;
import com.sep.treksphere.dto.response.ApiResponse;
import com.sep.treksphere.dto.response.BookingDetailResponse;
import com.sep.treksphere.dto.response.BookingResponse;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.security.CustomUserDetails;
import com.sep.treksphere.service.BookingService;
import com.sep.treksphere.service.PaymentService;
import com.sep.treksphere.service.CancellationService;
import com.sep.treksphere.dto.request.ManualRefundCompletionRequest;
import com.sep.treksphere.dto.response.RefundTransactionResponse;
import com.sep.treksphere.enums.booking.RefundReason;
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
    private final PaymentService paymentService;
    private final CancellationService cancellationService;

    @Operation(summary = "Quản lý danh sách đặt tour thuộc Vendor quản lý", description = "Lấy danh sách các đơn đặt tour với các bộ lọc phân trang, trạng thái, tour, từ khoá")
    @PreAuthorize("hasAnyRole('VENDOR_MANAGER', 'VENDOR_STAFF')")
    @GetMapping
    public ResponseEntity<ApiResponse<PaginationResponse<BookingResponse>>> getVendorBookings(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @ParameterObject @ModelAttribute VendorBookingFilterRequest request) {
        PaginationResponse<BookingResponse> result = bookingService.getVendorBookings(userDetails.getUsername(),
                request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, result));
    }

    @Operation(summary = "Xác nhận giữ chỗ chính thức cho đơn hàng", description = "Chuyển trạng thái đơn hàng sang CONFIRMED")
    @PreAuthorize("hasAnyRole('VENDOR_MANAGER', 'VENDOR_STAFF')")
    @PutMapping("/{id}/confirm-booking")
    public ResponseEntity<ApiResponse<BookingDetailResponse>> confirmBooking(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id) {
        BookingDetailResponse result = bookingService.confirmVendorBooking(userDetails.getUsername(), id);
        return ResponseEntity
                .ok(ApiResponse.success(HttpStatus.OK, result, MessageConstant.BOOKING_CONFIRMED_SUCCESSFULLY));
    }

    @Operation(summary = "Vendor từ chối hoặc hủy booking", description = "Hoàn 100% số tiền đã thu; dùng VENDOR_CANCEL hoặc INSUFFICIENT_PAX")
    @PreAuthorize("hasAnyRole('VENDOR_MANAGER', 'VENDOR_STAFF')")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<BookingDetailResponse>> cancelBooking(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id,
            @Valid @RequestBody VendorBookingCancelRequest request) {
        cancellationService.cancelByVendor(userDetails.getUsername(), id, request);
        BookingDetailResponse result = bookingService.getBookingDetail(userDetails.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, result));
    }

    @Operation(summary = "Gửi refund qua payOS Payout", description = "Chỉ Vendor Manager; request có idempotency và được đối soát tự động")
    @PreAuthorize("hasRole('VENDOR_MANAGER')")
    @PostMapping("/refunds/{refundId}/process")
    public ResponseEntity<ApiResponse<RefundTransactionResponse>> processRefund(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID refundId) {
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK,
                paymentService.processRefund(userDetails.getUsername(), refundId)));
    }

    @Operation(summary = "Gửi biên nhận hoàn tiền thủ công", description = "Ảnh biên nhận được admin đối soát; không thay đổi số tiền refund")
    @PreAuthorize("hasRole('VENDOR_MANAGER')")
    @PostMapping("/refunds/{refundId}/complete-manual")
    public ResponseEntity<ApiResponse<RefundTransactionResponse>> completeManualRefund(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID refundId,
            @Valid @RequestBody ManualRefundCompletionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK,
                paymentService.completeManualRefund(userDetails.getUsername(), refundId, request)));
    }

    @Operation(summary = "Từ chối / Hủy đơn đặt tour của khách", description = "Vendor từ chối đơn hàng (ví dụ: ảnh chuyển khoản không hợp lệ, sai thông tin). Hoàn trả slot và voucher.")
    @PreAuthorize("hasAnyRole('VENDOR_MANAGER', 'VENDOR_STAFF')")
    @PutMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<BookingDetailResponse>> rejectBooking(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id,
            @Valid @RequestBody BookingCancelRequest request) {
        VendorBookingCancelRequest vendorRequest = new VendorBookingCancelRequest();
        vendorRequest.setReason(RefundReason.VENDOR_CANCEL);
        vendorRequest.setReasonDetail(request.getCancellationReason());
        cancellationService.cancelByVendor(userDetails.getUsername(), id, vendorRequest);
        BookingDetailResponse result = bookingService.getBookingDetail(userDetails.getUsername(), id);
        return ResponseEntity
                .ok(ApiResponse.success(HttpStatus.OK, result, MessageConstant.BOOKING_REJECTED_SUCCESSFULLY));
    }
}
