package com.sep.treksphere.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep.treksphere.config.PaymentWorkflowProperties;
import com.sep.treksphere.dto.request.ManualRefundCompletionRequest;
import com.sep.treksphere.dto.request.RefundDestinationRequest;
import com.sep.treksphere.dto.response.PaymentCheckoutResponse;
import com.sep.treksphere.dto.response.PaymentTransactionResponse;
import com.sep.treksphere.dto.response.RefundTransactionResponse;
import com.sep.treksphere.entity.*;
import com.sep.treksphere.enums.booking.*;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;
import com.sep.treksphere.repository.*;
import com.sep.treksphere.service.PaymentService;
import com.sep.treksphere.service.payment.PayOsClientFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import vn.payos.PayOS;
import vn.payos.model.v1.payouts.Payout;
import vn.payos.model.v1.payouts.PayoutApprovalState;
import vn.payos.model.v1.payouts.PayoutTransactionState;
import vn.payos.model.v1.payouts.batch.PayoutBatchItem;
import vn.payos.model.v1.payouts.batch.PayoutBatchRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.webhooks.Webhook;
import vn.payos.model.webhooks.WebhookData;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Set<PaymentTransactionStatus> LIVE_PAYMENT_STATUSES = EnumSet.of(
            PaymentTransactionStatus.CREATED,
            PaymentTransactionStatus.PENDING,
            PaymentTransactionStatus.PROCESSING,
            PaymentTransactionStatus.PAID
    );
    private static final Set<RefundStatus> ACTIVE_REFUND_STATUSES = EnumSet.of(
            RefundStatus.PENDING, RefundStatus.PROCESSING, RefundStatus.REFUNDED
    );

    private final BookingRepository bookingRepository;
    private final TourScheduleRepository tourScheduleRepository;
    private final VoucherRepository voucherRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentWebhookEventRepository webhookEventRepository;
    private final RefundTransactionRepository refundTransactionRepository;
    private final BookingPolicySnapshotRepository bookingPolicySnapshotRepository;
    private final VendorPaymentAccountRepository vendorPaymentAccountRepository;
    private final UserRepository userRepository;
    private final VendorRepository vendorRepository;
    private final VendorStaffRepository vendorStaffRepository;
    private final PayOsClientFactory payOsClientFactory;
    private final PaymentWorkflowProperties properties;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    private record PreparedCheckout(UUID transactionId, UUID bookingId, VendorPaymentAccount account,
                                    Long orderCode, long amount, String description,
                                    LocalDateTime expiresAt, PaymentCheckoutResponse existing) {}

    @Override
    public PaymentCheckoutResponse createCheckout(String email, UUID bookingId) {
        PreparedCheckout prepared = transactionTemplate.execute(status -> prepareCheckout(email, bookingId));
        if (prepared == null) {
            throw new AppException(ErrorCode.PAYMENT_GATEWAY_ERROR);
        }
        if (prepared.existing() != null) {
            return prepared.existing();
        }

        PayOS client = payOsClientFactory.getClient(prepared.account());
        String returnUrl = withBookingId(properties.getReturnUrl(), prepared.bookingId());
        String cancelUrl = withBookingId(properties.getCancelUrl(), prepared.bookingId());
        CreatePaymentLinkRequest request = CreatePaymentLinkRequest.builder()
                .orderCode(prepared.orderCode())
                .amount(prepared.amount())
                .description(prepared.description())
                .returnUrl(returnUrl)
                .cancelUrl(cancelUrl)
                .expiredAt(prepared.expiresAt().atZone(BUSINESS_ZONE).toEpochSecond())
                .build();

        try {
            CreatePaymentLinkResponse gatewayResponse = client.paymentRequests().create(request);
            return transactionTemplate.execute(status -> completeCheckout(prepared.transactionId(), gatewayResponse));
        } catch (Exception exception) {
            log.error("payOS create checkout failed for transaction {}", prepared.transactionId(), exception);
            transactionTemplate.executeWithoutResult(status -> markPaymentFailed(
                    prepared.transactionId(), "PAYOS_CREATE_FAILED", safeMessage(exception)));
            throw new AppException(ErrorCode.PAYMENT_GATEWAY_ERROR);
        }
    }

    private PreparedCheckout prepareCheckout(String email, UUID bookingId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
        if (!booking.getUser().getUserId().equals(user.getUserId())) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        PaymentStage stage = resolveNextStage(booking);
        Optional<PaymentTransaction> existing = paymentTransactionRepository
                .findFirstByBooking_BookingIdAndPaymentStageAndStatusInAndIsDeletedFalseOrderByAttemptNumberDesc(
                        bookingId, stage, LIVE_PAYMENT_STATUSES);
        if (existing.isPresent()) {
            PaymentTransaction transaction = existing.get();
            if (transaction.getStatus() == PaymentTransactionStatus.PAID ||
                    (transaction.getExpiredAt() != null && transaction.getExpiredAt().isAfter(LocalDateTime.now()))) {
                return new PreparedCheckout(null, bookingId, null, null, 0, null, null, toCheckoutResponse(transaction));
            }
            transaction.setStatus(PaymentTransactionStatus.EXPIRED);
            paymentTransactionRepository.save(transaction);
        }

        VendorPaymentAccount account = vendorPaymentAccountRepository
                .findByVendor_VendorIdAndProviderAndOnboardingStatusAndIsDefaultTrueAndIsDeletedFalse(
                        booking.getSchedule().getTour().getVendor().getVendorId(),
                        PaymentProvider.PAYOS,
                        PaymentAccountStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_ACCOUNT_NOT_CONFIGURED));

        BigDecimal amount = calculateStageAmount(booking, stage).setScale(0, RoundingMode.HALF_UP);
        if (amount.signum() <= 0) {
            throw new AppException(ErrorCode.PAYMENT_NOT_ALLOWED, "Booking không còn số tiền cần thanh toán.");
        }

        short attempt = paymentTransactionRepository
                .findFirstByBooking_BookingIdAndPaymentStageAndIsDeletedFalseOrderByAttemptNumberDesc(bookingId, stage)
                .map(last -> (short) (last.getAttemptNumber() + 1))
                .orElse((short) 1);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plus(properties.getCheckoutLinkDuration());
        if (stage != PaymentStage.REMAINING && booking.getHoldExpiresAt() != null
                && booking.getHoldExpiresAt().isBefore(expiresAt)) {
            expiresAt = booking.getHoldExpiresAt();
        }
        if (!expiresAt.isAfter(now)) {
            throw new AppException(ErrorCode.PAYMENT_NOT_ALLOWED, "Thời gian giữ chỗ đã hết.");
        }

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setBooking(booking);
        transaction.setVendorPaymentAccount(account);
        transaction.setPaymentStage(stage);
        transaction.setAttemptNumber(attempt);
        transaction.setProvider(PaymentProvider.PAYOS);
        transaction.setGatewayOrderCode(paymentTransactionRepository.nextGatewayOrderCode());
        transaction.setIdempotencyKey("payment:" + bookingId + ":" + stage + ":" + attempt);
        transaction.setAmount(amount);
        transaction.setExpiredAt(expiresAt);
        transaction.setStatus(PaymentTransactionStatus.CREATED);
        paymentTransactionRepository.saveAndFlush(transaction);

        String description = ("TS " + booking.getBookingCode()).replace("-", "");
        if (description.length() > 25) description = description.substring(0, 25);
        return new PreparedCheckout(transaction.getPaymentTransactionId(), bookingId, account,
                transaction.getGatewayOrderCode(), amount.longValueExact(), description, expiresAt, null);
    }

    private PaymentStage resolveNextStage(Booking booking) {
        if (booking.getBookingStatus() == BookingStatus.PAYMENT_PENDING
                && booking.getPaymentStatus() == PaymentStatus.UNPAID) {
            return booking.getPaymentPlan() == PaymentPlan.DEPOSIT ? PaymentStage.DEPOSIT : PaymentStage.FULL;
        }
        if (booking.getPaymentPlan() == PaymentPlan.DEPOSIT
                && booking.getPaymentStatus() == PaymentStatus.PARTIALLY_PAID
                && EnumSet.of(BookingStatus.PENDING_CONFIRMATION, BookingStatus.CONFIRMED).contains(booking.getBookingStatus())) {
            if (booking.getRemainingDueAt() != null && LocalDateTime.now().isAfter(booking.getRemainingDueAt())) {
                throw new AppException(ErrorCode.PAYMENT_NOT_ALLOWED, "Đã quá hạn thanh toán phần còn lại.");
            }
            return PaymentStage.REMAINING;
        }
        throw new AppException(ErrorCode.PAYMENT_NOT_ALLOWED);
    }

    private BigDecimal calculateStageAmount(Booking booking, PaymentStage stage) {
        if (stage == PaymentStage.FULL) return booking.getTotalPrice();
        if (stage == PaymentStage.REMAINING) {
            return booking.getTotalPrice().subtract(paymentTransactionRepository.sumPaidByBooking(booking.getBookingId()));
        }
        BookingPolicySnapshot snapshot = bookingPolicySnapshotRepository.findById(booking.getBookingId())
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_ALLOWED, "Thiếu snapshot chính sách thanh toán."));
        Map<String, Object> policy = snapshot.getPaymentPolicyJson();
        String type = Objects.toString(policy.get("depositType"), "");
        BigDecimal value = decimal(policy.get("depositValue"));
        BigDecimal deposit = "PERCENTAGE".equals(type)
                ? booking.getTotalPrice().multiply(value).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                : value;
        if (deposit.signum() <= 0 || deposit.compareTo(booking.getTotalPrice()) >= 0) {
            throw new AppException(ErrorCode.PAYMENT_NOT_ALLOWED, "Cấu hình tiền cọc không hợp lệ.");
        }
        return deposit;
    }

    private PaymentCheckoutResponse completeCheckout(UUID transactionId, CreatePaymentLinkResponse response) {
        PaymentTransaction transaction = paymentTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_TRANSACTION_NOT_FOUND));
        if (!Objects.equals(transaction.getGatewayOrderCode(), response.getOrderCode())
                || transaction.getAmount().compareTo(BigDecimal.valueOf(response.getAmount())) != 0) {
            transaction.setStatus(PaymentTransactionStatus.FAILED);
            transaction.setFailureCode("PAYOS_RESPONSE_MISMATCH");
            paymentTransactionRepository.save(transaction);
            throw new AppException(ErrorCode.PAYMENT_GATEWAY_ERROR);
        }
        transaction.setGatewayPaymentLinkId(response.getPaymentLinkId());
        transaction.setCheckoutUrl(response.getCheckoutUrl());
        transaction.setQrCode(response.getQrCode());
        transaction.setCurrency(response.getCurrency());
        transaction.setStatus(PaymentTransactionStatus.PENDING);
        paymentTransactionRepository.save(transaction);
        return toCheckoutResponse(transaction);
    }

    private void markPaymentFailed(UUID transactionId, String code, String message) {
        paymentTransactionRepository.findById(transactionId).ifPresent(transaction -> {
            transaction.setStatus(PaymentTransactionStatus.FAILED);
            transaction.setFailureCode(code);
            transaction.setFailureMessage(message);
            paymentTransactionRepository.save(transaction);
        });
    }

    @Override
    public void handlePayOsWebhook(String channelId, Webhook webhook) {
        if (webhook == null || webhook.getData() == null || webhook.getData().getOrderCode() == null) {
            throw new AppException(ErrorCode.INVALID_PAYMENT_WEBHOOK);
        }
        VendorPaymentAccount account = vendorPaymentAccountRepository
                .findByProviderAndProviderChannelIdAndOnboardingStatusAndIsDeletedFalse(
                        PaymentProvider.PAYOS, channelId, PaymentAccountStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_ACCOUNT_NOT_CONFIGURED));
        PayOS client = payOsClientFactory.getClient(account);
        final WebhookData verified;
        try {
            verified = client.webhooks().verify(webhook);
        } catch (Exception exception) {
            log.warn("Rejected payOS webhook for order {}", webhook.getData().getOrderCode());
            throw new AppException(ErrorCode.INVALID_PAYMENT_WEBHOOK);
        }
        Optional<PaymentTransaction> lookup = paymentTransactionRepository
                .findByGatewayOrderCodeAndIsDeletedFalse(verified.getOrderCode());
        if (lookup.isEmpty()) {
            transactionTemplate.executeWithoutResult(status -> storeIgnoredWebhook(webhook, verified));
            return;
        }
        if (!lookup.get().getVendorPaymentAccount().getVendorPaymentAccountId()
                .equals(account.getVendorPaymentAccountId())) {
            throw new AppException(ErrorCode.INVALID_PAYMENT_WEBHOOK);
        }
        transactionTemplate.executeWithoutResult(status -> processVerifiedWebhook(webhook, verified));
    }

    private void storeIgnoredWebhook(Webhook webhook, WebhookData data) {
        String eventKey = data.getOrderCode() + ":" + Objects.toString(data.getReference(), "NO_REFERENCE");
        if (webhookEventRepository.existsByProviderAndGatewayEventKey(PaymentProvider.PAYOS, eventKey)) return;
        PaymentWebhookEvent event = new PaymentWebhookEvent();
        event.setProvider(PaymentProvider.PAYOS);
        event.setGatewayEventKey(eventKey);
        event.setGatewayOrderCode(data.getOrderCode());
        event.setGatewayPaymentLinkId(data.getPaymentLinkId());
        event.setGatewayReference(data.getReference());
        event.setSignature(webhook.getSignature());
        event.setPayload(objectMapper.convertValue(webhook, new TypeReference<>() {}));
        event.setProcessingStatus(PaymentWebhookEvent.ProcessingStatus.IGNORED);
        event.setErrorMessage("No TrekSphere payment transaction matched this signed webhook");
        event.setProcessedAt(LocalDateTime.now());
        webhookEventRepository.save(event);
    }

    private void processVerifiedWebhook(Webhook webhook, WebhookData data) {
        PaymentTransaction payment = paymentTransactionRepository.findByGatewayOrderCodeForUpdate(data.getOrderCode())
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_TRANSACTION_NOT_FOUND));
        String eventKey = data.getOrderCode() + ":" + Objects.toString(data.getReference(), "NO_REFERENCE");
        if (webhookEventRepository.existsByProviderAndGatewayEventKey(PaymentProvider.PAYOS, eventKey)) {
            return;
        }

        PaymentWebhookEvent event = new PaymentWebhookEvent();
        event.setPaymentTransaction(payment);
        event.setProvider(PaymentProvider.PAYOS);
        event.setGatewayEventKey(eventKey);
        event.setGatewayOrderCode(data.getOrderCode());
        event.setGatewayPaymentLinkId(data.getPaymentLinkId());
        event.setGatewayReference(data.getReference());
        event.setSignature(webhook.getSignature());
        event.setPayload(objectMapper.convertValue(webhook, new TypeReference<>() {}));

        if (!Boolean.TRUE.equals(webhook.getSuccess()) || !"00".equals(data.getCode())) {
            event.setProcessingStatus(PaymentWebhookEvent.ProcessingStatus.IGNORED);
            event.setProcessedAt(LocalDateTime.now());
            webhookEventRepository.save(event);
            return;
        }
        if (!Objects.equals(payment.getGatewayPaymentLinkId(), data.getPaymentLinkId())
                || payment.getAmount().compareTo(BigDecimal.valueOf(data.getAmount())) != 0
                || !payment.getCurrency().equalsIgnoreCase(data.getCurrency())) {
            event.setProcessingStatus(PaymentWebhookEvent.ProcessingStatus.FAILED);
            event.setErrorMessage("Webhook amount, currency, or paymentLinkId does not match the payment transaction");
            webhookEventRepository.save(event);
            throw new AppException(ErrorCode.INVALID_PAYMENT_WEBHOOK);
        }

        if (payment.getStatus() != PaymentTransactionStatus.PAID) {
            payment.setStatus(PaymentTransactionStatus.PAID);
            payment.setPaidAmount(payment.getAmount());
            payment.setPaidAt(LocalDateTime.now());
            payment.setGatewayReference(data.getReference());
            payment.getGatewayMetadata().put("counterAccountBankId", data.getCounterAccountBankId());
            payment.getGatewayMetadata().put("counterAccountNumber", data.getCounterAccountNumber());
            payment.getGatewayMetadata().put("counterAccountName", data.getCounterAccountName());
            paymentTransactionRepository.saveAndFlush(payment);
            applySuccessfulPayment(payment);
        }

        event.setProcessingStatus(PaymentWebhookEvent.ProcessingStatus.PROCESSED);
        event.setProcessedAt(LocalDateTime.now());
        try {
            webhookEventRepository.save(event);
        } catch (DataIntegrityViolationException duplicate) {
            log.debug("Duplicate payOS webhook ignored: {}", eventKey);
        }
    }

    private void applySuccessfulPayment(PaymentTransaction payment) {
        Booking booking = bookingRepository.findByIdForUpdate(payment.getBooking().getBookingId())
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
        BigDecimal totalPaid = paymentTransactionRepository.sumPaidByBooking(booking.getBookingId());

        if (payment.getPaymentStage() != PaymentStage.REMAINING) {
            if (booking.getBookingStatus() != BookingStatus.PAYMENT_PENDING
                    || booking.getHoldExpiresAt() == null
                    || !booking.getHoldExpiresAt().isAfter(LocalDateTime.now())) {
                createLatePaymentRefund(payment, booking);
                return;
            }
            TourSchedule schedule = tourScheduleRepository.findByIdForUpdate(booking.getSchedule().getScheduleId())
                    .orElseThrow(() -> new AppException(ErrorCode.SCHEDULE_NOT_FOUND));
            int pax = booking.getNumberOfParticipants();
            schedule.setHeldSlots(Math.max(0, schedule.getHeldSlots() - pax));
            schedule.setBookedSlots(schedule.getBookedSlots() + pax);
            tourScheduleRepository.save(schedule);
            consumeVoucher(booking);
            booking.setBookingStatus(BookingStatus.PENDING_CONFIRMATION);
            booking.setConfirmationExpiresAt(LocalDateTime.now().plus(properties.getVendorConfirmationDuration()));
            booking.setHoldExpiresAt(null);
        }

        booking.setPaymentStatus(totalPaid.compareTo(booking.getTotalPrice()) >= 0
                ? PaymentStatus.PAID : PaymentStatus.PARTIALLY_PAID);
        bookingRepository.save(booking);
    }

    private void consumeVoucher(Booking booking) {
        if (booking.getVoucher() == null || booking.getVoucherState() != VoucherReservationState.RESERVED) return;
        Voucher voucher = voucherRepository.findByCodeForUpdate(booking.getVoucher().getCode())
                .orElseThrow(() -> new AppException(ErrorCode.VOUCHER_NOT_FOUND));
        voucher.setReservedCount(Math.max(0, voucher.getReservedCount() - 1));
        voucher.setUsedCount(voucher.getUsedCount() + 1);
        voucherRepository.save(voucher);
        booking.setVoucherState(VoucherReservationState.CONSUMED);
    }

    private void createLatePaymentRefund(PaymentTransaction payment, Booking booking) {
        RefundTransaction refund = new RefundTransaction();
        refund.setPaymentTransaction(payment);
        refund.setBooking(booking);
        refund.setIdempotencyKey("late-payment:" + payment.getPaymentTransactionId());
        refund.setAmount(payment.getPaidAmount());
        refund.setReason(RefundReason.PAYMENT_ADJUSTMENT);
        refund.setReasonDetail("Payment arrived after the booking hold was no longer valid");
        refund.setRefundMethod(RefundMethod.GATEWAY_REFUND);
        refund.setDestinationBin(Objects.toString(payment.getGatewayMetadata().get("counterAccountBankId"), null));
        refund.setDestinationAccountNumber(Objects.toString(payment.getGatewayMetadata().get("counterAccountNumber"), null));
        refund.setDestinationAccountName(Objects.toString(payment.getGatewayMetadata().get("counterAccountName"), null));
        refundTransactionRepository.save(refund);
        booking.setPaymentStatus(PaymentStatus.REFUND_PENDING);
        bookingRepository.save(booking);
    }

    @Override
    public List<PaymentTransactionResponse> getBookingPayments(String email, UUID bookingId) {
        Booking booking = requireViewableBooking(email, bookingId);
        List<PaymentTransactionResponse> transactions = paymentTransactionRepository
                .findByBooking_BookingIdAndIsDeletedFalseOrderByCreatedAtAsc(bookingId)
                .stream().map(this::toPaymentResponse).toList();
        if (!transactions.isEmpty()) return transactions;

        PaymentTransactionResponse legacy = toLegacyPaymentResponse(booking);
        return legacy == null ? List.of() : List.of(legacy);
    }

    @Override
    public List<RefundTransactionResponse> getBookingRefunds(String email, UUID bookingId) {
        requireViewableBooking(email, bookingId);
        return refundTransactionRepository.findByBooking_BookingIdAndIsDeletedFalseOrderByCreatedAtAsc(bookingId)
                .stream().map(this::toRefundResponse).toList();
    }

    private Booking requireViewableBooking(String email, UUID bookingId) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        Booking booking = bookingRepository.findById(bookingId)
                .filter(b -> !Boolean.TRUE.equals(b.getIsDeleted()))
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
        if (booking.getUser().getUserId().equals(user.getUserId()) || isAdmin(user)) return booking;
        Vendor vendor = findAssociatedVendor(email);
        if (vendor != null && vendor.getVendorId().equals(booking.getSchedule().getTour().getVendor().getVendorId())) {
            return booking;
        }
        throw new AppException(ErrorCode.ACCESS_DENIED);
    }

    @Override
    public RefundTransactionResponse processRefund(String email, UUID refundId) {
        RefundTransaction prepared = transactionTemplate.execute(status -> prepareRefund(email, refundId));
        if (prepared == null) throw new AppException(ErrorCode.REFUND_NOT_PROCESSABLE);
        PayOS client = payOsClientFactory.getClient(prepared.getPaymentTransaction().getVendorPaymentAccount());

        String referenceId = "refund_" + prepared.getRefundTransactionId();
        PayoutBatchRequest request = PayoutBatchRequest.builder()
                .referenceId(referenceId)
                .validateDestination(true)
                .category(List.of("refund"))
                .payout(PayoutBatchItem.builder()
                        .referenceId(referenceId + "_1")
                        .amount(prepared.getAmount().setScale(0, RoundingMode.HALF_UP).longValueExact())
                        .description("Hoan tien " + prepared.getBooking().getBookingCode())
                        .toBin(prepared.getDestinationBin())
                        .toAccountNumber(prepared.getDestinationAccountNumber())
                        .build())
                .build();
        try {
            Payout payout = client.payouts().batch().create(request, prepared.getIdempotencyKey());
            return transactionTemplate.execute(status -> applyPayoutResult(refundId, payout));
        } catch (Exception exception) {
            log.error("payOS refund payout failed for refund {}", refundId, exception);
            return transactionTemplate.execute(status -> markRefundFailed(refundId, "PAYOS_PAYOUT_FAILED", safeMessage(exception)));
        }
    }

    private RefundTransaction prepareRefund(String email, UUID refundId) {
        User approver = userRepository.findByEmail(email).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        RefundTransaction refund = refundTransactionRepository.findByIdForUpdate(refundId)
                .orElseThrow(() -> new AppException(ErrorCode.REFUND_NOT_FOUND));
        Vendor vendor = requireAssociatedVendor(email);
        if (!vendor.getVendorId().equals(refund.getBooking().getSchedule().getTour().getVendor().getVendorId())) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        if (!EnumSet.of(RefundStatus.PENDING, RefundStatus.FAILED).contains(refund.getStatus())) {
            throw new AppException(ErrorCode.REFUND_NOT_PROCESSABLE);
        }
        requireRefundDestination(refund);
        refund.setApprovedBy(approver);
        refund.setRefundMethod(RefundMethod.GATEWAY_REFUND);
        refund.setStatus(RefundStatus.PROCESSING);
        refund.setProcessingAt(LocalDateTime.now());
        refund.setFailureCode(null);
        refund.setFailureMessage(null);
        return refundTransactionRepository.save(refund);
    }

    private RefundTransactionResponse applyPayoutResult(UUID refundId, Payout payout) {
        RefundTransaction refund = refundTransactionRepository.findByIdForUpdate(refundId)
                .orElseThrow(() -> new AppException(ErrorCode.REFUND_NOT_FOUND));
        refund.setGatewayRefundId(payout.getId());
        refund.getGatewayMetadata().put("referenceId", payout.getReferenceId());
        refund.getGatewayMetadata().put("approvalState", payout.getApprovalState().getValue());
        boolean completed = isPayoutCompleted(payout);
        if (completed) {
            refund.setStatus(RefundStatus.REFUNDED);
            refund.setCompletedAt(LocalDateTime.now());
        } else if (payout.getApprovalState() == PayoutApprovalState.FAILED
                || payout.getApprovalState() == PayoutApprovalState.REJECTED
                || payout.getApprovalState() == PayoutApprovalState.CANCELLED) {
            refund.setStatus(RefundStatus.FAILED);
            refund.setFailureCode("PAYOS_" + payout.getApprovalState().name());
        } else {
            refund.setStatus(RefundStatus.PROCESSING);
        }
        refundTransactionRepository.save(refund);
        refreshBookingPaymentStatus(refund.getBooking());
        return toRefundResponse(refund);
    }

    private boolean isPayoutCompleted(Payout payout) {
        return payout.getApprovalState() == PayoutApprovalState.COMPLETED
                && payout.getTransactions() != null
                && !payout.getTransactions().isEmpty()
                && payout.getTransactions().stream().allMatch(t -> t.getState() == PayoutTransactionState.SUCCEEDED);
    }

    private RefundTransactionResponse markRefundFailed(UUID refundId, String code, String message) {
        RefundTransaction refund = refundTransactionRepository.findByIdForUpdate(refundId)
                .orElseThrow(() -> new AppException(ErrorCode.REFUND_NOT_FOUND));
        refund.setStatus(RefundStatus.FAILED);
        refund.setFailureCode(code);
        refund.setFailureMessage(message);
        refundTransactionRepository.save(refund);
        refreshBookingPaymentStatus(refund.getBooking());
        return toRefundResponse(refund);
    }

    @Override
    public RefundTransactionResponse completeManualRefund(String email, UUID refundId, ManualRefundCompletionRequest request) {
        return transactionTemplate.execute(status -> {
            User approver = userRepository.findByEmail(email).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
            Vendor vendor = requireAssociatedVendor(email);
            RefundTransaction refund = refundTransactionRepository.findByIdForUpdate(refundId)
                    .orElseThrow(() -> new AppException(ErrorCode.REFUND_NOT_FOUND));
            if (!vendor.getVendorId().equals(refund.getBooking().getSchedule().getTour().getVendor().getVendorId())) {
                throw new AppException(ErrorCode.ACCESS_DENIED);
            }
            if (!EnumSet.of(RefundStatus.PENDING, RefundStatus.FAILED).contains(refund.getStatus())) {
                throw new AppException(ErrorCode.REFUND_NOT_PROCESSABLE);
            }
            requireRefundDestination(refund);
            refund.setApprovedBy(approver);
            refund.setRefundMethod(RefundMethod.MANUAL);
            refund.setGatewayRefundId(request.getBankReference());
            refund.getGatewayMetadata().put("manualNote", Objects.toString(request.getNote(), ""));
            refund.setStatus(RefundStatus.REFUNDED);
            refund.setCompletedAt(LocalDateTime.now());
            refundTransactionRepository.save(refund);
            refreshBookingPaymentStatus(refund.getBooking());
            return toRefundResponse(refund);
        });
    }

    @Override
    public RefundTransactionResponse updateRefundDestination(String email, UUID refundId, RefundDestinationRequest request) {
        return transactionTemplate.execute(status -> {
            User user = userRepository.findByEmail(email).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
            RefundTransaction refund = refundTransactionRepository.findByIdForUpdate(refundId)
                    .orElseThrow(() -> new AppException(ErrorCode.REFUND_NOT_FOUND));
            if (!refund.getBooking().getUser().getUserId().equals(user.getUserId())) {
                throw new AppException(ErrorCode.ACCESS_DENIED);
            }
            if (!EnumSet.of(RefundStatus.PENDING, RefundStatus.FAILED).contains(refund.getStatus())) {
                throw new AppException(ErrorCode.REFUND_NOT_PROCESSABLE);
            }
            refund.setDestinationBin(request.getBankBin().trim());
            refund.setDestinationAccountNumber(request.getAccountNumber().trim());
            refund.setDestinationAccountName(request.getAccountName().trim());
            refundTransactionRepository.save(refund);
            return toRefundResponse(refund);
        });
    }

    @Scheduled(fixedDelayString = "${application.payment.refund-reconciliation-delay-ms:300000}")
    public void reconcileProcessingRefunds() {
        for (RefundTransaction refund : refundTransactionRepository
                .findTop100ByStatusAndGatewayRefundIdIsNotNullAndIsDeletedFalseOrderByProcessingAtAsc(RefundStatus.PROCESSING)) {
            try {
                PayOS client = payOsClientFactory.getClient(refund.getPaymentTransaction().getVendorPaymentAccount());
                Payout payout = client.payouts().get(refund.getGatewayRefundId());
                transactionTemplate.executeWithoutResult(status -> applyPayoutResult(refund.getRefundTransactionId(), payout));
            } catch (Exception exception) {
                log.warn("Could not reconcile refund {}: {}", refund.getRefundTransactionId(), exception.getMessage());
            }
        }
    }

    private void refreshBookingPaymentStatus(Booking detachedBooking) {
        Booking booking = bookingRepository.findByIdForUpdate(detachedBooking.getBookingId())
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
        BigDecimal paid = paymentTransactionRepository.sumPaidByBooking(booking.getBookingId());
        BigDecimal refunded = refundTransactionRepository.sumByBookingAndStatuses(
                booking.getBookingId(), List.of(RefundStatus.REFUNDED));
        BigDecimal pending = refundTransactionRepository.sumByBookingAndStatuses(
                booking.getBookingId(), List.of(RefundStatus.PENDING, RefundStatus.PROCESSING));
        if (pending.signum() > 0) booking.setPaymentStatus(PaymentStatus.REFUND_PENDING);
        else if (refunded.signum() == 0) booking.setPaymentStatus(
                paid.compareTo(booking.getTotalPrice()) >= 0 ? PaymentStatus.PAID
                        : paid.signum() > 0 ? PaymentStatus.PARTIALLY_PAID : PaymentStatus.UNPAID);
        else if (refunded.compareTo(paid) >= 0) booking.setPaymentStatus(PaymentStatus.REFUNDED);
        else booking.setPaymentStatus(PaymentStatus.PARTIALLY_REFUNDED);
        booking.setRefundAmount(refunded);
        bookingRepository.save(booking);
    }

    private void requireRefundDestination(RefundTransaction refund) {
        if (isBlank(refund.getDestinationBin()) || isBlank(refund.getDestinationAccountNumber())
                || isBlank(refund.getDestinationAccountName())) {
            throw new AppException(ErrorCode.REFUND_DESTINATION_REQUIRED);
        }
    }

    private Vendor requireAssociatedVendor(String email) {
        Vendor vendor = findAssociatedVendor(email);
        if (vendor == null) throw new AppException(ErrorCode.ACCESS_DENIED);
        return vendor;
    }

    private Vendor findAssociatedVendor(String email) {
        return vendorRepository.findByManager_Email(email).orElseGet(() ->
                vendorStaffRepository.findByUser_Email(email).map(VendorStaff::getVendor).orElse(null));
    }

    private boolean isAdmin(User user) {
        return user.getRoles().stream().anyMatch(role -> "ADMIN".equals(role.getRoleName()));
    }

    private PaymentCheckoutResponse toCheckoutResponse(PaymentTransaction payment) {
        return PaymentCheckoutResponse.builder()
                .paymentTransactionId(payment.getPaymentTransactionId())
                .bookingId(payment.getBooking().getBookingId())
                .paymentStage(payment.getPaymentStage())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .orderCode(payment.getGatewayOrderCode())
                .checkoutUrl(payment.getCheckoutUrl())
                .qrCode(payment.getQrCode())
                .expiredAt(payment.getExpiredAt())
                .build();
    }

    private PaymentTransactionResponse toPaymentResponse(PaymentTransaction payment) {
        return PaymentTransactionResponse.builder()
                .paymentTransactionId(payment.getPaymentTransactionId())
                .paymentStage(payment.getPaymentStage())
                .attemptNumber(payment.getAttemptNumber())
                .amount(payment.getAmount())
                .paidAmount(payment.getPaidAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .orderCode(payment.getGatewayOrderCode())
                .checkoutUrl(payment.getCheckoutUrl())
                .expiredAt(payment.getExpiredAt())
                .paidAt(payment.getPaidAt())
                .failureCode(payment.getFailureCode())
                .failureMessage(payment.getFailureMessage())
                .source("PAYOS")
                .createdAt(payment.getCreatedAt())
                .build();
    }

    private PaymentTransactionResponse toLegacyPaymentResponse(Booking booking) {
        boolean hasLegacyProof = booking.getProofImageUrl() != null && !booking.getProofImageUrl().isBlank();
        boolean hasRecordedPayment = booking.getPaymentStatus() != PaymentStatus.UNPAID;
        if (!hasLegacyProof && !hasRecordedPayment) return null;

        PaymentTransactionStatus status = hasRecordedPayment
                ? PaymentTransactionStatus.PAID
                : PaymentTransactionStatus.PENDING;
        return PaymentTransactionResponse.builder()
                .paymentTransactionId(booking.getBookingId())
                .paymentStage(PaymentStage.FULL)
                .attemptNumber((short) 1)
                .amount(booking.getTotalPrice())
                .paidAmount(hasRecordedPayment ? booking.getTotalPrice() : BigDecimal.ZERO)
                .currency("VND")
                .status(status)
                .paidAt(hasRecordedPayment ? booking.getUpdatedAt() : null)
                .source("LEGACY_BANK_TRANSFER")
                .createdAt(booking.getCreatedAt())
                .build();
    }

    private RefundTransactionResponse toRefundResponse(RefundTransaction refund) {
        return RefundTransactionResponse.builder()
                .refundTransactionId(refund.getRefundTransactionId())
                .bookingId(refund.getBooking().getBookingId())
                .paymentTransactionId(refund.getPaymentTransaction().getPaymentTransactionId())
                .amount(refund.getAmount())
                .reason(refund.getReason())
                .reasonDetail(refund.getReasonDetail())
                .status(refund.getStatus())
                .refundMethod(refund.getRefundMethod())
                .destinationBin(refund.getDestinationBin())
                .maskedDestinationAccountNumber(mask(refund.getDestinationAccountNumber()))
                .destinationAccountName(refund.getDestinationAccountName())
                .gatewayRefundId(refund.getGatewayRefundId())
                .requestedAt(refund.getRequestedAt())
                .processingAt(refund.getProcessingAt())
                .completedAt(refund.getCompletedAt())
                .failureCode(refund.getFailureCode())
                .failureMessage(refund.getFailureMessage())
                .build();
    }

    private String withBookingId(String baseUrl, UUID bookingId) {
        if (isBlank(baseUrl)) throw new IllegalStateException("Payment return/cancel URL is not configured");
        return UriComponentsBuilder.fromUriString(baseUrl).queryParam("bookingId", bookingId).build().toUriString();
    }

    private BigDecimal decimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        return new BigDecimal(value.toString());
    }

    private String mask(String account) {
        if (account == null || account.length() <= 4) return account;
        return "*".repeat(account.length() - 4) + account.substring(account.length() - 4);
    }

    private String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null ? exception.getClass().getSimpleName() : message.substring(0, Math.min(message.length(), 500));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
