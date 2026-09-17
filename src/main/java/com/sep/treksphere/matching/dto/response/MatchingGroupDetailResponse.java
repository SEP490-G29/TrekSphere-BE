package com.sep.treksphere.matching.dto.response;

import com.sep.treksphere.matching.enums.JoinStatus;
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
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchingGroupDetailResponse {
    private UUID matchingGroupId;
    private MatchingGroupSourceType sourceType;

    // Tour info (if Tour-backed)
    private UUID tourId;
    private String tourName;
    private String tourDescription;
    private String tourLocation;

    // Custom Journey info (if Custom Journey)
    private UUID customJourneyId;
    private String customJourneyTitle;
    private String customJourneyDescription;
    private LocalDate customJourneyStartDate;
    private LocalDate customJourneyEndDate;
    private Boolean isLocked;
    private List<CustomJourneyCheckpointResponse> checkpoints;
    private List<CustomJourneyCostItemResponse> costItems;

    // Unified Matching Summary
    private String difficulty;
    private String location;
    private BigDecimal estimatedCost;

    // Group & Owner info
    private UUID ownerId;
    private String ownerName;
    private String ownerAvatarUrl;
    /** Trưởng nhóm HIỆN TẠI (role có thể đổi qua bầu cử) — khác owner (người tạo nhóm, cố định). */
    private UUID leaderId;
    private String leaderName;
    private String leaderAvatarUrl;
    private String groupName;
    private String description;
    private String coverImageUrl;
    private Integer maxSize;
    private Integer currentSize;
    private LocalDate targetDate;
    private LocalDateTime matchingDeadline;
    private MatchingGroupStatus status;
    private LocalDateTime createdAt;

    // Public member list (accepted members only)
    private List<MatchingMemberResponse> members;

    // Viewer context
    private Boolean isOwner;
    /** Role thật của viewer trong nhóm (LEADER/MEMBER) — khác với isOwner (chỉ là người tạo nhóm, cố định). */
    private MatchingRole myRole;
    private JoinStatus myMembershipStatus;
    private Boolean canJoin;
    private Boolean canLeave;
    private Boolean hasConversation;
    private Boolean isInConversation;
}
