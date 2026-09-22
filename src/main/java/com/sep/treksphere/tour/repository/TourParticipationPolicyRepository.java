package com.sep.treksphere.tour.repository;

import com.sep.treksphere.tour.entity.TourParticipationPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TourParticipationPolicyRepository extends JpaRepository<TourParticipationPolicy, UUID> {

    Optional<TourParticipationPolicy> findByTour_TourIdAndIsDeletedFalse(UUID tourId);

    @Modifying
    @Query("UPDATE TourParticipationPolicy p SET p.isDeleted = true, p.deletedAt = :deletedAt, p.deletedBy = :deletedBy " +
            "WHERE p.tour.tourId = :tourId AND p.isDeleted = false")
    void softDeleteByTourId(
            @Param("tourId") UUID tourId,
            @Param("deletedAt") LocalDateTime deletedAt,
            @Param("deletedBy") String deletedBy);

    @Modifying
    @Query("UPDATE TourParticipationPolicy p SET p.isDeleted = false, p.deletedAt = null, p.deletedBy = null " +
            "WHERE p.tour.tourId = :tourId AND p.deletedAt = :deletedAt")
    void restoreByTourIdAndDeletedAt(
            @Param("tourId") UUID tourId,
            @Param("deletedAt") LocalDateTime deletedAt);
}
