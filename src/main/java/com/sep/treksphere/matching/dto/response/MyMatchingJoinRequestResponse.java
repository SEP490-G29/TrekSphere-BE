package com.sep.treksphere.matching.dto.response;

import com.sep.treksphere.matching.enums.JoinStatus;
import com.sep.treksphere.matching.enums.MatchingGroupSourceType;
import com.sep.treksphere.matching.enums.MatchingGroupStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class MyMatchingJoinRequestResponse {
    private UUID matchingMemberId;
    private UUID matchingGroupId;
    private String groupName;
    private MatchingGroupStatus groupStatus;
    private MatchingGroupSourceType sourceType;
    private UUID tourId;
    private String tourName;
    private UUID customJourneyId;
    private String customJourneyTitle;
    private String difficulty;
    private String location;
    private UUID ownerId;
    private String ownerName;
    private String ownerAvatarUrl;
    private Integer currentSize;
    private Integer maxSize;
    private LocalDate targetDate;
    private LocalDateTime matchingDeadline;
    private JoinStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean canCancel;
    private boolean canWithdraw;
}
