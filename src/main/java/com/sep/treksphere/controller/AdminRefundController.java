package com.sep.treksphere.controller;

import com.sep.treksphere.dto.request.AdminManualRefundReviewRequest;
import com.sep.treksphere.dto.response.ApiResponse;
import com.sep.treksphere.dto.response.RefundTransactionResponse;
import com.sep.treksphere.enums.booking.RefundStatus;
import com.sep.treksphere.security.CustomUserDetails;
import com.sep.treksphere.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/refunds")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminRefundController {
    private final PaymentService paymentService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RefundTransactionResponse>>> getRefunds(
            @RequestParam(required = false) RefundStatus status) {
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK,
                paymentService.getAdminRefunds(status)));
    }

    @PostMapping("/{refundId}/review")
    public ResponseEntity<ApiResponse<RefundTransactionResponse>> reviewManualRefund(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID refundId,
            @Valid @RequestBody AdminManualRefundReviewRequest request) {
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK,
                paymentService.reviewManualRefund(user.getUsername(), refundId, request)));
    }
}
