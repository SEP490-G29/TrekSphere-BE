package com.sep.treksphere.dto.response.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnderCapacityAlertResponse {

    private UUID scheduleId;
    private String tourName;
    private LocalDate departureDate;
    private Long daysLeft;
    private Integer bookedSlots;
    private Integer minCapacity;
    private Integer missingSlots;
    private String alertMessage;
}
