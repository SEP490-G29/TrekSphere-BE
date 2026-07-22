package com.sep.treksphere.service.impl;

import com.sep.treksphere.dto.request.BookingCancelRequest;
import com.sep.treksphere.dto.request.BookingRequest;
import com.sep.treksphere.dto.request.PaymentProofRequest;
import com.sep.treksphere.dto.request.VendorBookingFilterRequest;
import com.sep.treksphere.dto.response.BookingDetailResponse;
import com.sep.treksphere.dto.response.BookingResponse;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.entity.*;
import com.sep.treksphere.enums.booking.BookingStatus;
import com.sep.treksphere.enums.booking.PaymentStatus;
import com.sep.treksphere.enums.tour.ScheduleStatus;
import com.sep.treksphere.enums.voucher.DiscountType;
import com.sep.treksphere.enums.voucher.VoucherStatus;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;
import com.sep.treksphere.mapper.BookingMapper;
import com.sep.treksphere.repository.*;
import com.sep.treksphere.service.BookingService;
import com.sep.treksphere.utils.PaginationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final BookingParticipantRepository bookingParticipantRepository;
    private final TourScheduleRepository tourScheduleRepository;
    private final VoucherRepository voucherRepository;
    private final UserRepository userRepository;
    private final VendorRepository vendorRepository;
    private final VendorStaffRepository vendorStaffRepository;
    private final CancellationPolicyRepository cancellationPolicyRepository;
    private final BookingMapper bookingMapper;

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<BookingResponse> getMyBookingHistory(String email, BookingStatus status, Pageable pageable) {
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

        boolean isTrekker = currentUser.getRoles().stream().anyMatch(r -> r.getRoleName().equals("TREKKER"));
        boolean isVendorStaff = currentUser.getRoles().stream().anyMatch(r -> r.getRoleName().equals("VENDOR_STAFF"));
        boolean isVendorManager = currentUser.getRoles().stream().anyMatch(r -> r.getRoleName().equals("VENDOR_MANAGER"));
        boolean isAdmin = currentUser.getRoles().stream().anyMatch(r -> r.getRoleName().equals("ADMIN"));

        if (isAdmin) {
            // Admins can see all bookings
            return bookingMapper.toBookingDetailResponse(booking);
        }

        if (isTrekker) {
            if (booking.getUser().getUserId().equals(currentUser.getUserId())) {
                return bookingMapper.toBookingDetailResponse(booking);
            }
        }

        if (isVendorStaff || isVendorManager) {
            Vendor vendor = getAssociatedVendor(email);
            if (booking.getSchedule().getTour().getVendor().getVendorId().equals(vendor.getVendorId())) {
                return bookingMapper.toBookingDetailResponse(booking);
            }
        }

        throw new AppException(ErrorCode.ACCESS_DENIED);
    }

    @Override
    @Transactional
    public BookingDetailResponse createBooking(String email, BookingRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        TourSchedule schedule = tourScheduleRepository.findByScheduleIdAndIsDeletedFalse(request.getScheduleId())
                .orElseThrow(() -> new AppException(ErrorCode.SCHEDULE_NOT_FOUND));

        if (schedule.getStatus() != ScheduleStatus.OPEN) {
            throw new AppException(ErrorCode.INVALID_BOOKING_STATUS, "Lịch khởi hành này hiện không mở nhận khách.");
        }

        if (schedule.getDepartureDate().isBefore(LocalDate.now())) {
            throw new AppException(ErrorCode.SCHEDULE_DEPARTURE_IN_PAST);
        }

        int participantCount = request.getParticipants().size();
        if (schedule.getAvailableSlots() < participantCount) {
            throw new AppException(ErrorCode.NOT_ENOUGH_SLOTS);
        }

        BigDecimal originalPrice = schedule.getPrice().multiply(BigDecimal.valueOf(participantCount));
        BigDecimal discountAmount = BigDecimal.ZERO;
        Voucher voucher = null;

        if (StringUtils.hasText(request.getVoucherCode())) {
            voucher = voucherRepository.findByCodeAndIsDeletedFalse(request.getVoucherCode())
                    .orElseThrow(() -> new AppException(ErrorCode.VOUCHER_NOT_FOUND));

            if (voucher.getStatus() != VoucherStatus.ACTIVE) {
                throw new AppException(ErrorCode.VOUCHER_EXPIRED);
            }

            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(voucher.getValidFrom()) || now.isAfter(voucher.getValidUntil())) {
                throw new AppException(ErrorCode.VOUCHER_EXPIRED);
            }

            if (voucher.getUsedCount() >= voucher.getMaxUsage()) {
                throw new AppException(ErrorCode.VOUCHER_MAX_USAGE_REACHED);
            }

            if (!voucher.getVendor().getVendorId().equals(schedule.getTour().getVendor().getVendorId())) {
                throw new AppException(ErrorCode.VOUCHER_VENDOR_MISMATCH);
            }

            if (originalPrice.compareTo(voucher.getMinOrderValue()) < 0) {
                throw new AppException(ErrorCode.VOUCHER_MIN_ORDER_VALUE_NOT_MET);
            }

            if (voucher.getDiscountType() == DiscountType.PERCENTAGE) {
                discountAmount = originalPrice.multiply(voucher.getDiscountValue())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            } else if (voucher.getDiscountType() == DiscountType.FIXED_AMOUNT) {
                discountAmount = voucher.getDiscountValue();
            }

            if (discountAmount.compareTo(originalPrice) > 0) {
                discountAmount = originalPrice;
            }
        }

        BigDecimal totalPrice = originalPrice.subtract(discountAmount);

        Booking booking = new Booking();
        booking.setBookingCode(generateBookingCode());
        booking.setUser(user);
        booking.setSchedule(schedule);
        booking.setVoucher(voucher);
        booking.setNumberOfParticipants(participantCount);
        booking.setOriginalPrice(originalPrice);
        booking.setTotalPrice(totalPrice);
        booking.setDiscountAmount(discountAmount);
        booking.setPaymentStatus(PaymentStatus.PENDING);
        booking.setBookingStatus(BookingStatus.PENDING);

        Booking savedBooking = bookingRepository.save(booking);

        for (var participantReq : request.getParticipants()) {
            BookingParticipant participant = new BookingParticipant();
            participant.setBooking(savedBooking);
            participant.setFullName(participantReq.getFullName());
            participant.setDateOfBirth(participantReq.getDateOfBirth());
            participant.setGender(participantReq.getGender());
            participant.setIdNumber(participantReq.getIdNumber());
            participant.setPhone(participantReq.getPhone());
            participant.setEmail(participantReq.getEmail());
            participant.setAddress(participantReq.getAddress());
            participant.setSpecialRequirements(participantReq.getSpecialRequirements());
            bookingParticipantRepository.save(participant);
            savedBooking.getParticipants().add(participant);
        }

        schedule.setBookedSlots(schedule.getBookedSlots() + participantCount);
        schedule.setAvailableSlots(schedule.getAvailableSlots() - participantCount);
        tourScheduleRepository.save(schedule);

        if (voucher != null) {
            voucher.setUsedCount(voucher.getUsedCount() + 1);
            voucherRepository.save(voucher);
        }

        return bookingMapper.toBookingDetailResponse(savedBooking);
    }

    @Override
    @Transactional
    public BookingDetailResponse cancelBooking(String email, UUID bookingId, BookingCancelRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Booking booking = bookingRepository.findById(bookingId)
                .filter(b -> !Boolean.TRUE.equals(b.getIsDeleted()))
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        if (!booking.getUser().getUserId().equals(user.getUserId())) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        if (booking.getBookingStatus() == BookingStatus.CANCELLED || booking.getBookingStatus() == BookingStatus.COMPLETED) {
            throw new AppException(ErrorCode.BOOKING_CANNOT_CANCEL);
        }

        BigDecimal refundAmount = BigDecimal.ZERO;
        TourSchedule schedule = booking.getSchedule();

        if (booking.getPaymentStatus() == PaymentStatus.PAID || booking.getPaymentStatus() == PaymentStatus.PARTIALLY_REFUNDED) {
            long daysBeforeDeparture = ChronoUnit.DAYS.between(LocalDate.now(), schedule.getDepartureDate());
            Vendor vendor = schedule.getTour().getVendor();

            List<CancellationPolicy> policies = cancellationPolicyRepository
                    .findByVendorAndIsActiveTrueAndIsDeletedFalseOrderByCancelBeforeDaysDesc(vendor);

            int applicableRefundPercentage = 0;
            for (CancellationPolicy policy : policies) {
                if (daysBeforeDeparture >= policy.getCancelBeforeDays()) {
                    applicableRefundPercentage = Math.max(applicableRefundPercentage, policy.getRefundPercentage());
                }
            }

            refundAmount = booking.getTotalPrice()
                    .multiply(BigDecimal.valueOf(applicableRefundPercentage))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            if (applicableRefundPercentage == 100) {
                booking.setPaymentStatus(PaymentStatus.REFUNDED);
            } else if (applicableRefundPercentage > 0) {
                booking.setPaymentStatus(PaymentStatus.PARTIALLY_REFUNDED);
            }
        } else {
            // Not paid yet
            booking.setPaymentStatus(PaymentStatus.PENDING);
        }

        booking.setBookingStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason(request.getCancellationReason());
        booking.setCancelledAt(LocalDateTime.now());
        booking.setRefundAmount(refundAmount);

        // Refund slots back to schedule
        int participantCount = booking.getNumberOfParticipants();
        schedule.setBookedSlots(Math.max(0, schedule.getBookedSlots() - participantCount));
        schedule.setAvailableSlots(schedule.getAvailableSlots() + participantCount);
        tourScheduleRepository.save(schedule);

        // Refund voucher usage if applied
        Voucher voucher = booking.getVoucher();
        if (voucher != null) {
            voucher.setUsedCount(Math.max(0, voucher.getUsedCount() - 1));
            voucherRepository.save(voucher);
        }

        Booking updatedBooking = bookingRepository.save(booking);
        return bookingMapper.toBookingDetailResponse(updatedBooking);
    }

    @Override
    @Transactional
    public BookingDetailResponse submitPaymentProof(String email, UUID bookingId, PaymentProofRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Booking booking = bookingRepository.findById(bookingId)
                .filter(b -> !Boolean.TRUE.equals(b.getIsDeleted()))
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        if (!booking.getUser().getUserId().equals(user.getUserId())) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        if (booking.getBookingStatus() != BookingStatus.PENDING) {
            throw new AppException(ErrorCode.INVALID_BOOKING_STATUS, "Chỉ có thể gửi minh chứng thanh toán cho đơn đang chờ.");
        }

        booking.setProofImageUrl(request.getProofImageUrl());
        Booking updatedBooking = bookingRepository.save(booking);

        return bookingMapper.toBookingDetailResponse(updatedBooking);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<BookingResponse> getVendorBookings(String email, VendorBookingFilterRequest request) {
        Vendor vendor = getAssociatedVendor(email);
        Page<Booking> bookingPage = bookingRepository.findVendorBookings(
                vendor.getVendorId(),
                request.getBookingStatus(),
                request.getPaymentStatus(),
                request.getTourId(),
                request.getKeyword(),
                request.getPageable()
        );
        return PaginationUtils.toPaginationResponse(bookingPage.map(bookingMapper::toBookingResponse));
    }

    @Override
    @Transactional
    public BookingDetailResponse confirmVendorPayment(String email, UUID bookingId) {
        Vendor vendor = getAssociatedVendor(email);
        Booking booking = bookingRepository.findById(bookingId)
                .filter(b -> !Boolean.TRUE.equals(b.getIsDeleted()))
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        if (!booking.getSchedule().getTour().getVendor().getVendorId().equals(vendor.getVendorId())) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        if (booking.getBookingStatus() == BookingStatus.CANCELLED) {
            throw new AppException(ErrorCode.INVALID_BOOKING_STATUS, "Không thể xác nhận thanh toán cho đơn đã bị huỷ.");
        }

        booking.setPaymentStatus(PaymentStatus.PAID);
        Booking updatedBooking = bookingRepository.save(booking);
        return bookingMapper.toBookingDetailResponse(updatedBooking);
    }

    @Override
    @Transactional
    public BookingDetailResponse confirmVendorBooking(String email, UUID bookingId) {
        Vendor vendor = getAssociatedVendor(email);
        Booking booking = bookingRepository.findById(bookingId)
                .filter(b -> !Boolean.TRUE.equals(b.getIsDeleted()))
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        if (!booking.getSchedule().getTour().getVendor().getVendorId().equals(vendor.getVendorId())) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        if (booking.getBookingStatus() != BookingStatus.PENDING) {
            throw new AppException(ErrorCode.INVALID_BOOKING_STATUS, "Chỉ có thể xác nhận giữ chỗ cho đơn đang ở trạng thái PENDING.");
        }

        booking.setBookingStatus(BookingStatus.CONFIRMED);
        Booking updatedBooking = bookingRepository.save(booking);
        return bookingMapper.toBookingDetailResponse(updatedBooking);
    }

    @Override
    @Transactional
    public BookingDetailResponse confirmVendorRefund(String email, UUID bookingId) {
        Vendor vendor = vendorRepository.findByManager_Email(email)
                .orElseThrow(() -> new AppException(ErrorCode.ACCESS_DENIED));

        Booking booking = bookingRepository.findById(bookingId)
                .filter(b -> !Boolean.TRUE.equals(b.getIsDeleted()))
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        if (!booking.getSchedule().getTour().getVendor().getVendorId().equals(vendor.getVendorId())) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        if (booking.getBookingStatus() != BookingStatus.CANCELLED) {
            throw new AppException(ErrorCode.BOOKING_NOT_CANCELLED);
        }

        booking.setPaymentStatus(PaymentStatus.REFUNDED);
        Booking updatedBooking = bookingRepository.save(booking);
        return bookingMapper.toBookingDetailResponse(updatedBooking);
    }

    private Vendor getAssociatedVendor(String email) {
        Optional<Vendor> vendorOpt = vendorRepository.findByManager_Email(email);
        if (vendorOpt.isPresent()) {
            return vendorOpt.get();
        }
        Optional<VendorStaff> staffOpt = vendorStaffRepository.findByUser_Email(email);
        if (staffOpt.isPresent()) {
            return staffOpt.get().getVendor();
        }
        throw new AppException(ErrorCode.ACCESS_DENIED);
    }

    private String generateBookingCode() {
        String timestamp = DateTimeFormatter.ofPattern("yyMMddHHmmss").format(LocalDateTime.now());
        int randomNum = (int) (Math.random() * 900) + 100;
        return "BK-" + timestamp + "-" + randomNum;
    }
}
