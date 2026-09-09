package com.sep.treksphere.matching.event;

import com.sep.treksphere.matching.enums.JoinStatus;
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
public class GroupApplicationDecidedEvent {
    private UUID eventId;
    private UUID groupId;
    private UUID matchingMemberId;
    private UUID applicantUserId;
    private UUID decidedByUserId;
    private JoinStatus decision; // ACCEPTED or REJECTED
    private LocalDateTime occurredAt;
}
