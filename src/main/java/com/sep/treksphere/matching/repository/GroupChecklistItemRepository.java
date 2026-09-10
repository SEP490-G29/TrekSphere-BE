package com.sep.treksphere.matching.repository;

import com.sep.treksphere.matching.entity.GroupChecklistItem;
import com.sep.treksphere.matching.enums.ChecklistItemScope;
import com.sep.treksphere.matching.enums.ChecklistItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupChecklistItemRepository extends JpaRepository<GroupChecklistItem, UUID> {

    Optional<GroupChecklistItem> findByGroupChecklistItemIdAndMatchingGroup_MatchingGroupIdAndIsDeletedFalse(
            UUID groupChecklistItemId, UUID matchingGroupId);

    List<GroupChecklistItem> findByMatchingGroup_MatchingGroupIdAndIsDeletedFalseOrderByCreatedAtAsc(
            UUID matchingGroupId);

    @Query("SELECT g FROM GroupChecklistItem g " +
           "WHERE g.matchingGroup.matchingGroupId = :groupId " +
           "AND g.isDeleted = false " +
           "AND (:scope IS NULL OR g.itemScope = :scope) " +
           "AND (:status IS NULL OR g.status = :status) " +
           "AND (:assigneeMemberId IS NULL OR (g.assigneeMatchingMember IS NOT NULL AND g.assigneeMatchingMember.matchingMemberId = :assigneeMemberId)) " +
           "AND (:isRequired IS NULL OR g.isRequired = :isRequired) " +
           "AND (:keyword IS NULL OR LOWER(g.title) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY g.createdAt ASC")
    List<GroupChecklistItem> findWithFilters(
            @Param("groupId") UUID groupId,
            @Param("scope") ChecklistItemScope scope,
            @Param("status") ChecklistItemStatus status,
            @Param("assigneeMemberId") UUID assigneeMemberId,
            @Param("isRequired") Boolean isRequired,
            @Param("keyword") String keyword);
}
