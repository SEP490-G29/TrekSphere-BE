package com.sep.treksphere.matching.dto.response;

import com.sep.treksphere.matching.enums.CheckpointProgressStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomJourneyCheckpointResponse {
    private UUID customJourneyCheckpointId;
    private Integer dayNo;
    private Integer checkpointOrder;
    private String title;
    private String description;
    private String locationName;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private LocalDateTime plannedStartAt;
    private LocalDateTime plannedEndAt;
    private String imageUrl;
    private List<String> imageUrls;
    private CheckpointProgressStatus status;
    private LocalDateTime progressUpdatedAt;
    private String progressUpdatedByName;
}
