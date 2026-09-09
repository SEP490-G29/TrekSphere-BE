package com.sep.treksphere.matching.repository;

import com.sep.treksphere.matching.entity.CustomJourney;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomJourneyRepository extends JpaRepository<CustomJourney, UUID> {

    Optional<CustomJourney> findByMatchingGroup_MatchingGroupIdAndIsDeletedFalse(UUID groupId);

    @Query("""
        SELECT cj FROM CustomJourney cj
        JOIN FETCH cj.matchingGroup mg
        WHERE mg.matchingGroupId = :groupId AND cj.isDeleted = false
    """)
    Optional<CustomJourney> findDetailByGroupId(@Param("groupId") UUID groupId);
}
