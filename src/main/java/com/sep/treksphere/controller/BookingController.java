package com.sep.treksphere.controller;

import com.sep.treksphere.constant.MessageConstant;
import com.sep.treksphere.dto.request.BookingCancelRequest;
import com.sep.treksphere.dto.request.BookingRequest;
import com.sep.treksphere.dto.request.RefundDestinationRequest;
import com.sep.treksphere.dto.response.ApiResponse;
import com.sep.treksphere.dto.response.BookingDetailResponse;
import com.sep.treksphere.dto.response.BookingResponse;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.dto.response.PaymentCheckoutResponse;
import com.sep.treksphere.dto.response.PaymentTransactionResponse;
import com.sep.treksphere.dto.response.RefundTransactionResponse;
import com.sep.treksphere.dto.response.CancellationQuoteResponse;
import com.sep.treksphere.dto.request.BookingHistoryRequest;
import com.sep.treksphere.enums.booking.BookingStatus;
import com.sep.treksphere.security.CustomUserDetails;
import com.sep.treksphere.service.BookingService;
import com.sep.treksphere.service.PaymentService;
import com.sep.treksphere.service.CancellationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Tag(name = "Booking Management", description = "Các API dành cho việc Đặt Tour và quản lý Booking")
@SecurityRequirement(name = "bearerAuth")
public class BookingController {

        private final BookingService bookingService;
        private final PaymentService paymentService;
        private final CancellationService cancellationService;

        @Operation(summary = "Xem lịch sử đặt tour của cá nhân (Trekker)", description = "Lấy danh sách các đơn đặt tour đã thực hiện bởi Trekker hiện tại")
        @PreAuthorize("hasRole('TREKKER')")
        @GetMapping("/my-history")
        public ResponseEntity<ApiResponse<PaginationResponse<BookingResponse>>> getMyHistory(
                        @AuthenticationPrincipal CustomUserDetails userDetails,
                        @Valid @ParameterObject @ModelAttribute BookingHistoryRequest request) {
                PaginationResponse<BookingResponse> result = bookingService.getMyBookingHistory(
                                userDetails.getUsername(), request.getStatus(), request.getPageable());
                return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, result));
        }

        @Operation(summary = "Xem chi tiết đơn đặt tour", description = "Xem thông tin chi tiết một đơn đặt tour dựa vào UUID")
        @PreAuthorize("hasAnyRole('TREKKER', 'VENDOR_STAFF', 'VENDOR_MANAGER')")
        @GetMapping("/{id}")
        public ResponseEntity<ApiResponse<BookingDetailResponse>> getBookingDetail(
                        @AuthenticationPrincipal CustomUserDetails userDetails,
                        @PathVariable UUID id) {
                BookingDetailResponse result = bookingService.getBookingDetail(userDetails.getUsername(), id);
                return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, result));
        }

        @Operation(summary = "Đặt tour", description = "Tạo đơn đặt tour mới kèm danh sách thành viên và mã giảm giá")
        @PreAuthorize("hasRole('TREKKER')")
        @PostMapping
        public ResponseEntity<ApiResponse<BookingDetailResponse>> createBooking(
                        @AuthenticationPrincipal CustomUserDetails userDetails,
                        @RequestHeader("Idempotency-Key") String idempotencyKey,
                        @Valid @RequestBody BookingRequest request) {
                BookingDetailResponse result = bookingService.createBooking(userDetails.getUsername(), idempotencyKey,
                                request);
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponse.success(HttpStatus.CREATED, result,
                                                MessageConstant.BOOKING_CREATED_SUCCESSFULLY));
        }

        @Operation(summary = "Gửi yêu cầu hủy tour", description = "Hủy đặt tour và tự động tính toán số tiền hoàn dựa vào chính sách")
        @PreAuthorize("hasRole('TREKKER')")
        @PostMapping("/{id}/cancel")
        public ResponseEntity<ApiResponse<BookingDetailResponse>> cancelBooking(
                        @AuthenticationPrincipal CustomUserDetails userDetails,
                        @PathVariable UUID id,
                        @Valid @RequestBody BookingCancelRequest request) {
                BookingDetailResponse result = bookingService.cancelBooking(userDetails.getUsername(), id, request);
                return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, result,
                                MessageConstant.BOOKING_CANCELLED_SUCCESSFULLY));
        }

        @Operation(summary = "Xem trước số tiền hoàn khi hủy", description = "Tính theo policy snapshot lúc booking, chưa thay đổi trạng thái")
        @PreAuthorize("hasRole('TREKKER')")
        @GetMapping("/{id}/cancellation-quote")
        public ResponseEntity<ApiResponse<CancellationQuoteResponse>> getCancellationQuote(
                        @AuthenticationPrincipal CustomUserDetails userDetails,
                        @PathVariable UUID id) {
                return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK,
                                cancellationService.quoteForTrekker(userDetails.getUsername(), id)));
        }

        @Operation(summary = "Tạo hoặc lấy checkout đang hoạt động")
        @PreAuthorize("hasRole('TREKKER')")
        @PostMapping("/{id}/payments/checkout")
        public ResponseEntity<ApiResponse<PaymentCheckoutResponse>> createCheckout(
                        @AuthenticationPrincipal CustomUserDetails userDetails,
                        @PathVariable UUID id) {
                return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK,
                                paymentService.createCheckout(userDetails.getUsername(), id)));
        }

        @Operation(summary = "Hủy phiên checkout và đồng bộ trạng thái với payOS")
        @PreAuthorize("hasRole('TREKKER')")
        @PostMapping("/{id}/payments/checkout/cancel")
        public ResponseEntity<ApiResponse<PaymentTransactionResponse>> cancelCheckout(
                        @AuthenticationPrincipal CustomUserDetails userDetails,
                        @PathVariable UUID id,
                        @RequestParam Long orderCode) {
                return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK,
                                paymentService.cancelCheckout(userDetails.getUsername(), id, orderCode)));
        }

        @Operation(summary = "Danh sách giao dịch thanh toán của booking")
        @PreAuthorize("hasAnyRole('TREKKER', 'VENDOR_STAFF', 'VENDOR_MANAGER')")
        @GetMapping("/{id}/payments")
        public ResponseEntity<ApiResponse<List<PaymentTransactionResponse>>> getPayments(
                        @AuthenticationPrincipal CustomUserDetails userDetails,
                        @PathVariable UUID id) {
                return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK,
                                paymentService.getBookingPayments(userDetails.getUsername(), id)));
        }

        @Operation(summary = "Danh sách giao dịch hoàn tiền của booking")
        @PreAuthorize("hasAnyRole('TREKKER', 'VENDOR_STAFF', 'VENDOR_MANAGER')")
        @GetMapping("/{id}/refunds")
        public ResponseEntity<ApiResponse<List<RefundTransactionResponse>>> getRefunds(
                        @AuthenticationPrincipal CustomUserDetails userDetails,
                        @PathVariable UUID id) {
                return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK,
                                paymentService.getBookingRefunds(userDetails.getUsername(), id)));
        }

        @Operation(summary = "Cập nhật tài khoản nhận refund")
        @PreAuthorize("hasRole('TREKKER')")
        @PutMapping("/refunds/{refundId}/destination")
        public ResponseEntity<ApiResponse<RefundTransactionResponse>> updateRefundDestination(
                        @AuthenticationPrincipal CustomUserDetails userDetails,
                        @PathVariable UUID refundId,
                        @Valid @RequestBody RefundDestinationRequest request) {
                return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK,
                                paymentService.updateRefundDestination(userDetails.getUsername(), refundId, request)));
        }

}
