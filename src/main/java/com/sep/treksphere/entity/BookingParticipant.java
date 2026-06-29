package com.sep.treksphere.entity;

import com.sep.treksphere.enums.user.Gender;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "booking_participant")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID participantID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(nullable = false, length = 255)
    private String fullName;

    @Column(nullable = false)
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Gender gender;

    @Column(nullable = false, length = 20)
    private String idNumber;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String emergencyContact;

    @Column(columnDefinition = "TEXT")
    private String specialRequirements;

    @Column(nullable = false)
    @Builder.Default
    private Boolean infoConfirmed = false;
}
