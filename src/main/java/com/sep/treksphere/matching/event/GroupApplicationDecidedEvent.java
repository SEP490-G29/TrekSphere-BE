package com.sep.treksphere.matching.event;

import com.sep.treksphere.matching.enums.JoinStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupApplicationDecidedEvent {
    private UUID eventId;
    private UUID groupId;
    private UUID matchingMemberId;
    private UUID applicantUserId;
    private UUID decidedByUserId;
    private JoinStatus decision; // ACCEPTED or REJECTED
    private LocalDateTime occurredAt;
}
