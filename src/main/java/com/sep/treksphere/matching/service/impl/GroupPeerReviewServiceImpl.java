package com.sep.treksphere.matching.service.impl;

import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import com.sep.treksphere.matching.dto.request.PeerReviewCreateRequest;
import com.sep.treksphere.matching.dto.response.PeerReviewCandidateResponse;
import com.sep.treksphere.matching.dto.response.PeerReviewResponse;
import com.sep.treksphere.matching.entity.GroupPeerReview;
import com.sep.treksphere.matching.entity.GroupTrip;
import com.sep.treksphere.matching.entity.MatchingGroup;
import com.sep.treksphere.matching.entity.MatchingMember;
import com.sep.treksphere.matching.enums.GroupTripStatus;
import com.sep.treksphere.matching.enums.JoinStatus;
import com.sep.treksphere.matching.enums.MatchingRole;
import com.sep.treksphere.matching.enums.PeerReviewModerationStatus;
import com.sep.treksphere.matching.mapper.PeerReviewMapper;
import com.sep.treksphere.matching.repository.GroupPeerReviewRepository;
import com.sep.treksphere.matching.repository.GroupTripRepository;
import com.sep.treksphere.matching.repository.MatchingGroupRepository;
import com.sep.treksphere.matching.repository.MatchingMemberRepository;
import com.sep.treksphere.matching.service.GroupPeerReviewService;
import com.sep.treksphere.matching.service.TrustScoreService;
import com.sep.treksphere.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupPeerReviewServiceImpl implements GroupPeerReviewService {

    private final MatchingGroupRepository matchingGroupRepository;
    private final MatchingMemberRepository matchingMemberRepository;
    private final GroupTripRepository groupTripRepository;
    private final GroupPeerReviewRepository groupPeerReviewRepository;
    private final UserRepository userRepository;
    private final TrustScoreService trustScoreService;
    private final PeerReviewMapper peerReviewMapper;

    @Override
    @Transactional
    public PeerReviewResponse submitPeerReview(UUID groupId, UUID reviewerUserId, PeerReviewCreateRequest request) {
        MatchingGroup group = matchingGroupRepository.findById(groupId)
                .filter(g -> !Boolean.TRUE.equals(g.getIsDeleted()))
                .orElseThrow(() -> new AppException(ErrorCode.MATCHING_GROUP_NOT_FOUND));

        GroupTrip trip = groupTripRepository.findByMatchingGroup(group)
                .orElseThrow(() -> new AppException(ErrorCode.MATCHING_GROUP_NOT_FOUND));

        // 1. Chỉ được review khi chuyến đi đã kết thúc (ENDED)
        if (trip.getStatus() != GroupTripStatus.ENDED) {
            throw new AppException(ErrorCode.TRIP_NOT_ENDED_FOR_REVIEW);
        }

        // 2. Xác thực reviewer là thành viên chính thức (ACCEPTED) của nhóm
        MatchingMember reviewerMember = matchingMemberRepository.findByGroupIdAndUserId(groupId, reviewerUserId)
                .filter(m -> m.getStatus() == JoinStatus.ACCEPTED)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_TRIP_PARTICIPANT));

        // 3. Xác thực reviewee là thành viên chính thức của nhóm
        MatchingMember revieweeMember;
        if (request.getRevieweeMemberId() != null) {
            revieweeMember = matchingMemberRepository.findMemberByIdAndGroupId(request.getRevieweeMemberId(), groupId)
                    .filter(m -> m.getStatus() == JoinStatus.ACCEPTED)
                    .orElseThrow(() -> new AppException(ErrorCode.NOT_TRIP_PARTICIPANT));
        } else if (request.getRevieweeUserId() != null) {
            revieweeMember = matchingMemberRepository.findByGroupIdAndUserId(groupId, request.getRevieweeUserId())
                    .filter(m -> m.getStatus() == JoinStatus.ACCEPTED)
                    .orElseThrow(() -> new AppException(ErrorCode.NOT_TRIP_PARTICIPANT));
        } else {
            throw new AppException(ErrorCode.NOT_TRIP_PARTICIPANT);
        }

        // 4. Chặn tự đánh giá (Self-review)
        if (reviewerMember.getUser().getUserId().equals(revieweeMember.getUser().getUserId())) {
            throw new AppException(ErrorCode.CANNOT_REVIEW_SELF);
        }

        // 5. Kiểm tra giá trị điểm rating 1..5
        validateRating(request.getActualEnduranceRating());
        validateRating(request.getPunctualityResponsibilityRating());
        validateRating(request.getFinancialFairnessRating());

        // 6. Kiểm tra xem cặp reviewer -> reviewee trong trip này đã đánh giá chưa (Unique)
        boolean alreadyReviewed = groupPeerReviewRepository
                .existsByGroupTrip_GroupTripIdAndReviewerMatchingMember_MatchingMemberIdAndRevieweeMatchingMember_MatchingMemberIdAndIsDeletedFalse(
                        trip.getGroupTripId(),
                        reviewerMember.getMatchingMemberId(),
                        revieweeMember.getMatchingMemberId()
                );
        if (alreadyReviewed) {
            throw new AppException(ErrorCode.ALREADY_REVIEWED_MEMBER);
        }

        // 7. Tạo và lưu entity GroupPeerReview
        GroupPeerReview peerReview = new GroupPeerReview();
        peerReview.setGroupTrip(trip);
        peerReview.setReviewerMatchingMember(reviewerMember);
        peerReview.setRevieweeMatchingMember(revieweeMember);
        peerReview.setActualEnduranceRating(request.getActualEnduranceRating());
        peerReview.setPunctualityResponsibilityRating(request.getPunctualityResponsibilityRating());
        peerReview.setFinancialFairnessRating(request.getFinancialFairnessRating());
        peerReview.setComment(request.getComment() != null ? request.getComment().trim() : null);
        peerReview.setModerationStatus(PeerReviewModerationStatus.VISIBLE);

        GroupPeerReview saved = groupPeerReviewRepository.save(peerReview);

        // 8. Tái tính Trust Score cho người được đánh giá
        try {
            trustScoreService.recalculateTrustScore(revieweeMember.getUser().getUserId());
        } catch (Exception ex) {
            log.error("Failed to recalculate trust score for user {}: {}",
                    revieweeMember.getUser().getUserId(), ex.getMessage(), ex);
        }

        return peerReviewMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PeerReviewCandidateResponse> getPeerReviewCandidates(UUID groupId, UUID currentUserId) {
        MatchingGroup group = matchingGroupRepository.findById(groupId)
                .filter(g -> !Boolean.TRUE.equals(g.getIsDeleted()))
                .orElseThrow(() -> new AppException(ErrorCode.MATCHING_GROUP_NOT_FOUND));

        GroupTrip trip = groupTripRepository.findByMatchingGroup(group)
                .orElseThrow(() -> new AppException(ErrorCode.MATCHING_GROUP_NOT_FOUND));

        // Xác thực người gọi là thành viên chính thức
        matchingMemberRepository.findByGroupIdAndUserId(groupId, currentUserId)
                .filter(m -> m.getStatus() == JoinStatus.ACCEPTED)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_TRIP_PARTICIPANT));

        // Lấy tất cả thành viên ACCEPTED trong nhóm
        List<MatchingMember> activeMembers = matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED);

        // Lấy các review do currentMember đã chấm trong chuyến này
        List<GroupPeerReview> myReviews = groupPeerReviewRepository
                .findMySubmittedReviews(trip.getGroupTripId(), currentUserId);

        return activeMembers.stream()
                .filter(m -> !m.getUser().getUserId().equals(currentUserId)) // Loại trừ chính mình
                .map(m -> {
                    Optional<GroupPeerReview> existing = myReviews.stream()
                            .filter(r -> r.getRevieweeMatchingMember() != null && (
                                    r.getRevieweeMatchingMember().getMatchingMemberId().equals(m.getMatchingMemberId())
                                            || (r.getRevieweeMatchingMember().getUser() != null
                                            && r.getRevieweeMatchingMember().getUser().getUserId().equals(m.getUser().getUserId()))
                            ))
                            .findFirst();

                    return PeerReviewCandidateResponse.builder()
                            .matchingMemberId(m.getMatchingMemberId())
                            .userId(m.getUser().getUserId())
                            .fullName(m.getUser().getFullName())
                            .avatarUrl(m.getUser().getAvatarUrl())
                            .role(m.getRole())
                            .roleLabel(m.getRole() == MatchingRole.LEADER ? "Trưởng nhóm" : "Thành viên")
                            .isReviewed(existing.isPresent())
                            .existingReviewId(existing.map(GroupPeerReview::getGroupPeerReviewId).orElse(null))
                            .build();
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PeerReviewResponse> getGroupPeerReviews(UUID groupId, UUID currentUserId) {
        MatchingGroup group = matchingGroupRepository.findById(groupId)
                .filter(g -> !Boolean.TRUE.equals(g.getIsDeleted()))
                .orElseThrow(() -> new AppException(ErrorCode.MATCHING_GROUP_NOT_FOUND));

        GroupTrip trip = groupTripRepository.findByMatchingGroup(group)
                .orElseThrow(() -> new AppException(ErrorCode.MATCHING_GROUP_NOT_FOUND));

        // Xác thực người gọi là thành viên chính thức của nhóm
        matchingMemberRepository.findByGroupIdAndUserId(groupId, currentUserId)
                .filter(m -> m.getStatus() == JoinStatus.ACCEPTED)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_TRIP_PARTICIPANT));

        // Trả về các đánh giá mà bạn đồng hành đã gửi CHO CHÍNH BẢN THÂN NGƯỜI DÙNG (currentUserId)
        List<GroupPeerReview> receivedReviews = groupPeerReviewRepository
                .findMyReceivedReviews(trip.getGroupTripId(), currentUserId);

        return receivedReviews.stream()
                .map(peerReviewMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PeerReviewResponse> getUserPeerReviews(UUID userId) {
        userRepository.findById(userId)
                .filter(u -> !Boolean.TRUE.equals(u.getIsDeleted()))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        List<GroupPeerReview> reviews = groupPeerReviewRepository
                .findByRevieweeMatchingMember_User_UserIdAndModerationStatusAndIsDeletedFalse(
                        userId, PeerReviewModerationStatus.VISIBLE);

        return reviews.stream()
                .map(peerReviewMapper::toResponse)
                .toList();
    }

    private void validateRating(Short rating) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new AppException(ErrorCode.INVALID_RATING_VALUE);
        }
    }
}
