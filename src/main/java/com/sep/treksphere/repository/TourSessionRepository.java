package com.sep.treksphere.repository;

import com.sep.treksphere.entity.TourSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TourSessionRepository extends JpaRepository<TourSession, UUID> {
    Optional<TourSession> findByTourSessionIdAndIsDeletedFalse(UUID tourSessionId);
}
