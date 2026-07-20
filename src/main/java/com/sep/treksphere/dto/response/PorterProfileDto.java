package com.sep.treksphere.dto.response;

import com.sep.treksphere.enums.user.Gender;
import com.sep.treksphere.enums.vendor.PorterStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class PorterProfileDto {
    private UUID porterId;
    private String fullName;
    private String phone;
    private Gender gender;
    private LocalDate dateOfBirth;
    private String address;
    private String avatarUrl;
    private LocalDate joinedDate;
    private PorterStatus status;
}
