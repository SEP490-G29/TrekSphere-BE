package com.sep.treksphere.dto.request;

import com.sep.treksphere.constant.MessageConstant;

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
    
    @NotBlank(message = MessageConstant.EMAIL_REQUIRED)
    @Email(message = MessageConstant.EMAIL_INVALID)
    private String email;
    
    @NotBlank(message = MessageConstant.PASSWORD_REQUIRED)
    private String password;
}
