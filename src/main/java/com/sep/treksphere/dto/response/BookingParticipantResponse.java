package com.sep.treksphere.dto.response;

import com.sep.treksphere.enums.user.Gender;
import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookingParticipantResponse {
    private String participantId;
    private String fullName;
    private LocalDate dateOfBirth;
    private Gender gender;
    private String idNumber;
    private String phone;
    private String email;
    private String address;
    private String specialRequirements;
}
