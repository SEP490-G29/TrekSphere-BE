package com.sep.treksphere.matching.dto.request;

import com.sep.treksphere.matching.enums.MomentVisibility;
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
public class MomentVisibilityUpdateRequest {

    @NotNull(message = "Quyền hiển thị không được để trống")
    private MomentVisibility visibility;
}
