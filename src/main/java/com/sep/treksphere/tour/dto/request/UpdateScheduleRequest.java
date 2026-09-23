package com.sep.treksphere.tour.dto.request;

import com.sep.treksphere.tour.enums.ScheduleStatus;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateScheduleRequest {

    private LocalDate departureDate;

    private LocalDate returnDate;

    private ScheduleStatus status;

    private String reason;
}
