package com.sep.treksphere.dto.request;

import com.sep.treksphere.constant.MessageConstant;
import com.sep.treksphere.constant.ValidationConstant;

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
public class ResetPasswordRequest {
    @NotBlank(message = MessageConstant.TOKEN_REQUIRED)
    private String token;

    @NotBlank(message = MessageConstant.PASSWORD_REQUIRED)
    @Size(min = 8, message = MessageConstant.PASSWORD_MIN_LENGTH)
    @Pattern(regexp = ValidationConstant.PASSWORD_REGEX,
            message = ValidationConstant.PASSWORD_MESSAGE)
    private String newPassword;
}
