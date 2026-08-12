package com.sep.treksphere.dto.request;

import com.sep.treksphere.enums.booking.BookingStatus;
import com.sep.treksphere.enums.booking.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class VendorBookingFilterRequest extends BaseFilterRequest {

    @Schema(description = "Lọc theo trạng thái booking",
            allowableValues = {"PAYMENT_PENDING", "PENDING_CONFIRMATION", "CONFIRMED", "IN_PROGRESS", "COMPLETED", "EXPIRED", "REJECTED", "CANCELLED"})
    private BookingStatus bookingStatus;

    @Schema(description = "Lọc theo trạng thái thanh toán",
            allowableValues = {"UNPAID", "PARTIALLY_PAID", "PAID", "REFUND_PENDING", "PARTIALLY_REFUNDED", "REFUNDED"})
    private PaymentStatus paymentStatus;

    @Schema(description = "Lọc theo UUID của Tour")
    private UUID tourId;
}
