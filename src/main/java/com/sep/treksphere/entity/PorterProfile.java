package com.sep.treksphere.entity;

import com.sep.treksphere.enums.user.Gender;
import com.sep.treksphere.enums.vendor.PorterStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "porter_profile")
@Getter
@Setter
@NoArgsConstructor
public class PorterProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID porterId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id")
    private Vendor vendor;

    @Column( nullable = false, length = 255)
    private String fullName;

    @Column(nullable = false, length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    private LocalDate dateOfBirth;

    @Column(length = 255)
    private String address;

    @Column(length = 500)
    private String avatarUrl;

    @Column(nullable = false)
    private LocalDate joinedDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PorterStatus status = PorterStatus.ACTIVE;
}
