package com.sep.treksphere.dto.response;

import com.sep.treksphere.enums.matching.MatchingGroupStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class MatchingGroupResponse {
    private UUID matchingGroupId;
    private UUID tourId;
    private String tourName;
    private UUID ownerId;
    private String ownerName;
    private String ownerAvatarUrl;
    private String groupName;
    private String description;
    private Integer maxSize;
    private Integer currentSize;
    private LocalDate targetDate;
    private LocalDateTime matchingDeadline;
    private MatchingGroupStatus status;
    private LocalDateTime createdAt;
}
