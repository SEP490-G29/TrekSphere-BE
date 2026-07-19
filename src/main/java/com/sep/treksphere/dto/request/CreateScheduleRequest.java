package com.sep.treksphere.dto.request;

import com.sep.treksphere.constant.MessageConstant;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateScheduleRequest {

    @NotNull(message = MessageConstant.SCHEDULE_DEPARTURE_REQUIRED)
    private LocalDate departureDate;

    @NotNull(message = MessageConstant.SCHEDULE_RETURN_REQUIRED)
    private LocalDate returnDate;

    @NotNull(message = MessageConstant.SCHEDULE_PRICE_REQUIRED)
    @DecimalMin(value = "0.0", inclusive = false, message = MessageConstant.SCHEDULE_PRICE_MIN)
    private BigDecimal price;

    @NotNull(message = MessageConstant.SCHEDULE_SLOTS_REQUIRED)
    @Min(value = 1, message = MessageConstant.SCHEDULE_SLOTS_MIN)
    private Integer availableSlots;
}
