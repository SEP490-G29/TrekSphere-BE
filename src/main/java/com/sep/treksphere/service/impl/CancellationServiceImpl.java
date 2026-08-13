package com.sep.treksphere.service.impl;

import com.sep.treksphere.config.PaymentWorkflowProperties;
import com.sep.treksphere.dto.request.BookingCancelRequest;
import com.sep.treksphere.dto.request.VendorBookingCancelRequest;
import com.sep.treksphere.dto.response.CancellationQuoteResponse;
import com.sep.treksphere.entity.*;
import com.sep.treksphere.enums.booking.*;
import com.sep.treksphere.enums.system.NotificationEventType;
import com.sep.treksphere.enums.system.ReferenceType;
import com.sep.treksphere.event.RefundRequestedEvent;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;
import com.sep.treksphere.repository.*;
import com.sep.treksphere.service.CancellationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CancellationServiceImpl implements CancellationService {

    private static final Set<RefundStatus> ACTIVE_REFUNDS = EnumSet.of(
            RefundStatus.PENDING, RefundStatus.PROCESSING, RefundStatus.AWAITING_VENDOR_FUNDS,
            RefundStatus.OVERDUE, RefundStatus.REFUNDED);

    private final BookingRepository bookingRepository;
    private final BookingPolicySnapshotRepository snapshotRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final RefundTransactionRepository refundTransactionRepository;
    private final TourScheduleRepository tourScheduleRepository;
    private final VoucherRepository voucherRepository;
    private final UserRepository userRepository;
    private final VendorRepository vendorRepository;
    private final VendorStaffRepository vendorStaffRepository;
    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final PaymentWorkflowProperties paymentProperties;

    private record QuoteData(BigDecimal paid, BigDecimal existingRefunds, BigDecimal availablePaid,
                             BigDecimal nonRefundableCost, int percentage, BigDecimal refund,
                             long daysBeforeDeparture, String description) {}

    @Override
    @Transactional(readOnly = true)
    public CancellationQuoteResponse quoteForTrekker(String email, UUID bookingId) {
        Booking booking = requireOwnedBooking(email, bookingId, false);
        validateCancellable(booking);
        return toResponse(calculateQuote(booking));
    }

    @Override
    @Transactional
    public Booking cancelByTrekker(String email, UUID bookingId, BookingCancelRequest request) {
        Booking booking = requireOwnedBooking(email, bookingId, true);
        if (booking.getBookingStatus() == BookingStatus.CANCELLED) return booking;
        validateCancellable(booking);
        QuoteData quote = calculateQuote(booking);
        if (quote.refund().signum() > 0) requireDestination(request);

        releaseCapacity(booking);
        releaseVoucherWhenEligible(booking, quote);
        cancelOpenPaymentAttempts(booking);

        if (quote.refund().signum() > 0) {
            createRefundTransactions(booking, quote.refund(), request);
            booking.setPaymentStatus(PaymentStatus.REFUND_PENDING);
        }
        booking.setBookingStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason(request.getCancellationReason());
        booking.setCancelledAt(LocalDateTime.now());
        booking.setConfirmationExpiresAt(null);
        booking.setHoldExpiresAt(null);
        booking.setRefundAmount(BigDecimal.ZERO);
        Booking saved = bookingRepository.save(booking);
        notifyVendorOfTrekkerCancellation(saved);
        return saved;
    }

    @Override
    @Transactional
    public Booking cancelByVendor(String email, UUID bookingId, VendorBookingCancelRequest request) {
        if (!EnumSet.of(RefundReason.VENDOR_CANCEL, RefundReason.INSUFFICIENT_PAX).contains(request.getReason())) {
            throw new AppException(ErrorCode.REFUND_NOT_PROCESSABLE,
                    "Vendor chỉ được dùng lý do VENDOR_CANCEL hoặc INSUFFICIENT_PAX.");
        }
        Vendor vendor = vendorRepository.findByManager_Email(email).orElseGet(() ->
                vendorStaffRepository.findByUser_EmailAndIsActiveTrueAndIsDeletedFalse(email).map(VendorStaff::getVendor)
                        .orElseThrow(() -> new AppException(ErrorCode.ACCESS_DENIED)));
        lockScheduleForBooking(bookingId);
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
        if (!vendor.getVendorId().equals(booking.getSchedule().getTour().getVendor().getVendorId())) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        if (EnumSet.of(BookingStatus.CANCELLED, BookingStatus.REJECTED).contains(booking.getBookingStatus())) {
            return booking;
        }
        validateCancellable(booking);

        boolean wasHeld = booking.getBookingStatus() == BookingStatus.PAYMENT_PENDING;
        releaseCapacity(booking);
        releaseVoucherForVendorCancellation(booking);
        cancelOpenPaymentAttempts(booking);
        BigDecimal paid = paymentTransactionRepository.sumPaidByBooking(bookingId);
        BigDecimal existing = refundTransactionRepository.sumByBookingAndStatuses(bookingId, ACTIVE_REFUNDS);
        BigDecimal refundAmount = paid.subtract(existing).max(BigDecimal.ZERO);
        if (refundAmount.signum() > 0) {
            createVendorRefundTransactions(booking, refundAmount, request);
            booking.setPaymentStatus(PaymentStatus.REFUND_PENDING);
        }
        booking.setBookingStatus(wasHeld ? BookingStatus.REJECTED : BookingStatus.CANCELLED);
        booking.setCancellationReason(request.getReasonDetail());
        booking.setCancelledAt(LocalDateTime.now());
        booking.setHoldExpiresAt(null);
        booking.setConfirmationExpiresAt(null);
        booking.setRefundAmount(BigDecimal.ZERO);
        Booking saved = bookingRepository.save(booking);
        notifyTrekkerOfVendorCancellation(saved);
        return saved;
    }

    private Booking requireOwnedBooking(String email, UUID bookingId, boolean lock) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        if (lock) lockScheduleForBooking(bookingId);
        Booking booking = (lock ? bookingRepository.findByIdForUpdate(bookingId) : bookingRepository.findById(bookingId))
                .filter(b -> !Boolean.TRUE.equals(b.getIsDeleted()))
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
        if (!booking.getUser().getUserId().equals(user.getUserId())) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        return booking;
    }

    private void lockScheduleForBooking(UUID bookingId) {
        UUID scheduleId = bookingRepository.findScheduleIdByBookingId(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
        tourScheduleRepository.findByIdForUpdate(scheduleId)
                .orElseThrow(() -> new AppException(ErrorCode.SCHEDULE_NOT_FOUND));
    }

    private void validateCancellable(Booking booking) {
        if (EnumSet.of(BookingStatus.CANCELLED, BookingStatus.EXPIRED, BookingStatus.REJECTED,
                BookingStatus.IN_PROGRESS, BookingStatus.COMPLETED).contains(booking.getBookingStatus())) {
            throw new AppException(ErrorCode.BOOKING_CANNOT_CANCEL);
        }
    }

    private QuoteData calculateQuote(Booking booking) {
        BigDecimal paid = paymentTransactionRepository.sumPaidByBooking(booking.getBookingId());
        BigDecimal existingRefunds = refundTransactionRepository.sumByBookingAndStatuses(
                booking.getBookingId(), ACTIVE_REFUNDS);
        BigDecimal availablePaid = paid.subtract(existingRefunds).max(BigDecimal.ZERO);
        long days = ChronoUnit.DAYS.between(LocalDate.now(), booking.getSchedule().getDepartureDate());

        BookingPolicySnapshot snapshot = snapshotRepository.findById(booking.getBookingId()).orElse(null);
        if (snapshot == null || snapshot.getPolicyJson() == null || snapshot.getPolicyJson().isEmpty()) {
            return new QuoteData(paid, existingRefunds, availablePaid, BigDecimal.ZERO,
                    100, availablePaid, days,
                    "Booking thiếu chính sách hủy tại thời điểm đặt nên được hoàn 100%.");
        }
        int percentage = 0;
        String description = "Không hoàn tiền theo chính sách đã áp dụng";
        int selectedThreshold = Integer.MIN_VALUE;
        for (Map<String, Object> tier : snapshot.getPolicyJson()) {
            int threshold = integer(tier.get("cancelBeforeDays"));
            if (days >= threshold && threshold > selectedThreshold) {
                selectedThreshold = threshold;
                percentage = integer(tier.get("refundPercentage"));
                description = Objects.toString(tier.get("description"), description);
            }
        }

        BigDecimal nonRefundable = snapshot.getNonRefundableCost().min(availablePaid).max(BigDecimal.ZERO);
        BigDecimal base = availablePaid.subtract(nonRefundable).max(BigDecimal.ZERO);
        BigDecimal refund = base.multiply(BigDecimal.valueOf(percentage))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                .min(availablePaid);
        return new QuoteData(paid, existingRefunds, availablePaid, nonRefundable,
                percentage, refund, days, description);
    }

    private void releaseCapacity(Booking booking) {
        TourSchedule schedule = tourScheduleRepository.findByIdForUpdate(booking.getSchedule().getScheduleId())
                .orElseThrow(() -> new AppException(ErrorCode.SCHEDULE_NOT_FOUND));
        int pax = booking.getNumberOfParticipants();
        if (booking.getBookingStatus() == BookingStatus.PAYMENT_PENDING) {
            schedule.setHeldSlots(Math.max(0, schedule.getHeldSlots() - pax));
        } else {
            schedule.setBookedSlots(Math.max(0, schedule.getBookedSlots() - pax));
        }
        schedule.setAvailableSlots(schedule.getAvailableSlots() + pax);
        tourScheduleRepository.save(schedule);
    }

    private void releaseVoucherWhenEligible(Booking booking, QuoteData quote) {
        if (booking.getVoucher() == null) return;
        Voucher voucher = voucherRepository.findByCodeForUpdate(booking.getVoucher().getCode())
                .orElseThrow(() -> new AppException(ErrorCode.VOUCHER_NOT_FOUND));
        if (booking.getVoucherState() == VoucherReservationState.RESERVED) {
            voucher.setReservedCount(Math.max(0, voucher.getReservedCount() - 1));
            booking.setVoucherState(VoucherReservationState.RELEASED);
        } else if (booking.getVoucherState() == VoucherReservationState.CONSUMED
                && quote.refund().compareTo(quote.availablePaid()) >= 0) {
            voucher.setUsedCount(Math.max(0, voucher.getUsedCount() - 1));
            booking.setVoucherState(VoucherReservationState.RELEASED);
        }
        voucherRepository.save(voucher);
    }

    private void releaseVoucherForVendorCancellation(Booking booking) {
        if (booking.getVoucher() == null) return;
        Voucher voucher = voucherRepository.findByCodeForUpdate(booking.getVoucher().getCode())
                .orElseThrow(() -> new AppException(ErrorCode.VOUCHER_NOT_FOUND));
        if (booking.getVoucherState() == VoucherReservationState.RESERVED) {
            voucher.setReservedCount(Math.max(0, voucher.getReservedCount() - 1));
        } else if (booking.getVoucherState() == VoucherReservationState.CONSUMED) {
            voucher.setUsedCount(Math.max(0, voucher.getUsedCount() - 1));
        } else return;
        booking.setVoucherState(VoucherReservationState.RELEASED);
        voucherRepository.save(voucher);
    }

    private void cancelOpenPaymentAttempts(Booking booking) {
        for (PaymentTransaction payment : paymentTransactionRepository
                .findByBooking_BookingIdAndIsDeletedFalseOrderByCreatedAtAsc(booking.getBookingId())) {
            if (EnumSet.of(PaymentTransactionStatus.CREATED, PaymentTransactionStatus.PENDING,
                    PaymentTransactionStatus.PROCESSING).contains(payment.getStatus())) {
                payment.setStatus(PaymentTransactionStatus.CANCELLED);
                payment.setCancelledAt(LocalDateTime.now());
                paymentTransactionRepository.save(payment);
            }
        }
    }

    private void createRefundTransactions(Booking booking, BigDecimal requestedAmount, BookingCancelRequest request) {
        BigDecimal remaining = requestedAmount;
        List<PaymentTransaction> paidTransactions = paymentTransactionRepository
                .findByBooking_BookingIdAndIsDeletedFalseOrderByCreatedAtAsc(booking.getBookingId())
                .stream().filter(p -> p.getStatus() == PaymentTransactionStatus.PAID).toList();
        for (PaymentTransaction payment : paidTransactions) {
            if (remaining.signum() <= 0) break;
            BigDecimal alreadyAllocated = refundTransactionRepository.sumByPaymentAndStatuses(
                    payment.getPaymentTransactionId(), ACTIVE_REFUNDS);
            BigDecimal refundableOnPayment = payment.getPaidAmount().subtract(alreadyAllocated).max(BigDecimal.ZERO);
            BigDecimal allocation = refundableOnPayment.min(remaining);
            if (allocation.signum() <= 0) continue;

            RefundTransaction refund = new RefundTransaction();
            refund.setPaymentTransaction(payment);
            refund.setBooking(booking);
            refund.setIdempotencyKey("trekker-cancel:" + booking.getBookingId() + ":" + payment.getPaymentTransactionId());
            refund.setAmount(allocation);
            refund.setReason(RefundReason.TREKKER_CANCEL);
            refund.setReasonDetail(request.getCancellationReason());
            refund.setRefundMethod(RefundMethod.GATEWAY_REFUND);
            refund.setDestinationBin(request.getRefundBankBin().trim());
            refund.setDestinationAccountNumber(request.getRefundAccountNumber().trim());
            refund.setDestinationAccountName(request.getRefundAccountName().trim());
            prepareAndPublishRefund(refund);
            remaining = remaining.subtract(allocation);
        }
        if (remaining.signum() > 0) {
            throw new AppException(ErrorCode.REFUND_NOT_PROCESSABLE,
                    "Không thể phân bổ đầy đủ số tiền hoàn vào các giao dịch đã thanh toán.");
        }
    }

    private void createVendorRefundTransactions(Booking booking, BigDecimal requestedAmount,
                                                VendorBookingCancelRequest request) {
        BigDecimal remaining = requestedAmount;
        for (PaymentTransaction payment : paymentTransactionRepository
                .findByBooking_BookingIdAndIsDeletedFalseOrderByCreatedAtAsc(booking.getBookingId())) {
            if (payment.getStatus() != PaymentTransactionStatus.PAID || remaining.signum() <= 0) continue;
            BigDecimal allocated = refundTransactionRepository.sumByPaymentAndStatuses(
                    payment.getPaymentTransactionId(), ACTIVE_REFUNDS);
            BigDecimal amount = payment.getPaidAmount().subtract(allocated).max(BigDecimal.ZERO).min(remaining);
            if (amount.signum() <= 0) continue;
            RefundTransaction refund = new RefundTransaction();
            refund.setPaymentTransaction(payment);
            refund.setBooking(booking);
            refund.setIdempotencyKey("vendor-cancel:" + booking.getBookingId() + ":" + payment.getPaymentTransactionId());
            refund.setAmount(amount);
            refund.setReason(request.getReason());
            refund.setReasonDetail(request.getReasonDetail());
            refund.setRefundMethod(RefundMethod.GATEWAY_REFUND);
            refund.setDestinationBin(string(payment.getGatewayMetadata().get("counterAccountBankId")));
            refund.setDestinationAccountNumber(string(payment.getGatewayMetadata().get("counterAccountNumber")));
            refund.setDestinationAccountName(string(payment.getGatewayMetadata().get("counterAccountName")));
            prepareAndPublishRefund(refund);
            remaining = remaining.subtract(amount);
        }
        if (remaining.signum() > 0) {
            throw new AppException(ErrorCode.REFUND_NOT_PROCESSABLE,
                    "Không thể phân bổ đầy đủ refund cho các payment đã trả.");
        }
    }

    private void prepareAndPublishRefund(RefundTransaction refund) {
        LocalDateTime now = LocalDateTime.now();
        java.time.Duration dueDuration = paymentProperties.getRefundDueDuration();
        if (dueDuration == null || dueDuration.isNegative() || dueDuration.isZero()) {
            dueDuration = java.time.Duration.ofHours(48);
        }
        refund.setRequestedAt(now);
        refund.setDueAt(now.plus(dueDuration));
        RefundTransaction saved = refundTransactionRepository.save(refund);
        eventPublisher.publishEvent(new RefundRequestedEvent(saved.getRefundTransactionId()));
    }

    private void notifyVendorOfTrekkerCancellation(Booking booking) {
        User manager = booking.getSchedule().getTour().getVendor().getManager();
        if (manager == null) return;
        saveCancellationNotification(manager,
                "Khách đã hủy booking",
                "Trekker đã hủy booking " + booking.getBookingCode() + ". Lý do: "
                        + booking.getCancellationReason(),
                booking.getBookingId());
    }

    private void notifyTrekkerOfVendorCancellation(Booking booking) {
        saveCancellationNotification(booking.getUser(),
                "Booking đã bị nhà tổ chức hủy",
                "Booking " + booking.getBookingCode() + " đã bị nhà tổ chức hủy. Lý do: "
                        + booking.getCancellationReason()
                        + (booking.getPaymentStatus() == PaymentStatus.REFUND_PENDING
                        ? " Yêu cầu hoàn tiền đã được tạo và đang được xử lý tự động." : ""),
                booking.getBookingId());
    }

    private void saveCancellationNotification(User recipient, String title, String content, UUID bookingId) {
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setTitle(title);
        notification.setEventType(NotificationEventType.BOOKING_CANCELLED);
        notification.setContent(content);
        notification.setReferenceType(ReferenceType.BOOKING);
        notification.setReferenceId(bookingId);
        notificationRepository.save(notification);
    }

    private CancellationQuoteResponse toResponse(QuoteData quote) {
        return CancellationQuoteResponse.builder()
                .paidAmount(quote.paid())
                .alreadyRefundedOrPendingAmount(quote.existingRefunds())
                .refundablePaidAmount(quote.availablePaid())
                .nonRefundableCost(quote.nonRefundableCost())
                .refundPercentage(quote.percentage())
                .refundAmount(quote.refund())
                .cancellationFee(quote.availablePaid().subtract(quote.refund()))
                .daysBeforeDeparture(quote.daysBeforeDeparture())
                .appliedPolicyDescription(quote.description())
                .refundDestinationRequired(quote.refund().signum() > 0)
                .build();
    }

    private void requireDestination(BookingCancelRequest request) {
        if (isBlank(request.getRefundBankBin()) || isBlank(request.getRefundAccountNumber())
                || isBlank(request.getRefundAccountName())) {
            throw new AppException(ErrorCode.REFUND_DESTINATION_REQUIRED);
        }
    }

    private int integer(Object value) {
        if (value == null) return 0;
        return new BigDecimal(value.toString()).intValueExact();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String string(Object value) {
        String result = value == null ? null : value.toString();
        return result == null || result.isBlank() ? null : result;
    }
}
