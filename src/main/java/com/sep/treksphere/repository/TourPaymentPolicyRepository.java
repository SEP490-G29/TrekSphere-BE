package com.sep.treksphere.repository;

import com.sep.treksphere.entity.TourPaymentPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TourPaymentPolicyRepository extends JpaRepository<TourPaymentPolicy, UUID> {
    Optional<TourPaymentPolicy> findByTourIdAndIsActiveTrueAndIsDeletedFalse(UUID tourId);

    boolean existsByTourIdAndIsActiveTrueAndIsDeletedFalse(UUID tourId);
}
