package com.sep.treksphere.matching.dto.request;

import com.sep.treksphere.matching.enums.ChecklistItemStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupChecklistItemStatusUpdateRequest {

    @NotNull(message = "Trạng thái checklist không được để trống")
    private ChecklistItemStatus status;
}
