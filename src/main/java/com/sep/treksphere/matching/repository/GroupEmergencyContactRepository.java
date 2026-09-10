package com.sep.treksphere.matching.repository;

import com.sep.treksphere.matching.entity.GroupEmergencyContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GroupEmergencyContactRepository extends JpaRepository<GroupEmergencyContact, UUID> {
    List<GroupEmergencyContact> findByMatchingGroup_MatchingGroupIdAndIsActiveTrue(UUID matchingGroupId);
}
