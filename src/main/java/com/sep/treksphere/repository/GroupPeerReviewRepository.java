package com.sep.treksphere.repository;

import com.sep.treksphere.entity.GroupPeerReview;
import com.sep.treksphere.enums.group.PeerReviewModerationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GroupPeerReviewRepository extends JpaRepository<GroupPeerReview, UUID> {

    /**
     * Mọi review mà user (qua bất kỳ matching_member nào của họ, ở bất kỳ nhóm nào)
     * là người được đánh giá — dùng làm nguồn tính lại users.trust_score (Mục 8.9).
     */
    List<GroupPeerReview> findByRevieweeMatchingMember_User_UserIdAndModerationStatusAndIsDeletedFalse(
            UUID userId, PeerReviewModerationStatus moderationStatus);
}
