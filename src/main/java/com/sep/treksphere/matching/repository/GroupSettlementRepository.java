package com.sep.treksphere.matching.repository;

import com.sep.treksphere.matching.entity.GroupSettlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupSettlementRepository extends JpaRepository<GroupSettlement, UUID> {

    List<GroupSettlement> findByGroupTrip_MatchingGroup_MatchingGroupIdAndIsDeletedFalseOrderByCreatedAtAsc(UUID matchingGroupId);

    Optional<GroupSettlement> findByGroupSettlementIdAndGroupTrip_MatchingGroup_MatchingGroupIdAndIsDeletedFalse(UUID settlementId, UUID matchingGroupId);

    List<GroupSettlement> findByGroupTrip_GroupTripIdAndIsDeletedFalseOrderByCreatedAtAsc(UUID groupTripId);
}

