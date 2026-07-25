package com.sep.treksphere.dto;

import com.sep.treksphere.enums.tour.TourSessionStatus;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class StaffScheduleResponse {
    private UUID coordinatorScheduleId;
    private UUID tourSessionId;
    private String tourName;
    private LocalDate departureDate;
    private LocalDate returnDate;
    private TourSessionStatus status;
    private Boolean isLead;
    private Boolean isCancelled;
}
