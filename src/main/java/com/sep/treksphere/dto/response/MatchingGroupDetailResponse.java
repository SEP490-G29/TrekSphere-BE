package com.sep.treksphere.dto.response;

import com.sep.treksphere.enums.matching.MatchingGroupStatus;
import com.sep.treksphere.enums.matching.JoinStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class MatchingGroupDetailResponse {
    private UUID matchingGroupId;
    private UUID tourId;
    private String tourName;
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
    private List<MatchingMemberResponse> members;
    private Boolean isOwner;
    private JoinStatus myMembershipStatus;
    private Boolean canJoin;
    private Boolean canLeave;
    private Boolean hasConversation;
    private Boolean isInConversation;
}
