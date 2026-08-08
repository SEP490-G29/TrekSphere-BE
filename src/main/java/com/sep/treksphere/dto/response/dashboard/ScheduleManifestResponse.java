package com.sep.treksphere.dto.response.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleManifestResponse {

    private UUID scheduleId;
    private String tourName;
    private LocalDate departureDate;
    private LocalDate returnDate;
    private Integer bookedSlots;
    private Integer maxCapacity;
    private Integer minCapacity;

    private List<ParticipantManifestResponse> participants;
}
