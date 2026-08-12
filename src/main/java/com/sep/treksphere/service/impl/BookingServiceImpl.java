package com.sep.treksphere.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep.treksphere.config.PaymentWorkflowProperties;
import com.sep.treksphere.dto.request.*;
import com.sep.treksphere.dto.response.*;
import com.sep.treksphere.entity.*;
import com.sep.treksphere.enums.booking.*;
import com.sep.treksphere.enums.tour.ScheduleStatus;
import com.sep.treksphere.enums.voucher.DiscountType;
import com.sep.treksphere.enums.voucher.VoucherStatus;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;
import com.sep.treksphere.mapper.BookingMapper;
import com.sep.treksphere.repository.*;
import com.sep.treksphere.service.BookingService;
import com.sep.treksphere.service.CancellationService;
import com.sep.treksphere.utils.PaginationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private static final Set<RefundStatus> PENDING_REFUNDS = EnumSet.of(
            RefundStatus.PENDING, RefundStatus.PROCESSING, RefundStatus.FAILED);

    private final BookingRepository bookingRepository;
    private final TourScheduleRepository tourScheduleRepository;
    private final VoucherRepository voucherRepository;
    private final UserRepository userRepository;
    private final VendorRepository vendorRepository;
    private final VendorStaffRepository vendorStaffRepository;
    private final TourPaymentPolicyRepository tourPaymentPolicyRepository;
    private final TourParticipationPolicyRepository tourParticipationPolicyRepository;
    private final VendorPaymentAccountRepository vendorPaymentAccountRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final RefundTransactionRepository refundTransactionRepository;
    private final CancellationService cancellationService;
    private final BookingMapper bookingMapper;
    private final PaymentWorkflowProperties paymentProperties;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<BookingResponse> getMyBookingHistory(String email, BookingStatus status,
            Pageable pageable) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        Page<Booking> bookingPage = bookingRepository.findByUserAndFilters(user, status, pageable);
        return PaginationUtils.toPaginationResponse(bookingPage.map(bookingMapper::toBookingResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public BookingDetailResponse getBookingDetail(String email, UUID bookingId) {
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        Booking booking = bookingRepository.findById(bookingId)
                .filter(b -> !Boolean.TRUE.equals(b.getIsDeleted()))
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
        assertCanView(currentUser, email, booking);
        return toDetail(booking);
    }

    @Override
    @Transactional
    public BookingDetailResponse createBooking(String email, String idempotencyKey, BookingRequest request) {
        User user = userRepository.findByEmailForUpdate(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        if (!StringUtils.hasText(idempotencyKey) || idempotencyKey.length() > 255) {
            throw new AppException(ErrorCode.VALIDATION_ERROR,
                    "Idempotency-Key là bắt buộc và không được dài quá 255 ký tự.");
        }
        String requestHash = hashBookingRequest(request);
        Optional<Booking> existing = bookingRepository
                .findByUser_UserIdAndBookingRequestKeyAndIsDeletedFalse(user.getUserId(), idempotencyKey.trim());
        if (existing.isPresent()) {
            if (!Objects.equals(existing.get().getBookingRequestHash(), requestHash)) {
                throw new AppException(ErrorCode.IDEMPOTENCY_CONFLICT);
            }
            return toDetail(existing.get());
        }
        TourSchedule schedule = tourScheduleRepository.findByIdForUpdate(request.getScheduleId())
                .orElseThrow(() -> new AppException(ErrorCode.SCHEDULE_NOT_FOUND));
        validateSchedule(schedule, request.getParticipants().size());
        requireOnlinePaymentAccount(schedule);

        TourParticipationPolicy participationPolicy = tourParticipationPolicyRepository
                .findByTourIdAndIsActiveTrueAndIsDeletedFalse(schedule.getTour().getTourId())
                .orElseThrow(() -> new AppException(ErrorCode.VALIDATION_ERROR,
                        "Tour chưa có điều kiện tham gia nên chưa thể nhận đặt online."));
        validateParticipation(participationPolicy, request, schedule);

        TourPaymentPolicy policy = tourPaymentPolicyRepository
                .findByTourIdAndIsActiveTrueAndIsDeletedFalse(schedule.getTour().getTourId())
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_PLAN_NOT_ALLOWED,
                        "Tour chưa có chính sách thanh toán hoạt động."));
        PaymentPlan plan = resolvePaymentPlan(request.getPaymentPlan(), policy, schedule);

        int participantCount = request.getParticipants().size();
        BigDecimal originalPrice = schedule.getPrice().multiply(BigDecimal.valueOf(participantCount));
        Voucher voucher = resolveAndReserveVoucher(request.getVoucherCode(), schedule, originalPrice);
        BigDecimal discountAmount = calculateDiscount(voucher, originalPrice);
        BigDecimal totalPrice = originalPrice.subtract(discountAmount).setScale(2, RoundingMode.HALF_UP);

        Booking booking = new Booking();
        booking.setBookingCode(generateBookingCode());
        booking.setBookingRequestKey(idempotencyKey.trim());
        booking.setBookingRequestHash(requestHash);
        booking.setUser(user);
        booking.setSchedule(schedule);
        booking.setVoucher(voucher);
        booking.setVoucherState(voucher == null ? VoucherReservationState.NONE : VoucherReservationState.RESERVED);
        booking.setNumberOfParticipants(participantCount);
        booking.setOriginalPrice(originalPrice);
        booking.setTotalPrice(totalPrice);
        booking.setDiscountAmount(discountAmount);
        booking.setPaymentPlan(plan);
        booking.setPaymentStatus(PaymentStatus.UNPAID);
        booking.setBookingStatus(BookingStatus.PAYMENT_PENDING);
        booking.setHoldExpiresAt(LocalDateTime.now().plus(paymentProperties.getHoldDuration()));
        booking.setParticipationPolicyAcceptedAt(LocalDateTime.now());
        if (plan == PaymentPlan.DEPOSIT) {
            booking.setRemainingDueAt(schedule.getDepartureDate().atTime(LocalTime.MIN)
                    .minusDays(policy.getRemainingDueDaysBeforeDeparture()));
        }

        for (var participantRequest : request.getParticipants()) {
            BookingParticipant participant = new BookingParticipant();
            participant.setBooking(booking);
            participant.setFullName(participantRequest.getFullName());
            participant.setDateOfBirth(participantRequest.getDateOfBirth());
            participant.setGender(participantRequest.getGender());
            participant.setIdNumber(participantRequest.getIdNumber());
            participant.setPhone(participantRequest.getPhone());
            participant.setEmail(participantRequest.getEmail());
            participant.setAddress(participantRequest.getAddress());
            participant.setSpecialRequirements(participantRequest.getSpecialRequirements());
            booking.getParticipants().add(participant);
        }

        schedule.setAvailableSlots(schedule.getAvailableSlots() - participantCount);
        schedule.setHeldSlots(schedule.getHeldSlots() + participantCount);
        tourScheduleRepository.save(schedule);
        Booking saved = bookingRepository.save(booking);
        return toDetail(saved);
    }

    private void validateSchedule(TourSchedule schedule, int participantCount) {
        if (schedule.getStatus() != ScheduleStatus.OPEN) {
            throw new AppException(ErrorCode.INVALID_BOOKING_STATUS, "Lịch khởi hành hiện không mở nhận khách.");
        }
        if (!schedule.getDepartureDate().isAfter(LocalDate.now())) {
            throw new AppException(ErrorCode.SCHEDULE_DEPARTURE_IN_PAST);
        }
        if (schedule.getAvailableSlots() < participantCount) {
            throw new AppException(ErrorCode.NOT_ENOUGH_SLOTS);
        }
    }

    private void requireOnlinePaymentAccount(TourSchedule schedule) {
        boolean enabled = vendorPaymentAccountRepository
                .existsByVendor_VendorIdAndProviderAndOnboardingStatusAndIsDefaultTrueAndIsDeletedFalse(
                        schedule.getTour().getVendor().getVendorId(),
                        PaymentProvider.PAYOS,
                        PaymentAccountStatus.ACTIVE);
        if (!enabled) {
            throw new AppException(ErrorCode.PAYMENT_ACCOUNT_NOT_CONFIGURED,
                    "Nhà tổ chức chưa hoàn tất kết nối payOS nên tour chưa nhận đặt online.");
        }
    }

    private void validateParticipation(TourParticipationPolicy policy, BookingRequest request,
                                       TourSchedule schedule) {
        if (!Boolean.TRUE.equals(request.getParticipationPolicyAccepted())) {
            throw new AppException(ErrorCode.VALIDATION_ERROR,
                    "Bạn cần xác nhận điều kiện tham gia trước khi đặt tour.");
        }

        for (BookingParticipantRequest participant : request.getParticipants()) {
            if (participant.getDateOfBirth() == null) continue;
            int ageAtDeparture = Period.between(participant.getDateOfBirth(), schedule.getDepartureDate()).getYears();
            if (policy.getMinAge() != null && ageAtDeparture < policy.getMinAge()) {
                throw new AppException(ErrorCode.VALIDATION_ERROR,
                        participant.getFullName() + " chưa đủ tuổi tối thiểu của tour ("
                                + policy.getMinAge() + " tuổi).");
            }
            if (policy.getMaxAge() != null && ageAtDeparture > policy.getMaxAge()) {
                throw new AppException(ErrorCode.VALIDATION_ERROR,
                        participant.getFullName() + " vượt quá độ tuổi tối đa của tour ("
                                + policy.getMaxAge() + " tuổi).");
            }
        }
    }

    private PaymentPlan resolvePaymentPlan(PaymentPlan requested, TourPaymentPolicy policy, TourSchedule schedule) {
        PaymentPlan selected = requested;
        if (selected == null) {
            selected = policy.getPaymentOption() == PaymentOption.DEPOSIT_ONLY
                    ? PaymentPlan.DEPOSIT
                    : PaymentPlan.FULL_PAYMENT;
        }
        if (policy.getPaymentOption() == PaymentOption.FULL_PAYMENT_ONLY && selected == PaymentPlan.DEPOSIT
                || policy.getPaymentOption() == PaymentOption.DEPOSIT_ONLY && selected == PaymentPlan.FULL_PAYMENT) {
            throw new AppException(ErrorCode.PAYMENT_PLAN_NOT_ALLOWED);
        }
        if (selected == PaymentPlan.DEPOSIT) {
            if (policy.getDepositType() == null || policy.getDepositValue() == null
                    || policy.getRemainingDueDaysBeforeDeparture() == null) {
                throw new AppException(ErrorCode.PAYMENT_PLAN_NOT_ALLOWED, "Cấu hình đặt cọc của tour chưa đầy đủ.");
            }
            LocalDate dueDate = schedule.getDepartureDate().minusDays(policy.getRemainingDueDaysBeforeDeparture());
            if (!dueDate.isAfter(LocalDate.now())) {
                throw new AppException(ErrorCode.PAYMENT_PLAN_NOT_ALLOWED,
                        "Đã qua hạn đặt cọc; booking này phải thanh toán toàn bộ.");
            }
        }
        return selected;
    }

    private Voucher resolveAndReserveVoucher(String voucherCode, TourSchedule schedule, BigDecimal originalPrice) {
        if (!StringUtils.hasText(voucherCode))
            return null;
        Voucher voucher = voucherRepository.findByCodeForUpdate(voucherCode.trim())
                .orElseThrow(() -> new AppException(ErrorCode.VOUCHER_NOT_FOUND));
        LocalDateTime now = LocalDateTime.now();
        if (voucher.getStatus() != VoucherStatus.ACTIVE
                || now.isBefore(voucher.getValidFrom()) || now.isAfter(voucher.getValidUntil())) {
            throw new AppException(ErrorCode.VOUCHER_EXPIRED);
        }
        if (voucher.getUsedCount() + voucher.getReservedCount() >= voucher.getMaxUsage()) {
            throw new AppException(ErrorCode.VOUCHER_MAX_USAGE_REACHED);
        }
        if (!voucher.getVendor().getVendorId().equals(schedule.getTour().getVendor().getVendorId())) {
            throw new AppException(ErrorCode.VOUCHER_VENDOR_MISMATCH);
        }
        if (originalPrice.compareTo(voucher.getMinOrderValue()) < 0) {
            throw new AppException(ErrorCode.VOUCHER_MIN_ORDER_VALUE_NOT_MET);
        }
        voucher.setReservedCount(voucher.getReservedCount() + 1);
        return voucherRepository.save(voucher);
    }

    private BigDecimal calculateDiscount(Voucher voucher, BigDecimal originalPrice) {
        if (voucher == null)
            return BigDecimal.ZERO.setScale(2);
        BigDecimal discount = voucher.getDiscountType() == DiscountType.PERCENTAGE
                ? originalPrice.multiply(voucher.getDiscountValue())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : voucher.getDiscountValue();
        return discount.min(originalPrice).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    @Transactional
    public BookingDetailResponse cancelBooking(String email, UUID bookingId, BookingCancelRequest request) {
        return toDetail(cancellationService.cancelByTrekker(email, bookingId, request));
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<BookingResponse> getVendorBookings(String email, VendorBookingFilterRequest request) {
        Vendor vendor = getAssociatedVendor(email);
        Page<Booking> bookingPage = bookingRepository.findVendorBookings(
                vendor.getVendorId(), request.getBookingStatus(), request.getPaymentStatus(),
                request.getTourId(), request.getKeyword(), request.getPageable());
        return PaginationUtils.toPaginationResponse(bookingPage.map(bookingMapper::toBookingResponse));
    }

    @Override
    @Transactional
    public BookingDetailResponse confirmVendorBooking(String email, UUID bookingId) {
        Vendor vendor = getAssociatedVendor(email);
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
        if (!booking.getSchedule().getTour().getVendor().getVendorId().equals(vendor.getVendorId())) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        if (booking.getBookingStatus() != BookingStatus.PENDING_CONFIRMATION
                || !EnumSet.of(PaymentStatus.PARTIALLY_PAID, PaymentStatus.PAID).contains(booking.getPaymentStatus())) {
            throw new AppException(ErrorCode.INVALID_BOOKING_STATUS,
                    "Chỉ xác nhận booking đang chờ vendor và đã thanh toán hợp lệ.");
        }
        if (booking.getConfirmationExpiresAt() != null
                && !booking.getConfirmationExpiresAt().isAfter(LocalDateTime.now())) {
            throw new AppException(ErrorCode.INVALID_BOOKING_STATUS, "Đã quá hạn phản hồi booking.");
        }
        booking.setBookingStatus(BookingStatus.CONFIRMED);
        booking.setConfirmationExpiresAt(null);
        return toDetail(bookingRepository.save(booking));
    }

    private BookingDetailResponse toDetail(Booking booking) {
        BookingDetailResponse response = bookingMapper.toBookingDetailResponse(booking);
        response.setPaidAmount(resolvePaidAmount(booking));
        response.setPendingRefundAmount(refundTransactionRepository.sumByBookingAndStatuses(
                booking.getBookingId(), PENDING_REFUNDS));
        response.setOnlinePaymentEnabled(vendorPaymentAccountRepository
                .existsByVendor_VendorIdAndProviderAndOnboardingStatusAndIsDefaultTrueAndIsDeletedFalse(
                        booking.getSchedule().getTour().getVendor().getVendorId(),
                        PaymentProvider.PAYOS,
                        PaymentAccountStatus.ACTIVE));
        return response;
    }

    private BigDecimal resolvePaidAmount(Booking booking) {
        BigDecimal transactionTotal = paymentTransactionRepository.sumPaidByBooking(booking.getBookingId());
        if (paymentTransactionRepository.existsByBooking_BookingIdAndIsDeletedFalse(booking.getBookingId())) {
            return transactionTotal;
        }
        if (booking.getPaymentStatus() != PaymentStatus.UNPAID) {
            return booking.getTotalPrice();
        }
        return BigDecimal.ZERO;
    }

    private void assertCanView(User currentUser, String email, Booking booking) {
        if (hasRole(currentUser, "ADMIN") || booking.getUser().getUserId().equals(currentUser.getUserId()))
            return;
        if (hasRole(currentUser, "VENDOR_STAFF") || hasRole(currentUser, "VENDOR_MANAGER")) {
            Vendor vendor = getAssociatedVendor(email);
            if (booking.getSchedule().getTour().getVendor().getVendorId().equals(vendor.getVendorId()))
                return;
        }
        throw new AppException(ErrorCode.ACCESS_DENIED);
    }

    private boolean hasRole(User user, String roleName) {
        return user.getRoles().stream().anyMatch(role -> roleName.equals(role.getRoleName()));
    }

    private Vendor getAssociatedVendor(String email) {
        return vendorRepository.findByManager_Email(email)
                .orElseGet(() -> vendorStaffRepository.findByUser_Email(email)
                        .map(VendorStaff::getVendor)
                        .orElseThrow(() -> new AppException(ErrorCode.ACCESS_DENIED)));
    }

    private String generateBookingCode() {
        String date = DateTimeFormatter.ofPattern("yyMMdd").format(LocalDate.now());
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase(Locale.ROOT);
        return "BK-" + date + "-" + random;
    }

    private String hashBookingRequest(BookingRequest request) {
        try {
            byte[] canonicalRequest = objectMapper.writeValueAsString(request).getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonicalRequest));
        } catch (JsonProcessingException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Không thể tạo dấu vân tay cho booking request", exception);
        }
    }
}
