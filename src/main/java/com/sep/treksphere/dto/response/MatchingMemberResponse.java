package com.sep.treksphere.dto.response;

import com.sep.treksphere.enums.matching.JoinStatus;
import com.sep.treksphere.enums.matching.MatchingRole;
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
