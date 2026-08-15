package com.sep.treksphere.service;

import com.sep.treksphere.dto.request.ManualRefundCompletionRequest;
import com.sep.treksphere.dto.request.AdminManualRefundReviewRequest;
import com.sep.treksphere.enums.booking.RefundStatus;
import com.sep.treksphere.dto.request.RefundDestinationRequest;
import com.sep.treksphere.dto.response.PaymentCheckoutResponse;
import com.sep.treksphere.dto.response.PaymentTransactionResponse;
import com.sep.treksphere.dto.response.RefundTransactionResponse;
import vn.payos.model.webhooks.Webhook;

import java.util.List;
import java.util.UUID;

public interface PaymentService {
    PaymentCheckoutResponse createCheckout(String email, UUID bookingId);
    PaymentTransactionResponse cancelCheckout(String email, UUID bookingId, Long orderCode);
    List<PaymentTransactionResponse> getBookingPayments(String email, UUID bookingId);
    List<RefundTransactionResponse> getBookingRefunds(String email, UUID bookingId);
    void handlePayOsWebhook(String channelId, Webhook webhook);
    RefundTransactionResponse processRefund(String email, UUID refundId);
    RefundTransactionResponse processRefundAutomatically(UUID refundId);
    RefundTransactionResponse completeManualRefund(String email, UUID refundId, ManualRefundCompletionRequest request);
    RefundTransactionResponse reviewManualRefund(String email, UUID refundId, AdminManualRefundReviewRequest request);
    List<RefundTransactionResponse> getAdminRefunds(RefundStatus status);
    RefundTransactionResponse updateRefundDestination(String email, UUID refundId, RefundDestinationRequest request);
}
