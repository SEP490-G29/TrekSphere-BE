package com.sep.treksphere.matching.dto.request;

import com.sep.treksphere.common.constant.MessageConstant;
import com.sep.treksphere.matching.enums.MomentVisibility;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request cập nhật quyền hiển thị của khoảnh khắc cá nhân")
public class MomentVisibilityUpdateRequest {

    @NotNull(message = MessageConstant.MOMENT_VISIBILITY_REQUIRED)
    @Schema(description = "Quyền hiển thị mới (GROUP_ONLY, PUBLIC_PROFILE, ONLY_ME)", example = "PUBLIC_PROFILE")
    private MomentVisibility visibility;
}
