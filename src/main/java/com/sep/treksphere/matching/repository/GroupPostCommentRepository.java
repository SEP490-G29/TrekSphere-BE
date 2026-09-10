package com.sep.treksphere.matching.repository;

import com.sep.treksphere.matching.entity.GroupPostComment;
import com.sep.treksphere.matching.enums.GroupContentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupPostCommentRepository extends JpaRepository<GroupPostComment, UUID> {

    List<GroupPostComment> findByGroupPost_GroupPostIdAndIsDeletedFalseOrderByCreatedAtAsc(
            UUID groupPostId);

    List<GroupPostComment> findByGroupPost_GroupPostIdAndStatusAndIsDeletedFalseOrderByCreatedAtAsc(
            UUID groupPostId, GroupContentStatus status);

    long countByGroupPost_GroupPostIdAndIsDeletedFalse(UUID groupPostId);

    long countByGroupPost_GroupPostIdAndStatusAndIsDeletedFalse(UUID groupPostId, GroupContentStatus status);

    Optional<GroupPostComment> findByGroupPostCommentIdAndGroupPost_GroupPostIdAndIsDeletedFalse(
            UUID groupPostCommentId, UUID groupPostId);
}
