package com.sep.treksphere.tour;

import com.sep.treksphere.tour.dto.request.CreateTourRequest;
import com.sep.treksphere.tour.dto.request.UpdateTourRequest;
import com.sep.treksphere.tour.policy.TourParticipationPolicy;
import com.sep.treksphere.tour.policy.TourParticipationPolicyRequest;
import com.sep.treksphere.tour.policy.TourParticipationPolicyResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TourMapper {

    public Tour toTour(CreateTourRequest request) {
        if (request == null) {
            return null;
        }

        Tour tour = new Tour();
        tour.setTourName(request.getTourName());
        tour.setDescription(request.getDescription());
        tour.setDifficulty(request.getDifficulty());
        tour.setLocation(request.getLocation());
        tour.setDurationDays(request.getDurationDays());
        tour.setMinCapacity(request.getMinCapacity() != null ? request.getMinCapacity() : 1);
        tour.setMaxCapacity(request.getMaxCapacity());
        tour.setPrice(request.getPrice());
        tour.setTotalDistanceKm(request.getTotalDistanceKm());
        tour.setHighlights(request.getHighlights());
        tour.setIncludes(request.getIncludes());
        tour.setExcludes(request.getExcludes());
        if (StringUtils.hasText(request.getCoverImageUrl())) {
            tour.setCoverImageUrl(request.getCoverImageUrl().trim());
        }

        return tour;
    }

    public void updateTourFromRequest(UpdateTourRequest request, Tour tour) {
        if (request == null || tour == null) {
            return;
        }

        if (request.getTourName() != null) {
            tour.setTourName(request.getTourName());
        }
        if (request.getDescription() != null) {
            tour.setDescription(request.getDescription());
        }
        if (request.getDifficulty() != null) {
            tour.setDifficulty(request.getDifficulty());
        }
        if (request.getLocation() != null) {
            tour.setLocation(request.getLocation());
        }
        if (request.getDurationDays() != null) {
            tour.setDurationDays(request.getDurationDays());
        }
        if (request.getMinCapacity() != null) {
            tour.setMinCapacity(request.getMinCapacity());
        }
        if (request.getMaxCapacity() != null) {
            tour.setMaxCapacity(request.getMaxCapacity());
        }
        if (request.getPrice() != null) {
            tour.setPrice(request.getPrice());
        }
        if (request.getTotalDistanceKm() != null) {
            tour.setTotalDistanceKm(request.getTotalDistanceKm());
        }
        if (request.getHighlights() != null) {
            tour.setHighlights(request.getHighlights());
        }
        if (request.getIncludes() != null) {
            tour.setIncludes(request.getIncludes());
        }
        if (request.getExcludes() != null) {
            tour.setExcludes(request.getExcludes());
        }
        if (StringUtils.hasText(request.getCoverImageUrl())) {
            tour.setCoverImageUrl(request.getCoverImageUrl().trim());
        }
    }

    public TourParticipationPolicy toParticipationPolicy(TourParticipationPolicyRequest request, Tour tour) {
        if (request == null || tour == null) {
            return null;
        }

        return TourParticipationPolicy.builder()
                .tour(tour)
                .minAge(request.getMinAge())
                .maxAge(request.getMaxAge())
                .minHeightCm(request.getMinHeightCm())
                .maxHeightCm(request.getMaxHeightCm())
                .minWeightKg(request.getMinWeightKg())
                .maxWeightKg(request.getMaxWeightKg())
                .fitnessLevel(StringUtils.hasText(request.getFitnessLevel()) ? request.getFitnessLevel().trim() : "ANY")
                .healthRequirements(request.getHealthRequirements())
                .restrictedMedicalConditions(request.getRestrictedMedicalConditions())
                .requiredExperience(request.getRequiredExperience())
                .requiredSkills(request.getRequiredSkills())
                .requiredEquipment(request.getRequiredEquipment())
                .requiredDocuments(request.getRequiredDocuments())
                .requiresHealthDeclaration(Boolean.TRUE.equals(request.getRequiresHealthDeclaration()))
                .requiresMedicalCertificate(Boolean.TRUE.equals(request.getRequiresMedicalCertificate()))
                .guardianRequiredUnderAge(request.getGuardianRequiredUnderAge())
                .additionalRequirements(request.getAdditionalRequirements())
                .build();
    }

    public void updateParticipationPolicy(TourParticipationPolicyRequest request, TourParticipationPolicy policy) {
        if (request == null || policy == null) {
            return;
        }

        policy.setMinAge(request.getMinAge());
        policy.setMaxAge(request.getMaxAge());
        policy.setMinHeightCm(request.getMinHeightCm());
        policy.setMaxHeightCm(request.getMaxHeightCm());
        policy.setMinWeightKg(request.getMinWeightKg());
        policy.setMaxWeightKg(request.getMaxWeightKg());
        if (StringUtils.hasText(request.getFitnessLevel())) {
            policy.setFitnessLevel(request.getFitnessLevel().trim());
        }
        policy.setHealthRequirements(request.getHealthRequirements());
        policy.setRestrictedMedicalConditions(request.getRestrictedMedicalConditions());
        policy.setRequiredExperience(request.getRequiredExperience());
        policy.setRequiredSkills(request.getRequiredSkills());
        policy.setRequiredEquipment(request.getRequiredEquipment());
        policy.setRequiredDocuments(request.getRequiredDocuments());
        if (request.getRequiresHealthDeclaration() != null) {
            policy.setRequiresHealthDeclaration(request.getRequiresHealthDeclaration());
        }
        if (request.getRequiresMedicalCertificate() != null) {
            policy.setRequiresMedicalCertificate(request.getRequiresMedicalCertificate());
        }
        policy.setGuardianRequiredUnderAge(request.getGuardianRequiredUnderAge());
        policy.setAdditionalRequirements(request.getAdditionalRequirements());
    }

    public TourParticipationPolicyResponse toParticipationPolicyResponse(TourParticipationPolicy policy) {
        if (policy == null) {
            return null;
        }

        return TourParticipationPolicyResponse.builder()
                .tourId(policy.getTour() != null ? policy.getTour().getTourId().toString() : null)
                .policyVersion(1)
                .minAge(policy.getMinAge())
                .maxAge(policy.getMaxAge())
                .minHeightCm(policy.getMinHeightCm())
                .maxHeightCm(policy.getMaxHeightCm())
                .minWeightKg(policy.getMinWeightKg())
                .maxWeightKg(policy.getMaxWeightKg())
                .fitnessLevel(policy.getFitnessLevel())
                .healthRequirements(policy.getHealthRequirements())
                .restrictedMedicalConditions(policy.getRestrictedMedicalConditions())
                .requiredExperience(policy.getRequiredExperience())
                .requiredSkills(policy.getRequiredSkills())
                .requiredEquipment(policy.getRequiredEquipment())
                .requiredDocuments(policy.getRequiredDocuments())
                .requiresHealthDeclaration(policy.getRequiresHealthDeclaration())
                .requiresMedicalCertificate(policy.getRequiresMedicalCertificate())
                .guardianRequiredUnderAge(policy.getGuardianRequiredUnderAge())
                .additionalRequirements(policy.getAdditionalRequirements())
                .build();
    }
}
