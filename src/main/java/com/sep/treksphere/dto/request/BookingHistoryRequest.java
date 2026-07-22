package com.sep.treksphere.dto.request;

import com.sep.treksphere.enums.booking.BookingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookingHistoryRequest extends BaseFilterRequest {

    @Schema(description = "Trạng thái đặt tour để lọc (PENDING, CONFIRMED, CANCELLED, COMPLETED)", example = "PENDING")
    private BookingStatus status;
}
