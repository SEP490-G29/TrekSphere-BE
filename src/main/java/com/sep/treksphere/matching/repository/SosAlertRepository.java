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

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SosAlertRepository extends JpaRepository<SosAlert, UUID> {

    Optional<SosAlert> findByGroupTrip_GroupTripIdAndSender_UserIdAndIdempotencyKeyAndIsDeletedFalse(
            UUID groupTripId, UUID senderId, String idempotencyKey);

    boolean existsByGroupTrip_GroupTripIdAndSender_UserIdAndStatusAndIsDeletedFalse(
            UUID groupTripId, UUID senderId, SosAlertStatus status);

    boolean existsByGroupTrip_GroupTripIdAndSender_UserIdAndStatusInAndIsDeletedFalse(
            UUID groupTripId, UUID senderId, Collection<SosAlertStatus> statuses);

    List<SosAlert> findByGroupTrip_GroupTripIdAndStatusAndIsDeletedFalseOrderByCreatedAtDesc(
            UUID groupTripId, SosAlertStatus status);

    List<SosAlert> findByGroupTrip_GroupTripIdAndStatusInAndIsDeletedFalseOrderByCreatedAtDesc(
            UUID groupTripId, Collection<SosAlertStatus> statuses);

    Page<SosAlert> findByGroupTrip_GroupTripIdAndIsDeletedFalseOrderByCreatedAtDesc(
            UUID groupTripId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SosAlert s WHERE s.sosAlertId = :sosId AND s.isDeleted = false")
    Optional<SosAlert> findByIdForUpdate(@Param("sosId") UUID sosId);
}
