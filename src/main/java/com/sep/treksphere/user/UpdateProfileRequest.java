package com.sep.treksphere.user;

import com.sep.treksphere.common.constant.MessageConstant;
import com.sep.treksphere.common.constant.ValidationConstant;
import com.sep.treksphere.user.Gender;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Data
public class UpdateProfileRequest {
    private String fullName;

    @Pattern(regexp = ValidationConstant.PHONE_REGEX, message = MessageConstant.INVALID_PHONE)
    private String phone;

    @Past(message = MessageConstant.INVALID_DOB)
    private LocalDate dateOfBirth;

    private Gender gender;

    private MultipartFile avatar;
}
