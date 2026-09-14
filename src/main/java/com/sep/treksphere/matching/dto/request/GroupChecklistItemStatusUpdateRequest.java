package com.sep.treksphere.matching.dto.request;

import com.sep.treksphere.common.constant.MessageConstant;
import com.sep.treksphere.matching.enums.ChecklistItemStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request cập nhật trạng thái mục checklist")
public class GroupChecklistItemStatusUpdateRequest {

    @NotNull(message = MessageConstant.CHECKLIST_STATUS_REQUIRED)
    @Schema(description = "Trạng thái mới của mục checklist (TODO, IN_PROGRESS, DONE)", example = "DONE")
    private ChecklistItemStatus status;
}
