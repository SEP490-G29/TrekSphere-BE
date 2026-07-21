package com.sep.treksphere.dto.request;

import com.sep.treksphere.constant.MessageConstant;
import com.sep.treksphere.constant.ValidationConstant;
import com.sep.treksphere.enums.user.Gender;
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
