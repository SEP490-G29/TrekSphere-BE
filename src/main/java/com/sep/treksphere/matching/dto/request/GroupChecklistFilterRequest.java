package com.sep.treksphere.matching.dto.request;

import com.sep.treksphere.matching.enums.ChecklistItemScope;
import com.sep.treksphere.matching.enums.ChecklistItemStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupChecklistFilterRequest {

    private ChecklistItemScope itemScope;

    private ChecklistItemStatus status;

    private UUID assigneeMatchingMemberId;

    private Boolean isRequired;

    private String keyword;
}
