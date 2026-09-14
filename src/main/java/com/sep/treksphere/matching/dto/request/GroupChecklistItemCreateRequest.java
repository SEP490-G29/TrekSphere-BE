package com.sep.treksphere.matching.dto.request;

import com.sep.treksphere.common.constant.MessageConstant;
import com.sep.treksphere.matching.enums.ChecklistItemScope;
import com.sep.treksphere.matching.enums.ChecklistItemType;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Request tạo mục đồ dùng checklist nhóm")
public class GroupChecklistItemCreateRequest {

    @NotBlank(message = MessageConstant.CHECKLIST_ITEM_TITLE_REQUIRED)
    @Size(max = 200, message = MessageConstant.CHECKLIST_ITEM_TITLE_MAX_LENGTH)
    @Schema(description = "Tiêu đề mục đồ dùng", example = "Bộ sơ cứu y tế")
    private String title;

    @NotNull(message = MessageConstant.CHECKLIST_ITEM_SCOPE_REQUIRED)
    @Schema(description = "Phạm vi sử dụng (GROUP_SHARED hoặc INDIVIDUAL)", example = "GROUP_SHARED")
    private ChecklistItemScope itemScope;

    @Schema(description = "Phân loại đồ dùng (GEAR, MEDICAL, FOOD_WATER, CLOTHING, DOCUMENTS, OTHER)")
    private ChecklistItemType itemTypeCode;

    @Schema(description = "Đồ dùng bắt buộc hay tùy chọn")
    private Boolean isRequired;

    @Schema(description = "Ghi chú thêm về đồ dùng")
    private String note;

    @Schema(description = "Mã thành viên được phân công chuẩn bị")
    private UUID assigneeMatchingMemberId;
}
