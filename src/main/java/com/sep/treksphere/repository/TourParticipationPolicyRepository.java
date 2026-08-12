package com.sep.treksphere.repository;

import com.sep.treksphere.entity.TourParticipationPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TourParticipationPolicyRepository extends JpaRepository<TourParticipationPolicy, UUID> {
    Optional<TourParticipationPolicy> findByTourIdAndIsActiveTrueAndIsDeletedFalse(UUID tourId);

    boolean existsByTourIdAndIsActiveTrueAndIsDeletedFalse(UUID tourId);
}
