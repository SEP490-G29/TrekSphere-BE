package com.sep.treksphere.dto.response.dashboard;

import com.sep.treksphere.enums.dashboard.ScheduleRiskStatus;
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
public class UpcomingScheduleResponse {

    private UUID scheduleId;
    private UUID tourId;
    private String tourName;
    private LocalDate departureDate;
    private Integer bookedSlots;
    private Integer maxCapacity;
    private Integer minCapacity;
    private Double occupancyRate;
    private ScheduleRiskStatus riskStatus;
    private String statusColor;
}
