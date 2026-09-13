package com.sep.treksphere.matching.repository;

import com.sep.treksphere.matching.entity.SosAlert;
import com.sep.treksphere.matching.enums.SosAlertStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SosAlertRepository extends JpaRepository<SosAlert, UUID> {

    Optional<SosAlert> findFirstByGroupTrip_GroupTripIdAndStatusAndIsDeletedFalseOrderByCreatedAtDesc(
            UUID groupTripId,
            SosAlertStatus status
    );

    Optional<SosAlert> findFirstByGroupTrip_GroupTripIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID groupTripId);

    @Query("""
    SELECT s
    FROM SosAlert s
    WHERE s.status = :status
      AND s.isDeleted = false
      AND (CAST(:vendorId AS uuid) IS NULL
          OR s.groupTrip.matchingGroup.tour.vendor.vendorId = :vendorId
      )
    ORDER BY s.createdAt DESC
    """)
    Page<SosAlert> findAlertsByStatus(
            @Param("status") SosAlertStatus status,
            @Param("vendorId") UUID vendorId,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SosAlert s WHERE s.sosAlertId = :sosId AND s.isDeleted = false")
    Optional<SosAlert> findByIdForUpdate(@Param("sosId") UUID sosId);
}
