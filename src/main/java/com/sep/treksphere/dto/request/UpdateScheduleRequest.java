package com.sep.treksphere.dto.request;

import com.sep.treksphere.constant.MessageConstant;
import com.sep.treksphere.enums.tour.ScheduleStatus;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class UpdateScheduleRequest {

    private LocalDate departureDate;

    private LocalDate returnDate;

    @DecimalMin(value = "0.0", inclusive = false, message = MessageConstant.SCHEDULE_PRICE_MIN)
    private BigDecimal price;

    private ScheduleStatus status;

    private String reason;
}
