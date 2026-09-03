package com.sep.treksphere.tour.schedule;

import com.sep.treksphere.common.constant.MessageConstant;
import com.sep.treksphere.tour.schedule.ScheduleStatus;
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
