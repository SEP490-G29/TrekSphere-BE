package com.sep.treksphere.repository;

import com.sep.treksphere.entity.SosAlert;
import com.sep.treksphere.enums.tour.SosAlertStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

@Repository
public interface SosAlertRepository extends JpaRepository<SosAlert, UUID> {

    @Query("""
    SELECT s
    FROM SosAlert s
    WHERE s.status = :status
      AND s.isDeleted = false
      AND (
          :vendorId IS NULL
          OR s.tourSession.tourSchedule.tour.vendor.vendorId = :vendorId
      )
    ORDER BY s.createdAt DESC
    """)
    Page<SosAlert> findAlertsByStatus(
            @Param("status") SosAlertStatus status,
            @Param("vendorId") UUID vendorId,
            Pageable pageable
    );
}
