package com.sep.treksphere.matching.dto.response;

import com.sep.treksphere.matching.JoinStatus;
import com.sep.treksphere.matching.MatchingRole;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class MatchingMemberResponse {
    private UUID matchingMemberId;
    private UUID userId;
    private String fullName;
    private String avatarUrl;
    private MatchingRole role;
    private JoinStatus status;
    private LocalDateTime createdAt;
    private Boolean isInConversation;
}
