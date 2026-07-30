package com.sep.treksphere.repository;

import com.sep.treksphere.entity.SessionEquipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SessionEquipmentRepository extends JpaRepository<SessionEquipment, UUID> {
    List<SessionEquipment> findByTourSession_TourSessionIdAndIsDeletedFalse(UUID sessionId);

    Optional<SessionEquipment> findBySessionEquipmentIdAndIsDeletedFalse(UUID sessionEquipmentId);
}
