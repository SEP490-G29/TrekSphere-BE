package com.sep.treksphere.matching.repository;

import com.sep.treksphere.matching.entity.GroupPost;
import com.sep.treksphere.matching.enums.GroupContentStatus;
import com.sep.treksphere.matching.enums.GroupPostType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupPostRepository extends JpaRepository<GroupPost, UUID> {

    Page<GroupPost> findByMatchingGroup_MatchingGroupIdAndIsDeletedFalseOrderByIsPinnedDescCreatedAtDesc(
            UUID matchingGroupId, Pageable pageable);

    Page<GroupPost> findByMatchingGroup_MatchingGroupIdAndStatusAndIsDeletedFalseOrderByIsPinnedDescCreatedAtDesc(
            UUID matchingGroupId, GroupContentStatus status, Pageable pageable);

    Page<GroupPost> findByMatchingGroup_MatchingGroupIdAndPostTypeAndIsDeletedFalseOrderByIsPinnedDescCreatedAtDesc(
            UUID matchingGroupId, GroupPostType postType, Pageable pageable);

    Page<GroupPost> findByMatchingGroup_MatchingGroupIdAndStatusAndPostTypeAndIsDeletedFalseOrderByIsPinnedDescCreatedAtDesc(
            UUID matchingGroupId, GroupContentStatus status, GroupPostType postType, Pageable pageable);

    Optional<GroupPost> findByGroupPostIdAndMatchingGroup_MatchingGroupIdAndIsDeletedFalse(
            UUID groupPostId, UUID matchingGroupId);

    List<GroupPost> findByMatchingGroup_MatchingGroupIdAndPostedBy_MatchingMemberIdAndIsDeletedFalse(
            UUID matchingGroupId, UUID matchingMemberId);
}
