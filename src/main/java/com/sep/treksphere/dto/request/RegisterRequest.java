package com.sep.treksphere.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import com.sep.treksphere.constant.ValidationConstant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Yêu cầu đăng ký tài khoản mới")
public class RegisterRequest {

    @Schema(description = "Địa chỉ email của người dùng", example = "trekker@treksphere.com")
    @NotBlank(message = "Email is required")
    @Email(message = "Email format is invalid")
    private String email;

    @Schema(description = "Họ và tên", example = "Trekker Name")
    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 255, message = "Full name must be between 2 and 255 characters")
    private String fullName;

    @Schema(description = "Mật khẩu. Quy tắc: ít nhất 1 chữ cái, 1 chữ số, và độ dài từ 8 ký tự trở lên.", example = "Password123")
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    @Pattern(regexp = ValidationConstant.PASSWORD_REGEX,
            message = ValidationConstant.PASSWORD_MESSAGE)
    private String password;
}
