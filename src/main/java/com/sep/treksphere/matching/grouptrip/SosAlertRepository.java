package com.sep.treksphere.matching.grouptrip;

import com.sep.treksphere.matching.grouptrip.SosAlert;
import com.sep.treksphere.matching.grouptrip.SosAlertStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;

import java.util.UUID;
import java.util.Optional;
import jakarta.persistence.LockModeType;

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
