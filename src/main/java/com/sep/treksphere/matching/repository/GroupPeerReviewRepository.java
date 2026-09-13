package com.sep.treksphere.matching.repository;

import com.sep.treksphere.matching.entity.GroupPeerReview;
import com.sep.treksphere.matching.enums.PeerReviewModerationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupPeerReviewRepository extends JpaRepository<GroupPeerReview, UUID> {

    /**
     * Mọi review mà user (qua bất kỳ matching_member nào của họ, ở bất kỳ nhóm nào)
     * là người được đánh giá — dùng làm nguồn tính lại users.trust_score.
     */
    List<GroupPeerReview> findByRevieweeMatchingMember_User_UserIdAndModerationStatusAndIsDeletedFalse(
            UUID userId, PeerReviewModerationStatus moderationStatus);

    /**
     * Kiểm tra xem cặp reviewer -> reviewee trong chuyến đi cụ thể đã tồn tại review chưa.
     */
    boolean existsByGroupTrip_GroupTripIdAndReviewerMatchingMember_MatchingMemberIdAndRevieweeMatchingMember_MatchingMemberIdAndIsDeletedFalse(
            UUID groupTripId, UUID reviewerMatchingMemberId, UUID revieweeMatchingMemberId);

    /**
     * Tìm review cụ thể của reviewer cho reviewee trong chuyến đi.
     */
    Optional<GroupPeerReview> findByGroupTrip_GroupTripIdAndReviewerMatchingMember_MatchingMemberIdAndRevieweeMatchingMember_MatchingMemberIdAndIsDeletedFalse(
            UUID groupTripId, UUID reviewerMatchingMemberId, UUID revieweeMatchingMemberId);

    /**
     * Lấy toàn bộ danh sách review trong một chuyến đi cụ thể.
     */
    List<GroupPeerReview> findByGroupTrip_GroupTripIdAndIsDeletedFalse(UUID groupTripId);

    /**
     * Lấy danh sách review do một user thực hiện trong một chuyến đi.
     */
    @Query("""
        SELECT pr FROM GroupPeerReview pr
        JOIN FETCH pr.revieweeMatchingMember remm
        JOIN FETCH remm.user u
        WHERE pr.groupTrip.groupTripId = :groupTripId
          AND pr.reviewerMatchingMember.user.userId = :userId
          AND pr.isDeleted = false
    """)
    List<GroupPeerReview> findMySubmittedReviews(
            @Param("groupTripId") UUID groupTripId,
            @Param("userId") UUID userId
    );

    /**
     * Lấy danh sách review mà user nhận được từ bạn đồng hành trong chuyến đi (bảo mật ẩn danh).
     */
    @Query("""
        SELECT pr FROM GroupPeerReview pr
        JOIN FETCH pr.revieweeMatchingMember remm
        JOIN FETCH remm.user u
        WHERE pr.groupTrip.groupTripId = :groupTripId
          AND pr.revieweeMatchingMember.user.userId = :userId
          AND pr.moderationStatus = com.sep.treksphere.matching.enums.PeerReviewModerationStatus.VISIBLE
          AND pr.isDeleted = false
        ORDER BY pr.createdAt DESC
    """)
    List<GroupPeerReview> findMyReceivedReviews(
            @Param("groupTripId") UUID groupTripId,
            @Param("userId") UUID userId
    );
}
