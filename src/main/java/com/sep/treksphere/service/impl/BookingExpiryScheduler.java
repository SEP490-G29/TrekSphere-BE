package com.sep.treksphere.service.impl;

import com.sep.treksphere.config.PaymentWorkflowProperties;
import com.sep.treksphere.entity.*;
import com.sep.treksphere.enums.booking.*;
import com.sep.treksphere.enums.system.NotificationEventType;
import com.sep.treksphere.enums.system.ReferenceType;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;
import com.sep.treksphere.repository.*;
import com.sep.treksphere.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingExpiryScheduler {

    private final BookingRepository bookingRepository;
    private final TourScheduleRepository scheduleRepository;
    private final VoucherRepository voucherRepository;
    private final PaymentTransactionRepository paymentRepository;
    private final RefundTransactionRepository refundRepository;
    private final TransactionTemplate transactionTemplate;
    private final PaymentWorkflowProperties paymentProperties;
    private final NotificationService notificationService;

    @Scheduled(fixedDelayString = "${application.payment.booking-expiry-delay-ms:60000}")
    public void expireStaleBookings() {
        LocalDateTime now = LocalDateTime.now();
        bookingRepository.findTop100ByBookingStatusAndHoldExpiresAtBeforeAndIsDeletedFalseOrderByHoldExpiresAtAsc(
                        BookingStatus.PAYMENT_PENDING, now)
                .forEach(candidate -> executeSafely(candidate.getBookingId(), this::expireUnpaidHold));

        bookingRepository.findTop100ByBookingStatusAndConfirmationExpiresAtBeforeAndIsDeletedFalseOrderByConfirmationExpiresAtAsc(
                        BookingStatus.PENDING_CONFIRMATION, now)
                .forEach(candidate -> executeSafely(candidate.getBookingId(), this::rejectConfirmationTimeout));

        bookingRepository.findTop100ByPaymentPlanAndPaymentStatusAndRemainingDueAtBeforeAndBookingStatusInAndIsDeletedFalseOrderByRemainingDueAtAsc(
                        PaymentPlan.DEPOSIT, PaymentStatus.PARTIALLY_PAID, now,
                        EnumSet.of(BookingStatus.PENDING_CONFIRMATION, BookingStatus.CONFIRMED))
                .forEach(candidate -> executeSafely(candidate.getBookingId(), this::cancelOverdueBalance));
    }

    private void executeSafely(UUID bookingId, java.util.function.Consumer<UUID> operation) {
        try {
            transactionTemplate.executeWithoutResult(status -> operation.accept(bookingId));
        } catch (Exception exception) {
            log.error("Scheduled booking transition failed for {}", bookingId, exception);
        }
    }

    private void expireUnpaidHold(UUID bookingId) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
        if (booking.getBookingStatus() != BookingStatus.PAYMENT_PENDING
                || booking.getHoldExpiresAt() == null
                || booking.getHoldExpiresAt().isAfter(LocalDateTime.now())) return;

        TourSchedule schedule = scheduleRepository.findByIdForUpdate(booking.getSchedule().getScheduleId())
                .orElseThrow(() -> new AppException(ErrorCode.SCHEDULE_NOT_FOUND));
        int pax = booking.getNumberOfParticipants();
        schedule.setHeldSlots(Math.max(0, schedule.getHeldSlots() - pax));
        schedule.setAvailableSlots(schedule.getAvailableSlots() + pax);
        scheduleRepository.save(schedule);
        releaseReservedVoucher(booking);
        expireOpenPayments(booking);
        booking.setBookingStatus(BookingStatus.EXPIRED);
        booking.setHoldExpiresAt(null);
        booking.setCancellationReason("Hết thời gian giữ chỗ trước khi thanh toán");
        booking.setCancelledAt(LocalDateTime.now());
        bookingRepository.save(booking);
        notifyBooking(booking, NotificationEventType.BOOKING_EXPIRED,
                "Booking đã hết hạn",
                "Booking " + booking.getBookingCode() + " đã hết hạn do chưa thanh toán đúng thời gian.");
    }

    private void rejectConfirmationTimeout(UUID bookingId) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
        if (booking.getBookingStatus() != BookingStatus.PENDING_CONFIRMATION
                || booking.getConfirmationExpiresAt() == null
                || booking.getConfirmationExpiresAt().isAfter(LocalDateTime.now())) return;

        releaseBookedCapacity(booking);
        releaseConsumedVoucher(booking);
        createFullRefunds(booking, RefundReason.VENDOR_CANCEL,
                "Vendor không phản hồi booking trong thời hạn xác nhận");
        booking.setBookingStatus(BookingStatus.REJECTED);
        booking.setPaymentStatus(PaymentStatus.REFUND_PENDING);
        booking.setConfirmationExpiresAt(null);
        booking.setCancellationReason("Vendor confirmation timeout");
        booking.setCancelledAt(LocalDateTime.now());
        bookingRepository.save(booking);
        notifyBooking(booking, NotificationEventType.BOOKING_REJECTED,
                "Booking không được xác nhận",
                "Booking " + booking.getBookingCode()
                        + " đã bị từ chối do nhà tổ chức không phản hồi đúng hạn.");
    }

    private void cancelOverdueBalance(UUID bookingId) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
        if (booking.getPaymentPlan() != PaymentPlan.DEPOSIT
                || booking.getPaymentStatus() != PaymentStatus.PARTIALLY_PAID
                || booking.getRemainingDueAt() == null
                || booking.getRemainingDueAt().isAfter(LocalDateTime.now())) return;

        releaseBookedCapacity(booking);
        expireOpenPayments(booking);
        booking.setBookingStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason("Tự động hủy do quá hạn thanh toán phần còn lại; tiền cọc không tự động hoàn");
        booking.setCancelledAt(LocalDateTime.now());
        booking.setRemainingDueAt(null);
        bookingRepository.save(booking);
        notifyBooking(booking, NotificationEventType.BOOKING_CANCELLED,
                "Booking đã bị hủy",
                "Booking " + booking.getBookingCode()
                        + " đã bị hủy do quá hạn thanh toán phần còn lại.");
    }

    private void releaseBookedCapacity(Booking booking) {
        TourSchedule schedule = scheduleRepository.findByIdForUpdate(booking.getSchedule().getScheduleId())
                .orElseThrow(() -> new AppException(ErrorCode.SCHEDULE_NOT_FOUND));
        int pax = booking.getNumberOfParticipants();
        schedule.setBookedSlots(Math.max(0, schedule.getBookedSlots() - pax));
        schedule.setAvailableSlots(schedule.getAvailableSlots() + pax);
        scheduleRepository.save(schedule);
    }

    private void releaseReservedVoucher(Booking booking) {
        if (booking.getVoucher() == null || booking.getVoucherState() != VoucherReservationState.RESERVED) return;
        Voucher voucher = voucherRepository.findByCodeForUpdate(booking.getVoucher().getCode())
                .orElseThrow(() -> new AppException(ErrorCode.VOUCHER_NOT_FOUND));
        voucher.setReservedCount(Math.max(0, voucher.getReservedCount() - 1));
        voucherRepository.save(voucher);
        booking.setVoucherState(VoucherReservationState.RELEASED);
    }

    private void releaseConsumedVoucher(Booking booking) {
        if (booking.getVoucher() == null || booking.getVoucherState() != VoucherReservationState.CONSUMED) return;
        Voucher voucher = voucherRepository.findByCodeForUpdate(booking.getVoucher().getCode())
                .orElseThrow(() -> new AppException(ErrorCode.VOUCHER_NOT_FOUND));
        voucher.setUsedCount(Math.max(0, voucher.getUsedCount() - 1));
        voucherRepository.save(voucher);
        booking.setVoucherState(VoucherReservationState.RELEASED);
    }

    private void expireOpenPayments(Booking booking) {
        for (PaymentTransaction payment : paymentRepository
                .findByBooking_BookingIdAndIsDeletedFalseOrderByCreatedAtAsc(booking.getBookingId())) {
            if (EnumSet.of(PaymentTransactionStatus.CREATED, PaymentTransactionStatus.PENDING,
                    PaymentTransactionStatus.PROCESSING).contains(payment.getStatus())) {
                payment.setStatus(PaymentTransactionStatus.EXPIRED);
                paymentRepository.save(payment);
            }
        }
    }

    private void createFullRefunds(Booking booking, RefundReason reason, String detail) {
        for (PaymentTransaction payment : paymentRepository
                .findByBooking_BookingIdAndIsDeletedFalseOrderByCreatedAtAsc(booking.getBookingId())) {
            if (payment.getStatus() != PaymentTransactionStatus.PAID) continue;
            BigDecimal allocated = refundRepository.sumByPaymentAndStatuses(payment.getPaymentTransactionId(),
                    EnumSet.of(RefundStatus.PENDING, RefundStatus.AWAITING_VENDOR_ACTION,
                            RefundStatus.PROCESSING, RefundStatus.MANUAL_REVIEW,
                            RefundStatus.OVERDUE, RefundStatus.REFUNDED));
            BigDecimal amount = payment.getPaidAmount().subtract(allocated).max(BigDecimal.ZERO);
            if (amount.signum() <= 0) continue;
            RefundTransaction refund = new RefundTransaction();
            refund.setPaymentTransaction(payment);
            refund.setBooking(booking);
            refund.setIdempotencyKey("confirmation-timeout:" + payment.getPaymentTransactionId());
            refund.setAmount(amount);
            refund.setReason(reason);
            refund.setReasonDetail(detail);
            refund.setRefundMethod(RefundMethod.MANUAL);
            refund.setDueAt(refundDueAt());
            refundRepository.save(refund);
        }
    }

    private LocalDateTime refundDueAt() {
        java.time.Duration duration = paymentProperties.getRefundDueDuration();
        if (duration == null || duration.isNegative() || duration.isZero()) {
            duration = java.time.Duration.ofHours(48);
        }
        return LocalDateTime.now().plus(duration);
    }

    private void notifyBooking(Booking booking, NotificationEventType eventType,
                               String title, String content) {
        notificationService.create(booking.getUser(), title, content, eventType,
                ReferenceType.BOOKING, booking.getBookingId(),
                "/trekker/bookings/" + booking.getBookingId());
    }
}
