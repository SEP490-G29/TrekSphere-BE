package com.sep.treksphere.service.impl;

import com.sep.treksphere.dto.request.BookingCancelRequest;
import com.sep.treksphere.config.PaymentWorkflowProperties;
import com.sep.treksphere.entity.*;
import com.sep.treksphere.enums.booking.*;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;
import com.sep.treksphere.repository.NotificationRepository;
import org.springframework.context.ApplicationEventPublisher;
import com.sep.treksphere.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CancellationServiceImplTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private BookingPolicySnapshotRepository snapshotRepository;
    @Mock private PaymentTransactionRepository paymentTransactionRepository;
    @Mock private RefundTransactionRepository refundTransactionRepository;
    @Mock private TourScheduleRepository tourScheduleRepository;
    @Mock private VoucherRepository voucherRepository;
    @Mock private UserRepository userRepository;
    @Mock private VendorRepository vendorRepository;
    @Mock private VendorStaffRepository vendorStaffRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private PaymentWorkflowProperties paymentProperties;

    @InjectMocks private CancellationServiceImpl service;

    private UUID bookingId;
    private User user;
    private Booking booking;
    private TourSchedule schedule;
    private BookingPolicySnapshot snapshot;
    private PaymentTransaction payment;

    @BeforeEach
    void setUp() {
        bookingId = UUID.randomUUID();
        user = new User();
        user.setUserId(UUID.randomUUID());
        user.setEmail("trekker@example.com");

        Vendor vendor = new Vendor();
        vendor.setVendorId(UUID.randomUUID());
        vendor.setManager(new User());
        Tour tour = new Tour();
        tour.setTourId(UUID.randomUUID());
        tour.setVendor(vendor);

        schedule = new TourSchedule();
        schedule.setScheduleId(UUID.randomUUID());
        schedule.setTour(tour);
        schedule.setDepartureDate(LocalDate.now().plusDays(10));
        schedule.setAvailableSlots(5);
        schedule.setBookedSlots(2);
        schedule.setHeldSlots(0);

        booking = new Booking();
        booking.setBookingId(bookingId);
        booking.setUser(user);
        booking.setSchedule(schedule);
        booking.setBookingStatus(BookingStatus.CONFIRMED);
        booking.setPaymentStatus(PaymentStatus.PAID);
        booking.setNumberOfParticipants(2);
        booking.setTotalPrice(new BigDecimal("3000000.00"));

        snapshot = new BookingPolicySnapshot();
        snapshot.setBookingId(bookingId);
        snapshot.setBooking(booking);
        snapshot.setNonRefundableCost(new BigDecimal("500000.00"));
        snapshot.setPolicyJson(List.of(
                Map.of("cancelBeforeDays", 14, "refundPercentage", 100, "description", "Từ 14 ngày"),
                Map.of("cancelBeforeDays", 7, "refundPercentage", 70, "description", "Từ 7 ngày"),
                Map.of("cancelBeforeDays", 3, "refundPercentage", 30, "description", "Từ 3 ngày")
        ));

        payment = new PaymentTransaction();
        payment.setPaymentTransactionId(UUID.randomUUID());
        payment.setBooking(booking);
        payment.setStatus(PaymentTransactionStatus.PAID);
        payment.setPaidAmount(new BigDecimal("3000000.00"));

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(snapshotRepository.findById(bookingId)).thenReturn(Optional.of(snapshot));
        when(paymentTransactionRepository.sumPaidByBooking(bookingId)).thenReturn(new BigDecimal("3000000.00"));
        when(refundTransactionRepository.sumByBookingAndStatuses(eq(bookingId), anyCollection()))
                .thenReturn(BigDecimal.ZERO);
        lenient().when(paymentProperties.getRefundDueDuration()).thenReturn(java.time.Duration.ofHours(48));
    }

    @Test
    void quoteUsesBookingSnapshotAndNonRefundableCost() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        var quote = service.quoteForTrekker(user.getEmail(), bookingId);

        assertEquals(70, quote.getRefundPercentage());
        assertEquals(new BigDecimal("1750000"), quote.getRefundAmount());
        assertEquals(new BigDecimal("1250000.00"), quote.getCancellationFee());
        assertEquals("Từ 7 ngày", quote.getAppliedPolicyDescription());
        assertTrue(quote.getRefundDestinationRequired());
    }

    @Test
    void legacyBookingWithoutCancellationPolicyGetsFullRefundQuote() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(snapshotRepository.findById(bookingId)).thenReturn(Optional.empty());

        var quote = service.quoteForTrekker(user.getEmail(), bookingId);

        assertEquals(100, quote.getRefundPercentage());
        assertEquals(new BigDecimal("3000000.00"), quote.getRefundAmount());
        assertEquals(BigDecimal.ZERO, quote.getNonRefundableCost());
    }

    @Test
    void quoteRequiresCustomerEnteredDestinationAndDoesNotPrefillFromWebhook() {
        payment.getGatewayMetadata().put("counterAccountBankId", "970422");
        payment.getGatewayMetadata().put("counterAccountBankName", "MB Bank");
        payment.getGatewayMetadata().put("counterAccountNumber", "0123456789");
        payment.getGatewayMetadata().put("counterAccountName", "NGUYEN VAN A");
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        var quote = service.quoteForTrekker(user.getEmail(), bookingId);

        assertTrue(quote.getRefundDestinationRequired());
        assertNull(quote.getRefundBankBin());
        assertNull(quote.getRefundBankName());
        assertNull(quote.getRefundAccountNumber());
        assertNull(quote.getRefundAccountName());
    }

    @Test
    void cancellationRejectsMissingCustomerDestinationEvenWhenWebhookHasSenderAccount() {
        payment.getGatewayMetadata().put("counterAccountBankId", "970422");
        payment.getGatewayMetadata().put("counterAccountBankName", "MB Bank");
        payment.getGatewayMetadata().put("counterAccountNumber", "0123456789");
        payment.getGatewayMetadata().put("counterAccountName", "NGUYEN VAN A");
        when(bookingRepository.findScheduleIdByBookingId(bookingId)).thenReturn(Optional.of(schedule.getScheduleId()));
        when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.of(booking));
        when(tourScheduleRepository.findByIdForUpdate(schedule.getScheduleId())).thenReturn(Optional.of(schedule));

        BookingCancelRequest request = new BookingCancelRequest();
        request.setCancellationReason("Thay đổi kế hoạch");

        AppException exception = assertThrows(AppException.class,
                () -> service.cancelByTrekker(user.getEmail(), bookingId, request));

        assertEquals(ErrorCode.REFUND_DESTINATION_REQUIRED, exception.getErrorCode());
        verify(paymentTransactionRepository, never())
                .findByBooking_BookingIdAndIsDeletedFalseOrderByCreatedAtAsc(bookingId);
    }

    @Test
    void cancellationReleasesCapacityAndCreatesPendingRefund() {
        when(bookingRepository.findScheduleIdByBookingId(bookingId)).thenReturn(Optional.of(schedule.getScheduleId()));
        when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.of(booking));
        when(tourScheduleRepository.findByIdForUpdate(schedule.getScheduleId())).thenReturn(Optional.of(schedule));
        when(paymentTransactionRepository.findByBooking_BookingIdAndIsDeletedFalseOrderByCreatedAtAsc(bookingId))
                .thenReturn(List.of(payment));
        when(refundTransactionRepository.sumByPaymentAndStatuses(eq(payment.getPaymentTransactionId()), anyCollection()))
                .thenReturn(BigDecimal.ZERO);
        when(refundTransactionRepository.save(any(RefundTransaction.class))).thenAnswer(invocation -> {
            RefundTransaction refund = invocation.getArgument(0);
            refund.setRefundTransactionId(UUID.randomUUID());
            return refund;
        });
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingCancelRequest request = new BookingCancelRequest();
        request.setCancellationReason("Thay đổi kế hoạch");
        request.setRefundBankBin("970422");
        request.setRefundBankName("MB Bank");
        request.setRefundAccountNumber("0123456789");
        request.setRefundAccountName("NGUYEN VAN A");

        Booking result = service.cancelByTrekker(user.getEmail(), bookingId, request);

        assertEquals(BookingStatus.CANCELLED, result.getBookingStatus());
        assertEquals(PaymentStatus.REFUND_PENDING, result.getPaymentStatus());
        assertEquals(0, schedule.getBookedSlots());
        assertEquals(7, schedule.getAvailableSlots());

        ArgumentCaptor<RefundTransaction> refundCaptor = ArgumentCaptor.forClass(RefundTransaction.class);
        verify(refundTransactionRepository).save(refundCaptor.capture());
        RefundTransaction refund = refundCaptor.getValue();
        assertEquals(new BigDecimal("1750000"), refund.getAmount());
        assertEquals(RefundStatus.PENDING, refund.getStatus());
        assertEquals(RefundReason.TREKKER_CANCEL, refund.getReason());
        assertEquals("MB Bank", refund.getDestinationBankName());
        assertEquals("0123456789", refund.getDestinationAccountNumber());
    }
}
