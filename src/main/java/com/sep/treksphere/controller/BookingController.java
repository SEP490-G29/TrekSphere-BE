package com.sep.treksphere.controller;

import com.sep.treksphere.constant.MessageConstant;
import com.sep.treksphere.dto.request.BookingCancelRequest;
import com.sep.treksphere.dto.request.BookingRequest;
import com.sep.treksphere.dto.request.PaymentProofRequest;
import com.sep.treksphere.dto.response.ApiResponse;
import com.sep.treksphere.dto.response.BookingDetailResponse;
import com.sep.treksphere.dto.response.BookingResponse;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.dto.request.BookingHistoryRequest;
import com.sep.treksphere.enums.booking.BookingStatus;
import com.sep.treksphere.security.CustomUserDetails;
import com.sep.treksphere.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Tag(name = "Booking Management", description = "Các API dành cho việc Đặt Tour và quản lý Booking")
@SecurityRequirement(name = "bearerAuth")
public class BookingController {

    private final BookingService bookingService;

    @Operation(summary = "Xem lịch sử đặt tour của cá nhân (Trekker)", description = "Lấy danh sách các đơn đặt tour đã thực hiện bởi Trekker hiện tại")
    @PreAuthorize("hasRole('TREKKER')")
    @GetMapping("/my-history")
    public ResponseEntity<ApiResponse<PaginationResponse<BookingResponse>>> getMyHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @ParameterObject @ModelAttribute BookingHistoryRequest request
    ) {
        PaginationResponse<BookingResponse> result = bookingService.getMyBookingHistory(
                userDetails.getUsername(), request.getStatus(), request.getPageable());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, result));
    }

    @Operation(summary = "Xem chi tiết đơn đặt tour", description = "Xem thông tin chi tiết một đơn đặt tour dựa vào UUID")
    @PreAuthorize("hasAnyRole('TREKKER', 'VENDOR_STAFF', 'VENDOR_MANAGER')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookingDetailResponse>> getBookingDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id
    ) {
        BookingDetailResponse result = bookingService.getBookingDetail(userDetails.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, result));
    }

    @Operation(summary = "Đặt tour", description = "Tạo đơn đặt tour mới kèm danh sách thành viên và mã giảm giá")
    @PreAuthorize("hasRole('TREKKER')")
    @PostMapping
    public ResponseEntity<ApiResponse<BookingDetailResponse>> createBooking(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody BookingRequest request
    ) {
        BookingDetailResponse result = bookingService.createBooking(userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, result, MessageConstant.BOOKING_CREATED_SUCCESSFULLY));
    }

    @Operation(summary = "Gửi yêu cầu hủy tour", description = "Hủy đặt tour và tự động tính toán số tiền hoàn dựa vào chính sách")
    @PreAuthorize("hasRole('TREKKER')")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<BookingDetailResponse>> cancelBooking(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id,
            @Valid @RequestBody BookingCancelRequest request
    ) {
        BookingDetailResponse result = bookingService.cancelBooking(userDetails.getUsername(), id, request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, result, MessageConstant.BOOKING_CANCELLED_SUCCESSFULLY));
    }

    @Operation(summary = "Gửi minh chứng chuyển khoản ngân hàng", description = "Cập nhật ảnh chụp giao dịch thanh toán cho đơn hàng đang chờ")
    @PreAuthorize("hasRole('TREKKER')")
    @PutMapping("/{id}/payment-proof")
    public ResponseEntity<ApiResponse<BookingDetailResponse>> submitPaymentProof(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id,
            @Valid @RequestBody PaymentProofRequest request
    ) {
        BookingDetailResponse result = bookingService.submitPaymentProof(userDetails.getUsername(), id, request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, result, MessageConstant.PAYMENT_PROOF_SUBMITTED));
    }
}
