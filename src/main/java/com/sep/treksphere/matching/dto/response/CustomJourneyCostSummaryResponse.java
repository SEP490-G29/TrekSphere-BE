package com.sep.treksphere.matching.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomJourneyCostSummaryResponse {

    private UUID customJourneyId;
    private UUID matchingGroupId;
    private BigDecimal totalEstimatedCost;
    private BigDecimal estimatedCostPerMember;
    private Integer activeMemberCount;
    private Integer maxSize;
    private List<CustomJourneyCostItemResponse> costItems;
}
