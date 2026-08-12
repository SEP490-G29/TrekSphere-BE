package com.sep.treksphere.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep.treksphere.config.PaymentWorkflowProperties;
import com.sep.treksphere.entity.Booking;
import com.sep.treksphere.entity.PaymentTransaction;
import com.sep.treksphere.entity.RefundTransaction;
import com.sep.treksphere.entity.Tour;
import com.sep.treksphere.entity.TourSchedule;
import com.sep.treksphere.entity.Vendor;
import com.sep.treksphere.enums.booking.BookingStatus;
import com.sep.treksphere.enums.booking.PaymentPlan;
import com.sep.treksphere.enums.booking.PaymentStage;
import com.sep.treksphere.enums.booking.PaymentStatus;
import com.sep.treksphere.enums.booking.PaymentTransactionStatus;
import com.sep.treksphere.enums.booking.RefundReason;
import com.sep.treksphere.enums.booking.RefundStatus;
import com.sep.treksphere.repository.BookingPolicySnapshotRepository;
import com.sep.treksphere.repository.BookingRepository;
import com.sep.treksphere.repository.PaymentTransactionRepository;
import com.sep.treksphere.repository.PaymentWebhookEventRepository;
import com.sep.treksphere.repository.RefundTransactionRepository;
import com.sep.treksphere.repository.TourScheduleRepository;
import com.sep.treksphere.repository.UserRepository;
import com.sep.treksphere.repository.VendorPaymentAccountRepository;
import com.sep.treksphere.repository.VendorRepository;
import com.sep.treksphere.repository.VendorStaffRepository;
import com.sep.treksphere.repository.VoucherRepository;
import com.sep.treksphere.service.payment.PayOsClientFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private TourScheduleRepository tourScheduleRepository;
    @Mock private VoucherRepository voucherRepository;
    @Mock private PaymentTransactionRepository paymentTransactionRepository;
    @Mock private PaymentWebhookEventRepository webhookEventRepository;
    @Mock private RefundTransactionRepository refundTransactionRepository;
    @Mock private BookingPolicySnapshotRepository bookingPolicySnapshotRepository;
    @Mock private VendorPaymentAccountRepository vendorPaymentAccountRepository;
    @Mock private UserRepository userRepository;
    @Mock private VendorRepository vendorRepository;
    @Mock private VendorStaffRepository vendorStaffRepository;
    @Mock private PayOsClientFactory payOsClientFactory;
    @Mock private PaymentWorkflowProperties properties;
    @Mock private ObjectMapper objectMapper;
    @Mock private TransactionTemplate transactionTemplate;

    @InjectMocks private PaymentServiceImpl service;

    private Booking booking;
    private TourSchedule schedule;
    private PaymentTransaction payment;

    @BeforeEach
    void setUp() {
        Vendor vendor = new Vendor();
        vendor.setVendorId(UUID.randomUUID());
        Tour tour = new Tour();
        tour.setTourId(UUID.randomUUID());
        tour.setVendor(vendor);

        schedule = new TourSchedule();
        schedule.setScheduleId(UUID.randomUUID());
        schedule.setTour(tour);
        schedule.setAvailableSlots(3);
        schedule.setBookedSlots(2);
        schedule.setHeldSlots(0);

        booking = new Booking();
        booking.setBookingId(UUID.randomUUID());
        booking.setBookingCode("TS-TEST");
        booking.setSchedule(schedule);
        booking.setNumberOfParticipants(2);
        booking.setTotalPrice(new BigDecimal("3000000.00"));
        booking.setPaymentPlan(PaymentPlan.DEPOSIT);
        booking.setPaymentStatus(PaymentStatus.PARTIALLY_PAID);
        booking.setBookingStatus(BookingStatus.CONFIRMED);
        booking.setRemainingDueAt(LocalDateTime.now().plusDays(1));

        payment = new PaymentTransaction();
        payment.setPaymentTransactionId(UUID.randomUUID());
        payment.setBooking(booking);
        payment.setPaymentStage(PaymentStage.REMAINING);
        payment.setStatus(PaymentTransactionStatus.PAID);
        payment.setAmount(new BigDecimal("2000000.00"));
        payment.setPaidAmount(new BigDecimal("2000000.00"));

        when(refundTransactionRepository.sumByBookingAndStatuses(any(), any())).thenReturn(BigDecimal.ZERO);
    }

    @Test
    void validRemainingPaymentCompletesBookingPayment() {
        stubBookingPersistence();
        when(paymentTransactionRepository.sumPaidByBooking(booking.getBookingId()))
                .thenReturn(new BigDecimal("3000000.00"));

        service.applySuccessfulPayment(payment);

        assertEquals(PaymentStatus.PAID, booking.getPaymentStatus());
        assertEquals(BookingStatus.CONFIRMED, booking.getBookingStatus());
        assertEquals(2, schedule.getBookedSlots());
        verify(refundTransactionRepository, never()).save(any());
        verify(tourScheduleRepository, never()).save(any());
    }

    @Test
    void remainingAmountUsesPaidMoneyNetOfCompletedRefunds() {
        when(paymentTransactionRepository.sumPaidByBooking(booking.getBookingId()))
                .thenReturn(new BigDecimal("2000000.00"));
        when(refundTransactionRepository.sumByBookingAndStatuses(any(), any()))
                .thenReturn(new BigDecimal("1000000.00"));

        BigDecimal remaining = service.calculateStageAmount(booking, PaymentStage.REMAINING);

        assertEquals(new BigDecimal("2000000.00"), remaining);
    }

    @Test
    void overdueRemainingPaymentCancelsBookingReleasesCapacityAndCreatesRefund() {
        stubBookingPersistence();
        booking.setRemainingDueAt(LocalDateTime.now().minusMinutes(1));
        when(paymentTransactionRepository.sumPaidByBooking(booking.getBookingId()))
                .thenReturn(new BigDecimal("3000000.00"));
        when(tourScheduleRepository.findByIdForUpdate(schedule.getScheduleId())).thenReturn(Optional.of(schedule));
        when(refundTransactionRepository.findByIdempotencyKeyAndIsDeletedFalse(
                "late-payment:" + payment.getPaymentTransactionId())).thenReturn(Optional.empty());

        service.applySuccessfulPayment(payment);

        assertEquals(BookingStatus.CANCELLED, booking.getBookingStatus());
        assertEquals(PaymentStatus.REFUND_PENDING, booking.getPaymentStatus());
        assertEquals(0, schedule.getBookedSlots());
        assertEquals(5, schedule.getAvailableSlots());
        assertNull(booking.getRemainingDueAt());

        ArgumentCaptor<RefundTransaction> captor = ArgumentCaptor.forClass(RefundTransaction.class);
        verify(refundTransactionRepository).save(captor.capture());
        RefundTransaction refund = captor.getValue();
        assertEquals(payment.getPaidAmount(), refund.getAmount());
        assertEquals(RefundReason.PAYMENT_ADJUSTMENT, refund.getReason());
        assertEquals("late-payment:" + payment.getPaymentTransactionId(), refund.getIdempotencyKey());
    }

    @Test
    void paymentArrivingAfterSchedulerCancellationDoesNotReleaseCapacityTwice() {
        stubBookingPersistence();
        booking.setBookingStatus(BookingStatus.CANCELLED);
        booking.setRemainingDueAt(null);
        when(paymentTransactionRepository.sumPaidByBooking(booking.getBookingId()))
                .thenReturn(new BigDecimal("3000000.00"));
        when(refundTransactionRepository.findByIdempotencyKeyAndIsDeletedFalse(any()))
                .thenReturn(Optional.empty());

        service.applySuccessfulPayment(payment);

        assertEquals(PaymentStatus.REFUND_PENDING, booking.getPaymentStatus());
        assertEquals(2, schedule.getBookedSlots());
        assertEquals(3, schedule.getAvailableSlots());
        verify(tourScheduleRepository, never()).save(any());
        verify(refundTransactionRepository).save(any(RefundTransaction.class));
    }

    @Test
    void duplicateRemainingPaymentRefundsTheExtraMoneyWithoutCancellingConfirmedBooking() {
        stubBookingPersistence();
        booking.setPaymentStatus(PaymentStatus.PAID);
        when(paymentTransactionRepository.sumPaidByBooking(booking.getBookingId()))
                .thenReturn(new BigDecimal("5000000.00"));
        when(refundTransactionRepository.findByIdempotencyKeyAndIsDeletedFalse(any()))
                .thenReturn(Optional.empty());

        service.applySuccessfulPayment(payment);

        assertEquals(BookingStatus.CONFIRMED, booking.getBookingStatus());
        assertEquals(PaymentStatus.REFUND_PENDING, booking.getPaymentStatus());
        assertEquals(2, schedule.getBookedSlots());
        verify(tourScheduleRepository, never()).save(any());
        verify(refundTransactionRepository).save(any(RefundTransaction.class));
    }

    @Test
    void repeatedLatePaymentHandlingReusesExistingRefund() {
        stubBookingPersistence();
        booking.setBookingStatus(BookingStatus.CANCELLED);
        booking.setRemainingDueAt(null);
        RefundTransaction existing = new RefundTransaction();
        existing.setRefundTransactionId(UUID.randomUUID());
        when(paymentTransactionRepository.sumPaidByBooking(booking.getBookingId()))
                .thenReturn(new BigDecimal("3000000.00"));
        when(refundTransactionRepository.findByIdempotencyKeyAndIsDeletedFalse(
                "late-payment:" + payment.getPaymentTransactionId())).thenReturn(Optional.of(existing));

        service.applySuccessfulPayment(payment);

        assertEquals(PaymentStatus.REFUND_PENDING, booking.getPaymentStatus());
        verify(refundTransactionRepository, never()).save(any());
    }

    @Test
    void lateInitialPaymentDoesNotConsumeHeldCapacity() {
        stubBookingPersistence();
        payment.setPaymentStage(PaymentStage.FULL);
        booking.setPaymentPlan(PaymentPlan.FULL_PAYMENT);
        booking.setPaymentStatus(PaymentStatus.UNPAID);
        booking.setBookingStatus(BookingStatus.EXPIRED);
        booking.setHoldExpiresAt(null);
        when(paymentTransactionRepository.sumPaidByBooking(booking.getBookingId()))
                .thenReturn(payment.getPaidAmount());
        when(refundTransactionRepository.findByIdempotencyKeyAndIsDeletedFalse(any()))
                .thenReturn(Optional.empty());

        service.applySuccessfulPayment(payment);

        assertEquals(BookingStatus.EXPIRED, booking.getBookingStatus());
        assertEquals(PaymentStatus.REFUND_PENDING, booking.getPaymentStatus());
        verify(tourScheduleRepository, never()).save(any());
        verify(refundTransactionRepository).save(any(RefundTransaction.class));
    }

    @Test
    void failedRefundKeepsBookingInRefundPendingState() {
        stubBookingPersistence();
        when(paymentTransactionRepository.sumPaidByBooking(booking.getBookingId()))
                .thenReturn(new BigDecimal("1000000.00"));
        when(refundTransactionRepository.sumByBookingAndStatuses(
                booking.getBookingId(), List.of(RefundStatus.REFUNDED)))
                .thenReturn(BigDecimal.ZERO);
        when(refundTransactionRepository.sumByBookingAndStatuses(
                booking.getBookingId(), List.of(
                        RefundStatus.PENDING,
                        RefundStatus.PROCESSING,
                        RefundStatus.FAILED)))
                .thenReturn(new BigDecimal("1000000.00"));

        service.refreshBookingPaymentStatus(booking);

        assertEquals(PaymentStatus.REFUND_PENDING, booking.getPaymentStatus());
    }

    @Test
    void completedAdjustmentRefundRestoresActiveBookingToNetPaymentState() {
        stubBookingPersistence();
        when(paymentTransactionRepository.sumPaidByBooking(booking.getBookingId()))
                .thenReturn(new BigDecimal("2000000.00"));
        when(refundTransactionRepository.sumByBookingAndStatuses(
                booking.getBookingId(), List.of(RefundStatus.REFUNDED)))
                .thenReturn(new BigDecimal("1000000.00"));
        when(refundTransactionRepository.sumByBookingAndStatuses(
                booking.getBookingId(), List.of(
                        RefundStatus.PENDING,
                        RefundStatus.PROCESSING,
                        RefundStatus.FAILED)))
                .thenReturn(BigDecimal.ZERO);

        service.refreshBookingPaymentStatus(booking);

        assertEquals(PaymentStatus.PARTIALLY_PAID, booking.getPaymentStatus());
        assertEquals(new BigDecimal("1000000.00"), booking.getRefundAmount());
    }

    private void stubBookingPersistence() {
        when(bookingRepository.findByIdForUpdate(booking.getBookingId())).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }
}
