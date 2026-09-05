package com.sep.treksphere.matching.dto.response;

import com.sep.treksphere.matching.enums.MatchingGroupSourceType;
import com.sep.treksphere.matching.enums.MatchingGroupStatus;
import com.sep.treksphere.matching.enums.MatchingRole;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchingGroupResponse {
    private UUID matchingGroupId;
    private MatchingGroupSourceType sourceType;

    // Tour info (if Tour-backed)
    private UUID tourId;
    private String tourName;

    // Custom Journey info (if Custom Journey)
    private UUID customJourneyId;
    private String customJourneyTitle;

    // Unified Matching Summary
    private String difficulty;
    private String location;
    private BigDecimal estimatedCost;

    // User relation info (for /my-groups endpoint)
    private Boolean isOwner;
    private MatchingRole myRole;

    // Group & Owner info
    private UUID ownerId;
    private String ownerName;
    private String ownerAvatarUrl;
    private String groupName;
    private String description;
    private Integer maxSize;
    private Integer currentSize;
    private LocalDate targetDate;
    private LocalDateTime matchingDeadline;
    private MatchingGroupStatus status;
    private LocalDateTime createdAt;
}
