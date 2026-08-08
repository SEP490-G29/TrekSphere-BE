package com.sep.treksphere.dto.request;

import com.sep.treksphere.enums.user.VendorStaffRole;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VendorStaffRoleUpdateRequest {

    @NotNull(message = "Vai trò nhân viên không được để trống")
    private VendorStaffRole role;
}
