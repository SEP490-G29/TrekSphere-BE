package com.sep.treksphere.dto.response;

import com.sep.treksphere.enums.tour.TourSessionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoordinatorScheduleResponse {

    private UUID coordinatorScheduleId;
    private Boolean isLead;
    private Boolean isCancelled;
    private UUID tourSessionId;
    private TourSessionStatus sessionStatus;
    private UUID tourId;
    private String tourName;
    private LocalDate departureDate;
    private LocalDate returnDate;
}
