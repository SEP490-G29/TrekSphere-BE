package com.sep.treksphere.tour.policy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TourParticipationPolicyResponse {

    private String tourId;
    private Integer policyVersion;
    private Integer minAge;
    private Integer maxAge;
    private BigDecimal minHeightCm;
    private BigDecimal maxHeightCm;
    private BigDecimal minWeightKg;
    private BigDecimal maxWeightKg;
    private String fitnessLevel;
    private String healthRequirements;
    private String restrictedMedicalConditions;
    private String requiredExperience;
    private String requiredSkills;
    private String requiredEquipment;
    private String requiredDocuments;
    private Boolean requiresHealthDeclaration;
    private Boolean requiresMedicalCertificate;
    private Integer guardianRequiredUnderAge;
    private String additionalRequirements;
}
