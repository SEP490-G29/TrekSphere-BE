package com.sep.treksphere.auth.dto.request;

import com.sep.treksphere.common.constant.MessageConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequest {
    
    @Schema(description = "Email đăng nhập", example = "trekker1@treksphere.com")
    @NotBlank(message = MessageConstant.EMAIL_REQUIRED)
    @Email(message = MessageConstant.EMAIL_INVALID)
    private String email;
    
    @Schema(description = "Mật khẩu tài khoản", example = "Pass123@")
    @NotBlank(message = MessageConstant.PASSWORD_REQUIRED)
    private String password;
}
