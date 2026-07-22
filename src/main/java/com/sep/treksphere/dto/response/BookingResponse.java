package com.sep.treksphere.dto.response;

import com.sep.treksphere.enums.booking.BookingStatus;
import com.sep.treksphere.enums.booking.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookingResponse {
    private String bookingId;
    private String bookingCode;
    private String tourName;
    private String coverImageUrl;
    private LocalDate departureDate;
    private LocalDate returnDate;
    private Integer numberOfParticipants;
    private BigDecimal totalPrice;
    private BookingStatus bookingStatus;
    private PaymentStatus paymentStatus;
    private LocalDateTime createdAt;
}
