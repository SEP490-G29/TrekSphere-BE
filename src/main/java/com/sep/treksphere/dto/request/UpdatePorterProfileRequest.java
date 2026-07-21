package com.sep.treksphere.dto.request;

import com.sep.treksphere.constant.MessageConstant;
import com.sep.treksphere.constant.ValidationConstant;
import com.sep.treksphere.enums.user.Gender;
import com.sep.treksphere.enums.vendor.PorterStatus;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Data
public class UpdatePorterProfileRequest {

    private String fullName;

    @Pattern(regexp = ValidationConstant.PHONE_REGEX, message = MessageConstant.PORTER_PHONE_INVALID)
    private String phone;

    private Gender gender;

    @Past(message = MessageConstant.INVALID_DOB)
    private LocalDate dateOfBirth;

    private String address;

    private String avatarUrl;

    private MultipartFile avatarFile;
    
    private PorterStatus status;
}
