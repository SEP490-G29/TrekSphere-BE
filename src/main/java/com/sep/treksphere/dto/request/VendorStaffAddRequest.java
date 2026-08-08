package com.sep.treksphere.dto.request;

import com.sep.treksphere.enums.user.VendorStaffRole;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VendorStaffAddRequest {

    @NotBlank(message = "Email nhân viên không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    private String fullName;

    private VendorStaffRole role = VendorStaffRole.VENDOR_STAFF;
}
