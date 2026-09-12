package com.sep.treksphere.matching.dto.request;

import com.sep.treksphere.common.dto.BaseFilterRequest;
import com.sep.treksphere.matching.enums.MomentStatus;
import com.sep.treksphere.matching.enums.MomentVisibility;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class MomentFilterRequest extends BaseFilterRequest {

    @Schema(description = "Lọc theo ID tác giả tạo khoảnh khắc")
    private UUID authorUserId;

    @Schema(description = "Lọc theo quyền riêng tư (GROUP_ONLY, PUBLIC_PROFILE, ONLY_ME)")
    private MomentVisibility visibility;

    @Schema(description = "Lọc theo trạng thái kiểm duyệt (VISIBLE, HIDDEN)")
    private MomentStatus status;
}
