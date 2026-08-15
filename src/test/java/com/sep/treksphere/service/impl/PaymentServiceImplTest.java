package com.sep.treksphere.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep.treksphere.config.PaymentWorkflowProperties;
import com.sep.treksphere.dto.request.AdminManualRefundReviewRequest;
import com.sep.treksphere.dto.request.ManualRefundCompletionRequest;
import com.sep.treksphere.entity.Booking;
import com.sep.treksphere.entity.PaymentTransaction;
import com.sep.treksphere.entity.RefundTransaction;
import com.sep.treksphere.entity.Role;
import com.sep.treksphere.entity.Tour;
import com.sep.treksphere.entity.TourSchedule;
import com.sep.treksphere.entity.User;
import com.sep.treksphere.entity.Vendor;
import com.sep.treksphere.entity.VendorPaymentAccount;
import com.sep.treksphere.enums.booking.BookingStatus;
import com.sep.treksphere.enums.booking.PaymentPlan;
import com.sep.treksphere.enums.booking.PaymentStage;
import com.sep.treksphere.enums.booking.PaymentStatus;
import com.sep.treksphere.enums.booking.PaymentTransactionStatus;
import com.sep.treksphere.enums.booking.RefundReason;
import com.sep.treksphere.enums.booking.RefundStatus;
import com.sep.treksphere.enums.booking.PaymentAccountStatus;
import com.sep.treksphere.repository.BookingPolicySnapshotRepository;
import com.sep.treksphere.repository.BookingRepository;
import com.sep.treksphere.repository.PaymentTransactionRepository;
import com.sep.treksphere.repository.PaymentWebhookEventRepository;
import com.sep.treksphere.repository.NotificationRepository;
import com.sep.treksphere.service.NotificationService;
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
import org.springframework.transaction.support.TransactionCallback;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.PaymentLink;
import vn.payos.model.v2.paymentRequests.PaymentLinkStatus;
import vn.payos.service.blocking.v2.paymentRequests.PaymentRequestsService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
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
    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationService notificationService;
    @Mock private PayOsClientFactory payOsClientFactory;
    @Mock private PayOS payOsClient;
    @Mock private PaymentRequestsService paymentRequestsService;
    @Mock private PaymentWorkflowProperties properties;
    @Mock private ObjectMapper objectMapper;
    @Mock private TransactionTemplate transactionTemplate;

    @InjectMocks private PaymentServiceImpl service;

    private Booking booking;
    private TourSchedule schedule;
    private PaymentTransaction payment;
    private User trekker;

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
        trekker = new User();
        trekker.setUserId(UUID.randomUUID());
        trekker.setEmail("trekker@example.com");
        booking.setUser(trekker);

        payment = new PaymentTransaction();
        payment.setPaymentTransactionId(UUID.randomUUID());
        payment.setBooking(booking);
        payment.setPaymentStage(PaymentStage.REMAINING);
        payment.setStatus(PaymentTransactionStatus.PAID);
        payment.setAmount(new BigDecimal("2000000.00"));
        payment.setPaidAmount(new BigDecimal("2000000.00"));

        org.mockito.Mockito.lenient()
                .when(refundTransactionRepository.sumByBookingAndStatuses(any(), any()))
                .thenReturn(BigDecimal.ZERO);
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
                        RefundStatus.FAILED,
                        RefundStatus.AWAITING_VENDOR_ACTION,
                        RefundStatus.MANUAL_REVIEW,
                        RefundStatus.OVERDUE)))
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
                        RefundStatus.FAILED,
                        RefundStatus.AWAITING_VENDOR_ACTION,
                        RefundStatus.MANUAL_REVIEW,
                        RefundStatus.OVERDUE)))
                .thenReturn(BigDecimal.ZERO);

        service.refreshBookingPaymentStatus(booking);

        assertEquals(PaymentStatus.PARTIALLY_PAID, booking.getPaymentStatus());
        assertEquals(new BigDecimal("1000000.00"), booking.getRefundAmount());
    }

    @Test
    void automaticRefundRetriesWhenVendorPayOsBalanceIsInsufficient() {
        RefundTransaction refund = pendingRefund();
        stubTransactionExecution();
        stubBookingPersistence();
        when(refundTransactionRepository.findByIdForUpdate(refund.getRefundTransactionId()))
                .thenReturn(Optional.of(refund));
        when(refundTransactionRepository.save(any(RefundTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentTransactionRepository.sumPaidByBooking(booking.getBookingId()))
                .thenReturn(new BigDecimal("1000000.00"));
        stubPendingRefundAmount();
        payment.getVendorPaymentAccount().setPayoutStatus(PaymentAccountStatus.ACTIVE);
        payment.getVendorPaymentAccount().setPayoutProviderChannelId("payout-client");
        payment.getVendorPaymentAccount().setPayoutApiKeyEncrypted("encrypted-api-key");
        payment.getVendorPaymentAccount().setPayoutChecksumKeyEncrypted("encrypted-checksum-key");
        when(payOsClientFactory.getPayoutClient(payment.getVendorPaymentAccount()))
                .thenThrow(new RuntimeException("Insufficient balance"));

        var response = service.processRefundAutomatically(refund.getRefundTransactionId());

        assertEquals(RefundStatus.AWAITING_VENDOR_ACTION, response.getStatus());
        assertEquals(1, response.getAttemptCount());
        assertNotNull(response.getNextRetryAt());
        assertEquals(PaymentStatus.REFUND_PENDING, booking.getPaymentStatus());
    }

    @Test
    void automaticRefundWaitsUntilCustomerProvidesCompleteDestination() {
        RefundTransaction refund = pendingRefund();
        refund.setDestinationBin(null);
        refund.setDestinationBankName(null);
        refund.setDestinationAccountNumber(null);
        refund.setDestinationAccountName(null);
        stubTransactionExecution();
        when(refundTransactionRepository.findByIdForUpdate(refund.getRefundTransactionId()))
                .thenReturn(Optional.of(refund));

        var response = service.processRefundAutomatically(refund.getRefundTransactionId());

        assertEquals(RefundStatus.PENDING, response.getStatus());
        assertNull(response.getDestinationBin());
        assertNull(response.getDestinationAccountNumber());
        verify(payOsClientFactory, never()).getPayoutClient(any());
    }

    @Test
    void cancellingPendingCheckoutClosesPayOsLinkAndLocalAttempt() {
        stubTransactionExecution();
        when(bookingRepository.findByIdForUpdate(booking.getBookingId())).thenReturn(Optional.of(booking));
        VendorPaymentAccount account = new VendorPaymentAccount();
        account.setVendorPaymentAccountId(UUID.randomUUID());
        account.setVendor(schedule.getTour().getVendor());
        payment.setVendorPaymentAccount(account);
        payment.setStatus(PaymentTransactionStatus.PENDING);
        payment.setGatewayOrderCode(123456L);
        payment.setAttemptNumber((short) 1);

        PaymentLink pendingLink = mock(PaymentLink.class);
        PaymentLink cancelledLink = mock(PaymentLink.class);
        when(pendingLink.getStatus()).thenReturn(PaymentLinkStatus.PENDING);
        when(cancelledLink.getStatus()).thenReturn(PaymentLinkStatus.CANCELLED);

        when(userRepository.findByEmail(trekker.getEmail())).thenReturn(Optional.of(trekker));
        when(paymentTransactionRepository.findByGatewayOrderCodeAndIsDeletedFalse(
                payment.getGatewayOrderCode())).thenReturn(Optional.of(payment));
        when(vendorPaymentAccountRepository.findById(account.getVendorPaymentAccountId()))
                .thenReturn(Optional.of(account));
        when(payOsClientFactory.getClient(account)).thenReturn(payOsClient);
        when(payOsClient.paymentRequests()).thenReturn(paymentRequestsService);
        when(paymentRequestsService.get(payment.getGatewayOrderCode())).thenReturn(pendingLink);
        when(paymentRequestsService.cancel(payment.getGatewayOrderCode(),
                "Khach hang huy phien thanh toan")).thenReturn(cancelledLink);
        when(paymentTransactionRepository.findByIdForUpdate(payment.getPaymentTransactionId()))
                .thenReturn(Optional.of(payment));
        when(paymentTransactionRepository.save(any(PaymentTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.cancelCheckout(
                trekker.getEmail(), booking.getBookingId(), payment.getGatewayOrderCode());

        assertEquals(PaymentTransactionStatus.CANCELLED, response.getStatus());
        assertEquals(PaymentTransactionStatus.CANCELLED, payment.getStatus());
        assertNotNull(payment.getCancelledAt());
        verify(paymentRequestsService).cancel(payment.getGatewayOrderCode(),
                "Khach hang huy phien thanh toan");
    }

    @Test
    void manualRefundSubmissionWaitsForAdminReview() {
        RefundTransaction refund = pendingRefund();
        User vendorManager = new User();
        vendorManager.setUserId(UUID.randomUUID());
        vendorManager.setEmail("vendor@example.com");
        ManualRefundCompletionRequest request = new ManualRefundCompletionRequest();
        request.setReceiptImageUrl("https://cdn.example.com/refund-receipt.jpg");
        request.setNote("Đã chuyển đủ tiền");
        stubTransactionExecution();
        stubBookingPersistence();
        when(userRepository.findByEmail(vendorManager.getEmail())).thenReturn(Optional.of(vendorManager));
        when(vendorRepository.findByManager_Email(vendorManager.getEmail()))
                .thenReturn(Optional.of(schedule.getTour().getVendor()));
        when(refundTransactionRepository.findByIdForUpdate(refund.getRefundTransactionId()))
                .thenReturn(Optional.of(refund));
        when(refundTransactionRepository.save(any(RefundTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentTransactionRepository.sumPaidByBooking(booking.getBookingId()))
                .thenReturn(refund.getAmount());
        stubPendingRefundAmount();
        when(userRepository.findDistinctByRoles_RoleNameAndIsDeletedFalse("ADMIN"))
                .thenReturn(List.of());

        var response = service.completeManualRefund(
                vendorManager.getEmail(), refund.getRefundTransactionId(), request);

        assertEquals(RefundStatus.MANUAL_REVIEW, response.getStatus());
        assertNull(response.getManualBankReference());
        assertEquals(request.getReceiptImageUrl(), response.getManualReceiptUrl());
        assertNull(response.getCompletedAt());
        assertEquals(PaymentStatus.REFUND_PENDING, booking.getPaymentStatus());
    }

    @Test
    void adminApprovalIsRequiredBeforeManualRefundBecomesRefunded() {
        RefundTransaction refund = pendingRefund();
        refund.setStatus(RefundStatus.MANUAL_REVIEW);
        refund.setManualBankReference("FT260813001");
        refund.setManualReceiptUrl("https://cdn.example.com/refund-receipt.jpg");
        booking.setBookingStatus(BookingStatus.CANCELLED);
        payment.getVendorPaymentAccount().setRefundHold(true);
        User admin = new User();
        admin.setUserId(UUID.randomUUID());
        admin.setEmail("admin@example.com");
        Role role = new Role();
        role.setRoleName("ADMIN");
        admin.getRoles().add(role);
        AdminManualRefundReviewRequest request = new AdminManualRefundReviewRequest();
        request.setApproved(true);
        request.setNote("Đã đối chiếu đúng số tiền và mã giao dịch");
        stubTransactionExecution();
        stubBookingPersistence();
        when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        when(refundTransactionRepository.findByIdForUpdate(refund.getRefundTransactionId()))
                .thenReturn(Optional.of(refund));
        when(refundTransactionRepository.save(any(RefundTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentTransactionRepository.sumPaidByBooking(booking.getBookingId()))
                .thenReturn(refund.getAmount());
        when(refundTransactionRepository.sumByBookingAndStatuses(
                booking.getBookingId(), List.of(RefundStatus.REFUNDED)))
                .thenReturn(refund.getAmount());
        when(vendorPaymentAccountRepository.findByVendor_VendorIdAndRefundHoldTrueAndIsDeletedFalse(
                schedule.getTour().getVendor().getVendorId()))
                .thenReturn(List.of(payment.getVendorPaymentAccount()));

        var response = service.reviewManualRefund(
                admin.getEmail(), refund.getRefundTransactionId(), request);

        assertEquals(RefundStatus.REFUNDED, response.getStatus());
        assertNotNull(response.getCompletedAt());
        assertEquals(PaymentStatus.REFUNDED, booking.getPaymentStatus());
        assertEquals(false, payment.getVendorPaymentAccount().getRefundHold());
    }

    @Test
    void overdueRefundPlacesVendorOnOnlineBookingHold() {
        RefundTransaction refund = pendingRefund();
        refund.setDueAt(LocalDateTime.now().minusMinutes(1));
        stubTransactionExecution();
        stubBookingPersistence();
        when(refundTransactionRepository.findPastDueIds(any(Collection.class), any(LocalDateTime.class), any()))
                .thenReturn(List.of(refund.getRefundTransactionId()));
        when(refundTransactionRepository.findDueForAutomaticProcessing(
                any(Collection.class), any(LocalDateTime.class), any())).thenReturn(List.of());
        when(refundTransactionRepository.findByIdForUpdate(refund.getRefundTransactionId()))
                .thenReturn(Optional.of(refund));
        when(refundTransactionRepository.save(any(RefundTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentTransactionRepository.sumPaidByBooking(booking.getBookingId()))
                .thenReturn(new BigDecimal("1000000.00"));
        stubPendingRefundAmount();
        when(userRepository.findDistinctByRoles_RoleNameAndIsDeletedFalse("ADMIN"))
                .thenReturn(List.of());

        service.processDueRefundsAutomatically();

        assertEquals(RefundStatus.OVERDUE, refund.getStatus());
        assertEquals(true, payment.getVendorPaymentAccount().getRefundHold());
        verify(vendorPaymentAccountRepository).save(payment.getVendorPaymentAccount());
    }

    private RefundTransaction pendingRefund() {
        VendorPaymentAccount account = new VendorPaymentAccount();
        account.setVendor(schedule.getTour().getVendor());
        payment.setVendorPaymentAccount(account);

        RefundTransaction refund = new RefundTransaction();
        refund.setRefundTransactionId(UUID.randomUUID());
        refund.setBooking(booking);
        refund.setPaymentTransaction(payment);
        refund.setAmount(new BigDecimal("1000000.00"));
        refund.setReason(RefundReason.TREKKER_CANCEL);
        refund.setIdempotencyKey("refund-test-" + refund.getRefundTransactionId());
        refund.setDestinationBin("970422");
        refund.setDestinationBankName("MB Bank");
        refund.setDestinationAccountNumber("0123456789");
        refund.setDestinationAccountName("NGUYEN VAN A");
        refund.setRequestedAt(LocalDateTime.now());
        refund.setDueAt(LocalDateTime.now().plusHours(48));
        return refund;
    }

    private void stubPendingRefundAmount() {
        when(refundTransactionRepository.sumByBookingAndStatuses(
                booking.getBookingId(), List.of(
                        RefundStatus.PENDING,
                        RefundStatus.PROCESSING,
                        RefundStatus.FAILED,
                        RefundStatus.AWAITING_VENDOR_ACTION,
                        RefundStatus.MANUAL_REVIEW,
                        RefundStatus.OVERDUE)))
                .thenReturn(new BigDecimal("1000000.00"));
    }

    @SuppressWarnings("unchecked")
    private void stubTransactionExecution() {
        org.mockito.Mockito.lenient().when(transactionTemplate.execute(any(TransactionCallback.class)))
                .thenAnswer(invocation ->
                        ((TransactionCallback<Object>) invocation.getArgument(0)).doInTransaction(null));
        org.mockito.Mockito.lenient().doAnswer(invocation -> {
            java.util.function.Consumer<org.springframework.transaction.TransactionStatus> callback =
                    invocation.getArgument(0);
            callback.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }

    private void stubBookingPersistence() {
        when(bookingRepository.findByIdForUpdate(booking.getBookingId())).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }
}
