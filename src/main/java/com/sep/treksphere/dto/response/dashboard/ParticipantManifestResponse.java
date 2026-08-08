package com.sep.treksphere.dto.response.dashboard;

import com.sep.treksphere.enums.booking.BookingStatus;
import com.sep.treksphere.enums.booking.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParticipantManifestResponse {

    private UUID bookingId;
    private String bookingCode;
    private String bookerName;
    private String bookerPhone;
    private String bookerEmail;

    private UUID participantId;
    private String fullName;
    private String gender;
    private LocalDate dateOfBirth;
    private String phoneNumber;
    private String specialNote;

    private PaymentStatus paymentStatus;
    private BookingStatus bookingStatus;
}
