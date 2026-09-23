package com.sep.treksphere.tour.dto.request;

import com.sep.treksphere.common.constant.MessageConstant;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateScheduleRequest {

    @NotNull(message = MessageConstant.SCHEDULE_DEPARTURE_REQUIRED)
    private LocalDate departureDate;

    @NotNull(message = MessageConstant.SCHEDULE_RETURN_REQUIRED)
    private LocalDate returnDate;
}
