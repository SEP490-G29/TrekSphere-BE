package com.sep.treksphere.matching.dto.request;

import com.sep.treksphere.common.constant.MessageConstant;
import com.sep.treksphere.matching.enums.ChecklistItemScope;
import com.sep.treksphere.matching.enums.ChecklistItemStatus;
import com.sep.treksphere.matching.enums.ChecklistItemType;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Request cập nhật mục checklist nhóm")
public class GroupChecklistItemUpdateRequest {

    @Size(max = 200, message = MessageConstant.CHECKLIST_ITEM_TITLE_MAX_LENGTH)
    @Schema(description = "Tiêu đề mục đồ dùng", example = "Bộ sơ cứu y tế")
    private String title;

    @Schema(description = "Phạm vi sử dụng")
    private ChecklistItemScope itemScope;

    @Schema(description = "Phân loại đồ dùng")
    private ChecklistItemType itemTypeCode;

    @Schema(description = "Đồ dùng bắt buộc hay tùy chọn")
    private Boolean isRequired;

    @Schema(description = "Ghi chú thêm")
    private String note;

    @Schema(description = "Mã thành viên được phân công")
    private UUID assigneeMatchingMemberId;

    @Schema(description = "Trạng thái chuẩn bị (TODO, IN_PROGRESS, DONE)")
    private ChecklistItemStatus status;
}
