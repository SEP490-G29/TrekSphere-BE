package com.sep.treksphere.dto.response;

import com.sep.treksphere.enums.tour.FitnessLevel;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class TourParticipationPolicyResponse {
    private UUID tourId;
    private Integer policyVersion;
    private Integer minAge;
    private Integer maxAge;
    private BigDecimal minHeightCm;
    private BigDecimal maxHeightCm;
    private BigDecimal minWeightKg;
    private BigDecimal maxWeightKg;
    private FitnessLevel fitnessLevel;
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
