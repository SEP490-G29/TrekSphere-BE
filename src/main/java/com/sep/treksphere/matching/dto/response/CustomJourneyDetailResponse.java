package com.sep.treksphere.matching.dto.response;

import com.sep.treksphere.matching.enums.JourneyDifficulty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomJourneyDetailResponse {
    private UUID customJourneyId;
    private UUID matchingGroupId;
    private String title;
    private String description;
    private JourneyDifficulty difficulty;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isLocked;
    private LocalDateTime lockedAt;
    private List<CustomJourneyCheckpointResponse> checkpoints;
    private List<CustomJourneyCostItemResponse> costItems;
}
