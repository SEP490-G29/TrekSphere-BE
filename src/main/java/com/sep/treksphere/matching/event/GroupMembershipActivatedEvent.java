package com.sep.treksphere.matching.event;

import com.sep.treksphere.matching.enums.MatchingRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMembershipActivatedEvent {
    private UUID eventId;
    private UUID groupId;
    private UUID matchingMemberId;
    private UUID userId;
    private MatchingRole role;
    private LocalDateTime occurredAt;
}
