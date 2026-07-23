package com.sep.treksphere.repository;

import com.sep.treksphere.entity.TourSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TourSessionRepository extends JpaRepository<TourSession, UUID> {
    Optional<TourSession> findByTourSessionIdAndIsDeletedFalse(UUID tourSessionId);

    @Query("SELECT ts FROM TourSession ts " +
           "JOIN FETCH ts.tourSchedule sch " +
           "JOIN FETCH sch.tour t " +
           "JOIN FETCH t.vendor v " +
           "WHERE ts.tourSessionId = :tourSessionId AND ts.isDeleted = false")
    Optional<TourSession> findByIdWithVendor(@Param("tourSessionId") UUID tourSessionId);
}
