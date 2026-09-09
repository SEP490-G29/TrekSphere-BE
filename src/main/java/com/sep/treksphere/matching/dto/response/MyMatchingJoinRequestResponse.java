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
    private UUID applicationId;
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
    private String message;
    private String rejectReason;
    private JoinStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime updatedAt;
    private LocalDateTime withdrawnAt;
    private boolean canCancel;
    private boolean canWithdraw;
}

