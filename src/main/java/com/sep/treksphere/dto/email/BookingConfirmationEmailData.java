package com.sep.treksphere.dto.email;

import java.util.UUID;

public record BookingConfirmationEmailData(
        UUID bookingId,
        String bookingCode,
        String recipientEmail,
        String recipientName,
        String tourName,
        String departureDate,
        String returnDate,
        String duration,
        String meetingPoint,
        Integer participantCount,
        String vendorName,
        String totalPrice,
        String paidAmount,
        String paymentStatus
) {
}
