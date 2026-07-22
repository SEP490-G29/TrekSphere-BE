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

    @Schema(description = "Lọc theo trạng thái đơn đặt tour (PENDING, CONFIRMED, CANCELLED, COMPLETED)")
    private BookingStatus bookingStatus;

    @Schema(description = "Lọc theo trạng thái thanh toán (PENDING, PAID, REFUNDED, PARTIALLY_REFUNDED)")
    private PaymentStatus paymentStatus;

    @Schema(description = "Lọc theo UUID của Tour")
    private UUID tourId;
}
