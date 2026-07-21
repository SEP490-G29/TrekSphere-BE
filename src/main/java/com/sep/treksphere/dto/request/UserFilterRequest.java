package com.sep.treksphere.dto.request;

import com.sep.treksphere.enums.user.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserFilterRequest extends BaseFilterRequest {
    
    @Schema(description = "Lọc theo trạng thái (ví dụ: ACTIVE, LOCKED)")
    private UserStatus status;

    @Schema(description = "Lọc theo Role (ví dụ: TREKKER, VENDOR_MANAGER)")
    private String roleName;
}
