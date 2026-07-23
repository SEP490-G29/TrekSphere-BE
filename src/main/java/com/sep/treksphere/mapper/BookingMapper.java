package com.sep.treksphere.mapper;

import com.sep.treksphere.dto.response.BookingDetailResponse;
import com.sep.treksphere.dto.response.BookingParticipantResponse;
import com.sep.treksphere.dto.response.BookingResponse;
import com.sep.treksphere.entity.Booking;
import com.sep.treksphere.entity.BookingParticipant;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
public class BookingMapper {

    public BookingParticipantResponse toParticipantResponse(BookingParticipant p) {
        if (p == null) {
            return null;
        }
        return BookingParticipantResponse.builder()
                .participantId(p.getParticipantId() != null ? p.getParticipantId().toString() : null)
                .fullName(p.getFullName())
                .dateOfBirth(p.getDateOfBirth())
                .gender(p.getGender())
                .idNumber(p.getIdNumber())
                .phone(p.getPhone())
                .email(p.getEmail())
                .address(p.getAddress())
                .specialRequirements(p.getSpecialRequirements())
                .build();
    }

    public BookingResponse toBookingResponse(Booking b) {
        if (b == null) {
            return null;
        }
        String tourName = null;
        String coverImageUrl = null;
        LocalDate depDate = null;
        LocalDate retDate = null;
        if (b.getSchedule() != null) {
            depDate = b.getSchedule().getDepartureDate();
            retDate = b.getSchedule().getReturnDate();
            if (b.getSchedule().getTour() != null) {
                tourName = b.getSchedule().getTour().getTourName();
                coverImageUrl = b.getSchedule().getTour().getCoverImageUrl();
            }
        }
        return BookingResponse.builder()
                .bookingId(b.getBookingId() != null ? b.getBookingId().toString() : null)
                .bookingCode(b.getBookingCode())
                .tourName(tourName)
                .coverImageUrl(coverImageUrl)
                .departureDate(depDate)
                .returnDate(retDate)
                .numberOfParticipants(b.getNumberOfParticipants())
                .totalPrice(b.getTotalPrice())
                .bookingStatus(b.getBookingStatus())
                .paymentStatus(b.getPaymentStatus())
                .createdAt(b.getCreatedAt())
                .build();
    }

    public BookingDetailResponse toBookingDetailResponse(Booking b) {
        if (b == null) {
            return null;
        }
        String tourId = null;
        String tourName = null;
        String coverImageUrl = null;
        LocalDate depDate = null;
        LocalDate retDate = null;
        BigDecimal pricePerSlot = null;
        if (b.getSchedule() != null) {
            depDate = b.getSchedule().getDepartureDate();
            retDate = b.getSchedule().getReturnDate();
            pricePerSlot = b.getSchedule().getPrice();
            if (b.getSchedule().getTour() != null) {
                tourId = b.getSchedule().getTour().getTourId() != null ? b.getSchedule().getTour().getTourId().toString() : null;
                tourName = b.getSchedule().getTour().getTourName();
                coverImageUrl = b.getSchedule().getTour().getCoverImageUrl();
            }
        }

        String userId = null;
        String userEmail = null;
        String userFullName = null;
        String userPhone = null;
        if (b.getUser() != null) {
            userId = b.getUser().getUserId() != null ? b.getUser().getUserId().toString() : null;
            userEmail = b.getUser().getEmail();
            userFullName = b.getUser().getFullName();
            userPhone = b.getUser().getPhone();
        }

        String voucherCode = b.getVoucher() != null ? b.getVoucher().getCode() : null;

        List<BookingParticipantResponse> participantResponses = null;
        if (b.getParticipants() != null) {
            participantResponses = b.getParticipants().stream()
                    .map(this::toParticipantResponse)
                    .toList();
        }

        return BookingDetailResponse.builder()
                .bookingId(b.getBookingId() != null ? b.getBookingId().toString() : null)
                .bookingCode(b.getBookingCode())
                .tourId(tourId)
                .tourName(tourName)
                .coverImageUrl(coverImageUrl)
                .departureDate(depDate)
                .returnDate(retDate)
                .pricePerSlot(pricePerSlot)
                .numberOfParticipants(b.getNumberOfParticipants())
                .originalPrice(b.getOriginalPrice())
                .discountAmount(b.getDiscountAmount())
                .totalPrice(b.getTotalPrice())
                .refundAmount(b.getRefundAmount())
                .bookingStatus(b.getBookingStatus())
                .paymentStatus(b.getPaymentStatus())
                .proofImageUrl(b.getProofImageUrl())
                .cancellationReason(b.getCancellationReason())
                .cancelledAt(b.getCancelledAt())
                .createdAt(b.getCreatedAt())
                .updatedAt(b.getUpdatedAt())
                .voucherCode(voucherCode)
                .userId(userId)
                .userEmail(userEmail)
                .userFullName(userFullName)
                .userPhone(userPhone)
                .participants(participantResponses)
                .build();
    }
}
