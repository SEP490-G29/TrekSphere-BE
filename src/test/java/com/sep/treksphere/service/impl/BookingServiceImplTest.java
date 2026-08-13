package com.sep.treksphere.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep.treksphere.config.PaymentWorkflowProperties;
import com.sep.treksphere.dto.request.BookingParticipantRequest;
import com.sep.treksphere.dto.request.BookingRequest;
import com.sep.treksphere.entity.Booking;
import com.sep.treksphere.entity.Tour;
import com.sep.treksphere.entity.TourSchedule;
import com.sep.treksphere.entity.User;
import com.sep.treksphere.entity.Vendor;
import com.sep.treksphere.enums.booking.BookingStatus;
import com.sep.treksphere.enums.booking.PaymentPlan;
import com.sep.treksphere.enums.booking.PaymentStatus;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;
import com.sep.treksphere.mapper.BookingMapper;
import com.sep.treksphere.repository.BookingRepository;
import com.sep.treksphere.repository.PaymentTransactionRepository;
import com.sep.treksphere.repository.RefundTransactionRepository;
import com.sep.treksphere.repository.TourParticipationPolicyRepository;
import com.sep.treksphere.repository.TourPaymentPolicyRepository;
import com.sep.treksphere.repository.TourScheduleRepository;
import com.sep.treksphere.repository.UserRepository;
import com.sep.treksphere.repository.VendorPaymentAccountRepository;
import com.sep.treksphere.repository.VendorRepository;
import com.sep.treksphere.repository.VendorStaffRepository;
import com.sep.treksphere.repository.VoucherRepository;
import com.sep.treksphere.service.CancellationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private TourScheduleRepository tourScheduleRepository;
    @Mock private VoucherRepository voucherRepository;
    @Mock private UserRepository userRepository;
    @Mock private VendorRepository vendorRepository;
    @Mock private VendorStaffRepository vendorStaffRepository;
    @Mock private TourPaymentPolicyRepository tourPaymentPolicyRepository;
    @Mock private TourParticipationPolicyRepository tourParticipationPolicyRepository;
    @Mock private VendorPaymentAccountRepository vendorPaymentAccountRepository;
    @Mock private PaymentTransactionRepository paymentTransactionRepository;
    @Mock private RefundTransactionRepository refundTransactionRepository;
    @Mock private CancellationService cancellationService;
    @Mock private PaymentWorkflowProperties paymentProperties;
    @Spy private BookingMapper bookingMapper = new BookingMapper();
    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks private BookingServiceImpl service;

    private User user;
    private BookingRequest request;
    private Booking existing;

    @BeforeEach
    void setUp() throws Exception {
        user = new User();
        user.setUserId(UUID.randomUUID());
        user.setEmail("trekker@example.com");

        BookingParticipantRequest participant = new BookingParticipantRequest();
        participant.setFullName("Nguyen Van A");
        participant.setPhone("0900000000");
        request = new BookingRequest();
        request.setScheduleId(UUID.randomUUID());
        request.setPaymentPlan(PaymentPlan.FULL_PAYMENT);
        request.setParticipationPolicyAccepted(true);
        request.setParticipants(List.of(participant));

        Vendor vendor = new Vendor();
        vendor.setVendorId(UUID.randomUUID());
        Tour tour = new Tour();
        tour.setTourId(UUID.randomUUID());
        tour.setVendor(vendor);
        TourSchedule schedule = new TourSchedule();
        schedule.setScheduleId(request.getScheduleId());
        schedule.setTour(tour);
        schedule.setPrice(new BigDecimal("1000000.00"));

        existing = new Booking();
        existing.setBookingId(UUID.randomUUID());
        existing.setBookingCode("BK-IDEMPOTENT");
        existing.setUser(user);
        existing.setSchedule(schedule);
        existing.setNumberOfParticipants(1);
        existing.setOriginalPrice(new BigDecimal("1000000.00"));
        existing.setDiscountAmount(BigDecimal.ZERO);
        existing.setTotalPrice(new BigDecimal("1000000.00"));
        existing.setPaymentPlan(PaymentPlan.FULL_PAYMENT);
        existing.setPaymentStatus(PaymentStatus.UNPAID);
        existing.setBookingStatus(BookingStatus.PAYMENT_PENDING);
        existing.setBookingRequestKey("stable-key");
        existing.setBookingRequestHash(hash(request));

        when(userRepository.findByEmailForUpdate(user.getEmail())).thenReturn(Optional.of(user));
        when(bookingRepository.findByUser_UserIdAndBookingRequestKeyAndIsDeletedFalse(
                user.getUserId(), "stable-key")).thenReturn(Optional.of(existing));
    }

    @Test
    void retryWithSameKeyAndPayloadReturnsExistingBookingWithoutHoldingAnotherSlot() {
        when(paymentTransactionRepository.sumPaidByBooking(existing.getBookingId())).thenReturn(BigDecimal.ZERO);
        when(paymentTransactionRepository.existsByBooking_BookingIdAndIsDeletedFalse(existing.getBookingId()))
                .thenReturn(false);
        when(refundTransactionRepository.sumByBookingAndStatuses(eq(existing.getBookingId()), anyCollection()))
                .thenReturn(BigDecimal.ZERO);

        var response = service.createBooking(user.getEmail(), "stable-key", request);

        assertEquals(existing.getBookingId().toString(), response.getBookingId());
        verify(tourScheduleRepository, never()).findByIdForUpdate(request.getScheduleId());
        verify(bookingRepository, never()).save(existing);
    }

    @Test
    void reuseOfSameKeyWithDifferentPayloadIsRejected() {
        request.setVoucherCode("DIFFERENT");

        AppException exception = assertThrows(AppException.class,
                () -> service.createBooking(user.getEmail(), "stable-key", request));

        assertEquals(ErrorCode.IDEMPOTENCY_CONFLICT, exception.getErrorCode());
        verify(tourScheduleRepository, never()).findByIdForUpdate(request.getScheduleId());
    }

    private String hash(BookingRequest bookingRequest) throws Exception {
        byte[] serialized = objectMapper.writeValueAsString(bookingRequest).getBytes(StandardCharsets.UTF_8);
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(serialized));
    }
}
