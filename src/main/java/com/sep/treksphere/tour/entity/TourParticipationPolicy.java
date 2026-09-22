package com.sep.treksphere.tour.policy;

import com.sep.treksphere.common.entity.BaseEntity;
import com.sep.treksphere.tour.Tour;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "tour_participation_policy")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TourParticipationPolicy extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "policy_id")
    private UUID policyId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_id", nullable = false, unique = true)
    private Tour tour;

    @Column(name = "min_age")
    private Integer minAge;

    @Column(name = "max_age")
    private Integer maxAge;

    @Column(name = "min_height_cm", precision = 5, scale = 2)
    private BigDecimal minHeightCm;

    @Column(name = "max_height_cm", precision = 5, scale = 2)
    private BigDecimal maxHeightCm;

    @Column(name = "min_weight_kg", precision = 5, scale = 2)
    private BigDecimal minWeightKg;

    @Column(name = "max_weight_kg", precision = 5, scale = 2)
    private BigDecimal maxWeightKg;

    @Column(name = "fitness_level", nullable = false, length = 20)
    @Builder.Default
    private String fitnessLevel = "ANY";

    @Column(name = "health_requirements", columnDefinition = "TEXT")
    private String healthRequirements;

    @Column(name = "restricted_medical_conditions", columnDefinition = "TEXT")
    private String restrictedMedicalConditions;

    @Column(name = "required_experience", columnDefinition = "TEXT")
    private String requiredExperience;

    @Column(name = "required_skills", columnDefinition = "TEXT")
    private String requiredSkills;

    @Column(name = "required_equipment", columnDefinition = "TEXT")
    private String requiredEquipment;

    @Column(name = "required_documents", columnDefinition = "TEXT")
    private String requiredDocuments;

    @Column(name = "requires_health_declaration", nullable = false)
    @Builder.Default
    private Boolean requiresHealthDeclaration = false;

    @Column(name = "requires_medical_certificate", nullable = false)
    @Builder.Default
    private Boolean requiresMedicalCertificate = false;

    @Column(name = "guardian_required_under_age")
    private Integer guardianRequiredUnderAge;

    @Column(name = "additional_requirements", columnDefinition = "TEXT")
    private String additionalRequirements;
}
