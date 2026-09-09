package com.sep.treksphere.matching.event;

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
public class GroupApplicationSubmittedEvent {
    private UUID eventId;
    private UUID groupId;
    private UUID matchingMemberId;
    private UUID applicantUserId;
    private UUID groupOwnerId;
    private LocalDateTime occurredAt;
}
