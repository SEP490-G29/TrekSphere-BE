package com.sep.treksphere.matching.repository;

import com.sep.treksphere.matching.entity.GroupTrip;
import com.sep.treksphere.matching.entity.MatchingGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupTripRepository extends JpaRepository<GroupTrip, UUID> {
    Optional<GroupTrip> findByMatchingGroup(MatchingGroup matchingGroup);
}
