package com.sep.treksphere.dto.request;

import com.sep.treksphere.enums.booking.BookingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookingHistoryRequest extends BaseFilterRequest {

    @Schema(description = "Trạng thái booking", example = "PAYMENT_PENDING",
            allowableValues = {"PAYMENT_PENDING", "PENDING_CONFIRMATION", "CONFIRMED", "IN_PROGRESS", "COMPLETED", "EXPIRED", "REJECTED", "CANCELLED"})
    private BookingStatus status;
}
