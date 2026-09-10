package com.sep.treksphere.matching.repository;

import com.sep.treksphere.matching.entity.GroupPost;
import com.sep.treksphere.matching.enums.GroupContentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupPostRepository extends JpaRepository<GroupPost, UUID> {

    Page<GroupPost> findByMatchingGroup_MatchingGroupIdAndIsDeletedFalse(
            UUID matchingGroupId, Pageable pageable);

    Page<GroupPost> findByMatchingGroup_MatchingGroupIdAndStatusAndIsDeletedFalse(
            UUID matchingGroupId, GroupContentStatus status, Pageable pageable);

    Optional<GroupPost> findByGroupPostIdAndMatchingGroup_MatchingGroupIdAndIsDeletedFalse(
            UUID groupPostId, UUID matchingGroupId);
}
