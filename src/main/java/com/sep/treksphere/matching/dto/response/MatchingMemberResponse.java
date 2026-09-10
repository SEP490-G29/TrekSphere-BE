package com.sep.treksphere.matching.dto.response;

import com.sep.treksphere.matching.enums.JoinStatus;
import com.sep.treksphere.matching.enums.MatchingRole;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class MatchingMemberResponse {
    private UUID matchingMemberId;
    private UUID applicationId;
    private UUID userId;
    private String fullName;
    private String avatarUrl;
    private MatchingRole role;
    private JoinStatus status;
    private String message;
    private String rejectReason;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime withdrawnAt;
    private LocalDateTime joinedAt;
    private LocalDateTime leftAt;
    private Boolean isInConversation;
}

