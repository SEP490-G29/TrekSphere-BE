package com.sep.treksphere.matching.dto.response;

import com.sep.treksphere.matching.enums.TimeSlot;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomJourneyActivityResponse {
    private UUID customJourneyActivityId;
    private UUID customJourneyId;
    private UUID customJourneyCheckpointId;
    private String checkpointTitle;
    private Integer dayNo;
    private TimeSlot timeSlot;
    private Integer activityOrder;
    private String title;
    private String description;
    private String plannedStartAt;
    private String plannedEndAt;

    public UUID getCheckpointId() {
        return customJourneyCheckpointId;
    }
}
