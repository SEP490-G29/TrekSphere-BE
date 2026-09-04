package com.sep.treksphere.matching.repository;

import com.sep.treksphere.matching.entity.GroupChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface GroupChecklistItemRepository extends JpaRepository<GroupChecklistItem, UUID> {
}
