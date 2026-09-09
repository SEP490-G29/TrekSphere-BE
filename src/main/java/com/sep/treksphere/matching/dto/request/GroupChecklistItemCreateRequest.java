package com.sep.treksphere.matching.dto.request;

import com.sep.treksphere.matching.enums.ChecklistItemScope;
import com.sep.treksphere.matching.enums.ChecklistItemType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupChecklistItemCreateRequest {

    @NotBlank(message = "Tiêu đề mục checklist không được để trống")
    @Size(max = 200, message = "Tiêu đề mục checklist không được vượt quá 200 ký tự")
    private String title;

    @NotNull(message = "Phạm vi mục checklist (itemScope) không được để trống")
    private ChecklistItemScope itemScope;

    private ChecklistItemType itemTypeCode;

    private Boolean isRequired;

    private String note;

    private UUID assigneeMatchingMemberId;
}
