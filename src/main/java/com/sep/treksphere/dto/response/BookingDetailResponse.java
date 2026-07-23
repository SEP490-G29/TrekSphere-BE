package com.sep.treksphere.dto.response;

import com.sep.treksphere.enums.booking.BookingStatus;
import com.sep.treksphere.enums.booking.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookingDetailResponse {
    private String bookingId;
    private String bookingCode;
    
    // Tour details
    private String tourId;
    private String tourName;
    private String coverImageUrl;
    private LocalDate departureDate;
    private LocalDate returnDate;
    private BigDecimal pricePerSlot;

    // Pricing details
    private Integer numberOfParticipants;
    private BigDecimal originalPrice;
    private BigDecimal discountAmount;
    private BigDecimal totalPrice;
    private BigDecimal refundAmount;

    // Status details
    private BookingStatus bookingStatus;
    private PaymentStatus paymentStatus;
    private String proofImageUrl;
    private String cancellationReason;
    private LocalDateTime cancelledAt;
    
    // Dates
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Applied Voucher (if any)
    private String voucherCode;

    // User details
    private String userId;
    private String userEmail;
    private String userFullName;
    private String userPhone;

    // Participants list
    private List<BookingParticipantResponse> participants;
}
