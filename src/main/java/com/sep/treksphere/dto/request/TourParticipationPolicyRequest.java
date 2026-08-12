package com.sep.treksphere.dto.request;

import com.sep.treksphere.enums.tour.FitnessLevel;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class TourParticipationPolicyRequest {
    @NotNull(message = "Tuổi tối thiểu là bắt buộc.")
    @Min(0)
    @Max(120)
    private Integer minAge;

    @Min(0)
    @Max(120)
    private Integer maxAge;

    @DecimalMin(value = "50")
    @DecimalMax(value = "250")
    private BigDecimal minHeightCm;

    @DecimalMin(value = "50")
    @DecimalMax(value = "250")
    private BigDecimal maxHeightCm;

    @DecimalMin(value = "10")
    @DecimalMax(value = "300")
    private BigDecimal minWeightKg;

    @DecimalMin(value = "10")
    @DecimalMax(value = "300")
    private BigDecimal maxWeightKg;

    private FitnessLevel fitnessLevel = FitnessLevel.ANY;
    private String healthRequirements;
    private String restrictedMedicalConditions;
    private String requiredExperience;
    private String requiredSkills;
    private String requiredEquipment;
    private String requiredDocuments;
    private Boolean requiresHealthDeclaration = true;
    private Boolean requiresMedicalCertificate = false;

    @Min(1)
    @Max(18)
    private Integer guardianRequiredUnderAge;

    private String additionalRequirements;
}
