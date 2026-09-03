package com.sep.treksphere.matching.grouptrip;

import com.sep.treksphere.matching.grouptrip.GroupChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface GroupChecklistItemRepository extends JpaRepository<GroupChecklistItem, UUID> {
}
