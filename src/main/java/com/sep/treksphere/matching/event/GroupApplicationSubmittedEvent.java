package com.sep.treksphere.matching.event;

import lombok.*;

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
