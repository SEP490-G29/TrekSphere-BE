package com.sep.treksphere.dto.request;

import com.sep.treksphere.constant.MessageConstant;
import com.sep.treksphere.constant.ValidationConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Yêu cầu thay đổi mật khẩu")
public class ChangePasswordRequest {

    @Schema(description = "Mật khẩu hiện tại", example = "OldPassword123")
    @NotBlank(message = MessageConstant.CURRENT_PASSWORD_REQUIRED)
    private String currentPassword;

    @Schema(description = "Mật khẩu mới. Quy tắc: ít nhất 1 chữ cái, 1 chữ số, và độ dài từ 8 ký tự trở lên.", example = "NewPassword123")
    @NotBlank(message = MessageConstant.NEW_PASSWORD_REQUIRED)
    @Size(min = 8, message = MessageConstant.PASSWORD_MIN_LENGTH)
    @Pattern(regexp = ValidationConstant.PASSWORD_REGEX,
            message = ValidationConstant.PASSWORD_MESSAGE)
    private String newPassword;
}
