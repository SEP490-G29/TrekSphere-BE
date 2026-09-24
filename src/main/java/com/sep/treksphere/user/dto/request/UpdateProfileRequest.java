package com.sep.treksphere.user.dto.request;

import com.sep.treksphere.common.constant.MessageConstant;
import com.sep.treksphere.common.constant.ValidationConstant;
import com.sep.treksphere.tour.enums.DifficultyLevel;
import com.sep.treksphere.user.enums.ExperienceLevel;
import com.sep.treksphere.user.enums.Gender;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@Data
public class UpdateProfileRequest {
    private String fullName;

    @Pattern(regexp = ValidationConstant.PHONE_REGEX, message = MessageConstant.INVALID_PHONE)
    private String phone;

    @Past(message = MessageConstant.INVALID_DOB)
    private LocalDate dateOfBirth;

    private Gender gender;

    private MultipartFile avatar;

    @Size(max = 2000)
    private String bio;

    private ExperienceLevel experienceLevel;

    private DifficultyLevel preferredDifficulty;

    @Size(max = 255)
    private String emergencyContactName;

    @Pattern(regexp = ValidationConstant.PHONE_REGEX, message = MessageConstant.INVALID_PHONE)
    private String emergencyContactPhone;

    @Size(max = 20)
    private List<String> preferredAreas;

    @Size(max = 30)
    private List<String> skills;
}
