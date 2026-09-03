package com.sep.treksphere.user;

import com.sep.treksphere.common.dto.BaseFilterRequest;
import com.sep.treksphere.user.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserFilterRequest extends BaseFilterRequest {
    
    @Schema(description = "Lọc theo trạng thái (ví dụ: ACTIVE, LOCKED)")
    private UserStatus status;

    @Schema(description = "Lọc theo Role (ví dụ: TREKKER, VENDOR)")
    private String roleName;
}
