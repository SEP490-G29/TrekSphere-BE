package com.sep.treksphere.entity;

import com.sep.treksphere.enums.tour.FitnessLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "tour_participation_policy")
@Getter
@Setter
@NoArgsConstructor
public class TourParticipationPolicy extends BaseEntity {

    @Id
    private UUID tourId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "tour_id")
    private Tour tour;

    @Column(nullable = false)
    private Integer policyVersion = 1;

    private Short minAge;
    private Short maxAge;

    @Column(precision = 5, scale = 2)
    private BigDecimal minHeightCm;

    @Column(precision = 5, scale = 2)
    private BigDecimal maxHeightCm;

    @Column(precision = 5, scale = 2)
    private BigDecimal minWeightKg;

    @Column(precision = 5, scale = 2)
    private BigDecimal maxWeightKg;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FitnessLevel fitnessLevel = FitnessLevel.ANY;

    @Column(columnDefinition = "TEXT")
    private String healthRequirements;

    @Column(columnDefinition = "TEXT")
    private String restrictedMedicalConditions;

    @Column(columnDefinition = "TEXT")
    private String requiredExperience;

    @Column(columnDefinition = "TEXT")
    private String requiredSkills;

    @Column(columnDefinition = "TEXT")
    private String requiredEquipment;

    @Column(columnDefinition = "TEXT")
    private String requiredDocuments;

    @Column(nullable = false)
    private Boolean requiresHealthDeclaration = true;

    @Column(nullable = false)
    private Boolean requiresMedicalCertificate = false;

    private Short guardianRequiredUnderAge;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> additionalRules = new HashMap<>();

    @Column(nullable = false)
    private Boolean isActive = true;
}
