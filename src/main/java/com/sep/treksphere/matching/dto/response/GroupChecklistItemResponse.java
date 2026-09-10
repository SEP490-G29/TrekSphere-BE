package com.sep.treksphere.matching.dto.response;

import com.sep.treksphere.matching.enums.ChecklistItemScope;
import com.sep.treksphere.matching.enums.ChecklistItemStatus;
import com.sep.treksphere.matching.enums.ChecklistItemType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupChecklistItemResponse {

    private UUID groupChecklistItemId;
    private UUID matchingGroupId;
    private String title;
    private ChecklistItemScope itemScope;
    private ChecklistItemType itemTypeCode;
    private Boolean isRequired;
    private String note;

    private UUID assigneeMatchingMemberId;
    private UUID assigneeUserId;
    private String assigneeFullName;
    private String assigneeAvatarUrl;

    private ChecklistItemStatus status;
    private LocalDateTime completedAt;

    private UUID completedByMatchingMemberId;
    private UUID completedByUserId;
    private String completedByFullName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
